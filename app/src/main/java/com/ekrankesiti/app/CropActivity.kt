package com.ekrankesiti.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class CropActivity : Activity() {

    private lateinit var cropView: CropView
    private lateinit var bar: LinearLayout
    private var shot: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bmp = ShotHolder.bitmap
        if (bmp == null) {
            finish()
            return
        }
        shot = bmp

        cropView = CropView(this, bmp)
        cropView.onSelectionChanged = { placeBar() }
        bar = buildBar()

        val root = FrameLayout(this)
        root.addView(cropView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(
            bar,
            FrameLayout.LayoutParams(
                WRAP_CONTENT, WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply { bottomMargin = dp(40) }
        )
        setContentView(root)

        window.setDecorFitsSystemWindows(false)
        window.insetsController?.let {
            it.hide(WindowInsets.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private fun buildBar(): LinearLayout {
        val l = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(0xE6202124.toInt())
            }
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        l.addView(btn("İptal", false) { done() })
        l.addView(btn("Kaydet", false) { save() })
        l.addView(btn("Paylaş", false) { share() })
        l.addView(btn("Kopyala", true) { copy() })
        return l
    }

    private fun btn(label: String, primary: Boolean, onClick: () -> Unit) =
        TextView(this).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(if (primary) 0xFF0B1F3A.toInt() else Color.WHITE)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            if (primary) {
                background = GradientDrawable().apply {
                    cornerRadius = dp(22).toFloat()
                    setColor(0xFF8AB4F8.toInt())
                }
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            }
        }

    /** Seçim ekranın alt yarısındaysa düğmeleri üste taşır. */
    private fun placeBar() {
        val sel = cropView.selectionRect()
        val toTop = sel != null && sel.centerY() > cropView.height / 2f
        val lp = bar.layoutParams as FrameLayout.LayoutParams
        val newGravity = (if (toTop) Gravity.TOP else Gravity.BOTTOM) or
            Gravity.CENTER_HORIZONTAL
        if (lp.gravity != newGravity) {
            lp.gravity = newGravity
            lp.topMargin = if (toTop) dp(56) else 0
            lp.bottomMargin = if (toTop) 0 else dp(40)
            bar.layoutParams = lp
        }
    }

    private fun copy() {
        try {
            ClipStore.copy(this, cropView.cropped())
            // Android 13+ zaten kendi "Kopyalandı" bildirimini gösteriyor.
            if (Build.VERSION.SDK_INT < 33) toast("Panoya kopyalandı")
        } catch (e: Exception) {
            toast("Kopyalanamadı")
        }
        done()
    }

    private fun share() {
        try {
            ClipStore.share(this, cropView.cropped())
        } catch (e: Exception) {
            toast("Paylaşılamadı")
        }
        done()
    }

    private fun save() {
        val ok = ClipStore.saveToGallery(this, cropView.cropped())
        toast(if (ok) "Galeriye kaydedildi" else "Kaydedilemedi")
        done()
    }

    private fun done() {
        if (ShotHolder.bitmap === shot) ShotHolder.bitmap = null
        finish()
    }

    override fun onStop() {
        super.onStop()
        // Ana ekrana çıkılırsa seçim ekranı açık kalmasın.
        if (!isFinishing && !isChangingConfigurations) done()
    }

    override fun onDestroy() {
        if (ShotHolder.bitmap === shot) ShotHolder.bitmap = null
        super.onDestroy()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
