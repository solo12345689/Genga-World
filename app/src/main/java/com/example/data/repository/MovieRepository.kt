package com.example.data.repository

import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.MovieDao
import com.example.data.model.*
import com.example.data.network.GetListRequest
import com.example.data.network.MovieBoxApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MovieRepository(
    private val api: MovieBoxApi,
    private val dao: MovieDao
) {
    // Room Databases operations
    val favorites: Flow<List<FavoriteEntity>> = dao.getAllFavorites()
    val history: Flow<List<HistoryEntity>> = dao.getAllHistory()

    suspend fun insertFavorite(favorite: FavoriteEntity) = withContext(Dispatchers.IO) {
        dao.insertFavorite(favorite)
    }

    suspend fun deleteFavorite(id: String) = withContext(Dispatchers.IO) {
        dao.deleteFavoriteById(id)
    }

    suspend fun syncFavorites(favorites: List<FavoriteEntity>) = withContext(Dispatchers.IO) {
        dao.syncFavorites(favorites)
    }

    fun isFavorite(id: String): Flow<Boolean> = dao.isFavorite(id)
    suspend fun isFavoriteDirect(id: String): Boolean = withContext(Dispatchers.IO) {
        dao.isFavoriteDirect(id)
    }

    suspend fun insertHistory(history: HistoryEntity) = withContext(Dispatchers.IO) {
        dao.insertHistory(history)
    }

    suspend fun deleteHistory(id: String) = withContext(Dispatchers.IO) {
        dao.deleteHistoryById(id)
    }

    suspend fun getHistoryById(id: String): HistoryEntity? = withContext(Dispatchers.IO) {
        dao.getHistoryById(id)
    }

    // Empty fallback as requested by the user to remove all fake/mock content
    private val fallbackMovies = emptyList<MovieSubject>()

    private val detailCache = java.util.concurrent.ConcurrentHashMap<String, MovieSubject>()

    suspend fun getTrending(page: Int = 1, pageSize: Int = 20): List<MovieSubject> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTrending(page, pageSize)
            val list = response.data?.let { data ->
                if (data.list.isNotEmpty()) data.list else data.items
            } ?: emptyList()
            if ((response.code == 200 || response.code == 0) && list.isNotEmpty()) {
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCategoryList(categoryId: Int, page: Int = 1, pageSize: Int = 20): List<MovieSubject> = withContext(Dispatchers.IO) {
        try {
            val response = api.getList(GetListRequest(categoryId, page, pageSize))
            val list = response.data?.let { data ->
                if (data.list.isNotEmpty()) data.list else data.items
            } ?: emptyList()
            if ((response.code == 200 || response.code == 0) && list.isNotEmpty()) {
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDetail(subjectId: String): MovieSubject? = withContext(Dispatchers.IO) {
        val cached = detailCache[subjectId]
        if (cached != null) return@withContext cached
        try {
            val response = api.getDetail(subjectId)
            if ((response.code == 200 || response.code == 0) && response.data != null) {
                detailCache[subjectId] = response.data
                response.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getEpisodes(subjectId: String, season: Int): List<Episode> = withContext(Dispatchers.IO) {
        val detail = getDetail(subjectId)
        if (detail != null && detail.seasonList.isNotEmpty()) {
            val seasonInfo = detail.seasonList.find { it.season == season }
            if (seasonInfo != null) {
                return@withContext seasonInfo.episodeList.map { epInfo ->
                    Episode(
                        episodeId = "${subjectId}_S${season}_E${epInfo.episode}",
                        title = epInfo.title.ifEmpty { "Episode ${epInfo.episode}" },
                        episodeNumber = epInfo.episode,
                        seasonNumber = season,
                        duration = epInfo.duration.ifEmpty { "45 min" },
                        stillPath = epInfo.stillPath
                    )
                }
            }
        }
        emptyList()
    }

    suspend fun getPlayInfo(
        subjectId: String,
        season: Int,
        episode: Int,
        quality: String = "1080p",
        resourceId: String? = null
    ): PlayInfo = withContext(Dispatchers.IO) {
        try {
            api.playInfo(subjectId, season, episode, quality, resourceId)
        } catch (e: Exception) {
            PlayInfo(emptyList(), emptyList())
        }
    }

    suspend fun search(query: String): List<MovieSubject> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val response = api.search(query, 1, 30)
            val items = response.data?.let { data ->
                if (data.items.isNotEmpty()) data.items else data.list
            } ?: emptyList()
            if ((response.code == 200 || response.code == 0) && items.isNotEmpty()) {
                items
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSearchSuggestions(query: String? = null): List<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSearchSuggestions(query)
            if (response.code == 0 || response.code == 200) {
                response.data
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
