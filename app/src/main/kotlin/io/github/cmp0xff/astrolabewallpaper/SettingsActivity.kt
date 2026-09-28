package io.github.cmp0xff.astrolabewallpaper

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

/** Opens Android's preview and manages the observing location. */
class SettingsActivity : Activity() {
    private val locationStore by lazy { LocationStore(this) }
    private val locationProvider by lazy { LocationProvider(this) }
    private val locationCurrent by lazy { findViewById<TextView>(R.id.location_current) }
    private val latitudeInput by lazy { findViewById<EditText>(R.id.latitude_input) }
    private val longitudeInput by lazy { findViewById<EditText>(R.id.longitude_input) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<Button>(R.id.open_preview).setOnClickListener { openWallpaperPreview() }
        findViewById<Button>(R.id.use_current_location).setOnClickListener { requestCurrentLocation() }
        findViewById<Button>(R.id.refresh_location).setOnClickListener { requestCurrentLocation() }
        findViewById<Button>(R.id.save_location).setOnClickListener { saveManualLocation() }
        displayLocation(locationStore.load())
    }

    private fun openWallpaperPreview() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, AstrolabeWallpaperService::class.java),
        )
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.preview_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun requestCurrentLocation() {
        val hasPermission =
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION_PERMISSION)
            return
        }
        fetchCurrentLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_LOCATION_PERMISSION) {
            return
        }
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchCurrentLocation() {
        locationProvider.fetch { location ->
            if (location != null) {
                locationStore.save(location)
                displayLocation(location)
            } else {
                // Preserve the previous selection; prompt for manual entry.
                Toast.makeText(this, R.string.location_fetch_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveManualLocation() {
        val latitude = latitudeInput.text.toString().toDoubleOrNull()
        val longitude = longitudeInput.text.toString().toDoubleOrNull()
        if (latitude == null || longitude == null) {
            showInvalidLocation()
            return
        }
        if (!ObservingLocation.isValidLatitude(latitude) || !ObservingLocation.isValidLongitude(longitude)) {
            showInvalidLocation()
            return
        }
        val location = ObservingLocation(latitude, longitude, ObservingLocation.Source.MANUAL)
        locationStore.save(location)
        displayLocation(location)
        Toast.makeText(this, R.string.location_saved, Toast.LENGTH_SHORT).show()
    }

    private fun showInvalidLocation() {
        Toast.makeText(this, R.string.location_invalid, Toast.LENGTH_LONG).show()
    }

    private fun displayLocation(location: ObservingLocation?) {
        locationCurrent.text =
            if (location == null) {
                getString(R.string.location_unset)
            } else {
                formatLocation(location)
            }
    }

    private fun formatLocation(location: ObservingLocation): String {
        val source =
            when (location.source) {
                ObservingLocation.Source.CURRENT_COARSE -> getString(R.string.location_current_source)
                ObservingLocation.Source.MANUAL -> getString(R.string.location_manual_source)
            }
        return "${location.latitude}, ${location.longitude} ($source)"
    }

    private companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
    }
}
