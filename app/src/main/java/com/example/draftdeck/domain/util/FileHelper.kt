package com.example.draftdeck.domain.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class FileHelper @Inject constructor() {

    fun getFileFromUri(uri: Uri, context: Context): File? {
        val documentFile = DocumentFile.fromSingleUri(context, uri) ?: return null
        val fileName = documentFile.name ?: return null

        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val outputFile = File(context.cacheDir, fileName)

        try {
            copyInputStreamToFile(inputStream, outputFile)
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            inputStream.close()
        }
    }

    fun saveResponseBodyToFile(
        body: ResponseBody,
        directory: File,
        fileName: String
    ): File {
        val file = File(directory, fileName)
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            inputStream = body.byteStream()
            outputStream = FileOutputStream(file)

            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()
            return file
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    private fun copyInputStreamToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file).use { outputStream ->
            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()
        }
    }

    fun isFileSizeValid(file: File, maxSizeMB: Int): Boolean {
        val fileSizeInMB = file.length() / (1024 * 1024)
        return fileSizeInMB <= maxSizeMB
    }

    fun getFileExtension(fileName: String): String? {
        return fileName.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
    }
}