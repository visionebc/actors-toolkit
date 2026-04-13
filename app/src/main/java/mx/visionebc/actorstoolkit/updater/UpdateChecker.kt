package mx.visionebc.actorstoolkit.updater

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val apkSizeBytes: Long,
    val appName: String
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val BASE_URL = "https://apps.visionebc.mx"
    private const val API_PATH = "/api/app/mx.visionebc.actorstoolkit/latest"

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL$API_PATH")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"

            if (conn.responseCode != 200) {
                Log.w(TAG, "API returned ${conn.responseCode}")
                return@withContext null
            }

            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            val serverVersionCode = json.getInt("version_code")
            if (serverVersionCode <= currentVersionCode) {
                Log.d(TAG, "App is up to date (current=$currentVersionCode, server=$serverVersionCode)")
                return@withContext null
            }

            val downloadPath = json.optString("download_url", "")
            if (downloadPath.isEmpty()) {
                Log.w(TAG, "No download URL in response")
                return@withContext null
            }

            UpdateInfo(
                versionName = json.getString("version_name"),
                versionCode = serverVersionCode,
                downloadUrl = "$BASE_URL$downloadPath",
                apkSizeBytes = json.optLong("apk_size_bytes", 0),
                appName = json.getString("app_name")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            null
        }
    }
}
