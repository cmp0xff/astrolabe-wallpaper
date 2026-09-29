package io.github.cmp0xff.astrolabewallpaper

import android.graphics.Canvas
import android.util.Log
import android.view.SurfaceHolder

/** Acquires, draws, and posts one frame, logging handled surface and rendering failures. */
internal fun drawWallpaperFrame(holder: SurfaceHolder, draw: (Canvas) -> Unit) {
    val surface = holder.surface
    if (surface == null || !surface.isValid) {
        // A missing surface can be expected during lifecycle changes; debug level distinguishes
        // that temporary state from an acquisition or rendering failure.
        Log.d(TAG, "skipping frame: surface not ready")
        return
    }
    val canvas = lockCanvasOrNull(holder) ?: return
    try {
        containRenderFailure { draw(canvas) }
    } finally {
        unlockCanvasAndPost(holder, canvas)
    }
}

// Logs the reason exactly once when no canvas is available: either the exception or a null return.
private fun lockCanvasOrNull(holder: SurfaceHolder): Canvas? {
    try {
        val canvas = holder.lockCanvas()
        if (canvas == null) {
            Log.w(TAG, "skipping frame: lockCanvas returned null")
        }
        return canvas
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "skipping frame: lockCanvas failed (surface released)", e)
    } catch (e: IllegalStateException) {
        Log.w(TAG, "skipping frame: lockCanvas failed (invalid surface state)", e)
    }
    return null
}

// Posts the canvas back, logging handled argument/state failures rather than propagating them.
private fun unlockCanvasAndPost(holder: SurfaceHolder, canvas: Canvas) {
    try {
        holder.unlockCanvasAndPost(canvas)
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "unlockCanvasAndPost failed: surface already released", e)
    } catch (e: IllegalStateException) {
        Log.e(TAG, "unlockCanvasAndPost failed: invalid surface state", e)
    }
}

private const val TAG = "AstrolabeWallpaperService"
