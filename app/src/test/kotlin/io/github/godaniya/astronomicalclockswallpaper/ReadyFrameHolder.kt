package io.github.godaniya.astronomicalclockswallpaper

import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.SurfaceHolder

/** A valid simulated surface for testing the engine's actual frame callback. */
internal class ReadyFrameHolder(delegate: SurfaceHolder) : SurfaceHolder by delegate {
    private val texture = SurfaceTexture(0)
    private val readySurface = Surface(texture)
    private val canvas = Canvas()

    override fun getSurface(): Surface = readySurface

    override fun lockCanvas(): Canvas = canvas

    override fun unlockCanvasAndPost(canvas: Canvas) = Unit

    fun release() {
        readySurface.release()
        texture.release()
    }
}
