plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.vanniktech.publish)
}

android {
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    lint {
        targetSdk = 35
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        targetSdk = 35
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = false
        buildConfig = true
    }

    //    https://github.com/Kotlin/kotlinx.coroutines/blob/master/README.md#avoiding-including-the-debug-infrastructure-in-the-resulting-apk
    packaging {
        resources.excludes += "DebugProbesKt.bin"
        resources.merges.addAll(
            listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
            )
        )
    }
    namespace = "com.telemetrydeck.sdk"
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.google.play.billing)
    implementation(project(":lib"))

}

mavenPublishing {
    coordinates("com.telemetrydeck", "kotlin-sdk-google-services", "6.2.2")

    pom {
        name = "TelemetryDeck SDK Google Services"
        description =
            "Google Services facilities for Kotlin SDK for TelemetryDeck, a privacy-conscious analytics service for apps and websites"
        url = "https://telemetrydeck.com"

        licenses {
            license {
                name = "MIT License"
                url = "https://raw.githubusercontent.com/TelemetryDeck/KotlinSDK/main/LICENSE"
            }
        }

        developers {
            developer {
                id = "winsmith"
                name = "Daniel Jilg"
                url = "https://github.com/winsmith"
                organization = "TelemetryDeck GmbH"
            }
        }

        scm {
            url = "https://github.com/TelemetryDeck/KotlinSDK"
        }
    }
}