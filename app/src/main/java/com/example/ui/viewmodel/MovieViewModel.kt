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
import java.security.MessageDigest
import okhttp3.MediaType.Companion.toMediaTypeOrNull

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

    private val _searchResults = MutableStateFlow<List<MovieSubject>>(emptyList())
    val searchResults: StateFlow<List<MovieSubject>> = _searchResults.asStateFlow()

    var isSearching by mutableStateOf(false)
    var isDashboardLoading by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    // Room Favorites & History Flow
    val favoritesList: StateFlow<List<FavoriteEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyList: StateFlow<List<HistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real User Authentication State
    var isLoggedIn by mutableStateOf(false)
        private set
    var currentUserProfile by mutableStateOf<com.example.data.model.UserProfile?>(null)
        private set
    var authErrorMsg by mutableStateOf("")
    var isAuthLoading by mutableStateOf(false)

    init {
        if (prefs.mockHost.contains("10.0.2.2") || prefs.mockHost.contains("run.app") || prefs.mockHost.contains("ais-dev-") || prefs.mockHost.isEmpty()) {
            prefs.mockHost = "http://127.0.0.1:3000"
            mockHost = "http://127.0.0.1:3000"
        }
        checkLockoutStatus()
        loadDashboardContent()
        checkUserSession()
        loadSearchSuggestions()
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
                if (code != 0) {
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

                if (accessKeyId.isEmpty() || accessKeySecret.isEmpty() || securityToken.isEmpty()) {
                    onError("STS credentials are empty")
                    return@launch
                }

                // 2. Generate unique key and date
                val timestamp = System.currentTimeMillis()
                val objectKey = "OSS${timestamp}_avatar.jpg"

                val sdf = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("GMT")
                val dateGmt = sdf.format(java.util.Date())

                // 3. Generate signature
                val canonicalHeaders = "x-oss-object-acl:public-read\nx-oss-security-token:$securityToken\n"
                val canonicalResource = "/$bucket/$objectKey"
                val stringToSign = "PUT\n\nimage/jpeg\n$dateGmt\n$canonicalHeaders$canonicalResource"

                val mac = javax.crypto.Mac.getInstance("HmacSHA1")
                val secretKeySpec = javax.crypto.spec.SecretKeySpec(accessKeySecret.toByteArray(Charsets.UTF_8), "HmacSHA1")
                mac.init(secretKeySpec)
                val signBytes = mac.doFinal(stringToSign.toByteArray(Charsets.UTF_8))
                val signature = android.util.Base64.encodeToString(signBytes, android.util.Base64.NO_WRAP)

                // 4. Execute PUT request to Aliyun OSS
                val okHttpClient = okhttp3.OkHttpClient()
                val requestBody = okhttp3.RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageBytes)
                val putUrl = "https://$bucket.$endpoint/$objectKey"

                val request = okhttp3.Request.Builder()
                    .url(putUrl)
                    .put(requestBody)
                    .header("Host", "$bucket.$endpoint")
                    .header("Date", dateGmt)
                    .header("Content-Type", "image/jpeg")
                    .header("x-oss-object-acl", "public-read")
                    .header("x-oss-security-token", securityToken)
                    .header("Authorization", "OSS $accessKeyId:$signature")
                    .build()

                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    // 5. Update user profile on official server
                    val modifyRes = api.modifyProfile(mapOf("nickname" to nickname, "avatar" to putUrl))
                    if (modifyRes.code == 0 || modifyRes.message == "ok") {
                        checkUserSession()
                        onSuccess()
                    } else {
                        onError(modifyRes.message.ifEmpty { "Failed to update profile" })
                    }
                } else {
                    onError("Upload to AliCloud OSS failed: ${response.message}")
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
                _categoryMovies.value = categories
                
                // Pre-fetch details in the background for instant load
                trending.take(12).forEach { item ->
                    launch { repository.getDetail(item.id) }
                }
                categories.take(12).forEach { item ->
                    launch { repository.getDetail(item.id) }
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
        viewModelScope.launch {
            val categories = repository.getCategoryList(categoryId, 1, 30)
            _categoryMovies.value = categories
            // Pre-fetch details in the background
            categories.take(12).forEach { item ->
                launch { repository.getDetail(item.id) }
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
                playInfo = resolvedPlayInfo
                selectedStream = resolvedPlayInfo.streamList.firstOrNull()
                val isDubSelected = selectedResource?.name?.contains("dub", ignoreCase = true) == true
                selectedSubtitle = if (isDubSelected) null else resolvedPlayInfo.subTitleList.firstOrNull()
                isPlaying = true
                playbackProgressMs = 0L
                mediaDurationMs = 1800000L // default to 30 mins
                
                // Check Room for previous resume progress
                val historyId = "${subject.id}_S${episode.seasonNumber}_E${episode.episodeNumber}"
                val existingProgress = repository.getHistoryById(historyId)
                if (existingProgress != null) {
                    playbackProgressMs = existingProgress.seeTime
                    mediaDurationMs = existingProgress.totalTime
                }
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
                playInfo = resolvedPlayInfo
                selectedStream = resolvedPlayInfo.streamList.firstOrNull()
                val isDubSelected = selectedResource?.name?.contains("dub", ignoreCase = true) == true
                selectedSubtitle = if (isDubSelected) null else resolvedPlayInfo.subTitleList.firstOrNull()
                isPlaying = true
                playbackProgressMs = 0L
                mediaDurationMs = 7200000L // default to 2 hours
                
                val existingProgress = repository.getHistoryById(subject.id)
                if (existingProgress != null) {
                    playbackProgressMs = existingProgress.seeTime
                    mediaDurationMs = existingProgress.totalTime
                }
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

    // Toggle Favorite operation via repository and cloud sync
    fun toggleFavorite(subject: MovieSubject) {
        viewModelScope.launch {
            val isFav = repository.isFavoriteDirect(subject.id)
            val nextState = !isFav
            
            if (isFav) {
                repository.deleteFavorite(subject.id)
            } else {
                repository.insertFavorite(
                    FavoriteEntity(
                        id = subject.id,
                        name = subject.name,
                        poster = subject.poster,
                        isTvShow = subject.isTvShow,
                        rating = subject.rating,
                        year = subject.year
                    )
                )
            }

            if (isLoggedIn) {
                try {
                    val api = MovieBoxRetrofitClient.getApi(prefs)
                    val action = if (nextState) 1 else 0
                    val subjectType = if (subject.isTvShow) 2 else 1
                    api.toggleWatchlist(com.example.data.model.WantToSeeRequest(subject.id, action, subjectType))
                    checkUserSession()
                } catch (e: Exception) {
                    // Ignore or log background error
                }
            }
        }
    }

    fun refreshWatchlist() {
        if (!isLoggedIn) return
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
                    repository.syncFavorites(favorites)
                }
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    fun isFavoriteFlow(id: String): Flow<Boolean> {
        return repository.isFavorite(id)
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
