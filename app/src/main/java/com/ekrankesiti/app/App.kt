package com.ekrankesiti.app

import android.app.Application
import android.content.Context
import android.util.Log
import java.io.File
import java.util.Date

/** Çökme olursa hatayı dosyaya yazar; ana ekranda gösterilir. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            CrashLog.save(this, e)
            prev?.uncaughtException(t, e)
        }
    }
}

object CrashLog {
    private fun file(ctx: Context) = File(ctx.filesDir, "hata.txt")

    fun save(ctx: Context, e: Throwable) {
        try {
            file(ctx).writeText(
                "Ekran Kesiti ${BuildConfig.VERSION_NAME} – ${Date()}\n" +
                    Log.getStackTraceString(e)
            )
        } catch (_: Throwable) {
        }
    }

    fun read(ctx: Context): String? =
        file(ctx).takeIf { it.exists() }?.readText()

    fun clear(ctx: Context) {
        file(ctx).delete()
    }
}
