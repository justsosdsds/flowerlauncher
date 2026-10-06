package com.flower.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.AtomicFile
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** null means transparent: let FLAG_SHOW_WALLPAPER display the actual system surface.
 * A wallpaper COLOR is not an image and must never be used as an opaque fallback.
 */
internal data class HomeWallpaperSnapshot(val image: ImageBitmap?, val revision: Any)

@Composable
internal fun rememberHomeWallpaperSnapshot(selectionRevision: Int): HomeWallpaperSnapshot {
    val context = LocalContext.current
    val systemRevision = remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { systemRevision.intValue++ }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    val key = listOf(selectionRevision, systemRevision.intValue)
    val snapshot by produceState(HomeWallpaperSnapshot(null, key), key) {
        // Discard old wallpaper immediately; decoding does not block gestures or composition.
        value = HomeWallpaperSnapshot(null, key)
        val image = withContext(Dispatchers.IO) { HomeWallpaperStore.load(context)?.asImageBitmap() }
        value = HomeWallpaperSnapshot(image, key)
    }
    // Both key and image identity participate so asynchronous decode invalidates the cache.
    val currentImage = snapshot.image.takeIf { snapshot.revision == key }
    return HomeWallpaperSnapshot(currentImage, listOf(key, currentImage))
}

@Composable
internal fun HomeWallpaperLayer(wallpaper: HomeWallpaperSnapshot, modifier: Modifier = Modifier) {
    val image = wallpaper.image ?: return
    Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}

/** The picker grants access to one image. Keep a bounded private copy; no broad storage permission. */
internal object HomeWallpaperStore {
    private fun file(context: Context) = AtomicFile(File(context.filesDir, "home_wallpaper.png"))
    private fun prefs(context: Context) = context.getSharedPreferences("home_wallpaper", Context.MODE_PRIVATE)

    fun useSystem(context: Context) {
        check(prefs(context).edit().putBoolean("use_image", false).commit())
    }

    fun load(context: Context): Bitmap? {
        if (!prefs(context).getBoolean("use_image", false)) return null
        return try {
            file(context).openRead().use { BitmapFactory.decodeStream(it) }
        } catch (failure: Exception) {
            Log.w("HomeCache", "Wallpaper image unavailable; using system wallpaper", failure)
            null
        }
    }

    fun importImage(context: Context, uri: Uri) {
        val metrics = context.resources.displayMetrics
        val maxEdge = maxOf(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(1)
        val bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val ratio = minOf(1f, maxEdge.toFloat() / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize(maxOf(1, (info.size.width * ratio).toInt()),
                    maxOf(1, (info.size.height * ratio).toInt()))
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image" }
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > maxEdge * 2) inSampleSize *= 2
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: error("Cannot decode wallpaper")
            // Respect camera orientation on Android 8/8.1 too.
            val orientation = context.contentResolver.openInputStream(uri)?.use {
                android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)
            } ?: 1
            val matrix = android.graphics.Matrix().apply {
                when (orientation) {
                    2 -> setScale(-1f, 1f)
                    3 -> setRotate(180f)
                    4 -> setScale(1f, -1f)
                    5 -> { setRotate(90f); postScale(-1f, 1f) }
                    6 -> setRotate(90f)
                    7 -> { setRotate(270f); postScale(-1f, 1f) }
                    8 -> setRotate(270f)
                }
            }
            val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (oriented !== decoded) decoded.recycle()
            val ratio = minOf(1f, maxEdge.toFloat() / maxOf(oriented.width, oriented.height))
            val scaled = Bitmap.createScaledBitmap(oriented, maxOf(1, (oriented.width * ratio).toInt()),
                maxOf(1, (oriented.height * ratio).toInt()), true)
            if (scaled !== oriented) oriented.recycle()
            scaled
        }
        try {
            val target = file(context)
            val output = target.startWrite()
            try {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                target.finishWrite(output)
            } catch (failure: Throwable) {
                target.failWrite(output)
                throw failure
            }
            check(prefs(context).edit().putBoolean("use_image", true).commit())
        } finally {
            bitmap.recycle()
        }
    }
}
