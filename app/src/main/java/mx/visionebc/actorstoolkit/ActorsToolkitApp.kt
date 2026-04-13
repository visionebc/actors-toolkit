package mx.visionebc.actorstoolkit

import android.app.Application
import android.os.Build
import android.util.Log
import mx.visionebc.actorstoolkit.updater.CrashReporter
import mx.visionebc.actorstoolkit.data.AppDatabase
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActorsToolkitApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        setupCrashHandler()
        // Send any pending crash reports to server
        CrashReporter.sendPendingReports(this)
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashDir = getExternalFilesDir(null) ?: filesDir
                val crashFile = File(crashDir, "crash_log.txt")
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stackTrace = sw.toString()

                // Extract root cause
                var rootCause: Throwable = throwable
                while (rootCause.cause != null && rootCause.cause !== rootCause) {
                    rootCause = rootCause.cause!!
                }

                val log = buildString {
                    appendLine("EXCEPTION: ${throwable.javaClass.name}")
                    appendLine("MESSAGE: ${throwable.message}")
                    if (rootCause !== throwable) {
                        appendLine("ROOT CAUSE: ${rootCause.javaClass.name}: ${rootCause.message}")
                    }
                    appendLine("THREAD: ${thread.name}")
                    appendLine("TIME: $timestamp")
                    appendLine("DEVICE: ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("ANDROID: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("APP: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("---STACK---")
                    appendLine(stackTrace)
                }
                crashFile.writeText(log)
                Log.e("ActorsToolkit", "CRASH LOGGED to ${crashFile.absolutePath}", throwable)
            } catch (e: Exception) {
                Log.e("ActorsToolkit", "Failed to log crash", e)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun getLastCrashLog(): String? {
        return try {
            val crashDir = getExternalFilesDir(null) ?: filesDir
            val crashFile = File(crashDir, "crash_log.txt")
            if (crashFile.exists()) crashFile.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    fun clearCrashLog() {
        try {
            val crashDir = getExternalFilesDir(null) ?: filesDir
            val crashFile = File(crashDir, "crash_log.txt")
            if (crashFile.exists()) crashFile.delete()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        lateinit var instance: ActorsToolkitApp
            private set
    }
}
