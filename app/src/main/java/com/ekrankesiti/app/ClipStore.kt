package com.ekrankesiti.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kopyala / Paylaş: görsel uygulamanın gizli önbelleğine yazılır (galeride görünmez)
 * ve 24 saat sonra silinir.
 * Kaydet: Resimler/Ekran Kesiti klasörüne kaydeder.
 */
object ClipStore {

    private const val MAX_AGE_MS = 24L * 60 * 60 * 1000

    private fun dir(ctx: Context): File =
        File(ctx.cacheDir, "clips").apply { mkdirs() }

    private fun cleanup(ctx: Context) {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        dir(ctx).listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }

    private fun writeTemp(ctx: Context, bmp: Bitmap): Uri {
        cleanup(ctx)
        val f = File(dir(ctx), "kesit_${System.currentTimeMillis()}.png")
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
    }

    fun copy(ctx: Context, bmp: Bitmap) {
        val uri = writeTemp(ctx, bmp)
        val cm = ctx.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newUri(ctx.contentResolver, "Ekran kesiti", uri))
    }

    fun share(act: Activity, bmp: Bitmap) {
        val uri = writeTemp(act, bmp)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        act.startActivity(
            Intent.createChooser(send, null)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }

    fun saveToGallery(ctx: Context, bmp: Bitmap): Boolean {
        val name = "Kesit_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/Ekran Kesiti"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = ctx.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        return try {
            resolver.openOutputStream(uri)?.use {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
            } ?: throw IOException("Akış açılamadı")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }
}
