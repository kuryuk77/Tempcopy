package com.ekrankesiti.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Erişilebilirlik servisi: izin penceresi çıkmadan ekran görüntüsü alır
 * (Android 11+ takeScreenshot API).
 */
class CaptureService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: CaptureService? = null
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private var busy = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    /** Bildirim/kontrol panelini kapatır, kısa bekleyip ekran görüntüsü alır. */
    fun capture(delayMs: Long) {
        if (busy) return
        busy = true
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        handler.postDelayed({ takeShot() }, delayMs)
    }

    private fun takeShot() {
        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                        try {
                            handleShot(result)
                        } catch (e: Throwable) {
                            fail(e)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        busy = false
                        toast("Ekran görüntüsü alınamadı (kod $errorCode)")
                    }
                }
            )
        } catch (e: Throwable) {
            fail(e)
        }
    }

    private fun handleShot(result: AccessibilityService.ScreenshotResult) {
        val buffer = result.hardwareBuffer
        val hw = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
        val bmp = hw?.copy(Bitmap.Config.ARGB_8888, false)
        hw?.recycle()
        buffer.close()
        busy = false
        if (bmp == null) {
            toast("Ekran görüntüsü alınamadı")
            return
        }
        ShotHolder.bitmap = bmp
        val i = Intent(this, CropActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        )
        startActivity(i)
    }

    private fun fail(e: Throwable) {
        busy = false
        CrashLog.save(this, e)
        toast("Hata: ${e.javaClass.simpleName}. Ayrıntı uygulamada.")
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
