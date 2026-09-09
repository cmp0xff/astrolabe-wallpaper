# Physical device testing

Prepared ahead of the physical Galaxy A57 session for #2, while the device is unavailable. It
covers connecting a real device from macOS, Debian, and Windows, and the `adb` commands #2's
acceptance criteria need. Every claim about the installed A57 firmware specifically (menu wording,
exact screens, One UI behavior) is marked **unverified until the A57 session** below; everything
else is either general Android platform behavior or specific to this repository's own scripts and
was checked directly.

## Common to all three platforms

1. Enable developer options: **Settings → About phone → Software information → tap Build number
   seven times.** *Unverified until the A57 session: this is the stock Android path; One UI may
   label or nest these screens differently on the installed firmware.*
2. Enable **Developer options → USB debugging**. *Unverified until the A57 session: exact menu
   wording on this firmware.*
3. Prefer **Wireless debugging** over USB (Android 11+, so available on API 26+ target devices
   running a current OS): Developer options → Wireless debugging → Pair device with pairing code,
   then:

   ```sh
   adb pair HOST:PORT
   adb connect HOST:PORT
   ```

   This avoids USB drivers entirely and is the fastest path on Windows. The first connection from
   a new machine shows an RSA key fingerprint prompt on the phone that must be accepted there.

## macOS (this machine)

No driver needed. `adb` is already installed at `~/Library/Android/sdk/platform-tools/adb` (part
of the toolchain set up for #1) but is not on `PATH` by default — the export block in
[`development.md#local-setup`](development.md#local-setup) adds it. That block is not yet in this
machine's shell profile; run it in the current shell before using `adb`, or add it permanently.

## Debian

Either:

```sh
sudo apt install android-sdk-platform-tools
```

or run this repository's own `ANDROID_HOME=$HOME/Android/Sdk scripts/setup-android-sdk.sh`, which
installs `platform-tools` for Linux x86_64 alongside the pinned SDK platform and build tools (see
[`development.md#local-setup`](development.md#local-setup)).

USB access additionally needs a udev rule for Samsung's vendor ID (`04e8`):

```sh
echo 'SUBSYSTEM=="usb", ATTR{idVendor}=="04e8", MODE="0660", GROUP="plugdev"' \
  | sudo tee /etc/udev/rules.d/51-android.rules
sudo usermod -aG plugdev "$USER"
sudo udevadm control --reload-rules
sudo udevadm trigger
```

Log out and back in for the new group membership to take effect. Without this rule, `adb devices`
lists the device as `no permissions` instead of showing it as ready. Wireless debugging (above)
skips all of this.

## Windows

`scripts/setup-android-sdk.sh` does **not** support Windows (it only recognizes
`Darwin-arm64`/`Linux-x86_64`) — download the
[standalone platform-tools zip](https://developer.android.com/tools/releases/platform-tools) for
Windows separately and extract it. USB debugging needs either the Samsung USB driver or the Google
USB Driver (installable via `sdkmanager --install "extras;google;usb_driver"`); wireless debugging
avoids needing either. PR #9 records `gradlew.bat` execution as unrun, so building this project on
Windows is itself unverified — that would be a small, separate, worthwhile task before or during
the #2 session if a Windows machine is in scope.

## Debugging commands

The debug build's application ID is `io.github.cmp0xff.astrolabewallpaper.debug`; the launcher
activity is `SettingsActivity`.

```sh
adb install -r app-debug.apk
adb shell am start -n io.github.cmp0xff.astrolabewallpaper.debug/io.github.cmp0xff.astrolabewallpaper.SettingsActivity
adb shell dumpsys wallpaper                  # which wallpaper is applied to which surface
adb logcat --pid="$(adb shell pidof -s io.github.cmp0xff.astrolabewallpaper.debug)"
adb logcat -b crash                          # after an unexpected teardown
adb shell screenrecord /sdcard/a57.mp4       # redacted visual evidence for #2's acceptance criteria
```

`adb shell screenrecord` writes to the device; pull it with
`adb pull /sdcard/a57.mp4` before deleting it from the device with `adb shell rm /sdcard/a57.mp4`.

## Mapping #2's lifecycle transitions to what the code does

#2's acceptance criteria ask for preview open/close, repeated lock/unlock, screen off/on, and
reboot, each observed on both the home screen and the lit lock screen. In the current placeholder
(`AstrolabeWallpaperService.kt`; a separate architecture doc is planned once #9 merges), these map
to:

| Device event | Engine callback | Observable via |
| --- | --- | --- |
| Preview opens/closes, home/lock surface becomes visible/hidden | `onVisibilityChanged(visible)` | logcat, once #2's moving-clock prototype logs it |
| Screen rotates, surface is (re)created | `onSurfaceChanged(...)` | logcat |
| Process death and restart (e.g. after reboot, or the system reclaiming memory) | new `Engine` instance via `onCreateEngine()` | `dumpsys wallpaper`, `adb shell dumpsys activity services` |

The current code has no scheduled redraw and no persisted state, so nothing here is expected to
*fail* to recover — there is nothing running to lose. The moving-clock prototype #2 adds is what
will actually exercise this table, and its own logging is what the `logcat --pid` commands above
are for.

## Not yet done

Physical A57 testing itself (device model/Android/One UI/firmware build recording, actual
clock-advance and lock-screen observation, the lifecycle exercises above, and dated results with
redacted evidence) is #2's remaining acceptance work, not this document's. Everything in this file
is preparation for that session, run once the device is available.
