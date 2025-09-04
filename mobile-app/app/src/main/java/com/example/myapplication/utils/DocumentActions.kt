package com.example.myapplication.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import okhttp3.*
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException



fun formatDate(dateTimeString: String): String {
    return try {
        val inputFormat = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val outputFormat = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val parsed = java.time.OffsetDateTime.parse(dateTimeString, inputFormat)
        outputFormat.format(parsed)
    } catch (_: Exception) {
        "-"
    }
}



fun downloadAndOpenDocument(
    token: String,
    documentId: Int,
    fileType: String?,
    context: Context
) {
    val url = "http://10.0.2.2:8000/api/v1/documents/$documentId/download"
    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $token")
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Toast.makeText(context, "İndirme hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                Toast.makeText(context, "Belge alınamadı", Toast.LENGTH_SHORT).show()
                return
            }

            val fileName = "belge_${System.currentTimeMillis()}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            val sink = file.sink().buffer()
            sink.writeAll(response.body!!.source())
            sink.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Belgeyi Aç"))
        }
    })
}


fun deleteDocumentById(
    token: String,
    documentId: Int,
    context: Context,
    onDeleted: () -> Unit
) {
    val url = "http://10.0.2.2:8000/api/v1/documents/$documentId"
    val request = Request.Builder()
        .url(url)
        .delete()
        .addHeader("Authorization", "Bearer $token")
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "Silme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onResponse(call: Call, response: Response) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Belge silindi", Toast.LENGTH_SHORT).show()
                    onDeleted()
                } else {
                    Toast.makeText(context, "Silinemedi (${response.code})", Toast.LENGTH_SHORT).show()
                }
            }
        }
    })
}




