#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK directory}"
apk=${1:-app/build/outputs/apk/debug/app-debug.apk}
mkdir -p build/reports
"$ANDROID_HOME/cmdline-tools/23.0/bin/apkanalyzer" manifest print "$apk" > build/reports/apk-manifest.xml
python3 - <<'PY'
import xml.etree.ElementTree as ET

android = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse('build/reports/apk-manifest.xml').getroot()
assert manifest.get('package') == 'io.github.cmp0xff.astrolabewallpaper.debug'
sdk = manifest.find('uses-sdk')
assert sdk.get(android + 'minSdkVersion') == '26'
assert sdk.get(android + 'targetSdkVersion') == '37'
assert not manifest.findall('uses-permission'), 'Bootstrap must not request permissions'
application = manifest.find('application')
assert application.get(android + 'debuggable') == 'true'
service = application.find('service')
assert service.get(android + 'name') == 'io.github.cmp0xff.astrolabewallpaper.AstrolabeWallpaperService'
assert service.get(android + 'exported') == 'true'
assert service.get(android + 'permission') == 'android.permission.BIND_WALLPAPER'
assert service.find('intent-filter/action').get(android + 'name') == 'android.service.wallpaper.WallpaperService'
metadata = service.find('meta-data')
assert metadata.get(android + 'name') == 'android.service.wallpaper'
assert metadata.get(android + 'resource')
print('APK application ID, SDK levels, debug flag, permissions, and wallpaper declaration verified.')
PY
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs "$apk" | tee build/reports/apk-signature.txt
# The certificate identity distinguishes the disposable Android debug key from a release key.
rg --quiet 'Signer #1 certificate DN: .*CN=Android Debug' build/reports/apk-signature.txt
shasum -a 256 "$apk" | tee build/reports/apk-sha256.txt
