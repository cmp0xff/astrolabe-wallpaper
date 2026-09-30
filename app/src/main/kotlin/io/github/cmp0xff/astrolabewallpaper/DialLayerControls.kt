package io.github.cmp0xff.astrolabewallpaper

import android.app.Activity
import android.widget.CheckBox

/** Initializes the layer controls from saved preferences and persists each explicit change. */
internal fun Activity.bindDialLayers() {
    val store = DialSettingsStore(applicationContext)
    val zodiac = findViewById<CheckBox>(R.id.zodiac_ring)
    val dayNight = findViewById<CheckBox>(R.id.day_and_night)
    val saved = store.load()
    zodiac.isChecked = saved.isZodiacRingEnabled
    dayNight.isChecked = saved.isDayAndNightEnabled
    zodiac.setOnCheckedChangeListener { _, checked ->
        store.save(store.load().copy(isZodiacRingEnabled = checked))
    }
    dayNight.setOnCheckedChangeListener { _, checked ->
        store.save(store.load().copy(isDayAndNightEnabled = checked))
    }
}
