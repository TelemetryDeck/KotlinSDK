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
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    lint {
        targetSdk = 34
    }

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

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
//    implementation(libs.androidx.lifecycle.extensions)
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
    implementation(libs.logback)


    testImplementation(libs.junit)
    testImplementation(libs.androidx.arch.core)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

//    testImplementation(libs.mockito.core)
//    testImplementation(libs.mockito.android)
//    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.android)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.robolectric)
}

//
//def ktor_version = '2.3.7'
//def logback_version = '1.2.10'
//def kotlinx_coroutines_version = '1.6.0'
//dependencies {
//
//    implementation 'androidx.core:core-ktx:1.7.0'
//    implementation 'androidx.appcompat:appcompat:1.4.0'
//    implementation 'com.google.android.material:material:1.4.0'
////    HTTP Client
//    implementation "io.ktor:ktor-client-core:$ktor_version"
//    implementation "io.ktor:ktor-client-okhttp:$ktor_version"
////    HTTP Serialization
//    implementation "io.ktor:ktor-client-serialization:$ktor_version"
//    implementation "io.ktor:ktor-client-content-negotiation:$ktor_version"
//    implementation "io.ktor:ktor-serialization-kotlinx-json:$ktor_version"
//    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1"
//    implementation "org.jetbrains.kotlinx:kotlinx-serialization-properties:1.5.1"
////    HTTP Logging
//    implementation "ch.qos.logback:logback-classic:$logback_version"
//    implementation "io.ktor:ktor-client-logging:$ktor_version"
//
//    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version"
//    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinx_coroutines_version"
//    implementation "androidx.lifecycle:lifecycle-process:2.4.0"
//    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.4.0"
//    implementation "androidx.lifecycle:lifecycle-extensions:2.2.0"
//
//    testImplementation 'junit:junit:4.13.2'
//    androidTestImplementation 'androidx.test.ext:junit:1.1.3'
//    testImplementation "androidx.test:runner:1.4.0"
//    testImplementation "androidx.arch.core:core-testing:2.1.0"
//    androidTestImplementation 'androidx.test.espresso:espresso-core:3.4.0'

//    testImplementation 'org.mockito:mockito-core:4.2.0'
//    androidTestImplementation 'org.mockito:mockito-android:4.2.0'
//    testImplementation "org.mockito.kotlin:mockito-kotlin:4.0.0"
//    testImplementation "org.robolectric:robolectric:4.7.3"
//}
//
//tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
//    kotlinOptions {
//        freeCompilerArgs += "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
//        freeCompilerArgs += "-opt-in=kotlinx.coroutines.ObsoleteCoroutinesApi"
//    }
//}
//
//mavenPublishing {
//    coordinates("com.telemetrydeck", "kotlin-sdk", "2.2.0")
//
//    pom {
//        name = 'TelemetryDeck SDK'
//        description = "Kotlin SDK for TelemetryDeck, a privacy-conscious analytics service for apps and websites"
//        url = "https://telemetrydeck.com"
//
//        licenses {
//            license {
//                name = "MIT License"
//                url = "https://raw.githubusercontent.com/TelemetryDeck/KotlinSDK/main/LICENSE"
//            }
//        }
//
//        developers {
//            developer {
//                id = "winsmith"
//                name = "Daniel Jilg"
//                url = "https://github.com/winsmith"
//                organization = "TelemetryDeck GmbH"
//            }
//        }
//
//        scm {
//            url = "https://github.com/TelemetryDeck/KotlinSDK"
//        }
//    }
//
//// publishToMavenCentral and signAllPublications are configured in gradle.properties
////    // Configure publishing to Maven Central
////    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
////
////    // Enable GPG signing for all publications
////    signAllPublications()
//}