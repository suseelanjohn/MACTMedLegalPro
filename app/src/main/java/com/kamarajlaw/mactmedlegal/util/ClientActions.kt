package com.kamarajlaw.mactmedlegal.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLEncoder

object ClientActions {

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
            "Vanakkam $petitionerName, update regarding your MACT case ($mcopNumber) from Advocate Office.",
            "UTF-8"
        )
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=$msg"))
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show() }
    }

    fun shareFile(context: Context, filePath: String, title: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File not found on device", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share $title"))
    }
}
