plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.karstonn.alarm.repos.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        )
    }
}

dependencies {
    implementation(project(":repos:java"))
    implementation(project(":proto"))

    implementation("androidx.datastore:datastore:1.2.1")
    implementation("androidx.datastore:datastore-guava:1.2.1")

    implementation("com.google.guava:guava:33.6.0-android")
}