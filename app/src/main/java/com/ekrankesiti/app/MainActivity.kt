package com.ekrankesiti.app

import android.app.Activity
import android.app.StatusBarManager
import android.content.ClipData
import android.content.ClipboardManager
import android.view.View
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

/** Kurulum ekranı. */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var crashBox: LinearLayout
    private lateinit var crashText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(48), dp(20), dp(32))
        }
        col.addView(TextView(this).apply {
            text = "Ekran Kesiti"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })
        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, dp(16), 0, dp(16))
        }
        col.addView(status)

        col.addView(button("1) Erişilebilirlik iznini aç") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        col.addView(button("2) Hızlı ayarlara düğme ekle") { addTile() })
        col.addView(button("Uygulama bilgisi (kısıtlı ayarlar / pil)") {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
            )
        })

        crashBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(20), 0, 0)
        }
        crashText = TextView(this).apply {
            textSize = 12f
            setTextIsSelectable(true)
            setTextColor(0xFFFF6B6B.toInt())
        }
        crashBox.addView(TextView(this).apply {
            text = "Son hata kaydı"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
        })
        crashBox.addView(crashText)
        crashBox.addView(button("Hata kaydını kopyala") {
            val t = CrashLog.read(this) ?: return@button
            getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("Hata", t))
            toast("Kopyalandı. Claude'a yapıştırın.")
        })
        crashBox.addView(button("Hata kaydını sil") {
            CrashLog.clear(this)
            refresh()
        })
        col.addView(crashBox)

        col.addView(TextView(this).apply {
            textSize = 15f
            setPadding(0, dp(20), 0, 0)
            text = HELP
        })

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        status.text = (if (CaptureService.instance != null)
            "✅ Servis açık. Hazır."
        else
            "⚠️ Servis kapalı. Önce 1. adımı yapın.") +
            "\nSürüm " + BuildConfig.VERSION_NAME
        val crash = CrashLog.read(this)
        crashBox.visibility = if (crash == null) View.GONE else View.VISIBLE
        crashText.text = crash ?: ""
    }

    private fun addTile() {
        if (Build.VERSION.SDK_INT >= 33) {
            val sbm = getSystemService(StatusBarManager::class.java)
            sbm.requestAddTileService(
                ComponentName(this, CaptureTileService::class.java),
                getString(R.string.app_name),
                Icon.createWithResource(this, R.drawable.ic_tile),
                mainExecutor
            ) { result ->
                when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED,
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                        toast("Düğme hızlı ayarlarda")
                    else ->
                        toast("Eklenemedi. Kontrol panelinde Düzenle'den elle ekleyin.")
                }
            }
        } else {
            toast("Kontrol panelinde Düzenle'den elle ekleyin.")
        }
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    companion object {
        private const val HELP = """Kullanım
• Kontrol panelinden "Ekran Kesiti" düğmesine basın.
• Alanı parmağınızla çizin. Köşelerden boyutlandırın, içinden tutup taşıyın.
• Kopyala: görsel panoya gider, galeriye kaydedilmez. Yapıştırmak için metin kutusuna uzun basıp Yapıştır deyin ya da klavyedeki pano simgesini kullanın.
• Geçici dosyalar 24 saat sonra kendiliğinden silinir.

Xiaomi ipuçları
• Erişilebilirlik izni gri ve açılmıyorsa: Uygulama bilgisi → sağ üst ⋮ → "Kısıtlı ayarlara izin ver".
• Servis kendiliğinden kapanmasın diye: Uygulama bilgisi → Pil tasarrufu → Kısıtlama yok. Otomatik başlatmayı da açın."""
    }
}
