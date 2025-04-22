package com.example.draftdeck.data.repository

import android.content.Context
import android.util.Log
import com.example.draftdeck.data.local.dao.ThesisDao
import com.example.draftdeck.data.local.entity.toThesis
import com.example.draftdeck.data.local.entity.toThesisEntity
import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.remote.api.ThesisApi
import com.example.draftdeck.data.remote.dto.toThesis
import com.example.draftdeck.domain.util.FileHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Repository interface for managing thesis data between remote API and local database.
 * This repository follows a single-source-of-truth principle and handles caching.
 */
interface ThesisRepository {
    // Primary method for thesis listing with filters
    fun getTheses(status: String? = null, type: String? = null, query: String? = null): Flow<NetworkResult<List<Thesis>>>
    fun getThesisById(thesisId: String): Flow<NetworkResult<Thesis>>
    suspend fun uploadThesis(
        title: String,
        description: String,
        studentId: String,
        advisorId: String,
        submissionType: String,
        file: File
    ): Flow<NetworkResult<Thesis>>
    suspend fun updateThesis(
        thesisId: String,
        title: String,
        description: String,
        submissionType: String,
        file: File?
    ): Flow<NetworkResult<Thesis>>
    suspend fun updateThesisStatus(thesisId: String, status: String): Flow<NetworkResult<Thesis>>
    suspend fun deleteThesis(thesisId: String): Flow<NetworkResult<Unit>>
    suspend fun downloadThesis(thesisId: String, context: Context): Flow<NetworkResult<File>>
}

class ThesisRepositoryImpl @Inject constructor(
    private val thesisApi: ThesisApi,
    private val thesisDao: ThesisDao,
    private val fileHelper: FileHelper
) : ThesisRepository {

    private val TAG = "ThesisRepoDebug"

    override fun getTheses(status: String?, type: String?, query: String?): Flow<NetworkResult<List<Thesis>>> = flow {
        val cacheKey = "theses-${status ?: "all"}-${type ?: "all"}-${query ?: "none"}"
        Log.d(TAG, "Getting theses with filters - status: $status, type: $type, query: $query (cache key: $cacheKey)")
        emit(NetworkResult.Loading)

        // Track if we need to make a network request
        var shouldMakeNetworkRequest = true

        // First emit data from local database if available
        try {
            Log.d(TAG, "Fetching theses from local database")
            // We need to get all theses from local db and filter them here,
            // since we can't know which user-specific theses are stored locally
            val allLocalTheses = thesisDao.getAllTheses().first()
            
            val filteredLocalTheses = allLocalTheses.filter { entity ->
                val matchesStatus = status == null || entity.status == status
                val matchesType = type == null || entity.submissionType == type
                val matchesQuery = query == null || 
                    entity.title.contains(query, ignoreCase = true) || 
                    entity.description.contains(query, ignoreCase = true)
                
                matchesStatus && matchesType && matchesQuery
            }.map { it.toThesis() }
            
            if (filteredLocalTheses.isNotEmpty()) {
                Log.d(TAG, "Emitting ${filteredLocalTheses.size} theses from local database")
                emit(NetworkResult.Success(filteredLocalTheses))
                
                // If we have data in the cache that's less than 1 minute old, skip network request
                val newestThesis = filteredLocalTheses.maxByOrNull { it.lastUpdated }
                newestThesis?.let {
                    val cacheAgeMs = System.currentTimeMillis() - it.lastUpdated.time
                    if (cacheAgeMs < CACHE_TIMEOUT_MS) {
                        Log.d(TAG, "Using cached data that's ${cacheAgeMs/1000}s old (timeout: ${CACHE_TIMEOUT_MS/1000}s)")
                        shouldMakeNetworkRequest = false
                    }
                }
            } else {
                Log.d(TAG, "No matching local data available, will make network request")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading from local database: ${e.message}")
            // Continue to API call if local database fails
        }

        // Then make network request if needed
        if (shouldMakeNetworkRequest) {
            try {
                Log.d(TAG, "Fetching theses from remote API with query params")
                
                // Build query parameters
                val queryParams = mutableMapOf<String, String>()
                status?.let { queryParams["status"] = it }
                type?.let { queryParams["type"] = it }
                query?.let { queryParams["query"] = it }
                
                val response = thesisApi.getTheses(queryParams)
                Log.d(TAG, "API response received: ${response.code()}")
                
                if (response.isSuccessful) {
                    response.body()?.let { apiResponse ->
                        val theses = apiResponse.theses.map { it.toThesis() }
                        Log.d(TAG, "Received ${theses.size} theses from API")
                        thesisDao.insertTheses(theses.map { it.toThesisEntity() })
                        emit(NetworkResult.Success(theses))
                    } ?: run {
                        Log.e(TAG, "Empty response body from API")
                        emit(NetworkResult.Error(Exception("Empty response body")))
                    }
                } else {
                    Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                    emit(NetworkResult.Error(HttpException(response)))
                }
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP Exception: ${e.message()}", e)
                emit(NetworkResult.Error(e))
            } catch (e: IOException) {
                Log.e(TAG, "IO Exception: ${e.message}", e)
                emit(NetworkResult.Error(e))
            } catch (e: Exception) {
                Log.e(TAG, "General Exception: ${e.message}", e)
                emit(NetworkResult.Error(e))
            }
        } else {
            Log.d(TAG, "Skipping network request, using cached data")
        }
    }

    override fun getThesisById(thesisId: String): Flow<NetworkResult<Thesis>> = flow {
        Log.d(TAG, "Getting thesis by ID: $thesisId")
        emit(NetworkResult.Loading)

        // First emit data from local database if available
        try {
            Log.d(TAG, "Fetching thesis from local database")
            val entity = thesisDao.getThesisById(thesisId).first()
            
            if (entity != null) {
                Log.d(TAG, "Found thesis in local database: ${entity.title}")
                emit(NetworkResult.Success(entity.toThesis()))
            } else {
                Log.d(TAG, "No local thesis data available, waiting for API data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading from local database: ${e.message}")
            // Continue to API call even if local database fails
        }

        // Then always fetch from remote and update local
        try {
            Log.d(TAG, "Fetching thesis from remote API")
            val response = thesisApi.getThesisById(thesisId)
            Log.d(TAG, "API response received: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val thesis = dto.toThesis()
                    Log.d(TAG, "Received thesis from API: ${thesis.title}")
                    thesisDao.insertThesis(thesis.toThesisEntity())
                    emit(NetworkResult.Success(thesis))
                } ?: run {
                    Log.e(TAG, "Empty response body from API")
                    emit(NetworkResult.Error(Exception("Empty response body")))
                }
            } else {
                Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP Exception: ${e.message()}", e)
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            Log.e(TAG, "IO Exception: ${e.message}", e)
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            Log.e(TAG, "General Exception: ${e.message}", e)
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun uploadThesis(
        title: String,
        description: String,
        studentId: String,
        advisorId: String,
        submissionType: String,
        file: File
    ): Flow<NetworkResult<Thesis>> = flow {
        emit(NetworkResult.Loading)

        try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val studentIdBody = studentId.toRequestBody("text/plain".toMediaTypeOrNull())
            val advisorIdBody = advisorId.toRequestBody("text/plain".toMediaTypeOrNull())
            val submissionTypeBody = submissionType.toRequestBody("text/plain".toMediaTypeOrNull())

            val fileType = when {
                file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                file.name.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                else -> "application/octet-stream"
            }

            val fileRequestBody = file.asRequestBody(fileType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, fileRequestBody)

            val response = thesisApi.uploadThesis(
                titleBody,
                descriptionBody,
                studentIdBody,
                advisorIdBody,
                submissionTypeBody,
                filePart
            )

            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val thesis = dto.toThesis()
                    thesisDao.insertThesis(thesis.toThesisEntity())
                    emit(NetworkResult.Success(thesis))
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun updateThesis(
        thesisId: String,
        title: String,
        description: String,
        submissionType: String,
        file: File?
    ): Flow<NetworkResult<Thesis>> = flow {
        emit(NetworkResult.Loading)

        try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val submissionTypeBody = submissionType.toRequestBody("text/plain".toMediaTypeOrNull())

            val filePart = file?.let {
                val fileType = when {
                    it.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    it.name.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    else -> "application/octet-stream"
                }

                val fileRequestBody = it.asRequestBody(fileType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("file", it.name, fileRequestBody)
            }

            val response = thesisApi.updateThesis(
                thesisId,
                titleBody,
                descriptionBody,
                submissionTypeBody,
                filePart
            )

            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val thesis = dto.toThesis()
                    thesisDao.insertThesis(thesis.toThesisEntity())
                    emit(NetworkResult.Success(thesis))
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun updateThesisStatus(
        thesisId: String,
        status: String
    ): Flow<NetworkResult<Thesis>> = flow {
        emit(NetworkResult.Loading)

        try {
            val response = thesisApi.updateThesisStatus(thesisId, status)

            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val thesis = dto.toThesis()
                    thesisDao.insertThesis(thesis.toThesisEntity())
                    emit(NetworkResult.Success(thesis))
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun deleteThesis(thesisId: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)

        try {
            val response = thesisApi.deleteThesis(thesisId)

            if (response.isSuccessful) {
                thesisDao.deleteThesis(thesisId)
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun downloadThesis(
        thesisId: String,
        context: Context
    ): Flow<NetworkResult<File>> = flow {
        emit(NetworkResult.Loading)

        try {
            val response = thesisApi.downloadThesis(thesisId)

            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val file = fileHelper.saveResponseBodyToFile(
                        responseBody,
                        context.cacheDir,
                        "thesis_$thesisId.pdf"
                    )
                    emit(NetworkResult.Success(file))
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    companion object {
        // Cache timeout - how long to consider local data fresh (1 minute)
        private const val CACHE_TIMEOUT_MS = 60 * 1000L
    }
}