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

Test build: local debug `app-debug.apk` from `feat/3-current-location` at a845a5d, the rebase of the
review fixes onto `main` at 7dbff58 (APK SHA-256
cf285da99ffc8b7cc3cc667b51fcf3042d01fd836751d17f16dd2391ed323310).

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
| 2026-09-29 | manual coordinates + relaunch | Entered `45.5, -120.25`, saved with **Save coordinates** ("Location saved." toast); the display read `45.5, -120.25 (manual)` and held it through a force-stop and relaunch |
| 2026-09-29 | use current location (grant) | The prompt asked only for the approximate location; granting stored a real network fix as CURRENT_COARSE, the display switched to `(current)`, and no failure was logged |
| 2026-09-29 | refresh | A fresh provider registration completed after one update; the current value was retained |

The `2026-09-28` rows apply to cf30cf5 only, and their failure paths (denial, disabled location,
corrupt preferences) have not been re-run since; those remain historical evidence alongside the
automated failure tests. The three happy paths above were re-verified on the rebased build on
`2026-09-29`. The rebase changed no application source, so the re-run and the API 26/36 suite
together cover the PR #23 review fixes (manual-save cancellation, permission-flow recreation, cache
age, provider failure recovery, wrongly typed preferences, localized coordinate entry, and settings
scrolling) on the post-merge codebase.

### Coordinate locale fix (2026-09-29)

Test build: local debug `app-debug.apk` from `feat/3-current-location` with the coordinate locale fix
on top of 5db6f02 (APK SHA-256
34c600ef0c162f8a32bd1c93852e2091ca3de92cae6505839243476bfa0a57a1). Same physical device, Android 16
(API 36), the device's own `de-DE` locale, and no locale override.

| Date | Check | Observed |
| --- | --- | --- |
| 2026-09-29 | manual entry, dot | Typed `1.2` / `2.3`; **Save coordinates** showed the "Location saved." toast, the display read `1.2, 2.3 (manual)`, and `observing_location.xml` held latitude 1.2 / longitude 2.3 as MANUAL |
| 2026-09-29 | manual entry, relaunch | Force-stop and relaunch restored `1.2, 2.3 (manual)` |
| 2026-09-29 | on-screen comma key | With the latitude field focused, Gboard's German numeric keypad accepted `,`, `0`, `5`, `.` in that order, leaving `,05.` in the field |
| 2026-09-29 | keyboard filter, before and after | The same `,`, `0`, `.` taps on a pre-fix build of 5db6f02 (APK SHA-256 f6e8a9e8759d5f321b71127ee3d59f2e4b2aae3e91eadabb105b6717b5bcabb1) left `0.` in the field: the layout filter admitted the dot and dropped the comma |
| 2026-09-29 | manual entry, comma | Entered `1,2` / `2,3` with `adb shell input text` (the virtual keymap's comma) and saved as `1.2, 2.3 (manual)` |

Coordinate entry uses `.` as its decimal separator in every locale and accepts the locale's separator
as an alias. `parseCoordinate` normalizes `.` to the locale decimal separator before parsing with the
locale-aware `NumberFormat`, and `CoordinateKeyListener` derives each field's accepted characters
from the same `textLocale`, so the character filter can no longer drop a separator the parser
accepts. That listener replaced the layout's `numberDecimal` filter, which under `de-DE` admitted `.`
but dropped the comma, leaving no fractional coordinate enterable on the deployment device.
`SettingsActivityLocaleTest` and `CoordinateKeyListenerTest` pin the behavior: `de-rDE` accepts `45,5`
and `8.5`, `ar-rEG` accepts `٤٥٫٥` and `45.5`, and `en-US` still filters `,` out of the field as a
grouping separator.

The `2026-09-29` manual-entry row in the table above the fix was driven with the app's locale
overridden to `en-US`, because `adb` synthetic input could not produce the comma the German keyboard
would; that override is no longer needed for manual entry, and the row stays as evidence for the
build it tested.

Unresolved limitations: the display now rounds coordinates to four decimals, as recorded under
"Coordinate display precision (2026-10-01)" below, and the entry fields are not seeded from the
saved site, so retyping a displayed coordinate loses the precision below the fourth decimal; that
residual is tracked in #38. The offline city chooser remains outstanding under #3.

## Saved-site timezone verification (#24)

Automated coverage uses fixed instants and explicit zones for site time, Prague's spring and
autumn DST boundaries, preference updates across multiple engines, hidden/destroyed engines,
and recreation. Settings tests change the phone zone while an acquisition is in flight to
verify capture at save time, and acquire on top of an already-saved site to verify that a refresh
retains its zone rather than re-capturing the phone's. These Robolectric checks do not establish
physical-device behavior.

Test build: local debug `app-debug.apk` from `feat/24-site-timezone` at d21c6a7 (APK SHA-256
d5e0235d5d772044324b02505e8d19486d13912f55b8c77852b21781d7dd8335).

Android version: 16 (API 36)
Firmware build: withheld (embeds the model identifier)

| Date | Check | Observed |
| --- | --- | --- |
| 2026-09-30 | save + display | Entered the neutral coordinates `35.68, 139.69`, which are not the device's location, and tapped **Save coordinates**; the display read `35.68, 139.69 (manual)` and `Timezone: Europe/Prague`, and the stored record held `"source":"MANUAL"` with `zoneId` `Europe/Prague` |
| 2026-09-30 | phone-zone change | Turned automatic zone selection off and moved the phone to `Asia/Kolkata` (UTC+05:30); `getprop persist.sys.timezone` and `date` both then showed +05:30, which is 3.5 h from the captured +02:00, and this was confirmed before any later observation |
| 2026-09-30 | core retention | Force-stop and relaunch while the phone was on `Asia/Kolkata` left the display and the record reading `Europe/Prague`. On the home screen at 20:38:36 UTC the dial's hands measured 320.5° and 234.0°; the saved site's civil time, 22:38:37 CEST, is 319.3° and 231.7°, while the status bar read 02:08 IST |
| 2026-09-30 | hide/show | Home, then system Settings, then Home: the dial redrew at the saved site's civil time (hands 324.0° and 275.0° at 20:45:26 UTC against 322.7° and 272.6° expected) and the provider pid was unchanged |
| 2026-09-30 | surface recreation | `wm size 1080x1921` then `wm size reset`: the dial recentred to (539,960) at radius 325 and back to (540,1170) at radius 324, the hands tracked the saved site throughout, and logcat showed `onSurfaceChanged`, `onSurfaceRedrawNeeded`, and a drawn window about 15 ms later in each direction, with no skipped or lost frames |
| 2026-09-30 | process restart | `am force-stop` emptied `pidof` and the provider did **not** rebind on returning home: the system fell back to its stock `ImageWallpaper` and the dial was gone until the wallpaper was applied again by hand, after which the saved site was still intact |
| 2026-09-30 | refresh inversion | With the phone restored to `Europe/Prague` and the saved site still on `Asia/Kolkata`, **Refresh location** replaced the manual record with a live coarse fix and re-captured the phone zone: the display and the record both flipped to `(current)` with `zoneId` `Asia/Kolkata` |
| 2026-09-30 | saved site, both directions | With the phone on `Europe/Prague` and the saved site on `Asia/Kolkata`, the dial showed the site's civil time (hands 70.0° and 109.5° at 20:47:57 UTC against 69.0° and 107.7° expected) while the status bar read 22:47 CEST, the reverse of the core retention row |

Applying the wallpaper by hand after a force-stop restores the saved site unchanged, but the system
does not rebind a force-stopped provider on its own, so a killed process leaves the stock wallpaper
in place. The `refresh inversion` row above records that build re-capturing the phone's current zone
and overwriting the saved one; that is the defect fixed by the build in **Refresh zone retention
(2026-09-30)** below, and the row stays as the d21c6a7 build's historical record.

A current-location acquisition with no site saved yet still captures the phone zone; establishing
each site's geographic timezone remains #24, with the offline city chooser in #21.

### Refresh zone retention (2026-09-30)

Test build: local debug `app-debug.apk` from `fix/24-refresh-zone-retention` at 12e4184, the fix
that keeps a saved site's zone when its coordinates are refreshed (APK SHA-256
eb78aa7167ead68e737ae11409432f5e9c533e0158fcf3fddcf59555abe61f6f).

Same physical device. Android version: 16 (API 36). Firmware build: withheld (embeds the model
identifier).

| Date | Check | Observed |
| --- | --- | --- |
| 2026-09-30 | save + baseline | Entered the neutral coordinates `35.68, 139.69` and tapped **Save coordinates** while the phone was on `Europe/Prague`; the display read `35.68, 139.69 (manual)` and `Timezone: Europe/Prague`, and the stored record held `"source":"MANUAL"` with `zoneId` `Europe/Prague` |
| 2026-09-30 | phone-zone change | Turned automatic zone selection off and moved the phone to `Asia/Kolkata` with `cmd alarm set-timezone`; `getprop persist.sys.timezone` read `Asia/Kolkata` and `date` read `Thu Oct 1 02:45:55 IST 2026`, which is 3.5 h from the baseline's +02:00, and this was confirmed before interpreting the refresh |
| 2026-09-30 | refresh retention | Tapped **Refresh location** with the phone still on `Asia/Kolkata`: the display and the record both flipped to `(current)`, so `source` became `CURRENT_COARSE` and the neutral coordinates were replaced by the live coarse fix, while the display's `Timezone:` line and the stored `zoneId` both stayed `Europe/Prague`. The live coordinates are the device's own position and are deliberately not recorded |

The run-as record read matched the on-screen `Timezone:` line in every row. The phone zone was
restored to `Europe/Prague` with automatic time-zone selection re-enabled afterwards. The pre-fix
behaviour this replaces is the `refresh inversion` row above, from the d21c6a7 build.

### Coordinate display precision (2026-10-01)

Test build: local debug `app-debug.apk` from `fix/24-refresh-zone-retention` with the coordinate
display fix on top of 12e4184 (APK SHA-256
6f2a54f349ce63de7fc87a3c413a1cca6264944013bc467cc6c2b9bbf457a409).

Same physical device. Android version: 16 (API 36). Firmware build: withheld (embeds the model
identifier).

| Date | Check | Observed |
| --- | --- | --- |
| 2026-10-01 | install + display | After reinstalling the APK, the record left by the run above — a `(current)` coarse fix — also rendered with four decimals on each side, with its `Timezone: Europe/Prague` line intact. The live coordinates are the device's own position and are deliberately not recorded |
| 2026-10-01 | high-precision entry | Entered `-33.86785` / `151.20732`, which are not the device's location, and tapped **Save coordinates**: the display read `-33.8678, 151.2073 (manual)`, while the stored record held `"latitude":-33.86785,"longitude":151.20732` unchanged, so only the display rounds. The value is rounded before it is formatted, which is why the fifth decimal does not carry |
| 2026-10-01 | neutral entry | Entered the neutral `35.68` / `139.69` and tapped **Save coordinates**: the display read `35.6800, 139.6900 (manual)` with `Timezone: Europe/Prague`, and the stored record held `"source":"MANUAL"` with `zoneId` `Europe/Prague` |

The run-as record read matched the on-screen `Timezone:` line in every row. The device ends on the
neutral record with the phone zone: **Refresh location** was tried twice at the end of the session
to restore the device's own site, and both attempts timed out after 10 s with the fetch-failure
toast, logging `network location update timed out after 10000ms` and leaving the display and the
record unchanged.

The display interpolates four decimals — about 11 m — and `.` in every locale, which matches the
coordinate entry convention recorded above. `formatCoordinate` rounds to four decimals and then
formats with `Locale.ROOT`, so the readout has a uniform width and does not change with the phone's
locale; the `Double.toString()` it replaces printed seventeen significant digits. `LocationStore`
is untouched: `save()` still writes the full `Double`, so a stored coordinate keeps the precision it
was entered with. Reading the rounded readout and retyping it is what now loses precision, because
the entry fields are not seeded from the saved site; that residual is tracked in #38.
