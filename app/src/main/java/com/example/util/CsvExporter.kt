package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object CsvExporter {

    fun escapeCell(raw: String): String {
        val clean = raw.replace("\r\n", " ").replace("\n", " ")
        return if (clean.contains(",") || clean.contains("\"")) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }

    fun buildCsv(headers: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.appendLine(headers.joinToString(",") { escapeCell(it) })
        rows.forEach { row ->
            sb.appendLine(row.joinToString(",") { escapeCell(it) })
        }
        return sb.toString()
    }

    fun exportAndShare(
        context: Context,
        fileNamePrefix: String,
        headers: List<String>,
        rows: List<List<String>>
    ): Result<String> {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeName = fileNamePrefix.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val file = File(exportDir, "${safeName}_${System.currentTimeMillis()}.csv")
            file.writeText(buildCsv(headers, rows), Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "NEXORA ONE Export: $fileNamePrefix")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Export CSV Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Result.success(file.name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
