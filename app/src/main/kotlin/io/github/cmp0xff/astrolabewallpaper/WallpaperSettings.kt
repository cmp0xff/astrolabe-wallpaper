package io.github.cmp0xff.astrolabewallpaper

/** One immutable settings snapshot used by a whole wallpaper frame. */
internal data class WallpaperSettings(val location: ObservingLocation?, val layers: DialLayers)
