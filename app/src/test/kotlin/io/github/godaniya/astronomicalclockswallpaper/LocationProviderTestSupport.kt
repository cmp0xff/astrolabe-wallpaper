package io.github.godaniya.astronomicalclockswallpaper

import android.location.LocationListener
import android.location.LocationManager
import org.robolectric.shadows.ShadowLocationManager

/** Retains listener access for late-callback tests; Robolectric exposes no nondeprecated replacement. */
@Suppress("DEPRECATION")
internal fun ShadowLocationManager.networkListeners(): List<LocationListener> =
    getLocationUpdateListeners(LocationManager.NETWORK_PROVIDER)

/** Synchronizes both API 26 location mode and Robolectric 4.17's provider-enabled field. */
internal fun ShadowLocationManager.enableNetworkProvider() {
    // When the first call changes location mode, the shadow skips updating its provider field.
    // Repeating the call updates that field too, preventing a spurious disabled callback on registration.
    setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
    setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
}
