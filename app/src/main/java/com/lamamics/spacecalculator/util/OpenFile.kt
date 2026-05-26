package com.lamamics.spacecalculator.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLConnection

/**
 * Hands a readable file to another app via ACTION_VIEW + a FileProvider
 * content:// URI (a raw file:// URI would throw FileUriExposedException). Shows
 * a toast and returns false if the file is unreadable or no app can open it.
 */
fun openFile(context: Context, path: String): Boolean {
    val file = File(path)
    if (!file.canRead()) {
        Toast.makeText(context, "Fichier non lisible par l'app", Toast.LENGTH_SHORT).show()
        return false
    }
    return try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = URLConnection.guessContentTypeFromName(file.name) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Ouvrir avec").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (t: Throwable) {
        Toast.makeText(context, "Aucune application pour ouvrir ce fichier", Toast.LENGTH_SHORT).show()
        false
    }
}
