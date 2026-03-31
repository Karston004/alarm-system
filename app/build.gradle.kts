import com.google.protobuf.gradle.proto

plugins {
    alias(libs.plugins.android.application)
    id("com.google.protobuf") version "0.9.6"
}

android {
    namespace = "com.karstonn.alarm"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.karstonn.alarm"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    sourceSets {
        getByName("main") {
            proto {
                srcDir("../proto")
            }
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation("io.grpc:grpc-okhttp:1.76.0")
    implementation("io.grpc:grpc-protobuf-lite:1.76.0")
    implementation("io.grpc:grpc-stub:1.76.0")
    implementation("com.google.protobuf:protobuf-javalite:4.31.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.31.1"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.76.0"
        }
    }

    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java") {
                    option("lite")
                }
            }
            plugins {
                create("grpc") {
                    option("lite")
                }
            }
        }
    }
}