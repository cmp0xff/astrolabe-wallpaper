# Astronomical Clock Wallpaper

An independent, offline Android live wallpaper of astronomical clocks, with the
Prague Orloj dial first, built with Kotlin, Canvas, and `WallpaperService`.

The intended design uses one selected observing site for both the sky and its
civil clock. Prague Orloj supplies the visual and projection reference for the
first dial, with geometry adapted to that site. See the
[product contract](docs/design.md).

The first release targets home and **lit** lock screens. Always On Display and
interactive sky exploration are outside its scope.

## Release plan

The project is still in development; no public release has been published yet.
The initial distribution plan is a personal APK. F-Droid distribution will be
evaluated later.

## Build and checks

Install the [pinned local toolchain](docs/development.md#local-setup), then run:

```sh
./gradlew qualityGate :app:assembleDebug
scripts/verify-apk.sh
```

`./gradlew check` also runs the complete gate. Use `./gradlew formatKotlin` to
explicitly format Kotlin source, tests, and Gradle scripts. CI never reformats files.

## Documentation

- [Product design and observing-site contract](docs/design.md)
- [Development setup, checking policy, and build artifacts](docs/development.md)
- [Astronomy calculations and implementation limits](docs/astronomy.md)
- [Orloj dial geometry and scope](docs/orloj.md)
- [Dependency and artwork provenance](docs/dependencies.md)
- [Physical-device procedures and evidence](docs/device-testing.md)
- [Bootstrap verification](docs/bootstrap-verification.md)

The [GitHub issues](https://github.com/godaniya/astronomical-clocks-wallpaper/issues) and
[milestones](https://github.com/godaniya/astronomical-clocks-wallpaper/milestones) track
unfinished work and release planning.

## Contributing

Read [AGENTS.md](AGENTS.md) for issue-linked worktrees, Conventional Commits,
verification, and AI attribution. Use the issue and pull request templates.
Record physical-device evidence separately from emulator or automated results,
including the Android version and APK/source revision. Withhold the device model,
OEM, serial number, firmware build string, personal identifiers, and precise
personal locations from public reports.

## License

Copyright 2026 Godānīya contributors. Licensed under [Apache-2.0](LICENSE).
Third-party dependencies and assets retain their own licenses and notices.
