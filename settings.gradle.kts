pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Astronomy Engine's Kotlin/JVM build is published on JitPack only, so the JVM
        // artifact (and its transitive Kotlin stdlib) resolves from here. Coordinates are
        // pinned to a full commit SHA; JitPack caches per SHA, so the build is stable.
        maven("https://jitpack.io") { content { includeGroup("com.github.cosinekitty") } }
    }
}

rootProject.name = "AstrolabeWallpaper"
include(":app")
