package io.github.cmp0xff.astrolabewallpaper

import android.app.Activity
import android.widget.CheckBox

/** Initializes the layer controls from saved preferences and persists each explicit change. */
internal fun Activity.bindDialLayers() {
    val store = DialSettingsStore(applicationContext)
    val zodiac = findViewById<CheckBox>(R.id.zodiac_ring)
    val sun = findViewById<CheckBox>(R.id.sun_layer)
    val saved = store.load()
    zodiac.isChecked = saved.isZodiacRingEnabled
    sun.isChecked = saved.isSunEnabled
    zodiac.setOnCheckedChangeListener { _, checked ->
        store.save(store.load().copy(isZodiacRingEnabled = checked))
    }
    sun.setOnCheckedChangeListener { _, checked ->
        store.save(store.load().copy(isSunEnabled = checked))
    }
}
