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
| Reboot | Process and engine recreated; clock resumes from the device wall time |

## Observed results

Test build: local debug `app-debug.apk` from `feat/2-device-feasibility` (SHA-256
881cb234654bd4967e3c9ca4c9a84003723ec8ac94f5ce2c80f2d133d58b5eef; logging-only change,
rendering unchanged).

Rendering was later extracted into `DialRenderer` with no output change, and is now verified by the
`DialRendererTest` pixel test; the extracted debug APK SHA-256 is
0e7f93eaf07835782b6d885f120264866af07eeade65a8d9993e779a32b50d4d (rendering unchanged;
pixel-tested).

Android version: 16 (API 36)
Firmware build: withheld (embeds the model identifier)

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

Unresolved limitations: none. Always On Display is out of scope per #2.
