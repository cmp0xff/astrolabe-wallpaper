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
851bdd8639d0f571569024bfbb5f27861f31ba94aa1c09634e54e1f3f5c205a7, the same artifact that was
installed on the physical device below.

Device verification on 2026-09-29 installed that APK and confirmed core rendering and ticking on the
physical device. Geometry was measured from `adb shell screencap` frames against the angles `ClockState`
produces: 60 tick strokes (12 long, 48 short) and three hands whose reach was 0.504, 0.754, and 0.851 x
the dial radius, matching the 0.50, 0.75, and 0.85 the renderer specifies for the hour, minute, and
second hand. At a capture near device time 17:01:49 the hour hand measured 151.03 degrees and the
minute hand 10.53, within 0.4 degrees of the 150.9 and 10.8 that wall time implies; the second hand
read 288.13 degrees, or 48 s. A lock-screen capture measured the same structure, 12 long and 48 short
ticks with hands reaching 0.505, 0.754, and 0.850 x radius, all three within 0.32 degrees of what
17:04:44 implies. Across 15 frames spanning 9.3 s the second hand's angle stayed within 0.15 degrees of
a whole 6 degree multiple, the discrete once-per-second step the engine schedules rather than a smooth
sweep. `adb logcat` for the wallpaper process reported no skipping-frame or renderer warnings while the
dial was visible.

The device's screenshot pipeline applies a colour transform (`#111923` reads back as `#131922`, and
`#D8B66A` as `#D1BC7E`), so pixel checks against a capture must allow for that offset. The shift is
far larger than the 4/255 green difference between `#D8B66A` and the rejected `#D8B26A`, so captures
cannot adjudicate that palette detail; `DialRendererTest` remains the authority for it.

Android version: 16 (API 36)
Firmware build: withheld (embeds the model identifier)

The `2026-09-28` observations below are from the `feat/2-device-feasibility` build. The `2026-09-29`
observations are from `feat/19-rendered-clock` at `c3bd25b`, which carries the `DialRenderer`
extraction.

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
| 2026-09-29 | preview | open | Dial rendered: dark slate background, gold dial, 60 ticks, 3 hands |
| 2026-09-29 | preview | close (apply) | Applied to home and lock screens via **Open wallpaper preview** → **Set wallpaper** |
| 2026-09-29 | home | apply | Measured 12 long + 48 short ticks and 3 hands at 0.504/0.754/0.851 x radius; hands matched the device clock |
| 2026-09-29 | home | ticking | Second hand held whole 6 degree steps, 6 degrees per second |
| 2026-09-29 | lit lock | apply | Same dial on the lock screen: 12 long + 48 short ticks, hands at 0.505/0.754/0.850 x radius |

Unresolved limitations: neither device-tested build had an observed failure. Reboot and
surface-recreation behavior is documented above but was not re-run for `feat/19-rendered-clock`; only
the `2026-09-28` rows cover those transitions. Always On Display is out of scope per #2.

## Location slice (#3)

Test build: local debug `app-debug.apk` from `feat/3-current-location` at cf30cf5 (APK SHA-256
7299ef24f4758ea53409650e8b1551319e3801f5654c71b1fdd1540a4e236647).

Android version: 16 (API 36)
Firmware build: withheld (embeds the model identifier)

| Date | Check | Observed |
| --- | --- | --- |
| 2026-09-28 | manual coordinates + relaunch | Saved `45.5, -120.25` as MANUAL; persisted through force-stop and relaunch |
| 2026-09-28 | use current location (grant) | Approximate-location prompt; stored a real network fix as CURRENT_COARSE; no failure logs |
| 2026-09-28 | refresh | Re-fetched cleanly; current value retained |
| 2026-09-28 | deny permission | Permission-denied toast; prior selection preserved |
| 2026-09-28 | location off → refresh | "Could not get the current location" toast; logged "network location provider disabled"; prior selection preserved |
| 2026-09-28 | corrupt stored prefs | Discarded invalid values; no crash; fell back to "No observing location set." |

Unresolved limitations: the current-location display shows raw double precision (cosmetic, noted for a
follow-up). The offline city chooser half of #3 is a separate follow-up issue.
