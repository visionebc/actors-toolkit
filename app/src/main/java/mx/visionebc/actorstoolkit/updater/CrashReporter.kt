package mx.visionebc.actorstoolkit.updater

import android.content.Context
import android.os.Build
import android.util.Log
import mx.visionebc.actorstoolkit.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object CrashReporter {
    private const val TAG = "CrashReporter"
    private const val REPORT_URL = "https://apps.visionebc.mx/api/crash-report"

    fun sendPendingReports(context: Context) {
        thread {
            try {
                val crashDir = context.getExternalFilesDir(null) ?: context.filesDir
                val crashFile = File(crashDir, "crash_log.txt")
                if (!crashFile.exists()) return@thread

                val crashLog = crashFile.readText()
                if (crashLog.isBlank()) return@thread

                // Parse the structured crash log
                val lines = crashLog.lines()
                val exceptionType = lines.find { it.startsWith("EXCEPTION:") }
                    ?.substringAfter("EXCEPTION:")?.trim() ?: "Unknown"
                val exceptionMsg = lines.find { it.startsWith("MESSAGE:") }
                    ?.substringAfter("MESSAGE:")?.trim() ?: ""
                val stackStart = lines.indexOfFirst { it == "---STACK---" }
                val stackTrace = if (stackStart >= 0) {
                    lines.drop(stackStart + 1).joinToString("\n")
                } else {
                    crashLog
                }

                val json = JSONObject().apply {
                    put("app_package", BuildConfig.APPLICATION_ID)
                    put("app_version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    put("device_model", "${Build.MANUFACTURER} ${Build.MODEL}")
                    put("android_version", "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    put("exception_type", exceptionType)
                    put("exception_msg", exceptionMsg)
                    put("stack_trace", stackTrace.take(10000))
                }

                val url = URL(REPORT_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { it.write(json.toString().toByteArray()) }

                val code = conn.responseCode
                if (code in 200..299) {
                    Log.d(TAG, "Crash report sent successfully")
                } else {
                    Log.w(TAG, "Crash report failed: HTTP $code")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send crash report", e)
            }
        }
    }
}
