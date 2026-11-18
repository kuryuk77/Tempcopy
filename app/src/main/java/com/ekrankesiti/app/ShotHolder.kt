package com.ekrankesiti.app

import android.graphics.Bitmap

/** Son alınan ekran görüntüsü. Diske yazılmaz, sadece bellekte durur. */
object ShotHolder {
    @Volatile
    var bitmap: Bitmap? = null
}
