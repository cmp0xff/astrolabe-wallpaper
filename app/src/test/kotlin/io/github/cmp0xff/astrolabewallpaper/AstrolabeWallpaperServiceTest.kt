package io.github.cmp0xff.astrolabewallpaper

import android.annotation.SuppressLint
import android.app.WallpaperInfo
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.service.wallpaper.WallpaperService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** Verifies Android discovery, binding protection, metadata, and service teardown. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 36])
class AstrolabeWallpaperServiceTest {
    private val controller = Robolectric.buildService(AstrolabeWallpaperService::class.java)

    @Before
    fun createService() {
        controller.create()
    }

    @After
    fun destroyService() {
        controller.destroy()
    }

    // This test queries only its own package, which is always visible.
    @SuppressLint("QueryPermissionsNeeded")
    @Test
    fun wallpaperDeclaration() {
        val service = controller.get()
        val intent = Intent(WallpaperService.SERVICE_INTERFACE).setPackage(service.packageName)
        val matches = service.packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)
        assertEquals(1, matches.size)
        val info = matches.single()
        assertTrue(info.serviceInfo.exported)
        assertEquals("android.permission.BIND_WALLPAPER", info.serviceInfo.permission)
        val wallpaper = WallpaperInfo(service, info)
        assertEquals(ComponentName(service, AstrolabeWallpaperService::class.java), wallpaper.component)
        assertEquals(SettingsActivity::class.java.name, wallpaper.settingsActivity)
    }

    @Test
    fun independentEngineLifecycle() {
        val service = controller.get()
        val first = service.onCreateEngine()
        val second = service.onCreateEngine()
        assertNotSame(first, second)
        first.onVisibilityChanged(false)
        first.onDestroy()
        second.onVisibilityChanged(false)
        second.onDestroy()
    }

    @Test
    fun visibleSchedulesHiddenCancels() {
        val engine = controller.get().onCreateEngine()
        val looper = shadowOf(Looper.getMainLooper())

        engine.onVisibilityChanged(true)
        assertTrue(looper.nextScheduledTaskTime > Duration.ZERO)

        engine.onVisibilityChanged(false)
        assertEquals(Duration.ZERO, looper.nextScheduledTaskTime)
    }

    @Test
    fun destroyCancelsScheduledRedraw() {
        val engine = controller.get().onCreateEngine()
        val looper = shadowOf(Looper.getMainLooper())

        engine.onVisibilityChanged(true)
        assertTrue(looper.nextScheduledTaskTime > Duration.ZERO)

        engine.onDestroy()
        assertEquals(Duration.ZERO, looper.nextScheduledTaskTime)
    }
}
