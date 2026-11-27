package com.misclaves.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(val versionCode: Int, val versionName: String, val apkUrl: String)

object UpdateManager {
    private const val RELEASES_API = "https://api.github.com/repos/Naruto2020-blip/MisClaves/releases/latest"

    suspend fun check(context: Context): AppUpdate? = withContext(Dispatchers.IO) {
        val connection = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MisClaves-Android")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IllegalStateException("GitHub no respondió correctamente.")
            val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val tag = release.optString("tag_name")
            val remoteCode = tag.substringAfterLast('.').toIntOrNull()
                ?: throw IllegalStateException("La versión publicada no tiene un formato válido.")
            @Suppress("DEPRECATION")
            val currentCode = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            if (remoteCode <= currentCode) return@withContext null
            val assets = release.optJSONArray("assets") ?: throw IllegalStateException("La publicación no contiene APK.")
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name") == "app-release.apk") {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
            val safeUrl = apkUrl ?: throw IllegalStateException("No se encontró el instalador de la versión.")
            if (!safeUrl.startsWith("https://github.com/")) throw IllegalStateException("El enlace de descarga no es válido.")
            AppUpdate(remoteCode, tag.removePrefix("v"), safeUrl)
        } finally { connection.disconnect() }
    }

    suspend fun downloadAndInstall(context: Context, update: AppUpdate) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            withContext(Dispatchers.Main) {
                val settings = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settings)
            }
            throw IllegalStateException("Permite a Mis Claves instalar esta actualización en los ajustes y vuelve a pulsar Actualizar.")
        }
        val folder = File(context.cacheDir, "updates").apply { mkdirs() }
        val apk = File(folder, "misclaves-update.apk")
        val connection = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "MisClaves-Android")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IllegalStateException("No se pudo descargar la actualización.")
            if (connection.contentLengthLong > 120L * 1024L * 1024L) throw IllegalStateException("El archivo de actualización es demasiado grande.")
            connection.inputStream.use { input -> apk.outputStream().use { output -> input.copyTo(output) } }
            if (apk.length() < 1000000L) throw IllegalStateException("El archivo descargado parece incompleto.")
        } finally { connection.disconnect() }
        withContext(Dispatchers.Main) {
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", apk)
            val install = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            context.startActivity(install)
        }
    }
}
