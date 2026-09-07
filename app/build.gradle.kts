import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.FailOnSeverity
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("dev.detekt")
}

android {
    namespace = "io.github.cmp0xff.astrolabewallpaper"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "io.github.cmp0xff.astrolabewallpaper"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        checkAllWarnings = true
        warningsAsErrors = true
        abortOnError = true
        checkTestSources = true
    }
}

androidComponents {
    beforeVariants { variantBuilder ->
        variantBuilder.hostTests.getValue("UnitTest").enable = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = true
        freeCompilerArgs.addAll(
            "-Wextra",
            "-Xjsr305=strict",
            "-Xjspecify-annotations=strict",
            "-Xnullability-annotations=@org.jetbrains.annotations:strict," +
                "@androidx.annotation:strict,@android.annotation:strict",
            "-Xreturn-value-checker=full",
            "-Xrender-internal-diagnostic-names",
        )
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.10")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    detektPlugins("dev.detekt:detekt-rules-ktlint-wrapper:2.0.0-alpha.6")
    add("kotlinCompilerClasspath", "org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.10")
}

detekt {
    toolVersion = "2.0.0-alpha.6"
    buildUponDefaultConfig = true
    allRules = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    failOnSeverity = FailOnSeverity.Warning
    ignoreFailures = false
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    exclude("**/build/**")
}

tasks.withType<Test>().configureEach {
    systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
}
