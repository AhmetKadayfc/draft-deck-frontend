package com.example.draftdeck.data.repository

import android.content.Context
import android.util.Log
import com.example.draftdeck.data.local.dao.ThesisDao
import com.example.draftdeck.data.local.entity.toThesis
import com.example.draftdeck.data.local.entity.toThesisEntity
import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.remote.api.ThesisApi
import com.example.draftdeck.data.remote.api.adminAssignAdvisorToThesis
import com.example.draftdeck.data.remote.dto.toThesis
import com.example.draftdeck.domain.util.FileHelper
import com.example.draftdeck.domain.util.NetworkConnectivityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.util.Date
import javax.inject.Inject

/**
 * Repository interface for managing thesis data between remote API and local database.
 * This repository follows a single-source-of-truth principle and handles caching.
 */
interface ThesisRepository {
    // Primary method for thesis listing with filters
    fun getTheses(
        status: String? = null, 
        type: String? = null, 
        query: String? = null,
        additionalParams: Map<String, String> = emptyMap()
    ): Flow<NetworkResult<List<Thesis>>>
    fun getThesisById(thesisId: String): Flow<NetworkResult<Thesis>>
    suspend fun uploadThesis(
        title: String,
        description: String,
        studentId: String,  // Still needed for local identification, but not sent to API
        advisorId: String,  // Still needed for local identification, but not sent to API
        submissionType: String,  // This is mapped to thesis_type in implementation
        file: File
    ): Flow<NetworkResult<Thesis>>
    suspend fun updateThesis(
        thesisId: String,
        title: String,
        description: String,
        submissionType: String,  // This is mapped to thesis_type in implementation
        file: File?
    ): Flow<NetworkResult<Thesis>>
    suspend fun updateThesisStatus(thesisId: String, status: String): Flow<NetworkResult<Thesis>>
    suspend fun deleteThesis(thesisId: String): Flow<NetworkResult<Unit>>
    suspend fun downloadThesis(thesisId: String, context: Context): Flow<NetworkResult<File>>
    /**
     * Assign an advisor to a thesis
     * This assigns the currently logged-in user as advisor
     */
    suspend fun assignAdvisorToThesis(thesisId: String, advisorId: String): Flow<NetworkResult<Thesis>>
    
    /**
     * Admin method to assign any advisor to a thesis
     * @param thesisId ID of the thesis to assign
     * @param advisorId ID of the advisor to assign
     */
    suspend fun adminAssignAdvisorToThesis(thesisId: String, advisorId: String): Flow<NetworkResult<Thesis>>
}

class ThesisRepositoryImpl @Inject constructor(
    private val thesisApi: ThesisApi,
    private val thesisDao: ThesisDao,
    private val fileHelper: FileHelper,
    private val userRepository: UserRepository,
    override val networkConnectivityManager: NetworkConnectivityManager
) : ThesisRepository, BaseRepository {

    override val TAG = "ThesisRepoDebug"
    private val CACHE_TIMEOUT_MS = 60 * 1000L // 1 minute

    override fun getTheses(
        status: String?, 
        type: String?, 
        query: String?,
        additionalParams: Map<String, String>
    ): Flow<NetworkResult<List<Thesis>>> {
        return getDataWithOfflineSupport(
            localDataSource = {
                // Get all theses from local db and filter them here
                try {
                    Log.d(TAG, "Fetching theses from local database")
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
                        filteredLocalTheses
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading from local database: ${e.message}")
                    null
                }
            },
            remoteDataSource = {
                // Build query parameters to match Flask API expectations
                val queryParams = mutableMapOf<String, String>()
                status?.let { queryParams["status"] = it }
                type?.let { queryParams["type"] = it } 
                query?.let { queryParams["query"] = it }
                
                // Add any additional parameters
                queryParams.putAll(additionalParams)
                
                // We can also add limit and offset if needed for pagination
                queryParams["limit"] = "20"
                queryParams["offset"] = "0"
                
                Log.d(TAG, "API Request: GET /theses with params: $queryParams")
                
                val response = thesisApi.getTheses(queryParams)
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.theses.map { it.toThesis() }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "API error: ${response.code()} - ${response.message()} - $errorBody")
                    throw HttpException(response)
                }
            },
            saveRemoteData = { theses ->
                thesisDao.insertTheses(theses.map { it.toThesisEntity() })
            }
        )
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
        studentId: String,  // Still needed for local identification, but not sent to API
        advisorId: String,  // Still needed for local identification, but not sent to API
        submissionType: String,  // This is mapped to thesis_type in implementation
        file: File
    ): Flow<NetworkResult<Thesis>> = flow {
        emit(NetworkResult.Loading)

        try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            // Convert to lowercase to match the API's enum values (draft or final)
            val thesisTypeBody = submissionType.lowercase().toRequestBody("text/plain".toMediaTypeOrNull())

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
                thesisTypeBody,
                filePart
            )

            if (response.isSuccessful) {
                response.body()?.let { createResponse ->
                    try {
                        // Use the data we already have for constructing a valid Thesis object
                        // Get current date for any missing date fields
                        val now = Date()
                        
                        // Extract thesis ID from response, providing a fallback if needed
                        val thesisId = createResponse.thesis.id.ifEmpty { 
                            Log.w(TAG, "Received empty thesis ID, generating temporary one")
                            java.util.UUID.randomUUID().toString()
                        }
                        
                        // Determine file type based on file extension
                        val fileType = when {
                            file.name.endsWith(".pdf", ignoreCase = true) -> "pdf"
                            file.name.endsWith(".docx", ignoreCase = true) -> "docx" 
                            else -> "" // Default empty if unknown
                        }
                        
                        // Create a thesis object with the data we have
                        val thesis = Thesis(
                            id = thesisId,
                            title = title,
                            description = description,
                            studentId = studentId,
                            studentName = "Student", // We'll update this with real data later if possible
                            advisorId = advisorId,
                            advisorName = "Advisor", // We'll update this with real data later if possible
                            submissionType = submissionType,
                            fileUrl = "", // Will be populated when downloading
                            fileType = fileType,
                            version = 1,
                            status = createResponse.thesis.status,
                            submissionDate = createResponse.thesis.createdAt ?: now,
                            lastUpdated = now
                        )
                        
                        // Save to local database
                        thesisDao.insertThesis(thesis.toThesisEntity())
                        
                        // Return the thesis
                        emit(NetworkResult.Success(thesis))
                        
                        // Try to update student and advisor names in the background if possible
                        try {
                            // This is a background update that shouldn't block the success response
                            val currentUser = userRepository.getCurrentUser().first()
                            if (currentUser != null) {
                                val studentName = "${currentUser.firstName} ${currentUser.lastName}".trim()
                                
                                // Create updated thesis with the student name
                                val updatedThesis = thesis.copy(studentName = studentName)
                                
                                // Update the local database
                                thesisDao.insertThesis(updatedThesis.toThesisEntity())
                            }
                        } catch (e: Exception) {
                            // Log but don't fail the thesis upload
                            Log.e(TAG, "Failed to update student name: ${e.message}", e)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing thesis creation response: ${e.message}", e)
                        emit(NetworkResult.Error(e))
                    }
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "API Error (${response.code()}): $errorBody")
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

    override suspend fun updateThesis(
        thesisId: String,
        title: String,
        description: String,
        submissionType: String,  // This is mapped to thesis_type in implementation
        file: File?
    ): Flow<NetworkResult<Thesis>> = flow {
        emit(NetworkResult.Loading)

        try {
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            // Map submissionType to thesis_type as expected by the API
            // Convert to lowercase to match the API's enum values (draft or final)
            val thesisTypeBody = submissionType.lowercase().toRequestBody("text/plain".toMediaTypeOrNull())

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
                thesisTypeBody,
                filePart
            )

            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    // Convert the DTO to a domain model
                    val thesis = dto.toThesis()
                    
                    // If file was updated, ensure the fileType is set correctly
                    val updatedThesis = if (file != null) {
                        // Extract file type from file name
                        val fileType = when {
                            file.name.endsWith(".pdf", ignoreCase = true) -> "pdf"
                            file.name.endsWith(".docx", ignoreCase = true) -> "docx"
                            else -> thesis.fileType // Keep existing if can't determine
                        }
                        thesis.copy(fileType = fileType)
                    } else {
                        thesis
                    }
                    
                    // Save to database and emit result
                    thesisDao.insertThesis(updatedThesis.toThesisEntity())
                    emit(NetworkResult.Success(updatedThesis))
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

    override suspend fun assignAdvisorToThesis(thesisId: String, advisorId: String): Flow<NetworkResult<Thesis>> = flow {
        emit(NetworkResult.Loading)
        try {
            // The current user (advisor) will be assigned to the thesis
            // The advisorId parameter is ignored because the API uses the current user's ID from the auth token
            val response = thesisApi.assignAdvisorToThesis(thesisId)
            if (response.isSuccessful) {
                val responseBody = response.body() ?: throw IOException("Empty response body")
                val thesis = responseBody.thesis.toThesis()
                // Update local cache
                thesisDao.insertThesis(thesis.toThesisEntity())
                emit(NetworkResult.Success(thesis))
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun adminAssignAdvisorToThesis(thesisId: String, advisorId: String): Flow<NetworkResult<Thesis>> = flow {
        emit(NetworkResult.Loading)
        try {
            // Add debug logs
            Log.d("ThesisRepo", "Admin assigning advisor $advisorId to thesis $thesisId")
            
            // Call the admin API endpoint to assign the specific advisor to the thesis
            // The extension function will handle creating the appropriate request body
            Log.d("ThesisRepo", "Making API call to assign advisor")
            val response = thesisApi.adminAssignAdvisorToThesis(thesisId, advisorId)
            Log.d("ThesisRepo", "API response: ${response.code()}")
            
            if (response.isSuccessful) {
                // Get the response body which contains a message and the thesis object
                val responseBody = response.body()
                Log.d("ThesisRepo", "Response body: $responseBody")
                
                if (responseBody == null) {
                    Log.e("ThesisRepo", "Response body is null")
                    throw IOException("Empty response body")
                }
                
                Log.d("ThesisRepo", "Assignment message: ${responseBody.message}")
                
                // Extract the thesis from the nested response
                val thesisDto = responseBody.thesis
                Log.d("ThesisRepo", "Thesis dto from response: $thesisDto")
                
                // Convert the DTO to our domain model
                val thesis = thesisDto.toThesis()
                Log.d("ThesisRepo", "Converted thesis: $thesis")
                
                // Since the response may only contain partial thesis data (id, title, advisor_id, updated_at)
                // We need to merge this with our existing data
                try {
                    val existingThesis = thesisDao.getThesisById(thesis.id).firstOrNull()
                    if (existingThesis != null) {
                        // Create a merged thesis with all the existing data plus the updated advisor ID
                        val updatedThesis = existingThesis.toThesis().copy(
                            // Always take the advisor ID from the response
                            advisorId = thesis.advisorId,
                            // Take the title from the response if present, otherwise from existing
                            title = if (thesis.title.isNotBlank()) thesis.title else existingThesis.title,
                            // For all other fields, prefer existing data over potentially empty response data
                            description = existingThesis.description,
                            studentId = existingThesis.studentId,
                            studentName = existingThesis.studentName,
                            advisorName = if (thesis.advisorName.isNotBlank()) thesis.advisorName else existingThesis.advisorName,
                            submissionType = existingThesis.submissionType,
                            fileUrl = existingThesis.fileUrl,
                            fileType = existingThesis.fileType,
                            version = existingThesis.version,
                            status = existingThesis.status,
                            submissionDate = existingThesis.submissionDate,
                            // Use the updated timestamp if available
                            lastUpdated = thesis.lastUpdated.takeIf { it.after(Date(0)) } ?: existingThesis.lastUpdated
                        )
                        
                        // Update the thesis with merged data
                        thesisDao.insertThesis(updatedThesis.toThesisEntity())
                        Log.d("ThesisRepo", "Updated thesis with merged data: $updatedThesis")
                        emit(NetworkResult.Success(updatedThesis))
                        return@flow
                    }
                } catch (e: Exception) {
                    Log.e("ThesisRepo", "Error merging with existing thesis: ${e.message}")
                    // Continue with the original thesis if there was an error
                }
                
                // If we couldn't merge with existing data (e.g., it doesn't exist locally),
                // just use the data from the response
                thesisDao.insertThesis(thesis.toThesisEntity())
                Log.d("ThesisRepo", "Saved thesis to local database")
                
                emit(NetworkResult.Success(thesis))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("ThesisRepo", "API error: ${response.code()} - $errorMsg")
                throw HttpException(response)
            }
        } catch (e: Exception) {
            Log.e("ThesisRepo", "Exception in adminAssignAdvisorToThesis: ${e.message}", e)
            emit(NetworkResult.Error(e))
        }
    }
}