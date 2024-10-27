plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.vanniktech.publish) apply false
}

//// Top-level build file where you can add configuration options common to all sub-projects/modules.
//buildscript {
//    repositories {
//        google()
//        mavenCentral()
//        maven { url 'https://jitpack.io' }
//    }
//    dependencies {
//        classpath 'com.android.tools.build:gradle:8.7.0'
//        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.22'
//    }
//}
//
//task clean(type: Delete) {
//    delete rootProject.buildDir
//}