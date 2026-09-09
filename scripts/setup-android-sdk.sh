#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
: "${ANDROID_HOME:?Set ANDROID_HOME to your user-local Android SDK directory}"
case "$(uname -s)-$(uname -m)" in
  Darwin-arm64)
    archive=commandlinetools-mac_arm64-16111833_latest.zip
    checksum=58da8f215781cc0208743e88b65ae259780c55e7eb40e50373d377cf369232d6
    ;;
  Linux-x86_64)
    archive=commandlinetools-linux-16111833_latest.zip
    checksum=0877a1d048fe4a24efe2eff536ca4223f7adeb58648bb81909d33c446918cfa8
    ;;
  *) echo 'Use Android command-line tools 23.0 for your platform; see docs/development.md.' >&2; exit 1 ;;
esac
sdk_tools="$ANDROID_HOME/cmdline-tools/23.0"
if [[ ! -x "$sdk_tools/bin/sdkmanager" ]]; then
  download_dir=$(mktemp -d)
  trap 'rm -rf "$download_dir"' EXIT
  curl --fail --location --silent --show-error "https://dl.google.com/android/repository/$archive" -o "$download_dir/tools.zip"
  printf '%s  %s\n' "$checksum" "$download_dir/tools.zip" | shasum -a 256 --check
  unzip -q "$download_dir/tools.zip" -d "$download_dir"
  mkdir -p "$sdk_tools"
  cp -R "$download_dir/cmdline-tools/." "$sdk_tools/"
fi
"$sdk_tools/bin/android" --sdk="$ANDROID_HOME" --no-metrics sdk install \
  'platforms;android-37.0' 'build-tools;36.0.0' 'platform-tools'
