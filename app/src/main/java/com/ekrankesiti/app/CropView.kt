package com.ekrankesiti.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Donmuş ekran görüntüsünü gösterir; parmakla dikdörtgen seçim yapılır.
 * - Boş yere sürükle: yeni seçim
 * - Köşeden sürükle: boyutlandır
 * - İçinden sürükle: taşı
 */
@SuppressLint("ViewConstructor")
class CropView(context: Context, private val bmp: Bitmap) : View(context) {

    var onSelectionChanged: (() -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val dst = RectF()
    private var scale = 1f
    private val sel = RectF()
    private var hasSel = false

    private enum class Mode { NONE, DRAW, MOVE }

    private var mode = Mode.NONE
    private var ax = 0f
    private var ay = 0f
    private var lastX = 0f
    private var lastY = 0f

    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val dimPaint = Paint().apply { color = 0x99000000.toInt() }
    private val lightDimPaint = Paint().apply { color = 0x44000000 }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = Color.WHITE
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f * density
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f * density, 0f, 0f, Color.BLACK)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scale = min(w.toFloat() / bmp.width, h.toFloat() / bmp.height)
        val dw = bmp.width * scale
        val dh = bmp.height * scale
        val l = (w - dw) / 2f
        val t = (h - dh) / 2f
        dst.set(l, t, l + dw, t + dh)
        hasSel = false
        sel.setEmpty()
    }

    private fun cx(x: Float) = x.coerceIn(dst.left, dst.right)
    private fun cy(y: Float) = y.coerceIn(dst.top, dst.bottom)

    /** 0: sol üst, 1: sağ üst, 2: sol alt, 3: sağ alt, -1: yok */
    private fun nearCorner(x: Float, y: Float): Int {
        val pts = arrayOf(
            sel.left to sel.top, sel.right to sel.top,
            sel.left to sel.bottom, sel.right to sel.bottom
        )
        val r = 36f * density
        var best = -1
        var bestD = Float.MAX_VALUE
        pts.forEachIndexed { i, (px, py) ->
            val d = hypot(x - px, y - py)
            if (d < r && d < bestD) {
                best = i
                bestD = d
            }
        }
        return best
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val corner = if (hasSel) nearCorner(x, y) else -1
                when {
                    corner >= 0 -> {
                        // Karşı köşeyi sabitle, sürüklenen köşeyle yeniden çiz.
                        ax = if (corner == 0 || corner == 2) sel.right else sel.left
                        ay = if (corner == 0 || corner == 1) sel.bottom else sel.top
                        mode = Mode.DRAW
                    }
                    hasSel && sel.contains(x, y) -> {
                        mode = Mode.MOVE
                        lastX = x
                        lastY = y
                    }
                    else -> {
                        ax = cx(x)
                        ay = cy(y)
                        hasSel = false
                        sel.set(ax, ay, ax, ay)
                        mode = Mode.DRAW
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                when (mode) {
                    Mode.DRAW -> {
                        val px = cx(x)
                        val py = cy(y)
                        sel.set(min(ax, px), min(ay, py), max(ax, px), max(ay, py))
                        hasSel = sel.width() > 12f * density && sel.height() > 12f * density
                    }
                    Mode.MOVE -> {
                        val dx = (x - lastX).coerceIn(dst.left - sel.left, dst.right - sel.right)
                        val dy = (y - lastY).coerceIn(dst.top - sel.top, dst.bottom - sel.bottom)
                        sel.offset(dx, dy)
                        lastX = x
                        lastY = y
                    }
                    Mode.NONE -> {}
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = Mode.NONE
                if (!hasSel) sel.setEmpty()
                invalidate()
                onSelectionChanged?.invoke()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(bmp, null, dst, bmpPaint)
        if (!sel.isEmpty) {
            canvas.save()
            canvas.clipOutRect(sel)
            canvas.drawRect(dst, dimPaint)
            canvas.restore()
            canvas.drawRect(sel, borderPaint)
            val hr = 7f * density
            canvas.drawCircle(sel.left, sel.top, hr, handlePaint)
            canvas.drawCircle(sel.right, sel.top, hr, handlePaint)
            canvas.drawCircle(sel.left, sel.bottom, hr, handlePaint)
            canvas.drawCircle(sel.right, sel.bottom, hr, handlePaint)
        } else {
            canvas.drawRect(dst, lightDimPaint)
            canvas.drawText(
                "Kopyalamak istediğiniz alanı çizin",
                width / 2f, height * 0.42f, hintPaint
            )
        }
    }

    fun selectionRect(): RectF? = if (hasSel) RectF(sel) else null

    /** Seçili alanı orijinal çözünürlükte döndürür; seçim yoksa tüm ekranı. */
    fun cropped(): Bitmap {
        if (!hasSel) return bmp
        val l = ((sel.left - dst.left) / scale).roundToInt().coerceIn(0, bmp.width - 1)
        val t = ((sel.top - dst.top) / scale).roundToInt().coerceIn(0, bmp.height - 1)
        val r = ((sel.right - dst.left) / scale).roundToInt().coerceIn(l + 1, bmp.width)
        val b = ((sel.bottom - dst.top) / scale).roundToInt().coerceIn(t + 1, bmp.height)
        return Bitmap.createBitmap(bmp, l, t, r - l, b - t)
    }
}
