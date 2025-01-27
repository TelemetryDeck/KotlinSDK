import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.publish)
}

android {
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    lint {
        targetSdk = 34
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        targetSdk = 34
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
        compose = true
        buildConfig = true
    }

    //    https://github.com/Kotlin/kotlinx.coroutines/blob/master/README.md#avoiding-including-the-debug-infrastructure-in-the-resulting-apk
    packaging {
        resources.excludes += "DebugProbesKt.bin"
        resources {
            pickFirsts += "META-INF/INDEX.LIST"
        }
    }
    namespace = "com.telemetrydeck.sdk"
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    // explicit dependency on compose seems to be required to use the compose compiler
    // this is not used by the library atm
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.appcompat)
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.properties)
    implementation(libs.ktor.client.logging)


    testImplementation(libs.junit)
    testImplementation(libs.androidx.arch.core)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.mockk)
    testImplementation(libs.mockk.android)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.robolectric)
}

mavenPublishing {
    coordinates("com.telemetrydeck", "kotlin-sdk", "4.0.3")

    pom {
        name = "TelemetryDeck SDK"
        description = "Kotlin SDK for TelemetryDeck, a privacy-conscious analytics service for apps and websites"
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

// publishToMavenCentral and signAllPublications are configured in gradle.properties
//    // Configure publishing to Maven Central
//    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
//
//    // Enable GPG signing for all publications
//    signAllPublications()
}