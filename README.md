# Astrolabe Wallpaper

An independent Android live wallpaper with an original astrolabe-style astronomical
clock, initially targeting a personal Android device. The first delivery is a signed
personal APK; store distribution (F-Droid, Galaxy Store, Play Store) is a later
evaluation.

**Status: Android bootstrap.** The app provides a static Canvas wallpaper and a
settings activity that opens Android's wallpaper preview. Strict local and CI checks
produce a debug APK. Home-screen and lit lock-screen behavior must be
verified on the installed firmware in #2 before compatibility is claimed.

## Planned experience

- A moving clock with optional Sun, Moon and lunar phase, planet, star, horizon,
  ecliptic, sunrise/sunset, and twilight layers.
- An original dial with persistent size, position, and brightness controls.
- Initial setup requests current location through Android's built-in location API
  and accepts approximate location. If permission is denied, location is disabled,
  or the request fails or times out, offer an offline city chooser or coordinate
  entry. Save the selection and allow an explicit refresh from settings.
- Clock time follows the phone's timezone. Astronomy uses the saved observing
  location; changing the phone's timezone does not move that location.
- Offline runtime operation, with no proprietary SDK dependencies. Location
  acquisition is optional; manual setup must work without connectivity.

The first release covers the home screen and **lit** lock screen. Always On
Display and interactive sky exploration are outside its scope. This project is
independent.

## Technical direction

- Kotlin, Android Canvas, and `WallpaperService`, plus a small settings app.
- Stable release application ID: `io.github.cmp0xff.astrolabewallpaper`.
- [Astronomy Engine](https://github.com/cosinekitty/astronomy) (MIT) for astronomical
  calculations, retaining upstream license and dependency notices when integrated. See
  [dependency provenance](docs/dependencies.md#astronomy-engine-maintenance-assessment)
  for its maintenance status. Any bundled star or city data must have documented
  provenance and licensing.
- Pinned JDK 21, Gradle 9.6.1, AGP 9.3.2, Kotlin 2.4.10, and Android API 37,
  with minimum API 26. See [development setup](docs/development.md) for exact versions.
- Stop rendering while hidden, release resources with the wallpaper lifecycle,
  and refresh correctly after waking and time or timezone changes.

## Build and checks

Install the [pinned local toolchain](docs/development.md#local-setup), then run:

```sh
./gradlew qualityGate :app:assembleDebug
scripts/verify-apk.sh
```

`./gradlew check` also runs the complete gate. Use `./gradlew formatKotlin` to explicitly
format Kotlin source, tests, and Gradle scripts. CI never reformats files.

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Download the
`debug-apk-<source revision>` artifact from the **Android quality gate** GitHub Actions
run; reports and tool versions are included. Debug installs use
`io.github.cmp0xff.astrolabewallpaper.debug` and a disposable debug key. The release
ID remains `io.github.cmp0xff.astrolabewallpaper`; no release key is needed for checks.

See [checking policy, exceptions, and artifact instructions](docs/development.md).

## Roadmap

The [GitHub milestones](https://github.com/cmp0xff/astrolabe-wallpaper/milestones)
and issues are the live backlog. Dependencies below are prerequisites for closing
an issue; preparatory work may overlap where practical.

| Milestone | Issue | Depends on |
| --- | --- | --- |
| v0.1 — Device feasibility | #1 Bootstrap Android project and CI | None |
| v0.1 — Device feasibility | #2 Verify live wallpaper on the physical device | #1 |
| v0.2 — Functional astrolabe | #3 Implement current location and manual fallback | #2 |
| v0.2 — Functional astrolabe | #4 Implement astronomical calculations | #1 |
| v0.2 — Functional astrolabe | #5 Build the dial and display settings | #2, #3, #4 |
| v0.3 — Personal APK | #6 Qualify lifecycle, accuracy and battery behavior | #5 |
| v0.3 — Personal APK | #7 Package a signed personal release | #6 |
| Future — distribution | #8 Prepare F-Droid packaging and evaluate reproducibility | #7 |
| Future — distribution | #14 Evaluate Galaxy Store distribution | #7 |
| Future — distribution | #15 Evaluate Play Store distribution | #7 |

A future iOS presence is a separate project — an astronomy clock app with
Lock/Home-Screen widgets in Swift/SwiftUI — not part of this roadmap (#16).

## Distribution and signing

Retain one release-signing key from the first durable personal APK. Keep the key
and passwords outside Git and maintain a private backup. Releases must identify
the source tag and publish the APK checksum and signing-certificate fingerprint;
installation and upgrades must preserve settings. Git commit signing and APK
release signing are separate concerns.

The same Android APK can reach F-Droid, Galaxy Store, and Play Store; the stores
differ in signing and policy rather than the app itself.

Future F-Droid work will audit dependencies and assets against its
[inclusion policy](https://f-droid.org/en/docs/Inclusion_Policy/) and investigate
reproducible builds. F-Droid can distribute a developer-signed APK after verifying
reproducibility; otherwise its signing key can differ. See the
[F-Droid signing guidance](https://f-droid.org/en/docs/FAQ_-_App_Developers/#what-about-signing).

Galaxy Store distributes standard live-wallpaper APKs and can reuse the retained
personal key; it requires a Samsung Seller Portal account (Commercial Seller,
D-U-N-S, bank/PayPal details) and the Android identity-verification rollout.
Samsung's Galaxy Themes is a separate proprietary theme ecosystem and is not the
path for this `WallpaperService` APK.

Play Store is a normal live-wallpaper category; a $25 one-time developer fee and
Play App Signing apply. Under Play App Signing Google holds the distribution key
and the project uploads with an upload key, so the Play signature differs from the
personal/Galaxy key. Target-API, policy, Data-safety, and privacy-policy compliance
apply.

Because personal and Galaxy Store builds can share one key while Play App Signing
and F-Droid use different keys, updates are not seamless across stores. Do not
promise cross-store signature continuity before each path is verified. Submission
is deferred to the later decisions in #8, #14, and #15.

The App Store is not a live-wallpaper target: iOS exposes no public API for
third-party live wallpapers. A future iOS presence would be a separate product — an
astronomy clock app with Lock/Home-Screen widgets in Swift/SwiftUI — not a port of
this wallpaper. See #16.

## Contributing

Read [AGENTS.md](AGENTS.md) for issue-linked worktrees, Conventional Commits,
verification, and AI attribution. Use the issue and pull request templates.
Record physical-device evidence separately from emulator or automated results,
including the Android version and firmware build; omit personal identifiers
and precise personal locations from public reports.

## License

Copyright 2026 cmp0xff and contributors. Licensed under [Apache-2.0](LICENSE).
Third-party dependencies and assets retain their own licenses and notices.
