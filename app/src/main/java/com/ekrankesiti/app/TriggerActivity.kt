package com.ekrankesiti.app

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle

/**
 * Görünmez ara ekran. Hızlı ayar düğmesi bunu "startActivityAndCollapse" ile açar;
 * böylece sistem kontrol panelini kesin olarak kapatır. Sonra ekran görüntüsü alınır.
 */
class TriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val svc = CaptureService.instance
        if (svc == null) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
            return
        }
        finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        // Panelin kapanma animasyonu bitsin.
        svc.capture(800)
    }
}
