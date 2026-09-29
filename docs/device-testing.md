# Physical-device testing

Procedures and acceptance results for verifying the live wallpaper on a physical Android device.
This page is deliberately device-agnostic about identity: the device is referred to only as
"the physical device", and its model, OEM, and serial number are omitted from public surfaces per
the project's device-privacy policy. Android version is non-identifying and is recorded below; the
firmware build string embeds the model identifier, so it is withheld.

## Connecting

Prefer wireless debugging (Android 11+, API 30+) so no USB cable or vendor ID is needed:

```sh
adb pair <host>:<port> <pairing-code>   # one-time pairing from Developer options
adb connect <host>:<port>
adb devices                             # confirm the device is listed as "device"
```

A USB connection works too; the steps below are transport-independent.

## Install and launch

```sh
adb install -r app-debug.apk   # replaces the previous debug build in place
adb shell am start -n \
  io.github.cmp0xff.astrolabewallpaper.debug/io.github.cmp0xff.astrolabewallpaper.SettingsActivity
```

In **Astrolabe Wallpaper**, tap **Open wallpaper preview**, then apply it to the home and lock screens.

## Inspecting state

```sh
adb shell dumpsys wallpaper                            # active component and visibility
adb shell pidof io.github.cmp0xff.astrolabewallpaper.debug
adb logcat --pid=<pid> -v time                         # follow the running wallpaper process
adb shell screenrecord /sdcard/clock.mp4               # record; press Ctrl-C to stop
adb pull /sdcard/clock.mp4
```

## Lifecycle transitions

Each #2 transition is mapped to the `ClockEngine` callback it should trigger, so an observation can
be checked against the expected handler.

| Transition | Expected `ClockEngine` behavior |
| --- | --- |
| Preview open | `onCreateEngine`, then `onVisibilityChanged(true)` draws and schedules the next tick |
| Preview close | `onVisibilityChanged(false)` cancels the pending tick |
| Lock / unlock (lit lock screen) | `onVisibilityChanged(false)` / `(true)` across the surface switch |
| Screen off | `onVisibilityChanged(false)` cancels the pending tick |
| Screen on | `onVisibilityChanged(true)` redraws and reschedules from wall time |
| Surface recreation (no visibility change) | `onSurfaceDestroyed` cancels the pending tick; `onSurfaceChanged` redraws and reschedules while visible |
| Reboot | Process and engine recreated; clock resumes from the device wall time |

## Observed results

Test build: local debug `app-debug.apk` from `feat/2-device-feasibility` (SHA-256
881cb234654bd4967e3c9ca4c9a84003723ec8ac94f5ce2c80f2d133d58b5eef; logging-only change,
rendering unchanged).

Rendering was later extracted into `DialRenderer` on `feat/19-rendered-clock` with no intended
visual change. The reviewed renderer preserves the original `#D8B66A` dial colour. Host verification
on 2026-09-29 uses `DialRendererTest` on SDK 26 and 36: four cardinal-time checks anchor orientation,
and all three hands are independently checked at 12:20:43, 04:42:03, and 08:03:23. Literal expected
angles, isolated 3x3 presence windows, and single-pixel overdraw probes account for pixel rounding
and antialiasing; the test documents the distances from other hands and ticks. It also checks the
tick bands, non-square canvas, palette, and renderer reuse on a 200x200 bitmap.

Regression checks first confirmed that the original-colour assertion rejects `#D8B26A`. Temporarily
setting each hand's length to zero, rotating it by 90 degrees, or doubling its length (one change at
a time) failed all three dispersed-time checks on both SDKs for every hand. All nine mutations were
restored before final verification.

`AstrolabeWallpaperServiceTest` covers resuming ticks after surface recreation. `WallpaperFrameTest`
injects null/throwing acquisition, drawing failures, and posting failures through the engine's real
frame operation. It verifies exception logging, one posting attempt for each acquired canvas, and a
subsequent successful scheduled frame. Unrelated drawing exceptions still propagate after posting.
These are host checks only; they do not verify a physical wallpaper surface.

`./gradlew qualityGate :app:assembleDebug` passed with 78 tests per build variant (SDK 26/36 combined),
including strict detekt and Android Lint. `scripts/verify-apk.sh` verified the local debug APK, SHA-256
1c9d025ca9e37731fc744fdffd2d98148adb434802371bb4d153e1ea0c4a314e. Physical-device verification of
this revision remains unrun.

Android version: 16 (API 36)
Firmware build: withheld (embeds the model identifier)

The observations below are from the `feat/2-device-feasibility` build; the later `DialRenderer`
extraction has not been device-tested.

| Date | Surface | Transition | Observed |
| --- | --- | --- | --- |
| 2026-09-28 | preview | open | Clock advanced |
| 2026-09-28 | preview | close (apply) | Applied to home and lock screens via **Open wallpaper preview** → **Set wallpaper** |
| 2026-09-28 | home | apply | Clock advanced |
| 2026-09-28 | home | repeated lock/unlock | Clock advanced after each of three cycles; process survived |
| 2026-09-28 | home | screen off/on | Clock advanced |
| 2026-09-28 | lit lock | lock/unlock | Clock advanced |
| 2026-09-28 | lit lock | screen off/on | Clock advanced |
| 2026-09-28 | home + lit lock | reboot | Clock recovered; wallpaper persisted and service restarted |

Unresolved limitations: device verification of the `feat/19-rendered-clock` revision is unrun; the
last device-tested build (`feat/2-device-feasibility`) had no observed failures. Always On Display is
out of scope per #2.
