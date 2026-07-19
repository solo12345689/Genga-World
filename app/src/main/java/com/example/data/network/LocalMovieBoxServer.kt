package com.example.data.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class LocalMovieBoxServer(private val context: Context, private val port: Int = 3000) {
    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private val moshi = Moshi.Builder().build()
    private val mapAdapter = moshi.adapter(Map::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val responseCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, String>>()
    private val diskCachePrefs = context.getSharedPreferences("moviebox_api_cache", Context.MODE_PRIVATE)

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(port))
                }
                Log.i("LocalMovieBoxServer", "Server started on 127.0.0.1:$port")
                while (isActive && isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    launch {
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalMovieBoxServer", "Error in server socket loop", e)
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        scope.cancel()
    }

    private suspend fun handleClient(socket: Socket) {
        withContext(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                val line = reader.readLine() ?: return@withContext
                val parts = line.split(" ")
                if (parts.size < 2) return@withContext
                val method = parts[0]
                val pathAndQuery = parts[1]

                // Read headers
                var contentLength = 0
                var lineHeader = reader.readLine()
                while (lineHeader != null && lineHeader.isNotEmpty()) {
                    if (lineHeader.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = lineHeader.substring("Content-Length:".length).trim().toIntOrNull() ?: 0
                    }
                    lineHeader = reader.readLine()
                }

                // Read body if POST
                val body = if (contentLength > 0) {
                    val buffer = CharArray(contentLength)
                    var totalRead = 0
                    while (totalRead < contentLength) {
                        val read = reader.read(buffer, totalRead, contentLength - totalRead)
                        if (read == -1) break
                        totalRead += read
                    }
                    String(buffer, 0, totalRead)
                } else {
                    ""
                }

                val questionMarkIndex = pathAndQuery.indexOf('?')
                val path = if (questionMarkIndex != -1) pathAndQuery.substring(0, questionMarkIndex) else pathAndQuery
                val query = if (questionMarkIndex != -1) pathAndQuery.substring(questionMarkIndex + 1) else ""

                 Log.i("LocalMovieBoxServer", "Incoming: $method $pathAndQuery")
                 val responsePair = routeRequest(method, path, query, body)
                 val contentType = when {
                     path == "/docs" || path == "/docs/" -> "text/html; charset=utf-8"
                     path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
                     path.endsWith(".png") -> "image/png"
                     else -> "application/json; charset=utf-8"
                 }
                 sendResponse(socket, responsePair.first, responsePair.second, contentType)
             } catch (e: Exception) {
                 Log.e("LocalMovieBoxServer", "Error handling client", e)
                 try {
                     sendResponse(socket, 500, "{\"code\": 500, \"msg\": \"Internal Server Error\"}")
                 } catch (ex: Exception) {}
             } finally {
                 try {
                     socket.close()
                 } catch (e: Exception) {}
             }
         }
     }
 
     private fun sendResponse(socket: Socket, statusCode: Int, body: String, contentType: String = "application/json; charset=utf-8") {
         val output: OutputStream = socket.getOutputStream()
         val bodyBytes = if (contentType.startsWith("image/")) {
             body.toByteArray(Charsets.ISO_8859_1)
         } else {
             body.toByteArray(StandardCharsets.UTF_8)
         }
         val responseHeaders = "HTTP/1.1 $statusCode OK\r\n" +
                 "Content-Type: $contentType\r\n" +
                 "Content-Length: ${bodyBytes.size}\r\n" +
                 "Connection: close\r\n" +
                 "Access-Control-Allow-Origin: *\r\n" +
                 "\r\n"
         output.write(responseHeaders.toByteArray(StandardCharsets.UTF_8))
         output.write(bodyBytes)
         output.flush()
     }

    private fun routeRequest(method: String, path: String, query: String, body: String): Pair<Int, String> {
        return try {
            val qParams = parseQueryParams(query)
            
            when {
                path == "/docs" || path == "/docs/" -> {
                    Pair(200, DocsHtml.getHtml(port))
                }
                
                path == "/user-info" -> {
                    val prefs = context.getSharedPreferences("moviebox_prefs", Context.MODE_PRIVATE)
                    val token = prefs.getString("auth_token", null)
                    if (token.isNullOrEmpty()) {
                        Pair(200, "{\"logged_in\": false, \"mode\": \"Guest Access\", \"session_id\": \"local_session\"}")
                    } else {
                        val rawProfile = try {
                            makeOfficialRequest("GET", "/wefeed-mobile-bff/user-api/profile/v2")
                        } catch(e: Exception) { "" }
                        
                        val responseMap = try {
                            parseJsonToMap(rawProfile)
                        } catch(e: Exception) { emptyMap() }
                        
                        val userMap = responseMap["data"] as? Map<String, Any?> ?: run {
                            val userInfoJson = prefs.getString("user_info_json", null)
                            if (!userInfoJson.isNullOrEmpty()) {
                                try {
                                    parseJsonToMap(userInfoJson)
                                } catch (e: Exception) { null }
                            } else null
                        }
                        
                        val userInfo = userMap?.get("userInfo") as? Map<String, Any?>
                        val vipInfo = userMap?.get("vipInfo") as? Map<String, Any?>
                        val mySubject = userMap?.get("mySubject") as? Map<String, Any?>
                        val favoriteInfo = userMap?.get("favoriteInfo") as? Map<String, Any?>

                        val targetUser = mapOf(
                            "username" to (userInfo?.get("username") ?: userMap?.get("userName") ?: userMap?.get("nickname") ?: userInfo?.get("nickname") ?: "User"),
                            "nickname" to (userInfo?.get("nickname") ?: userMap?.get("nickname") ?: "User"),
                            "avatar" to (userInfo?.get("avatar") ?: userMap?.get("avatarUrl") ?: ""),
                            "email" to (userInfo?.get("mail") ?: userMap?.get("email") ?: ""),
                            "vip" to (if (vipInfo?.get("isActive") == true || userMap?.get("vip")?.toString()?.toDoubleOrNull()?.toInt() == 1) 1 else 0),
                            "userId" to (userInfo?.get("userId") ?: userMap?.get("userId") ?: ""),
                            "vipExpire" to (vipInfo?.get("expiryDate") ?: ""),
                            "vipDaysLeft" to (vipInfo?.get("daysLeft")?.toString()?.toDoubleOrNull()?.toInt() ?: 0),
                            "vipPoint" to (vipInfo?.get("point")?.toString()?.toDoubleOrNull()?.toInt() ?: 0),
                            "wantToSeeCount" to (mySubject?.get("wantToSeeCount")?.toString()?.toDoubleOrNull()?.toInt() ?: 0),
                            "haveSeenCount" to (mySubject?.get("haveSeenCount")?.toString()?.toDoubleOrNull()?.toInt() ?: 0),
                            "favoriteCount" to (favoriteInfo?.get("favoriteCount")?.toString()?.toDoubleOrNull()?.toInt() ?: 0)
                        )
                        
                        val responseObj = mapOf(
                            "logged_in" to true,
                            "mode" to "Official Account",
                            "user" to targetUser,
                            "session_id" to "local_session"
                        )
                        Pair(200, toJsonString(responseObj))
                    }
                }
                
                path == "/request-otp" -> {
                    val requestMap = parseJsonToMap(body)
                    val account = requestMap["account"]?.toString() ?: ""
                    val type = requestMap["type"]?.toString()?.toDoubleOrNull()?.toInt() ?: 1

                    val payload = mutableMapOf<String, Any>(
                        "package_name" to "com.community.mbox.in",
                        "type" to type,
                        "mail" to account,
                        "authType" to 1
                    )

                    val payloadString = toJsonString(payload)
                    val rawResponse = makeOfficialRequest("POST", "/wefeed-mobile-bff/user-api/get-sms-code", emptyMap(), payloadString)

                    val responseMap = parseJsonToMap(rawResponse)
                    val code = responseMap["code"]?.toString()?.toDoubleOrNull()?.toInt() ?: -1
                    if (code == 0 || code == 200) {
                        Pair(200, "{\"status\": \"success\", \"msg\": \"OTP Sent\"}")
                    } else {
                        val msg = responseMap["msg"]?.toString() ?: responseMap["message"]?.toString() ?: "Failed to send OTP"
                        Pair(400, "{\"status\": \"error\", \"message\": \"$msg\"}")
                    }
                }
                
                path == "/login" -> {
                    val requestMap = parseJsonToMap(body)
                    val account = requestMap["account"]?.toString() ?: ""
                    val password = requestMap["password"]?.toString() ?: ""
                    val authType = requestMap["authType"]?.toString()?.toDoubleOrNull()?.toInt() ?: 1

                    val hashedPwd = md5(password)
                    val payload = mutableMapOf<String, Any>(
                        "password" to hashedPwd,
                        "package_name" to "com.community.mbox.in",
                        "authType" to 1,
                        "type" to 0,
                        "mail" to account
                    )

                    val payloadString = toJsonString(payload)
                    val rawResponse = makeOfficialRequest("POST", "/wefeed-mobile-bff/user-api/login", emptyMap(), payloadString)

                    val responseMap = parseJsonToMap(rawResponse)
                    val code = responseMap["code"]?.toString()?.toDoubleOrNull()?.toInt() ?: -1
                    val data = responseMap["data"] as? Map<String, Any?>
                    val token = data?.get("token")?.toString()

                    if ((code == 0 || code == 200) && !token.isNullOrEmpty()) {
                        val prefs = context.getSharedPreferences("moviebox_prefs", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putString("auth_token", token)
                            .putString("user_info_json", toJsonString(data))
                            .apply()
                        
                        val targetResponse = mapOf(
                            "status" to "success",
                            "user" to mapOf(
                                "username" to (data["userName"] ?: data["nickname"] ?: account),
                                "nickname" to (data["nickname"] ?: account),
                                "avatar" to (data["avatarUrl"] ?: ""),
                                "email" to (data["email"] ?: account),
                                "vip" to (data["vip"] ?: 0),
                                "userId" to (data["userId"] ?: "")
                            )
                        )
                        Pair(200, toJsonString(targetResponse))
                    } else {
                        val msg = responseMap["msg"]?.toString() ?: responseMap["message"]?.toString() ?: "Login failed"
                        Pair(400, "{\"status\": \"error\", \"message\": \"$msg\"}")
                    }
                }
                
                path == "/register" -> {
                    val requestMap = parseJsonToMap(body)
                    val account = requestMap["account"]?.toString() ?: ""
                    val password = requestMap["password"]?.toString() ?: ""
                    val otp = requestMap["otp"]?.toString() ?: ""
                    val authType = requestMap["authType"]?.toString()?.toDoubleOrNull()?.toInt() ?: 1

                    val hashedPwd = md5(password)
                    val payload = mutableMapOf<String, Any>(
                        "password" to hashedPwd,
                        "verificationCode" to otp,
                        "package_name" to "com.community.mbox.in",
                        "authType" to 1,
                        "type" to 1,
                        "mail" to account
                    )

                    val payloadString = toJsonString(payload)
                    android.util.Log.d("LocalMovieBoxServer", "Register payload: $payloadString")
                    val rawResponse = makeOfficialRequest("POST", "/wefeed-mobile-bff/user-api/register", emptyMap(), payloadString)
                    android.util.Log.d("LocalMovieBoxServer", "Register rawResponse: $rawResponse")

                    val responseMap = parseJsonToMap(rawResponse)
                    val code = responseMap["code"]?.toString()?.toDoubleOrNull()?.toInt() ?: -1
                    val data = responseMap["data"] as? Map<String, Any?>

                    if (code == 0 || code == 200) {
                        val targetResponse = mapOf(
                            "status" to "success",
                            "user" to mapOf(
                                "username" to (data?.get("userName") ?: account),
                                "nickname" to (data?.get("nickname") ?: account),
                                "avatar" to (data?.get("avatarUrl") ?: ""),
                                "email" to (data?.get("email") ?: account),
                                "vip" to (data?.get("vip") ?: 0),
                                "userId" to (data?.get("userId") ?: "")
                            )
                        )
                        Pair(200, toJsonString(targetResponse))
                    } else {
                        val msg = responseMap["msg"]?.toString() ?: responseMap["message"]?.toString() ?: "Registration failed"
                        Pair(400, "{\"status\": \"error\", \"message\": \"$msg\"}")
                    }
                }
                
                path == "/logout" -> {
                    val prefs = context.getSharedPreferences("moviebox_prefs", Context.MODE_PRIVATE)
                    prefs.edit().remove("auth_token").remove("user_info_json").apply()
                    Pair(200, "{\"status\": \"success\"}")
                }

                path == "/modify" -> {
                    try {
                        val requestMap = parseJsonToMap(body)
                        val nickname = requestMap["nickname"]?.toString() ?: ""
                        val avatar = requestMap["avatar"]?.toString() ?: ""

                        val payload = mutableMapOf<String, Any>(
                            "nickname" to nickname
                        )
                        if (avatar.isNotEmpty()) {
                            payload["avatar"] = avatar
                        }

                        val payloadString = toJsonString(payload)
                        val rawResponse = makeOfficialRequest("POST", "/wefeed-mobile-bff/user-api/modify", emptyMap(), payloadString)
                        Pair(200, rawResponse)
                    } catch (e: Exception) {
                        Pair(500, "{\"code\": 500, \"message\": \"${e.message}\"}")
                    }
                }

                path == "/sts-token" -> {
                    try {
                        val rawResponse = makeOfficialRequest("GET", "/wefeed-mobile-bff/upload/sts-token/v2")
                        Pair(200, rawResponse)
                    } catch (e: Exception) {
                        Pair(500, "{\"code\": 500, \"message\": \"${e.message}\"}")
                    }
                }

                path == "/upload-avatar" -> {
                    try {
                        val requestMap = parseJsonToMap(body)
                        val base64Str = requestMap["image"]?.toString() ?: ""
                        if (base64Str.isNotEmpty()) {
                            val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.NO_WRAP)
                            val file = java.io.File(context.filesDir, "avatar.jpg")
                            file.outputStream().use { out ->
                                out.write(decodedBytes)
                            }
                            Pair(200, "{\"status\": \"success\", \"url\": \"http://127.0.0.1:3000/uploads/avatar.jpg\"}")
                        } else {
                            Pair(400, "{\"status\": \"error\", \"message\": \"Image data is empty\"}")
                        }
                    } catch (e: Exception) {
                        Pair(500, "{\"status\": \"error\", \"message\": \"${e.message}\"}")
                    }
                }

                path == "/uploads/avatar.jpg" -> {
                    try {
                        val file = java.io.File(context.filesDir, "avatar.jpg")
                        if (file.exists()) {
                            val bytes = file.readBytes()
                            val bodyString = String(bytes, Charsets.ISO_8859_1)
                            Pair(200, bodyString)
                        } else {
                            Pair(404, "Not Found")
                        }
                    } catch (e: Exception) {
                        Pair(500, e.message ?: "Error")
                    }
                }

                path == "/wefeed-mobile-bff/subject-api/want-to-see" -> {
                    try {
                        val rawResponse = makeOfficialRequest("POST", "/wefeed-mobile-bff/subject-api/want-to-see", emptyMap(), body)
                        Pair(200, rawResponse)
                    } catch (e: Exception) {
                        Pair(500, "{\"code\": 500, \"message\": \"${e.message}\"}")
                    }
                }

                path == "/wefeed-mobile-bff/subject-api/see-list-v2" -> {
                    try {
                        val page = qParams["page"] ?: "1"
                        val pageSize = qParams["pageSize"] ?: "20"
                        val seeType = qParams["seeType"] ?: "1"
                        val params = mapOf(
                            "page" to page,
                            "pageSize" to pageSize,
                            "seeType" to seeType
                        )
                        val rawResponse = makeOfficialRequest("GET", "/wefeed-mobile-bff/subject-api/see-list-v2", params)
                        Pair(200, rawResponse)
                    } catch (e: Exception) {
                        Pair(500, "{\"code\": 500, \"message\": \"${e.message}\"}")
                    }
                }
                
                path == "/trending" -> {
                    try {
                        val rawResponse = makeOfficialRequest("POST", "/wefeed-mobile-bff/subject-api/trending/v2", emptyMap(), "{}")
                        val responseMap = parseJsonToMap(rawResponse)
                        val dataMap = responseMap["data"] as? Map<String, Any?>
                        val list = (dataMap?.get("list") ?: dataMap?.get("items") ?: dataMap?.get("subjects")) as? List<Map<String, Any?>> 
                            ?: responseMap["data"] as? List<Map<String, Any?>> 
                            ?: emptyList()
                        val mapped = list.mapNotNull { mapItem(it) }
                        val responseBody = mapOf(
                            "code" to 0,
                            "data" to mapped
                        )
                        Pair(200, toJsonString(responseBody))
                    } catch (e: Exception) {
                        Log.e("LocalMovieBoxServer", "Trending error: ${e.message}")
                        val responseBody = mapOf(
                            "code" to 0,
                            "data" to emptyList<Map<String, Any?>>()
                        )
                        Pair(200, toJsonString(responseBody))
                    }
                }
                
                path == "/discovery" -> {
                    try {
                        val rawResponse = makeOfficialRequest("POST", "/wefeed-mobile-bff/subject-api/top-rec", emptyMap(), "{}")
                        val responseMap = parseJsonToMap(rawResponse)
                        val dataMap = responseMap["data"] as? Map<String, Any?>
                        val list = (dataMap?.get("list") ?: dataMap?.get("items") ?: dataMap?.get("subjects")) as? List<Map<String, Any?>> 
                            ?: responseMap["data"] as? List<Map<String, Any?>> 
                            ?: emptyList()
                        val mapped = list.mapNotNull { mapItem(it) }
                        val responseBody = mapOf(
                            "code" to 0,
                            "data" to mapped
                        )
                        Pair(200, toJsonString(responseBody))
                    } catch (e: Exception) {
                        Log.e("LocalMovieBoxServer", "Discovery error: ${e.message}")
                        val responseBody = mapOf(
                            "code" to 0,
                            "data" to emptyList<Map<String, Any?>>()
                        )
                        Pair(200, toJsonString(responseBody))
                    }
                }
                
                path == "/search" -> {
                    val q = qParams["q"] ?: ""
                    val page = qParams["page"]?.toIntOrNull() ?: 1
                    try {
                        val payload = mapOf(
                            "keyword" to q,
                            "page" to page,
                            "pageSize" to 20
                        )
                        val payloadString = toJsonString(payload)
                        val rawResponse = makeOfficialRequest("POST", "/wefeed-mobile-bff/subject-api/search", emptyMap(), payloadString)
                        val responseMap = parseJsonToMap(rawResponse)
                        val dataMap = responseMap["data"] as? Map<String, Any?>
                        val list = (dataMap?.get("list") ?: dataMap?.get("items") ?: dataMap?.get("subjects")) as? List<Map<String, Any?>> 
                            ?: responseMap["data"] as? List<Map<String, Any?>> 
                            ?: emptyList()
                        val mapped = list.mapNotNull { mapItem(it) }
                        val responseBody = mapOf(
                            "code" to 0,
                            "data" to mapOf("items" to mapped)
                        )
                        Pair(200, toJsonString(responseBody))
                    } catch (e: Exception) {
                        Log.e("LocalMovieBoxServer", "Search error: ${e.message}")
                        val responseBody = mapOf(
                            "code" to 0,
                            "data" to mapOf("items" to emptyList<Map<String, Any?>>())
                        )
                        Pair(200, toJsonString(responseBody))
                    }
                }
                
                path == "/search-suggestions" -> {
                    val q = qParams["q"] ?: ""
                    try {
                        if (q.isNotEmpty()) {
                            val payload = mapOf(
                                "keyword" to q,
                                "page" to 1,
                                "pageSize" to 20
                            )
                            val payloadString = toJsonString(payload)
                            val rawResponse = makeOfficialRequest("POST", "/wefeed-mobile-bff/subject-api/search", emptyMap(), payloadString)
                            val responseMap = parseJsonToMap(rawResponse)
                            val dataMap = responseMap["data"] as? Map<String, Any?>
                            val list = (dataMap?.get("list") ?: dataMap?.get("items") ?: dataMap?.get("subjects")) as? List<Map<String, Any?>> 
                                ?: responseMap["data"] as? List<Map<String, Any?>> 
                                ?: emptyList()
                            val suggestions = list.mapNotNull { i ->
                                i["title"]?.toString() ?: i["name"]?.toString() ?: i["keyword"]?.toString()
                            }
                            val responseBody = mapOf(
                                "code" to 0,
                                "data" to suggestions
                            )
                            Pair(200, toJsonString(responseBody))
                        } else {
                            val rawResponse = makeOfficialRequest("GET", "/wefeed-mobile-bff/subject-api/search-rank", mapOf("page" to "1", "pageSize" to "10"))
                            val responseMap = parseJsonToMap(rawResponse)
                            val dataMap = responseMap["data"] as? Map<String, Any?>
                            val list = (dataMap?.get("list") ?: dataMap?.get("items") ?: dataMap?.get("subjects") ?: dataMap?.get("keywords")) as? List<Map<String, Any?>> 
                                ?: responseMap["data"] as? List<Map<String, Any?>> 
                                ?: emptyList()
                            val suggestions = list.mapNotNull { i ->
                                i["keyword"] ?: i["title"] ?: i["name"]
                            }
                            val responseBody = mapOf(
                                "code" to 0,
                                "data" to suggestions
                            )
                            Pair(200, toJsonString(responseBody))
                        }
                    } catch (e: Exception) {
                        Log.w("LocalMovieBoxServer", "Search-suggestions fallback triggered: ${e.message}")
                        val suggestions = listOf("Matrix", "Spider-Man", "Dune", "Oppenheimer", "Interstellar")
                        val responseBody = mapOf(
                            "code" to 0,
                            "data" to suggestions
                        )
                        Pair(200, toJsonString(responseBody))
                    }
                }
                
                path.startsWith("/detail/") -> {
                    val subjectId = path.substringAfter("/detail/").substringBefore("?").substringBefore("/")
                    try {
                        val rawResponse = makeOfficialRequest("GET", "/wefeed-mobile-bff/subject-api/get", mapOf("subjectId" to subjectId))
                        val responseMap = parseJsonToMap(rawResponse)
                        var data = responseMap["data"] as? Map<String, Any?> ?: return Pair(404, "{\"code\": 1, \"msg\": \"Not found\"}")

                        // Resolve original series metadata if this is a dub entry
                        val dubsList = data["dubs"] as? List<Map<String, Any?>> ?: emptyList()
                        val originalDub = dubsList.find { it["original"] == true || it["original"]?.toString()?.toBoolean() == true }
                        val mainSubjectId = originalDub?.get("subjectId")?.toString()
                        if (!mainSubjectId.isNullOrEmpty() && mainSubjectId != subjectId) {
                            try {
                                val mainResponseRaw = makeOfficialRequest("GET", "/wefeed-mobile-bff/subject-api/get", mapOf("subjectId" to mainSubjectId))
                                val mainResponseMap = parseJsonToMap(mainResponseRaw)
                                val mainData = mainResponseMap["data"] as? Map<String, Any?>
                                if (mainData != null) {
                                    val mutableData = data.toMutableMap()
                                    listOf("description", "duration", "year", "imdbRating", "genres", "staffList", "actorList", "totalSeasons", "totalEpisode").forEach { key ->
                                        val mainVal = mainData[key]
                                        val currVal = data[key]
                                        if (mainVal != null && (currVal == null || currVal.toString().isEmpty() || currVal.toString() == "0" || currVal.toString() == "0 min")) {
                                            mutableData[key] = mainVal
                                        }
                                    }
                                    data = mutableData
                                }
                            } catch (e: Exception) {
                                Log.w("LocalMovieBoxServer", "Failed to fetch main detail for dub: ${e.message}")
                            }
                        }

                        val mapped = mapItem(data) ?: data
                        val mutableMapped = mapped.toMutableMap()

                        // Cast mapping
                        val rawCast = data["staffList"] as? List<Map<String, Any?>> ?: data["actorList"] as? List<Map<String, Any?>> ?: emptyList()
                        val cast = rawCast.mapNotNull { castMember ->
                            val name = castMember["name"] ?: castMember["actorName"] ?: "Unknown"
                            val role = castMember["character"] ?: castMember["role"] ?: "Cast"
                            val avatar = castMember["avatarUrl"] ?: castMember["avatar"] ?: castMember["photo"] ?: castMember["poster"] ?: ""
                            mapOf("name" to name, "role" to role, "avatar" to avatar)
                        }
                        mutableMapped["cast"] = cast

                        // Available Languages / Dubs (Consolidated into resourceDetectors)
                        val consolidatedDetectors = mutableListOf<Map<String, Any?>>()
                        val rawDubs = data["dubs"] as? List<Map<String, Any?>> ?: emptyList()
                        for (dub in rawDubs) {
                            val dubSubId = dub["subjectId"]?.toString() ?: continue
                            consolidatedDetectors.add(mapOf(
                                "resourceId" to "dub_$dubSubId",
                                "name" to (dub["lanName"] ?: "Custom Dub"),
                                "totalEpisode" to 0
                            ))
                        }
                        val detectors = data["resourceDetectors"] as? List<Map<String, Any?>> ?: emptyList()
                        for (d in detectors) {
                            val resId = d["resourceId"]?.toString() ?: continue
                            consolidatedDetectors.add(mapOf(
                                "resourceId" to "res_$resId",
                                "name" to (d["name"] ?: d["uploadBy"] ?: "Resource"),
                                "totalEpisode" to (d["totalEpisode"] ?: 0)
                            ))
                        }
                        mutableMapped["resourceDetectors"] = consolidatedDetectors

                        val responseBody = mapOf(
                            "code" to 0,
                            "data" to mutableMapped
                        )
                        Pair(200, toJsonString(responseBody))
                    } catch (e: Exception) {
                        Log.w("LocalMovieBoxServer", "Detail fallback triggered: ${e.message}")
                        val mockMovie = getMockTrending().find { it["subjectId"] == subjectId } ?: getMockTrending()[0]
                        val mutableMapped = mockMovie.toMutableMap()
                        mutableMapped["cast"] = listOf(
                            mapOf("name" to "Keanu Reeves", "role" to "Neo", "avatar" to "https://image.tmdb.org/t/p/w500/b9v69EMT2g7uY78MKg6Y67688v6.jpg"),
                            mapOf("name" to "Carrie-Anne Moss", "role" to "Trinity", "avatar" to "https://image.tmdb.org/t/p/w500/8gRE3687bcu6797hY0A1vGZ6P6V.jpg")
                        )
                        mutableMapped["languages"] = listOf(
                            mapOf("id" to "sub_en", "subjectId" to subjectId, "name" to "English Dub", "type" to "dub"),
                            mapOf("id" to "res_hd", "subjectId" to subjectId, "name" to "Google Resource (1080p)", "type" to "resource")
                        )
                        val responseBody = mapOf(
                            "code" to 0,
                            "data" to mutableMapped
                        )
                        Pair(200, toJsonString(responseBody))
                    }
                }
                
                path.startsWith("/episodes/") -> {
                    val seriesId = path.substringAfter("/episodes/").substringBefore("?").substringBefore("/")
                    try {
                        val rawResponse = makeOfficialRequest("GET", "/wefeed-mobile-bff/subject-api/season-info", mapOf("subjectId" to seriesId))
                        val responseMap = parseJsonToMap(rawResponse)
                        val data = responseMap["data"] as? Map<String, Any?> ?: responseMap

                        val rawSeasons = data["seasons"] as? List<Map<String, Any?>> ?: data["seasonList"] as? List<Map<String, Any?>> ?: data["list"] as? List<Map<String, Any?>> ?: emptyList()
                        val mappedSeasons = mutableListOf<Map<String, Any?>>()

                        for (sRaw in rawSeasons) {
                            val num = sRaw["se"] ?: sRaw["seasonNumber"] ?: 1
                            val eps = mutableListOf<Map<String, Any?>>()
                            
                            val pool = when {
                                sRaw["allEp"] != null -> sRaw["allEp"]
                                sRaw["epList"] != null -> sRaw["epList"]
                                sRaw["episodeList"] != null -> sRaw["episodeList"]
                                sRaw["episodes"] != null -> sRaw["episodes"]
                                sRaw["list"] != null -> sRaw["list"]
                                sRaw["items"] != null -> sRaw["items"]
                                else -> null
                            }
                            
                            if (pool is String) {
                                pool.split(",").forEach { eNum ->
                                    if (eNum.isNotEmpty()) {
                                        eps.add(mapOf("episodeNumber" to eNum, "title" to "Episode $eNum", "id" to "${seriesId}_${num}_${eNum}"))
                                    }
                                }
                            } else if (pool is List<*>) {
                                for (item in pool) {
                                    if (item is Map<*, *>) {
                                        val en = item["ep"] ?: item["episodeNumber"] ?: item["episode_number"]
                                        if (en != null) {
                                            eps.add(mapOf("episodeNumber" to en.toString(), "title" to (item["title"] ?: "Episode $en"), "id" to "${seriesId}_${num}_${en}"))
                                        }
                                    } else if (item != null) {
                                        eps.add(mapOf("episodeNumber" to item.toString(), "title" to "Episode $item", "id" to "${seriesId}_${num}_${item}"))
                                    }
                                }
                            }
                            
                            if (eps.isEmpty()) {
                                val maxEpVal = sRaw["maxEp"] ?: sRaw["max_ep"] ?: 0
                                val maxEp = maxEpVal.toString().toDoubleOrNull()?.toInt() ?: 0
                                if (maxEp > 0) {
                                    for (i in 1..maxEp) {
                                        eps.add(mapOf("episodeNumber" to i.toString(), "title" to "Episode $i", "id" to "${seriesId}_${num}_${i}"))
                                    }
                                }
                            }
                            
                            if (eps.isNotEmpty()) {
                                mappedSeasons.add(mapOf("seasonNumber" to num, "episodes" to eps))
                            }
                        }
                        if (mappedSeasons.isEmpty()) {
                            throw Exception("No seasons from official API")
                        }
                        val responseBody = mapOf(
                            "code" to 0,
                            "data" to mapOf("seasons" to mappedSeasons)
                        )
                        Pair(200, toJsonString(responseBody))
                    } catch (e: Exception) {
                        Log.w("LocalMovieBoxServer", "Episodes fallback triggered: ${e.message}")
                        val mappedSeasons = listOf(
                            mapOf(
                                "seasonNumber" to 1,
                                "episodes" to listOf(
                                    mapOf("episodeNumber" to "1", "title" to "Pilot", "id" to "${seriesId}_1_1"),
                                    mapOf("episodeNumber" to "2", "title" to "The Beginning", "id" to "${seriesId}_1_2"),
                                    mapOf("episodeNumber" to "3", "title" to "Into the Dark", "id" to "${seriesId}_1_3")
                                )
                            )
                        )
                        val responseBody = mapOf(
                            "code" to 0,
                            "data" to mapOf("seasons" to mappedSeasons)
                        )
                        Pair(200, toJsonString(responseBody))
                    }
                }
                
                path.startsWith("/stream/") -> {
                    val subjectId = path.substringAfter("/stream/").substringBefore("?").substringBefore("/")
                    val season = qParams["season"] ?: "1"
                    val episode = qParams["episode"] ?: "1"
                    val quality = qParams["quality"] ?: "720p"
                    val resourceId = qParams["resource_id"]

                    try {
                        var isMovie = false
                        var subResourceId = subjectId
                        try {
                            val detailResponseRaw = makeOfficialRequest("GET", "/wefeed-mobile-bff/subject-api/get", mapOf("subjectId" to subjectId))
                            val detailResponse = parseJsonToMap(detailResponseRaw)
                            val detailData = detailResponse["data"] as? Map<String, Any?>
                            val type = detailData?.get("subjectType") ?: detailData?.get("type") ?: 1
                            isMovie = type.toString().toDoubleOrNull()?.toInt() == 1
                            
                            val detectors = detailData?.get("resourceDetectors") as? List<Map<String, Any?>> ?: emptyList()
                            if (detectors.isNotEmpty()) {
                                subResourceId = detectors[0]["resourceId"]?.toString() ?: subjectId
                            }
                        } catch (e: Exception) {}

                        var finalSubjectId = subjectId
                        var finalResourceId: String? = null
                        if (resourceId != null) {
                            if (resourceId.startsWith("dub_")) {
                                finalSubjectId = resourceId.substringAfter("dub_")
                            } else if (resourceId.startsWith("res_")) {
                                finalResourceId = resourceId.substringAfter("res_")
                            } else {
                                finalResourceId = resourceId
                            }
                        }

                        val resSe = if (isMovie) null else season
                        val resEp = if (isMovie) null else episode

                        val params = mutableMapOf<String, String>(
                            "subjectId" to finalSubjectId,
                            "host" to "api6.aoneroom.com"
                        )
                        if (resSe != null) params["se"] = resSe
                        if (resEp != null) params["ep"] = resEp
                        if (finalResourceId != null) params["resourceId"] = finalResourceId

                        val rawResponse = makeOfficialRequest("GET", "/wefeed-mobile-bff/subject-api/play-info", params)
                        val responseMap = parseJsonToMap(rawResponse)
                        val data = responseMap["data"] as? Map<String, Any?> ?: emptyMap<String, Any?>()

                        val streams = data["streamList"] as? List<Map<String, Any?>> ?: data["streams"] as? List<Map<String, Any?>> ?: emptyList()
                        val globalCookie = responseMap["signCookie"] ?: data["signCookie"] ?: ""

                        val prioritizedStreams = streams.map { it }
                        var workingUrl = ""
                        var workingCookie = globalCookie.toString()

                        if (prioritizedStreams.isNotEmpty()) {
                            val sortedDescending = prioritizedStreams.sortedWith { s1, s2 ->
                                val u1 = (s1["url"] ?: "").toString().lowercase()
                                val u2 = (s2["url"] ?: "").toString().lowercase()
                                
                                fun getScore(u: String): Int {
                                    val isH265 = u.contains("h265") || u.contains("x265") || u.contains("hev1")
                                    if (u.contains(".m3u8")) return if (isH265) 3 else 10
                                    if (u.contains(".mpd")) return if (isH265) 2 else 9
                                    if (u.contains(".mp4")) return if (isH265) 1 else 8
                                    return if (isH265) 0 else 5
                                }
                                getScore(u2).compareTo(getScore(u1))
                            }
                            
                            val bestStream = sortedDescending[0]
                            workingUrl = bestStream["url"]?.toString() ?: ""
                            val bestCookie = bestStream["signCookie"]?.toString() ?: ""
                            if (bestCookie.isNotEmpty()) {
                                workingCookie = bestCookie
                            }
                        }

                        val subtitlesMapped = mutableListOf<Map<String, Any?>>()
                        
                        // 1. Fetch external subtitles (get-ext-captions)
                        try {
                            val subParams = mapOf(
                                "subjectId" to finalSubjectId,
                                "resourceId" to subResourceId,
                                "episode" to episode
                            )
                            val subResponseRaw = makeOfficialRequest("GET", "/wefeed-mobile-bff/subject-api/get-ext-captions", subParams)
                            val subResponseMap = parseJsonToMap(subResponseRaw)
                            val subData = subResponseMap["data"] as? Map<String, Any?> ?: emptyMap()
                            val extList = (subData["extCaptions"] ?: subData["list"] ?: emptyList<Any?>()) as? List<Map<String, Any?>> ?: emptyList()
                            
                            for (sub in extList) {
                                val lan = sub["lan"]?.toString() ?: ""
                                val lanName = sub["lanName"]?.toString() ?: ""
                                val subUrl = sub["url"]?.toString() ?: ""
                                if (subUrl.isNotEmpty()) {
                                    subtitlesMapped.add(mapOf(
                                        "lan" to lan,
                                        "lanName" to lanName,
                                        "url" to subUrl
                                    ))
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("LocalMovieBoxServer", "Failed to fetch external subtitles: ${e.message}")
                        }

                        // 2. Fetch stream-internal subtitles and merge
                        val subtitleList = data["subTitleList"] as? List<Map<String, Any?>> ?: emptyList()
                        for (sub in subtitleList) {
                            val lan = sub["lan"]?.toString() ?: ""
                            val lanName = sub["lanName"]?.toString() ?: ""
                            val subUrl = sub["url"]?.toString() ?: ""
                            if (subUrl.isNotEmpty() && !subtitlesMapped.any { it["url"] == subUrl }) {
                                subtitlesMapped.add(mapOf(
                                    "lan" to lan,
                                    "lanName" to lanName,
                                    "url" to subUrl
                                ))
                            }
                        }

                        val responseBody = mapOf(
                            "code" to 0,
                            "url" to workingUrl,
                            "quality" to quality,
                            "cookie" to workingCookie,
                            "resolutions" to (if (prioritizedStreams.isNotEmpty()) prioritizedStreams[0]["resolutions"] ?: "" else ""),
                            "subtitles" to subtitlesMapped
                        )
                        Pair(200, toJsonString(responseBody))
                    } catch (e: Exception) {
                        Log.w("LocalMovieBoxServer", "Stream fallback triggered: ${e.message}")
                        val responseBody = mapOf(
                            "code" to 0,
                            "url" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                            "quality" to quality,
                            "cookie" to "",
                            "subtitles" to listOf(
                                mapOf("lan" to "en", "lanName" to "English", "url" to "https://raw.githubusercontent.com/andreyvit/subtitle-tools/master/test.srt")
                            )
                        )
                        Pair(200, toJsonString(responseBody))
                    }
                }
                
                else -> {
                    // Category list handlers: movies, anime, asian, western, short-tv, kids, education, music, nollywood, game
                    val categoryId = when (path) {
                        "/movies" -> 2
                        "/anime" -> 8
                        "/asian" -> 18
                        "/western" -> 19
                        "/short-tv" -> 13
                        "/kids" -> 23
                        "/education" -> 3
                        "/music" -> 4
                        "/nollywood" -> 28
                        "/game" -> 11
                        else -> null
                    }
                    
                    if (categoryId != null) {
                        val page = qParams["page"] ?: "1"
                        try {
                            val rawResponse = makeOfficialRequest(
                                "GET",
                                "/wefeed-mobile-bff/tab-operating",
                                mapOf("tabId" to categoryId.toString(), "page" to page, "pageSize" to "24")
                            )
                            val responseMap = parseJsonToMap(rawResponse)
                            val dataMap = responseMap["data"] as? Map<String, Any?>
                            val list = (dataMap?.get("list") ?: dataMap?.get("items") ?: dataMap?.get("subjects")) as? List<Map<String, Any?>> 
                                ?: responseMap["data"] as? List<Map<String, Any?>> 
                                ?: emptyList()
                            val formatted = formatTabSections(list)
                            if (formatted.isEmpty()) {
                                throw Exception("Empty list from official API")
                            }
                            val responseBody = mapOf(
                                "code" to 0,
                                "data" to mapOf("list" to formatted)
                            )
                            Pair(200, toJsonString(responseBody))
                        } catch (e: Exception) {
                            Log.w("LocalMovieBoxServer", "Category fallback triggered: ${e.message}")
                            val formatted = emptyList<Map<String, Any?>>()
                            val responseBody = mapOf(
                                "code" to 0,
                                "data" to mapOf("list" to formatted)
                            )
                            Pair(200, toJsonString(responseBody))
                        }
                    } else {
                        Pair(404, "{\"code\": 404, \"msg\": \"Not Found\"}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocalMovieBoxServer", "Error routing request $path", e)
            Pair(500, "{\"code\": 500, \"msg\": \"${e.localizedMessage}\"}")
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (query.isEmpty()) return result
        query.split("&").forEach { param ->
            val parts = param.split("=", limit = 2)
            val key = URLDecoder.decode(parts[0], "UTF-8")
            val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else ""
            result[key] = value
        }
        return result
    }

    private fun formatTabSections(items: List<Map<String, Any?>>): List<Map<String, Any?>> {
        val sections = mutableListOf<Map<String, Any?>>()
        
        var isDirectMovies = true
        for (row in items) {
            if (row.containsKey("list") || row.containsKey("items") || row.containsKey("subjects") || row.containsKey("movieList") || row.containsKey("customData") || row.containsKey("banner")) {
                isDirectMovies = false
                break
            }
        }
        
        if (isDirectMovies && items.isNotEmpty()) {
            val mapped = items.mapNotNull { mapItem(it) }
            if (mapped.isNotEmpty()) {
                return listOf(mapOf("title" to "Content", "items" to mapped))
            }
        }
        
        for (row in items) {
            val title = row["title"] ?: row["name"] ?: "Section"
            
            val inner = when {
                (row["customData"] as? Map<*, *>)?.get("items").let { it is List<*> && it.isNotEmpty() } -> {
                    val customData = row["customData"] as Map<String, Any?>
                    customData["items"] as List<Map<String, Any?>>
                }
                (row["banner"] as? Map<*, *>)?.get("banners").let { it is List<*> && it.isNotEmpty() } -> {
                    val banner = row["banner"] as Map<String, Any?>
                    banner["banners"] as List<Map<String, Any?>>
                }
                row["list"].let { it is List<*> && it.isNotEmpty() } -> {
                    row["list"] as List<Map<String, Any?>>
                }
                row["items"].let { it is List<*> && it.isNotEmpty() } -> {
                    row["items"] as List<Map<String, Any?>>
                }
                row["subjects"].let { it is List<*> && it.isNotEmpty() } -> {
                    row["subjects"] as List<Map<String, Any?>>
                }
                row["movieList"].let { it is List<*> && it.isNotEmpty() } -> {
                    row["movieList"] as List<Map<String, Any?>>
                }
                else -> emptyList()
            }
            
            val realMovies = mutableListOf<Map<String, Any?>>()
            for (i in inner) {
                val subject = i["subject"] as? Map<String, Any?>
                if (subject != null) {
                    realMovies.add(subject)
                } else if (i.containsKey("subjectId") || i.containsKey("id")) {
                    realMovies.add(i)
                }
            }
            
            if (realMovies.isNotEmpty()) {
                val mapped = realMovies.mapNotNull { mapItem(it) }
                sections.add(mapOf(
                    "title" to title,
                    "type" to (row["subjectType"] ?: row["type"] ?: "SUBJECTS_MOVIE"),
                    "items" to mapped
                ))
            }
        }
        return sections
    }

    private fun mapItem(item: Map<String, Any?>): Map<String, Any?>? {
        val itemData = (item["subject"] as? Map<String, Any?>) ?: item
        val sid = (itemData["subjectId"] ?: itemData["id"] ?: "").toString()
        if (sid.isEmpty()) return null
        
        val title = (
            itemData["title"] ?: 
            itemData["name"] ?: 
            itemData["subjectName"] ?: 
            itemData["subject_name"] ?: 
            itemData["categoryName"] ?: 
            itemData["content"] ?: 
            itemData["keyword"] ?: 
            itemData["keywordName"] ?: 
            itemData["itemName"] ?: 
            itemData["show_name"] ?: 
            itemData["showTitle"] ?: 
            itemData["titleName"] ?: 
            itemData["title_en"] ?: 
            itemData["tag"] ?: 
            itemData["label"] ?: 
            item["title"] ?:
            item["name"] ?:
            "Unknown"
        ).toString()
        
        var posterUrl = ""
        val poster = itemData["poster"]
        if (poster is Map<*, *>) {
            posterUrl = poster["url"]?.toString() ?: ""
        } else if (poster is String) {
            posterUrl = poster
        }
        if (posterUrl.isEmpty()) {
            val cover = itemData["cover"]
            if (cover is Map<*, *>) {
                posterUrl = cover["url"]?.toString() ?: ""
            } else if (cover is String) {
                posterUrl = cover
            }
        }
        if (posterUrl.isEmpty()) {
            val hp = itemData["horizontalPoster"] ?: itemData["horizontalCover"]
            if (hp is Map<*, *>) {
                posterUrl = hp["url"]?.toString() ?: ""
            } else if (hp is String) {
                posterUrl = hp
            }
        }
        if (posterUrl.startsWith("//")) {
            posterUrl = "https:$posterUrl"
        }
        
        val score = (itemData["imdbRatingValue"] ?: itemData["imdbRate"] ?: itemData["starRating"] ?: itemData["score"] ?: "8.5").toString()
        val releaseDate = (itemData["releaseDate"] ?: itemData["releaseTime"] ?: itemData["year"] ?: "2026").toString()
        val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "2026"
        val runtime = itemData["duration"] ?: itemData["runtime"] ?: itemData["minute"] ?: ""
        
        if (title.isEmpty() || title == "Unknown" || title.isBlank() || posterUrl.isEmpty() || posterUrl.isBlank()) {
            return null
        }

        return mapOf(
            "subjectId" to sid,
            "id" to sid,
            "title" to title,
            "cover" to posterUrl,
            "poster" to posterUrl,
            "score" to score,
            "releaseTime" to year,
            "subjectType" to (itemData["subjectType"] ?: itemData["type"] ?: 1),
            "runtime" to runtime,
            "description" to (itemData["description"] ?: "")
        )
    }

    private fun makeOfficialRequest(
        method: String,
        endpoint: String,
        queryParams: Map<String, String> = emptyMap(),
        bodyString: String = ""
    ): String {
        val isCacheable = (method == "GET" || endpoint.contains("/subject-api/get") || endpoint.contains("/subject-api/see-list-v2") || endpoint.contains("/subject-api/trending") || endpoint.contains("/subject-api/top-rec") || endpoint.contains("/subject-api/search") || endpoint.contains("/subject-api/search-rank") || endpoint.contains("/subject-api/season-info") || endpoint.contains("/subject-api/play-info") || endpoint.contains("/subject-api/get-ext-captions"))
        val cacheKey = "$method|$endpoint|${queryParams.entries.sortedBy { it.key }.joinToString("&") { "${it.key}=${it.value}" }}|$bodyString"
        if (isCacheable) {
            val now = System.currentTimeMillis()
            val cached = responseCache[cacheKey]
            if (cached != null && (now - cached.first) < 300000) {
                return cached.second
            }
            val cachedDiskStr = diskCachePrefs.getString(cacheKey, null)
            if (cachedDiskStr != null) {
                val pipeIndex = cachedDiskStr.indexOf('|')
                if (pipeIndex != -1) {
                    val timeStr = cachedDiskStr.substring(0, pipeIndex)
                    val body = cachedDiskStr.substring(pipeIndex + 1)
                    val timeVal = timeStr.toLongOrNull() ?: 0L
                    if ((now - timeVal) < 300000) {
                        responseCache[cacheKey] = Pair(timeVal, body)
                        return body
                    }
                }
            }
        }

        val timestamp = System.currentTimeMillis().toString()
        val reversed = timestamp.reversed()
        val clientToken = "$timestamp,${md5(reversed)}"

        val baseUrl = "https://api6.aoneroom.com"
        val fullUrlWithoutQuery = "$baseUrl$endpoint"

        val urlBuilder = fullUrlWithoutQuery.toHttpUrl().newBuilder()
        if (!queryParams.containsKey("host")) {
            urlBuilder.addQueryParameter("host", "api6.aoneroom.com")
        }
        queryParams.forEach { (k, v) ->
            urlBuilder.addQueryParameter(k, v)
        }
        val httpUrl = urlBuilder.build()

        val sortedQueryString = httpUrl.queryParameterNames.sorted().joinToString("&") { name ->
            val value = httpUrl.queryParameter(name) ?: ""
            "$name=$value"
        }

        var actualBodyLength = ""
        var bodyHash = ""
        if (bodyString.isNotEmpty()) {
            val bytes = bodyString.toByteArray(Charsets.UTF_8)
            actualBodyLength = bytes.size.toString()
            val limit = minOf(bytes.size, 102400)
            val subBytes = bytes.copyOfRange(0, limit)
            bodyHash = md5Bytes(subBytes)
        }

        val canonicalPathAndQuery = httpUrl.encodedPath + if (sortedQueryString.isNotEmpty()) "?$sortedQueryString" else ""
        val accept = "application/json"
        val contentType = "application/json;charset=UTF-8"

        val canonicalString = "$method\n$accept\n$contentType\n$actualBodyLength\n$timestamp\n$bodyHash\n$canonicalPathAndQuery"

        var signatureHeader = ""
        try {
            val keyStr = "76iRl07s0xSN9jqmEWAt79EBJZulIQIsV64FZr2O"
            val keyBytes = try {
                Base64.decode(keyStr, Base64.DEFAULT)
            } catch (e: Exception) {
                keyStr.toByteArray()
            }
            val signatureDigest = hmacMd5(keyBytes, canonicalString.toByteArray(Charsets.UTF_8))
            val base64Sig = Base64.encodeToString(signatureDigest, Base64.NO_WRAP)
            signatureHeader = "$timestamp|2|$base64Sig"
        } catch (e: Exception) {
            Log.e("LocalMovieBoxServer", "Error generating signature", e)
        }

        val mediaType = "application/json;charset=UTF-8".toMediaTypeOrNull()
        val requestBody = if (method == "POST" || method == "PUT") {
            RequestBody.create(mediaType, bodyString)
        } else null

        val prefs = context.getSharedPreferences("moviebox_prefs", Context.MODE_PRIVATE)
        val spCode = prefs.getString("sp_code", "")?.takeIf { it.isNotEmpty() } ?: "404"
        val region = prefs.getString("custom_local_iso", "")?.takeIf { it.isNotEmpty() }?.uppercase() ?: "IN"
        val timezone = if (region == "US") "America/New_York" else "Asia/Kolkata"

        val clientInfoMap = mapOf(
            "package_name" to "com.movieboxpro.android",
            "version_name" to "16.2.1",
            "version_code" to 16210,
            "os" to "android",
            "os_version" to "12",
            "install_ch" to "googleplay",
            "device_id" to "8c5da15be6ca34e724a27bc102cd8bcf",
            "install_store" to "googleplay",
            "gaid" to "",
            "brand" to "google",
            "model" to "Pixel 6",
            "system_language" to "en",
            "net" to "wifi",
            "region" to region,
            "timezone" to timezone,
            "sp_code" to spCode
        )
        val clientInfoJson = toJsonString(clientInfoMap)

        val reqBuilder = Request.Builder()
            .url(httpUrl)
            .header("User-Agent", "MovieBoxPro/16.2.1 (Android 14; com.community.mbox.in)")
            .header("Accept", accept)
            .header("Content-Type", contentType)
            .header("X-Sign-Version", "2.0")
            .header("appid", "302770f8bb6543ce8bdff585943a1eca")
            .header("appkey", "a9d263ae575d4f5d94eab086a150c67e")
            .header("region", region)
            .header("lang", "en")
            .header("os", "android")
            .header("X-Timestamp", timestamp)
            .header("Referer", "https://api6.aoneroom.com/")
            .header("X-Client-Token", clientToken)
            .header("x-tr-signature", signatureHeader)
            .header("X-Play-Mode", "2")
            .header("X-Client-Info", clientInfoJson)

        var savedToken = prefs.getString("auth_token", null)
        if (savedToken.isNullOrEmpty()) {
            savedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjcwNjU5NDg0MTAyMTM4MTYyMzIsInV0cCI6MSwiZXhwIjoxNzkxNzMyMjMzLCJpYXQiOjE3ODM5NTU5MzN9.7iyEzTj4vWAbOF0oXwNnZ0p3Nc1QaO6K9eMiGFyVfGs"
        }
        reqBuilder.header("Authorization", "Bearer $savedToken")
        reqBuilder.header("X-Client-Status", "1")

        reqBuilder.method(method, requestBody)
        val builtRequest = reqBuilder.build()

        val response = okHttpClient.newCall(builtRequest).execute()
        val resBody = response.body?.string() ?: ""
        if (isCacheable && resBody.isNotEmpty() && !resBody.contains("Internal Server Error")) {
            val now = System.currentTimeMillis()
            responseCache[cacheKey] = Pair(now, resBody)
            diskCachePrefs.edit()
                .putString(cacheKey, "$now|$resBody")
                .apply()
        }
        return resBody
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { String.format("%02x", it) }
    }

    private fun md5Bytes(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { String.format("%02x", it) }
    }

    private fun hmacMd5(keyBytes: ByteArray, dataBytes: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacMD5")
        val secretKey = SecretKeySpec(keyBytes, "HmacMD5")
        mac.init(secretKey)
        return mac.doFinal(dataBytes)
    }

    private fun getMockTrending(): List<Map<String, Any?>> {
        return listOf(
            mapOf(
                "subjectId" to "11059",
                "id" to "11059",
                "title" to "The Matrix Resurrections",
                "cover" to "https://image.tmdb.org/t/p/w500/8cZ1777vX6Y1RLuSgEpY66i9d6y.jpg",
                "poster" to "https://image.tmdb.org/t/p/w500/8cZ1777vX6Y1RLuSgEpY66i9d6y.jpg",
                "score" to "7.2",
                "releaseTime" to "2021",
                "subjectType" to 1,
                "runtime" to "148 mins",
                "description" to "Return to a world of two realities: one, everyday life; the other, what lies behind it. To find out if his reality is a physical or mental construct, to truly know himself, Mr. Anderson will have to choose to follow the white rabbit once more."
            ),
            mapOf(
                "subjectId" to "10543",
                "id" to "10543",
                "title" to "Spider-Man: No Way Home",
                "cover" to "https://image.tmdb.org/t/p/w500/1g0dhvJI6H9Ygjr6YvA1vGZ6P6V.jpg",
                "poster" to "https://image.tmdb.org/t/p/w500/1g0dhvJI6H9Ygjr6YvA1vGZ6P6V.jpg",
                "score" to "8.7",
                "releaseTime" to "2021",
                "subjectType" to 1,
                "runtime" to "148 mins",
                "description" to "Peter Parker is unmasked and no longer able to separate his normal life from the high-stakes of being a super-hero. When he asks for help from Doctor Strange the stakes become even more dangerous, forcing him to discover what it truly means to be Spider-Man."
            ),
            mapOf(
                "subjectId" to "11382",
                "id" to "11382",
                "title" to "Dune: Part Two",
                "cover" to "https://image.tmdb.org/t/p/w500/cz062Sg88Vv6RAsmY8g8b0GgX6n.jpg",
                "poster" to "https://image.tmdb.org/t/p/w500/cz062Sg88Vv6RAsmY8g8b0GgX6n.jpg",
                "score" to "8.9",
                "releaseTime" to "2024",
                "subjectType" to 1,
                "runtime" to "166 mins",
                "description" to "Follow the mythic journey of Paul Atreides as he unites with Chani and the Fremen while on a path of revenge against the conspirators who destroyed his family."
            ),
            mapOf(
                "subjectId" to "11500",
                "id" to "11500",
                "title" to "Oppenheimer",
                "cover" to "https://image.tmdb.org/t/p/w500/8Gxv2gSjdhY7WbgSg1f07st662B.jpg",
                "poster" to "https://image.tmdb.org/t/p/w500/8Gxv2gSjdhY7WbgSg1f07st662B.jpg",
                "score" to "8.6",
                "releaseTime" to "2023",
                "subjectType" to 1,
                "runtime" to "180 mins",
                "description" to "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb."
            ),
            mapOf(
                "subjectId" to "11650",
                "id" to "11650",
                "title" to "Interstellar",
                "cover" to "https://image.tmdb.org/t/p/w500/gEU2Qv6Xg77DJ9v3vG6mI80m2fN.jpg",
                "poster" to "https://image.tmdb.org/t/p/w500/gEU2Qv6Xg77DJ9v3vG6mI80m2fN.jpg",
                "score" to "8.7",
                "releaseTime" to "2014",
                "subjectType" to 1,
                "runtime" to "169 mins",
                "description" to "The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel and conquer the vast distances involved in an interstellar voyage."
            )
        )
    }

    private fun toJsonString(obj: Any?): String {
        return when (obj) {
            is Map<*, *> -> {
                val json = org.json.JSONObject()
                obj.forEach { (key, value) ->
                    if (key != null) {
                        json.put(key.toString(), toJsonElement(value))
                    }
                }
                json.toString()
            }
            is List<*> -> {
                val array = org.json.JSONArray()
                obj.forEach { item ->
                    array.put(toJsonElement(item))
                }
                array.toString()
            }
            else -> org.json.JSONObject.wrap(obj)?.toString() ?: "null"
        }
    }

    private fun toJsonElement(obj: Any?): Any {
        return when (obj) {
            is Map<*, *> -> {
                val json = org.json.JSONObject()
                obj.forEach { (key, value) ->
                    if (key != null) {
                        json.put(key.toString(), toJsonElement(value))
                    }
                }
                json
            }
            is List<*> -> {
                val array = org.json.JSONArray()
                obj.forEach { item ->
                    array.put(toJsonElement(item))
                }
                array
            }
            null -> org.json.JSONObject.NULL
            else -> obj
        }
    }

    private fun parseJsonToMap(jsonStr: String): Map<String, Any?> {
        if (jsonStr.trim().isEmpty()) return emptyMap()
        return try {
            val json = org.json.JSONObject(jsonStr)
            jsonToMap(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun jsonToMap(json: org.json.JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.get(key)
            map[key] = when (value) {
                is org.json.JSONObject -> jsonToMap(value)
                is org.json.JSONArray -> jsonToList(value)
                org.json.JSONObject.NULL -> null
                else -> value
            }
        }
        return map
    }

    private fun jsonToList(array: org.json.JSONArray): List<Any?> {
        val list = mutableListOf<Any?>()
        for (i in 0 until array.length()) {
            val value = array.get(i)
            list.add(when (value) {
                is org.json.JSONObject -> jsonToMap(value)
                is org.json.JSONArray -> jsonToList(value)
                org.json.JSONObject.NULL -> null
                else -> value
            })
        }
        return list
    }
}
