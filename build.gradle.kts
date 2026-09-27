import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.FailOnSeverity

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    base
    id("com.android.application") version "9.3.2" apply false
    id("dev.detekt") version "2.0.0-alpha.6"
}

dependencies {
    detektPlugins("dev.detekt:detekt-rules-ktlint-wrapper:2.0.0-alpha.6")
}

detekt {
    toolVersion = "2.0.0-alpha.6"
    buildUponDefaultConfig = true
    allRules = true
    config.setFrom(files("config/detekt/detekt.yml"))
    source.setFrom(
        fileTree(rootDir) {
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", "**/.gradle/**", "**/.git/**", "**/.kotlin/**", "**/.android-sdk/**")
        },
    )
    failOnSeverity = FailOnSeverity.Warning
    ignoreFailures = false
}

tasks.register<Detekt>("formatKotlin") {
    description = "Explicitly format maintained Kotlin sources and Gradle scripts."
    group = "formatting"
    setSource(detekt.source)
    config.setFrom(detekt.config)
    buildUponDefaultConfig = true
    allRules = true
    disableDefaultRuleSets = true
    autoCorrect = true
}

tasks.register("qualityGate") {
    description = "Compile, analyze, check formatting, and test debug and release code."
    group = "verification"
    dependsOn(
        "detekt",
        ":app:compileDebugKotlin",
        ":app:compileReleaseKotlin",
        ":app:detektDebug",
        ":app:detektRelease",
        ":app:detektDebugUnitTest",
        ":app:detektReleaseUnitTest",
        ":app:lintDebug",
        ":app:lintRelease",
        ":app:testDebugUnitTest",
        ":app:testReleaseUnitTest",
    )
}

tasks.check {
    dependsOn("qualityGate")
}

tasks.wrapper {
    gradleVersion = "9.6.1"
    distributionType = Wrapper.DistributionType.BIN
    distributionSha256Sum = "9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14"
}
