# Physical-device testing

Procedures and acceptance results for verifying the live wallpaper on a physical Android device.
This page is deliberately device-agnostic about identity: the device is referred to only as
"the physical device", and its model, OEM, and serial number are omitted from public surfaces per
the project's device-privacy policy. Android version and firmware build are non-identifying
software metadata and are recorded below, as #2 and README.md require.

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

<!-- Filled in during the physical-device session. Record Android version and firmware build, but
     keep model, OEM, and serial number out. -->

Android version: <!-- e.g. 15 -->
Firmware build: <!-- non-identifying build string; redact any model/OEM fragment -->

| Date | Surface | Transition | Observed |
| --- | --- | --- | --- |
| <!-- YYYY-MM-DD --> | home | preview open/close | <!-- advancing clock, recovery --> |
| <!-- YYYY-MM-DD --> | lit lock | lock/unlock, screen off/on, reboot | <!-- advancing clock, recovery --> |

Unresolved limitations: <!-- none, or describe. Always On Display is out of scope per #2. -->
