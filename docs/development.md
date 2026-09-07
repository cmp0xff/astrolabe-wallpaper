# Android development

## Pinned toolchain

| Component | Version |
| --- | --- |
| Eclipse Temurin JDK | 21.0.12.1+1 (HotSpot) |
| Gradle wrapper | 9.6.1 |
| Android Gradle Plugin (AGP) | 9.3.2 |
| Kotlin compiler and standard library | 2.4.10 |
| detekt plugin, engine, ktlint wrapper | 2.0.0-alpha.6 |
| Wrapped ktlint | 1.8.0 |
| Android compile/target SDK | API 37 (`platforms;android-37.0`) |
| Android Build Tools | 36.0.0 |
| Android command-line tools | 23.0, archive build 16111833 |
| JUnit | 4.13.2 |
| Robolectric | 4.16.1 |

The versions align with [detekt's tested toolchain](https://detekt.dev/docs/introduction/compatibility/),
with the subsequent [AGP 9.3.2 patch](https://developer.android.com/build/releases/agp-9-3-0-release-notes).
The detekt prerelease is an accepted development dependency; it is not packaged into the APK.
AGP supplies built-in Kotlin integration. The build explicitly pins the Kotlin Gradle plugin dependency
and compiler classpath instead of applying the separate Kotlin Android plugin. Gradle's embedded Kotlin
for build scripts is independent of the application's compiler; `./gradlew --version` records it.
Java and Kotlin produce Java 17 bytecode while Gradle runs on JDK 21.

API 26 is a conservative initial minimum, not a device compatibility claim. API 37 is the compile/target
level supported by AGP 9.3. Physical A57 behavior and firmware qualification remain in #2.

## Local setup

Android Studio is optional. Install the pinned JDK, `curl`, `unzip`, Python 3, and ripgrep (`rg`).
On Apple Silicon macOS, download and verify the official Temurin archive:

```sh
curl -fL 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.12.1_1.tar.gz' -o /tmp/astrolabe-jdk.tar.gz
printf '%s  %s\n' 3623232f33a9c3baadf304480b2535f9a3cba8a58d42ecbb438ba267315d9998 /tmp/astrolabe-jdk.tar.gz | shasum -a 256 --check
mkdir -p "$HOME/Library/Java/JavaVirtualMachines"
tar -xzf /tmp/astrolabe-jdk.tar.gz -C "$HOME/Library/Java/JavaVirtualMachines"
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/jdk-21.0.12.1+1/Contents/Home"
unset ANDROID_SDK_ROOT
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Use the matching Linux x64 archive from the
[Temurin release](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1)
on Linux, verify its published SHA-256, and set `JAVA_HOME` to the extracted JDK and `ANDROID_HOME` to
`$HOME/Android/Sdk`. CI downloads and checksum-verifies the exact Linux JDK archive; setup-java does not
accept this release's four-part version string.
Keep these exports in your own shell configuration; do not commit local SDK paths.

From the checkout:

```sh
scripts/setup-android-sdk.sh
java -version
./gradlew --version
./gradlew qualityGate :app:assembleDebug
scripts/verify-apk.sh
```

The setup script supports Apple Silicon macOS and Linux x64. It verifies the command-line archive's
SHA-256 and installs API 37.0, Build Tools 36.0.0, and platform-tools in `ANDROID_HOME`. Other platforms
can install command-line tools 23.0 manually and run the same package installation. The Android CLI
may present SDK license terms; review and accept them to install the SDK. Downloads and Gradle caches
are outside maintained source. Initial builds require network access; the app itself works offline.

## Checking policy

| Python role | Kotlin / Android equivalent | Enforcement |
| --- | --- | --- |
| mypy / Pyright | Kotlin compiler | Mandatory type checking for production and tests |
| Strict type/warning options | Kotlin compiler | `allWarningsAsErrors`, `-Wextra`, strict Java nullability, full unused-return-value checking |
| Ruff / pylint correctness rules | detekt | All rules, upstream defaults, strict config validation, fail on warnings |
| Framework/platform analysis | Android Lint | All warnings including normally disabled checks, test sources, warnings as errors |
| Black / Ruff formatting | ktlint through detekt | Official style, all optional wrapped rules, four spaces, 120 columns |
| pytest | JUnit + Robolectric | Activity behavior, manifest discovery, and service lifecycle |

`./gradlew qualityGate` compiles and tests debug and release variants, runs type-resolved
`detektDebug`, `detektRelease`, `detektDebugUnitTest`, and `detektReleaseUnitTest`, checks all maintained
Kotlin files and Gradle scripts through root detekt, and runs `lintDebug` and `lintRelease`.
`./gradlew check` includes the same gate. Gradle Kotlin DSL warnings and Gradle deprecation warnings
also fail the build. Generated build output, caches, and downloaded SDK sources are excluded.

`./gradlew formatKotlin` is the explicit formatting command. The quality gate and CI never reformat.
There are no baselines or blanket suppressions. Fix findings first; for a demonstrated false positive
or incompatible rules, record the rule ID, concrete example, reason, and narrow scope below and in the
PR. Removing a finding by lowering global severity or excluding production/test directories is not a fix.

## Rule exceptions

| Rule ID | Example and reason | Scope |
| --- | --- | --- |
| detekt `UnnecessaryInnerClass` | `StaticEngine : WallpaperService.Engine()` needs its enclosing service because the superclass is a Java non-static inner class. detekt does not recognize that implicit outer-instance use. | Only `StaticEngine`, annotated in source |
| Kotlin `DEPRECATION` | The preview test reads a `ComponentName` with the legacy `getParcelableExtra` overload because the typed overload is unavailable on its API 26 test environment. | Only the local test value reading this extra |
| Lint `QueryPermissionsNeeded` | `queryIntentServices` in the manifest test restricts the query to its own package, which is always visible. Adding external package queries would misstate app needs. | Only `wallpaperDeclaration` test method |
| Lint `UnsupportedChromeOsHardware` | `android.software.live_wallpaper` is required because wallpaper rendering is the app's core feature; devices lacking it cannot provide that feature. | Only that manifest `uses-feature` element |
| Lint `AndroidGradlePluginVersion` | Lint suggests Gradle 9.7.1 over 9.6.1. The explicit 9.6.1 pin follows the selected detekt compatibility family; network-discovered upgrade suggestions must not change this bootstrap's agreed toolchain. | Only `gradle/wrapper/gradle-wrapper.properties`, via `app/lint.xml` |

Upstream defaults remain the starting point, including per-rule defaults for test documentation and
magic numbers. Tests are still compiled with the same strict compiler and analyzed with type resolution;
no entire test or production source directory is excluded. Dependency and artwork provenance is in
[dependencies.md](dependencies.md).

See [bootstrap-verification.md](bootstrap-verification.md) for the local positive and negative checks.

## Tests and artifacts

Robolectric tests use API 26 and API 36 environments. They are JVM simulations and do not establish
physical A57, lit lock-screen, or actual wallpaper surface behavior. API 37 compilation and Android
Lint additionally check against the selected target. The placeholder never schedules animation work.

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`, with application ID
`io.github.cmp0xff.astrolabewallpaper.debug`. `scripts/verify-apk.sh` checks its ID, SDK metadata,
wallpaper declaration, absence of requested permissions, and debug signature, then records SHA-256.
The stable release ID is `io.github.cmp0xff.astrolabewallpaper`; release signing belongs to #7.
Debug signing keys are disposable and local/CI APKs may require uninstalling the previous debug app.

GitHub Actions runs on pull requests and pushes to `main`. Actions use immutable commit references,
and the job has only `contents: read`. Open the **Android quality gate** run and download
`debug-apk-<source revision>` or `check-reports-<source revision>`. The PR run checks GitHub's merge
revision, recorded in the artifact name and `toolchain.txt`. Check reports upload even on failure;
the APK uploads only after a successful gate and APK verification. No release credentials are used.

Install a downloaded debug APK with `adb install -r app-debug.apk`, open **Astrolabe Wallpaper**, and
tap **Open wallpaper preview**. Device testing is deliberately left to #2, including actual firmware,
wake/surface/process recreation, home/lock-screen behavior, and battery observation.
