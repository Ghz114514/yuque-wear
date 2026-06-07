package com.yuquewatch

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Process
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Installs a global crash handler. Because we cannot read logcat on the Xiaomi watch,
 * any uncaught exception is written to a file AND shown in [CrashActivity] (plain Views,
 * separate process) so the stack trace is readable directly on the device — screenshot it.
 */
class YuqueApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Apply the rotary-haptic preference before any ScalingLazyColumn composes (it reads
        // the constant once). Toggling later therefore takes effect on the next launch.
        runCatching {
            val on = getSharedPreferences("yuque_prefs", MODE_PRIVATE)
                .getBoolean("haptic_enabled", true)
            com.google.wear.input.WearHapticFeedbackConstants.ENABLED = on
        }

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val report = buildReport(error)
                runCatching { File(filesDir, CRASH_FILE).writeText(report) }
                runCatching {
                    getExternalFilesDir(null)?.let { File(it, CRASH_FILE).writeText(report) }
                }
                val intent = Intent(this, CrashActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra(EXTRA_REPORT, report)
                startActivity(intent)
            } catch (_: Throwable) {
                // If even the crash screen fails, fall back to the default handler.
                previous?.uncaughtException(thread, error)
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    private fun buildReport(error: Throwable): String {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return buildString {
            appendLine("语雀小记 崩溃报告 / Crash Report")
            appendLine("时间: $time")
            appendLine("机型: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("系统: Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
            appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("应用版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("----------------------------------------")
            append(sw.toString())
        }
    }

    companion object {
        const val CRASH_FILE = "last_crash.txt"
        const val EXTRA_REPORT = "report"
    }
}
