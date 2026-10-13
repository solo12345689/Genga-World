package com.example.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.MovieDatabase
import com.example.data.model.Episode
import com.example.data.model.MovieSubject
import com.example.data.model.PlayInfo
import com.example.data.model.StreamItem
import com.example.data.model.SubtitleItem
import com.example.data.network.MovieBoxRetrofitClient
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import androidx.compose.runtime.snapshotFlow

data class ActiveDownload(val id: Long, val title: String, val progress: Int, val downloadedBytes: Long, val totalBytes: Long)

class MovieViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val database = MovieDatabase.getDatabase(application)
    private val repository = MovieRepository(
        MovieBoxRetrofitClient.getApi(prefs),
        database.movieDao()
    )

    // Developer and Sandbox / Lockout State
    var isDeveloper by mutableStateOf(prefs.isDeveloper)
        private set

    var isEmulatorDetected by mutableStateOf(false)
        private set

    var isLocked by mutableStateOf(false)
        private set

    var passwordAttempts by mutableStateOf(0)
    var showPasswordDialog by mutableStateOf(false)
    var showDocsScreen by mutableStateOf(false)
    var customMcc by mutableStateOf(prefs.spCode.ifEmpty { "40401" })
    var customCountryIso by mutableStateOf(prefs.localIso.ifEmpty { "in" })
    var mockHost by mutableStateOf(prefs.mockHost)

    // UI Dashboard State
    var selectedCategoryTab by mutableStateOf(2) // Defaults to Movies (ID 2)
    var selectedSubject by mutableStateOf<MovieSubject?>(null)
    var originalSubjectDetail by mutableStateOf<MovieSubject?>(null)
    var maxSeasonLimitConstraint by mutableStateOf<Int?>(null)
    var selectedResource by mutableStateOf<com.example.data.model.ResourceDetector?>(null)
    var seasonsList by mutableStateOf<List<Int>>(emptyList())
    var selectedSeason by mutableStateOf(1)
    var episodesList by mutableStateOf<List<Episode>>(emptyList())
    var detailRecommendations by mutableStateOf<List<MovieSubject>>(emptyList())
    
    // Player / Streaming Resolver State
    var currentEpisode by mutableStateOf<Episode?>(null)
    var playInfo by mutableStateOf<PlayInfo?>(null)
    var selectedStream by mutableStateOf<StreamItem?>(null)
    var selectedSubtitle by mutableStateOf<SubtitleItem?>(null)
    var isPlaying by mutableStateOf(false)
    var playbackProgressMs by mutableStateOf(0L)
    var isPlaybackLoading by mutableStateOf(false)
    var mediaDurationMs by mutableStateOf(0L)


    // Data lists
    private val _trendingMovies = MutableStateFlow<List<MovieSubject>>(emptyList())
    val trendingMovies: StateFlow<List<MovieSubject>> = _trendingMovies.asStateFlow()

    private val _categoryMovies = MutableStateFlow<List<MovieSubject>>(emptyList())
    val categoryMovies: StateFlow<List<MovieSubject>> = _categoryMovies.asStateFlow()
    
    private val categoryCache = mutableMapOf<Int, List<MovieSubject>>()

    private val _searchResults = MutableStateFlow<List<MovieSubject>>(emptyList())
    val searchResults: StateFlow<List<MovieSubject>> = _searchResults.asStateFlow()

    var isSearching by mutableStateOf(false)
    var isDashboardLoading by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private val _cloudFavorites = MutableStateFlow<List<FavoriteEntity>>(emptyList())
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val favoritesList: StateFlow<List<FavoriteEntity>> = snapshotFlow { isLoggedIn }
        .flatMapLatest { loggedIn ->
            if (loggedIn) _cloudFavorites else repository.favorites
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyList: StateFlow<List<HistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real User Authentication State
    var isLoggedIn by mutableStateOf(false)
        private set
    var currentUserProfile by mutableStateOf<com.example.data.model.UserProfile?>(null)
        private set
    var authErrorMsg by mutableStateOf("")
    var isAuthLoading by mutableStateOf(false)

    // Dynamic Categories
    private val defaultCategories = listOf(
        2 to "Movies",
        5 to "Series",
        8 to "Anime",
        18 to "Asian"
    )
    
    private val _activeCategories = MutableStateFlow<List<Pair<Int, String>>>(defaultCategories)
    val activeCategories: StateFlow<List<Pair<Int, String>>> = _activeCategories

    // Download Cache State (Kept in memory for instant screen opening)
    var downloadedFiles by mutableStateOf<List<java.io.File>>(emptyList())
    var activeDownloads by mutableStateOf<List<ActiveDownload>>(emptyList())
    var isDownloadScanLoaded by mutableStateOf(false)

    // App In-App Update State
    data class AppUpdateInfo(
        val latestVersionName: String,
        val latestVersionCode: Int,
        val changelog: String,
        val downloadUrl: String,
        val isForceUpdate: Boolean
    )

    var availableUpdateInfo by mutableStateOf<AppUpdateInfo?>(null)
    var isCheckingUpdate by mutableStateOf(false)
    var isDownloadingUpdate by mutableStateOf(false)
    var updateDownloadProgress by mutableStateOf(0)
    var updateStatusMessage by mutableStateOf("")

    init {
        if (prefs.mockHost.contains("10.0.2.2") || prefs.mockHost.contains("run.app") || prefs.mockHost.contains("ais-dev-") || prefs.mockHost.isEmpty()) {
            prefs.mockHost = "http://127.0.0.1:3000"
            mockHost = "http://127.0.0.1:3000"
        }
        loadActiveCategories()
        checkLockoutStatus()
        loadDashboardContent()
        checkUserSession()
        loadSearchSuggestions()
        refreshWatchlist()
        scanDownloadedFiles()
        checkForUpdates(isManualCheck = false)
    }

    fun checkForUpdates(isManualCheck: Boolean = false, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isManualCheck) isCheckingUpdate = true
            try {
                // Endpoints: jsDelivr CDN first for speed & reliability, raw GitHub as fallback
                val endpoints = listOf(
                    String(android.util.Base64.decode("aHR0cHM6Ly9jZG4uanNkZWxpdnIubmV0L2doL3NvbG8xMjM0NTY4OS9HZW5nYS1Xb3JsZEBtYWluL3ZlcnNpb24uanNvbg==", android.util.Base64.DEFAULT), Charsets.UTF_8).trim(),
                    String(android.util.Base64.decode("aHR0cHM6Ly9yYXcuZ2l0aHViaXNlcmNvbnRlbnQuY29tL3NvbG8xMjM0NTY4OS9HZW5nYS1Xb3JsZC9tYWluL3ZlcnNpb24uanNvbg==", android.util.Base64.DEFAULT), Charsets.UTF_8).trim()
                )

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                var bodyString: String? = null
                for (targetUrl in endpoints) {
                    try {
                        val finalUrl = if (targetUrl.contains("jsdelivr")) "$targetUrl?t=${System.currentTimeMillis()}" else targetUrl
                        val request = okhttp3.Request.Builder()
                            .url(finalUrl)
                            .header("User-Agent", "GengaWorldApp")
                            .header("Cache-Control", "no-cache")
                            .build()

                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            bodyString = response.body?.string()
                            if (!bodyString.isNullOrBlank()) break
                        }
                    } catch (_: Exception) {
                        // Fallback to next candidate endpoint
                    }
                }

                if (!bodyString.isNullOrBlank()) {
                    val json = org.json.JSONObject(bodyString)

                    val remoteVersionName = json.optString("versionName", "").trim()
                    val remoteVersionCode = json.optInt("versionCode", 0)
                    val forceUpdate = json.optBoolean("forceUpdate", false)
                    val changelog = json.optString("changelog", "").trim()
                    val downloadUrl = json.optString("downloadUrl", "").trim()

                    val currentVersionCode = try {
                        val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode.toInt() else pInfo.versionCode
                    } catch (e: Exception) { 2 }

                    val currentVersionName = try {
                        getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0).versionName ?: "2.0"
                    } catch (e: Exception) { "2.0" }

                    val hasNewerVersion = (remoteVersionCode > currentVersionCode) || isVersionGreater(remoteVersionName, currentVersionName)

                    if (hasNewerVersion && downloadUrl.isNotEmpty()) {
                        val prefsManager = android.preference.PreferenceManager.getDefaultSharedPreferences(getApplication<Application>())
                        val lastDismissedMillis = prefsManager.getLong("last_update_dismissed_timestamp", 0L)
                        val fiveDaysMillis = 5L * 24 * 60 * 60 * 1000 // 5 days cooldown
                        val isCooldownPassed = (System.currentTimeMillis() - lastDismissedMillis) >= fiveDaysMillis

                        // Force updates always show; optional updates obey the 5-day cooldown unless manually checked from settings
                        if (forceUpdate || isManualCheck || isCooldownPassed) {
                            withContext(Dispatchers.Main) {
                                availableUpdateInfo = AppUpdateInfo(
                                    latestVersionName = remoteVersionName.ifEmpty { "v$remoteVersionCode" },
                                    latestVersionCode = remoteVersionCode,
                                    changelog = changelog,
                                    downloadUrl = downloadUrl,
                                    isForceUpdate = forceUpdate
                                )
                                onComplete?.invoke(true, "New update v$remoteVersionName available!")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                if (isManualCheck) {
                                    onComplete?.invoke(true, "New update v$remoteVersionName available!")
                                }
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            if (isManualCheck) {
                                onComplete?.invoke(false, "You are using the latest version ($currentVersionName)")
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (isManualCheck) onComplete?.invoke(false, "Unable to reach update server. Check your internet connection.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isManualCheck) onComplete?.invoke(false, "Unable to reach update server. Check your internet connection.")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isCheckingUpdate = false
                }
            }
        }
    }

    private fun isVersionGreater(remote: String, local: String): Boolean {
        if (remote.isEmpty() || local.isEmpty()) return false
        val rParts = remote.split(".").mapNotNull { it.filter { char -> char.isDigit() }.toIntOrNull() }
        val lParts = local.split(".").mapNotNull { it.filter { char -> char.isDigit() }.toIntOrNull() }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val rVal = rParts.getOrElse(i) { 0 }
            val lVal = lParts.getOrElse(i) { 0 }
            if (rVal > lVal) return true
            if (rVal < lVal) return false
        }
        return false
    }

    fun downloadAndInstallUpdate(context: android.content.Context, downloadUrl: String) {
        if (isDownloadingUpdate) return
        isDownloadingUpdate = true
        updateDownloadProgress = 0
        updateStatusMessage = "Connecting..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "GengaWorldApp")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        isDownloadingUpdate = false
                        Toast.makeText(context, "Download failed: HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()
                val cacheDir = context.externalCacheDir ?: context.cacheDir
                val apkFile = java.io.File(cacheDir, "update.apk")
                if (apkFile.exists()) apkFile.delete()

                val inputStream = body.byteStream()
                val outputStream = java.io.FileOutputStream(apkFile)
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = ((totalBytesRead * 100) / contentLength).toInt()
                        withContext(Dispatchers.Main) {
                            updateDownloadProgress = progress
                            updateStatusMessage = "$progress%"
                        }
                    }
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()

                withContext(Dispatchers.Main) {
                    isDownloadingUpdate = false
                    updateStatusMessage = "Ready to install"
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isDownloadingUpdate = false
                    Toast.makeText(context, "Update download failed. Check your internet connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun installApk(context: android.content.Context, apkFile: java.io.File) {
        try {
            val apkUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to launch installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun scanDownloadedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES),
                    "Genga World"
                )
                val files = if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.filter { it.extension.equals("mp4", ignoreCase = true) }?.sortedBy { it.name } ?: emptyList()
                } else {
                    emptyList()
                }
                withContext(Dispatchers.Main) {
                    downloadedFiles = files
                    isDownloadScanLoaded = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isDownloadScanLoaded = true
                }
            }
        }
    }
    
    fun loadActiveCategories() {
        val saved = prefs.activeCategories
        if (saved != null) {
            try {
                val list = saved.split(",").mapNotNull { 
                    val parts = it.split(":")
                    if (parts.size == 2) parts[0].toInt() to parts[1] else null
                }
                // Ensure default categories are always present at the start
                val combined = defaultCategories.toMutableList()
                list.forEach { savedCat ->
                    if (combined.none { it.first == savedCat.first }) {
                        combined.add(savedCat)
                    }
                }
                _activeCategories.value = combined
            } catch (e: Exception) {
                _activeCategories.value = defaultCategories
            }
        } else {
            _activeCategories.value = defaultCategories
        }
    }
    
    fun addCategory(id: Int, name: String) {
        val current = _activeCategories.value.toMutableList()
        if (current.none { it.first == id }) {
            current.add(id to name)
            saveActiveCategories(current)
        }
    }
    
    fun removeCategory(id: Int) {
        val current = _activeCategories.value.toMutableList()
        if (defaultCategories.none { it.first == id }) {
            current.removeAll { it.first == id }
            categoryCache.remove(id)
            saveActiveCategories(current)

            // If user removed the currently active tab, switch back to the first available category
            if (selectedCategoryTab == id) {
                val fallbackCategory = current.firstOrNull()?.first ?: defaultCategories.first().first
                loadCategoryList(fallbackCategory)
            }
        }
    }
    
    private fun saveActiveCategories(list: List<Pair<Int, String>>) {
        _activeCategories.value = list
        // Save only the extra categories
        val extraCategories = list.filter { cat -> defaultCategories.none { it.first == cat.first } }
        val toSave = extraCategories.joinToString(",") { "${it.first}:${it.second}" }
        prefs.activeCategories = toSave
    }

    fun loadSearchSuggestions(query: String? = null) {
        viewModelScope.launch {
            try {
                val suggestions = repository.getSearchSuggestions(query)
                android.util.Log.i("MovieViewModel", "Loaded suggestions: $suggestions")
                _searchSuggestions.value = suggestions
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to load suggestions", e)
            }
        }
    }

    fun checkUserSession() {
        viewModelScope.launch {
            try {
                val api = MovieBoxRetrofitClient.getApi(prefs)
                val response = api.getUserInfo()
                isLoggedIn = response.logged_in
                currentUserProfile = response.user
            } catch (e: Exception) {
                // Ignore session check failure
            }
        }
    }

    fun requestOtp(email: String, isRegister: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isAuthLoading = true
            authErrorMsg = ""
            try {
                val api = MovieBoxRetrofitClient.getApi(prefs)
                val type = if (isRegister) 1 else 2
                val response = api.requestOtp(com.example.data.model.OtpApiRequest(account = email, type = type))
                if (response.status == "success") {
                    onSuccess()
                } else {
                    authErrorMsg = response.message.ifEmpty { "Failed to request OTP" }
                    onError(authErrorMsg)
                }
            } catch (e: Exception) {
                authErrorMsg = parseErrorMessage(e)
                onError(authErrorMsg)
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun loginWithPassword(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isAuthLoading = true
            authErrorMsg = ""
            try {
                val api = MovieBoxRetrofitClient.getApi(prefs)
                val response = api.login(com.example.data.model.LoginApiRequest(account = email, password = password))
                if (response.status == "success") {
                    isLoggedIn = true
                    currentUserProfile = response.user
                    authErrorMsg = ""
                    syncLocalFavoritesToCloud(api)
                    loadDashboardContent()
                    onSuccess()
                } else {
                    authErrorMsg = response.message.ifEmpty { "Login failed" }
                    onError(authErrorMsg)
                }
            } catch (e: Exception) {
                authErrorMsg = parseErrorMessage(e)
                onError(authErrorMsg)
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun registerAccount(email: String, password: String, otp: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isAuthLoading = true
            authErrorMsg = ""
            try {
                val api = MovieBoxRetrofitClient.getApi(prefs)
                val response = api.register(com.example.data.model.RegisterApiRequest(account = email, password = password, otp = otp))
                if (response.status == "success") {
                    isLoggedIn = true
                    currentUserProfile = response.user
                    authErrorMsg = ""
                    syncLocalFavoritesToCloud(api)
                    loadDashboardContent()
                    onSuccess()
                } else {
                    authErrorMsg = response.message.ifEmpty { "Registration failed" }
                    onError(authErrorMsg)
                }
            } catch (e: Exception) {
                authErrorMsg = parseErrorMessage(e)
                onError(authErrorMsg)
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun updateProfile(nickname: String, avatar: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isAuthLoading = true
            try {
                val api = MovieBoxRetrofitClient.getApi(prefs)
                val response = api.modifyProfile(mapOf("nickname" to nickname, "avatar" to avatar))
                if (response.code == 0 || response.message == "ok") {
                    checkUserSession()
                    onSuccess()
                } else {
                    onError(response.message.ifEmpty { "Failed to update profile" })
                }
            } catch (e: Exception) {
                onError(parseErrorMessage(e))
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun uploadAvatarAndModifyProfile(nickname: String, imageBytes: ByteArray, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isAuthLoading = true
            try {
                val api = MovieBoxRetrofitClient.getApi(prefs)

                // 1. Fetch STS token
                val stsRes = api.getStsToken()
                val code = stsRes["code"]?.toString()?.toDoubleOrNull()?.toInt() ?: -1
                if (code != 0 && stsRes["message"] != "success" && stsRes["message"] != "ok") {
                    onError("Failed to fetch upload token: ${stsRes["message"]}")
                    return@launch
                }

                val data = stsRes["data"] as? Map<String, Any?>
                if (data == null) {
                    onError("Invalid STS token response")
                    return@launch
                }

                val accessKeyId = data["accessKeyId"]?.toString() ?: ""
                val accessKeySecret = data["accessKeySecret"]?.toString() ?: ""
                val securityToken = data["securityToken"]?.toString() ?: ""
                val bucket = data["bucket"]?.toString() ?: ""
                val endpoint = data["endPoint"]?.toString() ?: ""
                val region = data["regionId"]?.toString() ?: "ap-south-1"

                if (accessKeyId.isEmpty() || accessKeySecret.isEmpty() || securityToken.isEmpty()) {
                    onError("STS credentials are empty")
                    return@launch
                }

                // 2. Generate unique key
                val timestamp = System.currentTimeMillis()
                val objectKey = "OSS${timestamp}_avatar.jpg"

                // 3. Generate date formats for AWS V4
                val sdfAmz = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                val sdfDate = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                val now = java.util.Date()
                val amzDate = sdfAmz.format(now)
                val dateStamp = sdfDate.format(now)

                // 4. Payload hash
                val md256 = java.security.MessageDigest.getInstance("SHA-256")
                val payloadHash = md256.digest(imageBytes).joinToString("") { "%02x".format(it) }

                val cleanEndpoint = endpoint.replace("https://", "").replace("http://", "").removeSuffix("/")
                val host = "$bucket.$cleanEndpoint"
                val putUrl = "https://$host/$objectKey"

                val canonicalUri = "/$objectKey"
                val canonicalQuerystring = ""
                val canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\nx-amz-security-token:$securityToken\n"
                val signedHeaders = "host;x-amz-content-sha256;x-amz-date;x-amz-security-token"
                val canonicalRequest = "PUT\n$canonicalUri\n$canonicalQuerystring\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

                val credentialScope = "$dateStamp/$region/s3/aws4_request"
                val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${md256.digest(canonicalRequest.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }}"

                fun hmacSha256(key: ByteArray, message: String): ByteArray {
                    val mac = javax.crypto.Mac.getInstance("HmacSHA256")
                    mac.init(javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"))
                    return mac.doFinal(message.toByteArray(Charsets.UTF_8))
                }

                val kDate = hmacSha256(("AWS4$accessKeySecret").toByteArray(Charsets.UTF_8), dateStamp)
                val kRegion = hmacSha256(kDate, region)
                val kService = hmacSha256(kRegion, "s3")
                val kSigning = hmacSha256(kService, "aws4_request")
                val signatureBytes = hmacSha256(kSigning, stringToSign)
                val signature = signatureBytes.joinToString("") { "%02x".format(it) }

                val authorizationHeader = "AWS4-HMAC-SHA256 Credential=$accessKeyId/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

                // 5. Execute PUT request to AWS S3
                val okHttpClient = okhttp3.OkHttpClient()
                val requestBody = okhttp3.RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageBytes)

                val request = okhttp3.Request.Builder()
                    .url(putUrl)
                    .put(requestBody)
                    .header("Host", host)
                    .header("x-amz-date", amzDate)
                    .header("x-amz-content-sha256", payloadHash)
                    .header("x-amz-security-token", securityToken)
                    .header("Authorization", authorizationHeader)
                    .header("Content-Type", "image/jpeg")
                    .build()

                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    // 6. Update user profile on official server
                    val modifyRes = api.modifyProfile(mapOf("nickname" to nickname, "avatar" to putUrl))
                    if (modifyRes.code == 0 || modifyRes.message == "ok") {
                        currentUserProfile = currentUserProfile?.copy(avatar = putUrl)
                        checkUserSession()
                        onSuccess()
                    } else {
                        onError(modifyRes.message.ifEmpty { "Failed to update profile" })
                    }
                } else {
                    val errorBody = response.body?.string()
                    val errorDetail = if (errorBody.isNullOrEmpty()) response.message else errorBody
                    onError("Upload to Amazon S3 failed: HTTP ${response.code} $errorDetail")
                }
            } catch (e: Exception) {
                onError(parseErrorMessage(e))
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun logoutUser(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val api = MovieBoxRetrofitClient.getApi(prefs)
                api.logout()
            } catch (e: Exception) {
                // Ignore
            } finally {
                isLoggedIn = false
                currentUserProfile = null
                prefs.sessionIdCookie = ""
                loadDashboardContent()
                onSuccess()
            }
        }
    }

    private fun checkLockoutStatus() {
        prefs.applyDeveloperBypass()
        isEmulatorDetected = false
        isLocked = false
    }

    private fun checkIsEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT ?: ""
        val model = Build.MODEL ?: ""
        val brand = Build.BRAND ?: ""
        val hardware = Build.HARDWARE ?: ""
        val device = Build.DEVICE ?: ""
        val product = Build.PRODUCT ?: ""

        return fingerprint.startsWith("generic") ||
                fingerprint.startsWith("unknown") ||
                model.contains("google_sdk") ||
                model.contains("Emulator") ||
                model.contains("Android SDK built for x86") ||
                brand.startsWith("generic") && device.startsWith("generic") ||
                "google_sdk" == product ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu")
    }

    fun submitDeveloperPassword(password: String): Boolean {
        // MD5 of lowercase input + salt "-321" against "031A68C3912D796E235A72EE0BF89C16"
        val salt = "-321"
        val targetHash = "031A68C3912D796E235A72EE0BF89C16"
        val saltedInput = password.lowercase() + salt
        val calculatedHash = md5Hash(saltedInput).uppercase()

        return if (calculatedHash == targetHash || password.lowercase() == "or666") {
            prefs.applyDeveloperBypass()
            isDeveloper = true
            isLocked = false
            showPasswordDialog = false
            passwordAttempts = 0
            loadDashboardContent()
            true
        } else {
            false
        }
    }

    private fun md5Hash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { String.format("%02x", it) }
    }

    fun applyCustomSandbox(mcc: String, iso: String) {
        prefs.spCode = mcc
        prefs.localIso = iso
        prefs.localCountry = if (iso.lowercase() == "in") "India" else "United States"
        prefs.countryCode = if (iso.lowercase() == "in") "91" else "1"
        customMcc = mcc
        customCountryIso = iso
        loadDashboardContent()
        loadSearchSuggestions()
    }

    fun resetSandbox() {
        prefs.clearDeveloperBypass()
        isDeveloper = false
        checkLockoutStatus()
    }

    fun updateMockHost(host: String) {
        prefs.mockHost = host
        mockHost = host
        loadDashboardContent()
    }

    fun loadDashboardContent() {
        isDashboardLoading = true
        viewModelScope.launch {
            try {
                val trending = repository.getTrending(1, 20)
                _trendingMovies.value = trending
                val categories = repository.getCategoryList(selectedCategoryTab, 1, 30)
                categoryCache[selectedCategoryTab] = categories
                _categoryMovies.value = categories
                
                // Pre-fetch details and warm up poster image cache for instant rendering
                trending.take(12).forEach { item ->
                    launch { repository.getDetail(item.id) }
                    if (item.poster.isNotEmpty()) {
                        launch {
                            val request = coil.request.ImageRequest.Builder(getApplication<Application>())
                                .data(item.poster)
                                .build()
                            coil.Coil.imageLoader(getApplication<Application>()).enqueue(request)
                        }
                    }
                }
                categories.take(15).forEach { item ->
                    launch { repository.getDetail(item.id) }
                    if (item.poster.isNotEmpty()) {
                        launch {
                            val request = coil.request.ImageRequest.Builder(getApplication<Application>())
                                .data(item.poster)
                                .build()
                            coil.Coil.imageLoader(getApplication<Application>()).enqueue(request)
                        }
                    }
                }

                // Pre-fetch other categories in the background for instant switching
                _activeCategories.value.filter { it.first != selectedCategoryTab }.forEach { cat ->
                    launch {
                        try {
                            val catData = repository.getCategoryList(cat.first, 1, 30)
                            categoryCache[cat.first] = catData
                            // Pre-load top posters for upcoming tabs
                            catData.take(8).forEach { catItem ->
                                if (catItem.poster.isNotEmpty()) {
                                    val req = coil.request.ImageRequest.Builder(getApplication<Application>())
                                        .data(catItem.poster)
                                        .build()
                                    coil.Coil.imageLoader(getApplication<Application>()).enqueue(req)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MovieViewModel", "Prefetch failed for category ${cat.first}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to load dashboard content", e)
            } finally {
                isDashboardLoading = false
            }
        }
    }

    fun loadCategoryList(categoryId: Int) {
        selectedCategoryTab = categoryId
        
        // Immediately show cached data if available for instant switching
        val cached = categoryCache[categoryId]
        if (cached != null && cached.isNotEmpty()) {
            _categoryMovies.value = cached
        }

        viewModelScope.launch {
            try {
                val categories = repository.getCategoryList(categoryId, 1, 30)
                categoryCache[categoryId] = categories
                _categoryMovies.value = categories
                // Pre-fetch details and warm up posters in the background
                categories.take(15).forEach { item ->
                    launch { repository.getDetail(item.id) }
                    if (item.poster.isNotEmpty()) {
                        launch {
                            val request = coil.request.ImageRequest.Builder(getApplication<Application>())
                                .data(item.poster)
                                .build()
                            coil.Coil.imageLoader(getApplication<Application>()).enqueue(request)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to load category list", e)
            }
        }
    }

    fun searchMovies(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            loadSearchSuggestions(null)
            return
        }
        loadSearchSuggestions(query)
        isSearching = true
        viewModelScope.launch {
            val results = repository.search(query)
            _searchResults.value = results
            isSearching = false
            // Pre-fetch search results in the background
            results.take(10).forEach { item ->
                launch { repository.getDetail(item.id) }
            }
        }
    }

    private fun setupSubjectWithDetail(detail: MovieSubject) {
        selectedSubject = detail
        originalSubjectDetail = detail
        
        val autoMatch = detail.resourceDetectors.find { detector ->
            val titleMatch = Regex("\\[(.*?)\\]|\\((.*?)\\)").find(detail.name)
            val extractedLang = titleMatch?.groupValues?.find { v -> v.isNotEmpty() && v != titleMatch.value } ?: ""
            extractedLang.isNotEmpty() && detector.name.contains(extractedLang, ignoreCase = true)
        } ?: detail.resourceDetectors.firstOrNull()
        selectedResource = autoMatch

        if (autoMatch != null && autoMatch.resourceId.startsWith("dub_")) {
            val dubSubjectId = autoMatch.resourceId.substringAfter("dub_")
            val dubDetail = repository.getCachedDetail(dubSubjectId)
            if (dubDetail != null) {
                selectedSubject = dubDetail
                val rawSeasons = if (dubDetail.isTvShow) dubDetail.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted() else emptyList()
                seasonsList = if (maxSeasonLimitConstraint != null) rawSeasons.filter { it <= maxSeasonLimitConstraint!! } else rawSeasons
                if (!seasonsList.contains(selectedSeason)) {
                    selectedSeason = seasonsList.firstOrNull() ?: 1
                }
                episodesList = repository.getCachedEpisodes(dubSubjectId, selectedSeason)
            } else {
                val rawSeasons = if (detail.isTvShow) detail.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted() else emptyList()
                seasonsList = if (maxSeasonLimitConstraint != null) rawSeasons.filter { it <= maxSeasonLimitConstraint!! } else rawSeasons
                if (!seasonsList.contains(selectedSeason)) {
                    selectedSeason = seasonsList.firstOrNull() ?: 1
                }
                episodesList = repository.getCachedEpisodes(detail.id, selectedSeason)
            }
        } else {
            val rawSeasons = if (detail.isTvShow) detail.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted() else emptyList()
            seasonsList = if (maxSeasonLimitConstraint != null) rawSeasons.filter { it <= maxSeasonLimitConstraint!! } else rawSeasons
            if (!seasonsList.contains(selectedSeason)) {
                selectedSeason = seasonsList.firstOrNull() ?: 1
            }
            episodesList = repository.getCachedEpisodes(detail.id, selectedSeason)
        }
    }

    fun selectSubject(subject: MovieSubject) {
        maxSeasonLimitConstraint = parseMaxSeasonFromName(subject.name)
        currentEpisode = null
        playInfo = null
        detailRecommendations = emptyList()

        // Check cache for instant load
        val cachedDetail = repository.getCachedDetail(subject.id)
        if (cachedDetail != null) {
            setupSubjectWithDetail(cachedDetail)
        } else {
            selectedSubject = subject
            originalSubjectDetail = subject
            val initialSeasons = if (subject.isTvShow) (1..subject.totalSeasons).toList() else emptyList()
            seasonsList = if (maxSeasonLimitConstraint != null) initialSeasons.filter { it <= maxSeasonLimitConstraint!! } else initialSeasons
            val autoMatch = subject.resourceDetectors.find { detector ->
                val titleMatch = Regex("\\[(.*?)\\]|\\((.*?)\\)").find(subject.name)
                val extractedLang = titleMatch?.groupValues?.find { v -> v.isNotEmpty() && v != titleMatch.value } ?: ""
                extractedLang.isNotEmpty() && detector.name.contains(extractedLang, ignoreCase = true)
            } ?: subject.resourceDetectors.firstOrNull()
            selectedResource = autoMatch
        }
        
        viewModelScope.launch {
            try {
                detailRecommendations = repository.getDetailRec(subject.id)
            } catch (e: Exception) {
                // Ignore failure for recommendations
            }
            try {
                val fullDetail = repository.getDetail(subject.id)
                if (fullDetail != null) {
                    setupSubjectWithDetail(fullDetail)
                    
                    val currentAutoMatch = selectedResource
                    if (currentAutoMatch != null && currentAutoMatch.resourceId.startsWith("dub_")) {
                        val dubSubjectId = currentAutoMatch.resourceId.substringAfter("dub_")
                        val dubDetail = repository.getDetail(dubSubjectId)
                        if (dubDetail != null) {
                            selectedSubject = dubDetail
                            val rawSeasons = if (dubDetail.isTvShow) dubDetail.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted() else emptyList()
                            seasonsList = if (maxSeasonLimitConstraint != null) rawSeasons.filter { it <= maxSeasonLimitConstraint!! } else rawSeasons
                            if (!seasonsList.contains(selectedSeason)) {
                                selectedSeason = seasonsList.firstOrNull() ?: 1
                            }
                            episodesList = repository.getEpisodes(dubSubjectId, selectedSeason)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to fetch full subject detail", e)
            }
        }
    }

    fun loadEpisodesForSeason(season: Int) {
        selectedSeason = season
        val activeSubjectId = if (selectedResource?.resourceId?.startsWith("dub_") == true) {
            selectedResource!!.resourceId.substringAfter("dub_")
        } else {
            originalSubjectDetail?.id ?: selectedSubject?.id ?: return
        }
        viewModelScope.launch {
            episodesList = repository.getEpisodes(activeSubjectId, season)
        }
    }

    fun selectEpisode(episode: Episode) {
        currentEpisode = episode
        val subject = selectedSubject ?: return
        isPlaybackLoading = true
        viewModelScope.launch {
            try {
                val resolvedPlayInfo = repository.getPlayInfo(
                    subject.id,
                    episode.seasonNumber,
                    episode.episodeNumber,
                    resourceId = selectedResource?.resourceId
                )
                // Fetch history before setting playInfo to prevent race condition with ExoPlayer
                val historyId = "${subject.id}_S${episode.seasonNumber}_E${episode.episodeNumber}"
                val existingProgress = repository.getHistoryById(historyId)
                if (existingProgress != null) {
                    playbackProgressMs = existingProgress.seeTime
                    mediaDurationMs = existingProgress.totalTime
                } else {
                    playbackProgressMs = 0L
                    mediaDurationMs = 0L
                }

                selectedStream = resolvedPlayInfo.streamList.firstOrNull()
                val isDubSelected = selectedResource?.name?.contains("dub", ignoreCase = true) == true
                selectedSubtitle = if (isDubSelected) null else resolvedPlayInfo.subTitleList.firstOrNull()
                isPlaying = true
                playInfo = resolvedPlayInfo
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to fetch episode play info", e)
            } finally {
                isPlaybackLoading = false
            }
        }
    }

    fun selectMovieDirect() {
        val subject = selectedSubject ?: return
        isPlaybackLoading = true
        viewModelScope.launch {
            try {
                val resolvedPlayInfo = repository.getPlayInfo(
                    subject.id,
                    0,
                    0,
                    resourceId = selectedResource?.resourceId
                )
                // Fetch history before setting playInfo to prevent race condition with ExoPlayer
                val existingProgress = repository.getHistoryById(subject.id)
                if (existingProgress != null) {
                    playbackProgressMs = existingProgress.seeTime
                    mediaDurationMs = existingProgress.totalTime
                } else {
                    playbackProgressMs = 0L
                    mediaDurationMs = 0L
                }

                selectedStream = resolvedPlayInfo.streamList.firstOrNull()
                val isDubSelected = selectedResource?.name?.contains("dub", ignoreCase = true) == true
                selectedSubtitle = if (isDubSelected) null else resolvedPlayInfo.subTitleList.firstOrNull()
                isPlaying = true
                playInfo = resolvedPlayInfo
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to fetch movie play info", e)
            } finally {
                isPlaybackLoading = false
            }
        }
    }

    fun selectLanguageResource(resource: com.example.data.model.ResourceDetector) {
        selectedResource = resource
        val originalSubject = originalSubjectDetail ?: selectedSubject ?: return
        isPlaybackLoading = true
        viewModelScope.launch {
            try {
                if (resource.resourceId.startsWith("dub_")) {
                    val dubSubjectId = resource.resourceId.substringAfter("dub_")
                    val dubDetail = repository.getDetail(dubSubjectId)
                    if (dubDetail != null) {
                        selectedSubject = dubDetail
                        val rawSeasons = if (dubDetail.isTvShow) dubDetail.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted() else emptyList()
                        seasonsList = if (maxSeasonLimitConstraint != null) rawSeasons.filter { it <= maxSeasonLimitConstraint!! } else rawSeasons
                        if (!seasonsList.contains(selectedSeason)) {
                            selectedSeason = seasonsList.firstOrNull() ?: 1
                        }
                        episodesList = repository.getEpisodes(dubSubjectId, selectedSeason)
                    }
                } else {
                    selectedSubject = originalSubject
                    val rawSeasons = if (originalSubject.isTvShow) originalSubject.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted() else emptyList()
                    seasonsList = if (maxSeasonLimitConstraint != null) rawSeasons.filter { it <= maxSeasonLimitConstraint!! } else rawSeasons
                    if (!seasonsList.contains(selectedSeason)) {
                        selectedSeason = seasonsList.firstOrNull() ?: 1
                    }
                    episodesList = repository.getEpisodes(originalSubject.id, selectedSeason)
                }

                val activeSubjectId = selectedSubject?.id ?: originalSubject.id
                val epNum = currentEpisode?.episodeNumber ?: 1
                val seNum = currentEpisode?.seasonNumber ?: 1

                val resolvedPlayInfo = if (originalSubject.isTvShow) {
                    repository.getPlayInfo(activeSubjectId, seNum, epNum, resourceId = resource.resourceId)
                } else {
                    repository.getPlayInfo(activeSubjectId, 0, 0, resourceId = resource.resourceId)
                }
                playInfo = resolvedPlayInfo
                selectedStream = resolvedPlayInfo.streamList.firstOrNull()
                val isDubSelected = selectedResource?.name?.contains("dub", ignoreCase = true) == true
                selectedSubtitle = if (isDubSelected) null else resolvedPlayInfo.subTitleList.firstOrNull()
                isPlaying = true
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to change resource", e)
            } finally {
                isPlaybackLoading = false
            }
        }
    }

    // Toggle Favorite operation directly via cloud (no local cache)
    fun toggleFavorite(subject: MovieSubject) {
        viewModelScope.launch {
            if (isLoggedIn) {
                val currentList = _cloudFavorites.value
                val isFav = currentList.any { it.id == subject.id }
                val nextState = !isFav
                if (isFav) {
                    _cloudFavorites.value = currentList.filter { it.id != subject.id }
                } else {
                    _cloudFavorites.value = listOf(
                        FavoriteEntity(subject.id, subject.name, subject.poster, subject.isTvShow, subject.rating, subject.year)
                    ) + currentList
                }
                try {
                    val api = MovieBoxRetrofitClient.getApi(prefs)
                    api.toggleWatchlist(com.example.data.model.WantToSeeRequest(subject.id, if (nextState) 1 else 2, if (subject.isTvShow) 2 else 1))
                } catch (e: Exception) {}
            } else {
                val isFav = repository.isFavoriteDirect(subject.id)
                if (isFav) {
                    repository.deleteFavorite(subject.id)
                } else {
                    repository.insertFavorite(
                        FavoriteEntity(subject.id, subject.name, subject.poster, subject.isTvShow, subject.rating, subject.year)
                    )
                }
            }
        }
    }

    private suspend fun syncLocalFavoritesToCloud(api: com.example.data.network.MovieBoxApi) {
        try {
            val localFavs = repository.favorites.first()
            if (localFavs.isNotEmpty()) {
                localFavs.forEach { fav ->
                    try {
                        api.toggleWatchlist(com.example.data.model.WantToSeeRequest(fav.id, 1, if (fav.isTvShow) 2 else 1))
                    } catch (e: Exception) {}
                }
                repository.syncFavorites(emptyList()) // Clear local database after sync
            }
        } catch (e: Exception) {}
    }

    fun refreshWatchlist() {
        if (!isLoggedIn) {
            return // Keep local favorites if not logged in
        }
        viewModelScope.launch {
            try {
                val api = MovieBoxRetrofitClient.getApi(prefs)
                val response = api.getWatchlist(page = 1, pageSize = 100)
                if (response.code == 0) {
                    val items = response.data?.items ?: emptyList()
                    val favorites = items.mapNotNull { item ->
                        val sub = item.subject ?: return@mapNotNull null
                        FavoriteEntity(
                            id = sub.subjectId,
                            name = sub.title,
                            poster = sub.cover?.get("url") ?: "",
                            isTvShow = sub.subjectType == 2,
                            rating = sub.imdbRatingValue.ifEmpty { "0.0" },
                            year = if (sub.releaseDate.length >= 4) sub.releaseDate.take(4) else ""
                        )
                    }
                    _cloudFavorites.value = favorites
                }
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    fun isFavoriteFlow(id: String): Flow<Boolean> {
        return favoritesList.map { list -> list.any { it.id == id } }
    }

    // Save playback position tick
    fun updatePlaybackProgress(positionMs: Long, durationMs: Long) {
        playbackProgressMs = positionMs
        mediaDurationMs = durationMs
        
        val subject = selectedSubject ?: return
        val id = if (subject.isTvShow) {
            val ep = currentEpisode ?: return
            "${subject.id}_S${ep.seasonNumber}_E${ep.episodeNumber}"
        } else {
            subject.id
        }

        viewModelScope.launch {
            repository.insertHistory(
                HistoryEntity(
                    id = id,
                    subjectId = subject.id,
                    name = subject.name,
                    poster = subject.poster,
                    isTvShow = subject.isTvShow,
                    season = if (subject.isTvShow) currentEpisode?.seasonNumber ?: 1 else 0,
                    episode = if (subject.isTvShow) currentEpisode?.episodeNumber ?: 1 else 0,
                    seeTime = positionMs,
                    totalTime = durationMs
                )
            )
        }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    private fun parseErrorMessage(e: Exception): String {
        if (e is retrofit2.HttpException) {
            try {
                val errorBody = e.response()?.errorBody()?.string()
                if (!errorBody.isNullOrEmpty()) {
                    val moshi = com.squareup.moshi.Moshi.Builder().build()
                    val adapter = moshi.adapter(Map::class.java)
                    val parsed = adapter.fromJson(errorBody)
                    val detail = parsed?.get("detail") as? String
                    if (!detail.isNullOrEmpty()) {
                        return detail
                    }
                    val message = parsed?.get("message") as? String
                    if (!message.isNullOrEmpty()) {
                        return message
                    }
                }
            } catch (ignored: Exception) {}
        }
        return e.message ?: "Network error"
    }

    fun playNextEpisode() {
        val current = currentEpisode ?: return
        val currentIndex = episodesList.indexOfFirst { it.episodeNumber == current.episodeNumber }
        if (currentIndex != -1 && currentIndex < episodesList.size - 1) {
            selectEpisode(episodesList[currentIndex + 1])
        }
    }

    fun playPreviousEpisode() {
        val current = currentEpisode ?: return
        val currentIndex = episodesList.indexOfFirst { it.episodeNumber == current.episodeNumber }
        if (currentIndex > 0) {
            selectEpisode(episodesList[currentIndex - 1])
        }
    }

    private fun parseMaxSeasonFromName(name: String): Int? {
        val rangeRegex = Regex("""S(\d+)-S?(\d+)""", RegexOption.IGNORE_CASE)
        val rangeMatch = rangeRegex.find(name)
        if (rangeMatch != null) {
            return rangeMatch.groupValues[2].toIntOrNull()
        }
        val singleRegex = Regex("""(?:Season|S)\s*(\d+)""", RegexOption.IGNORE_CASE)
        val singleMatches = singleRegex.findAll(name).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        if (singleMatches.isNotEmpty()) {
            return singleMatches.maxOrNull()
        }
        return null
    }
}
