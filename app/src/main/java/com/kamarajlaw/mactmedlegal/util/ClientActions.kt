package com.kamarajlaw.mactmedlegal.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

object ClientActions {

    val inrFormat: NumberFormat = NumberFormat.getCurrencyInstance(
        Locale.Builder().setLanguage("en").setRegion("IN").build()
    ).apply {
        maximumFractionDigits = 0
    }

    fun formatInr(amount: Double): String = inrFormat.format(amount)

    fun dialClient(context: Context, phone: String) {
        val clean = phone.filter { it.isDigit() || it == '+' }
        if (clean.isBlank()) {
            Toast.makeText(context, "No client phone number saved", Toast.LENGTH_SHORT).show()
            return
        }
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
    }

    fun openWhatsApp(context: Context, phone: String, mcopNumber: String, petitionerName: String) {
        val digits = phone.filter { it.isDigit() }.let { if (it.length == 10) "91$it" else it }
        if (digits.isBlank()) {
            Toast.makeText(context, "No client phone number saved", Toast.LENGTH_SHORT).show()
            return
        }
        val msg = URLEncoder.encode(
            "Vanakkam $petitionerName, update regarding your MACT case ($mcopNumber) from Kamaraj Law Office.",
            "UTF-8"
        )
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=$msg"))
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show() }
    }

    fun copyText(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    fun copyUriToInternalCaseVault(context: Context, caseId: Long, uri: Uri): Triple<String, String, String>? {
        return runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "application/pdf"
            var displayName = "Attached_Document"
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }
            val ext = when {
                mime.contains("pdf", true) -> ".pdf"
                mime.contains("png", true) -> ".png"
                mime.contains("jpeg", true) || mime.contains("jpg", true) -> ".jpg"
                else -> ""
            }
            val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val finalName = if (safeName.contains(".")) safeName else "$safeName$ext"
            val dir = File(context.filesDir, "case_docs").apply { mkdirs() }
            val dest = File(dir, "ATT_${caseId}_${System.currentTimeMillis()}_$finalName")
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            Triple(dest.absolutePath, displayName, mime)
        }.getOrNull()
    }

    fun openFile(context: Context, filePath: String, mimeType: String = "application/pdf") {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Document not found on device", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(viewIntent, "View Document")) }
            .onFailure { Toast.makeText(context, "No viewer app found for $mimeType", Toast.LENGTH_SHORT).show() }
    }

    fun shareFile(context: Context, filePath: String, title: String, mimeType: String = "application/pdf") {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File not found on device", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share $title"))
    }
}
