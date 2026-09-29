#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK directory}"
apk=${1:-app/build/outputs/apk/debug/app-debug.apk}
mkdir -p build/reports
"$ANDROID_HOME/cmdline-tools/23.0/bin/apkanalyzer" manifest print "$apk" > build/reports/apk-manifest.xml
python3 - <<'PY'
import sys
import xml.etree.ElementTree as ET

android = '{http://schemas.android.com/apk/res/android}'


def check(condition, message):
    if not condition:
        print(f'APK verification failed: {message}', file=sys.stderr)
        raise SystemExit(1)


manifest = ET.parse('build/reports/apk-manifest.xml').getroot()
check(manifest.get('package') == 'io.github.cmp0xff.astrolabewallpaper.debug',
      'unexpected application ID')
sdk = manifest.find('uses-sdk')
check(sdk is not None, 'missing uses-sdk element')
check(sdk.get(android + 'minSdkVersion') == '26', 'minSdkVersion must be 26')
check(sdk.get(android + 'targetSdkVersion') == '37', 'targetSdkVersion must be 37')
permission_names = [e.get(android + 'name') for e in manifest.iter() if e.tag.startswith('uses-permission')]
check(permission_names == ['android.permission.ACCESS_COARSE_LOCATION'],
      'unexpected permissions: expected only ACCESS_COARSE_LOCATION')
application = manifest.find('application')
check(application is not None, 'missing application element')
check(application.get(android + 'debuggable') == 'true', 'application must be debuggable')
service = application.find('service')
check(service is not None, 'missing service element')
check(service.get(android + 'name') == 'io.github.cmp0xff.astrolabewallpaper.AstrolabeWallpaperService',
      'service name mismatch')
check(service.get(android + 'exported') == 'true', 'service must be exported')
check(service.get(android + 'permission') == 'android.permission.BIND_WALLPAPER',
      'service must require BIND_WALLPAPER')
action = service.find('intent-filter/action')
check(action is not None, 'missing intent-filter action')
check(action.get(android + 'name') == 'android.service.wallpaper.WallpaperService',
      'missing wallpaper intent-filter action')
metadata = service.find('meta-data')
check(metadata is not None, 'missing meta-data element')
check(metadata.get(android + 'name') == 'android.service.wallpaper', 'meta-data name mismatch')
check(metadata.get(android + 'resource'), 'meta-data must declare a resource')
print('APK application ID, SDK levels, debug flag, permissions, and wallpaper declaration verified.')
PY
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs "$apk" | tee build/reports/apk-signature.txt
# The certificate identity distinguishes the disposable Android debug key from a release key.
grep -Eq 'Signer #1 certificate DN: .*CN=Android Debug' build/reports/apk-signature.txt
shasum -a 256 "$apk" | tee build/reports/apk-sha256.txt
