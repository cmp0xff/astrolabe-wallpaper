# Dependency and artwork provenance

All application source and the placeholder Canvas/vector dial are original project work under
[Apache-2.0](../LICENSE). No astronomy engine, star/city catalog, location library, or external artwork
is bundled in this bootstrap. Astronomy Engine integration and its notices belong to #4.

| Input | Source | License / use |
| --- | --- | --- |
| Kotlin standard library 2.4.10 | [JetBrains Kotlin](https://github.com/JetBrains/kotlin/tree/v2.4.10) | Apache-2.0; runtime |
| JetBrains annotations 13.0 (transitive) | [java-annotations](https://github.com/JetBrains/java-annotations) | Apache-2.0; Kotlin's annotation dependency |
| Android framework API | [Android Open Source Project](https://source.android.com/) | Device-provided framework; SDK governed by Android SDK terms |
| Gradle wrapper 9.6.1 | [Gradle](https://github.com/gradle/gradle/tree/v9.6.1) | Apache-2.0; generated scripts/JAR retained, build only |
| Android Gradle Plugin 9.3.2 | [Android tools](https://android.googlesource.com/platform/tools/base/) | Apache-2.0; build only |
| detekt 2.0.0-alpha.6 | [detekt](https://github.com/detekt/detekt/tree/v2.0.0-alpha.6) | Apache-2.0; analysis only |
| ktlint 1.8.0 | [ktlint](https://github.com/pinterest/ktlint/tree/1.8.0) | MIT; analysis/formatting only |
| JUnit 4.13.2 | [JUnit 4](https://github.com/junit-team/junit4/tree/r4.13.2) | EPL-1.0; tests only |
| Robolectric 4.16.1 | [Robolectric](https://github.com/robolectric/robolectric/tree/robolectric-4.16.1) | MIT; tests only |
| Hamcrest (JUnit transitive dependency) | [Hamcrest](https://github.com/hamcrest/JavaHamcrest) | BSD-3-Clause; tests only |
| Eclipse Temurin 21.0.12.1+1 | [Adoptium](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1) | GPL-2.0 with Classpath Exception; build/test JDK only |

Resolved dependency graphs can be inspected with `./gradlew :app:dependencies` and
`./gradlew :app:dependencyInsight --configuration debugRuntimeClasspath --dependency kotlin-stdlib`.
The detekt wrapper shades ktlint; the pinned upstream
[version catalog](https://github.com/detekt/detekt/blob/v2.0.0-alpha.6/gradle/libs.versions.toml)
records ktlint 1.8.0. Upstream artifacts retain their embedded notices. Review the full resolved graph and packaging
notices again when adding runtime dependencies or preparing distribution in #7/#8.
