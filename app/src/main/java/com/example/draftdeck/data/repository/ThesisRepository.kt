package com.example.draftdeck.data.repository

import android.content.Context
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import javax.inject.Inject

interface ThesisRepository {
    fun getAllTheses(): Flow<NetworkResult<List<Thesis>>>
    fun getThesesByStudentId(studentId: String): Flow<NetworkResult<List<Thesis>>>
    fun getThesesByAdvisorId(advisorId: String): Flow<NetworkResult<List<Thesis>>>
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

    override fun getAllTheses(): Flow<NetworkResult<List<Thesis>>> = flow {
        emit(NetworkResult.Loading)

        // First emit data from local database
        thesisDao.getAllTheses()
            .map { entities -> entities.map { it.toThesis() } }
            .collect { localTheses ->
                emit(NetworkResult.Success(localTheses))
            }

        // Then fetch from remote and update local
        try {
            val response = thesisApi.getAllTheses()
            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    val theses = dtos.map { it.toThesis() }
                    thesisDao.insertTheses(theses.map { it.toThesisEntity() })
                    emit(NetworkResult.Success(theses))
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

    override fun getThesesByStudentId(studentId: String): Flow<NetworkResult<List<Thesis>>> = flow {
        emit(NetworkResult.Loading)

        // First emit data from local database
        thesisDao.getThesesByStudentId(studentId)
            .map { entities -> entities.map { it.toThesis() } }
            .collect { localTheses ->
                emit(NetworkResult.Success(localTheses))
            }

        // Then fetch from remote and update local
        try {
            val response = thesisApi.getThesesByStudentId(studentId)
            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    val theses = dtos.map { it.toThesis() }
                    thesisDao.insertTheses(theses.map { it.toThesisEntity() })
                    emit(NetworkResult.Success(theses))
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

    override fun getThesesByAdvisorId(advisorId: String): Flow<NetworkResult<List<Thesis>>> = flow {
        emit(NetworkResult.Loading)

        // First emit data from local database
        thesisDao.getThesesByAdvisorId(advisorId)
            .map { entities -> entities.map { it.toThesis() } }
            .collect { localTheses ->
                emit(NetworkResult.Success(localTheses))
            }

        // Then fetch from remote and update local
        try {
            val response = thesisApi.getThesesByAdvisorId(advisorId)
            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    val theses = dtos.map { it.toThesis() }
                    thesisDao.insertTheses(theses.map { it.toThesisEntity() })
                    emit(NetworkResult.Success(theses))
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

    override fun getThesisById(thesisId: String): Flow<NetworkResult<Thesis>> = flow {
        emit(NetworkResult.Loading)

        // First emit data from local database
        thesisDao.getThesisById(thesisId)
            .catch { e -> emit(NetworkResult.Error(e as Exception)) }
            .collect { entity ->
                entity?.let {
                    emit(NetworkResult.Success(it.toThesis()))
                }
            }

        // Then fetch from remote and update local
        try {
            val response = thesisApi.getThesisById(thesisId)
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
}