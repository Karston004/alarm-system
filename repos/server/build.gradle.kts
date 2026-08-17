plugins {
    id("java-library")
}

base {
    archivesName.set("turso-repo")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(project(":proto"))
    implementation(project(":repos:java"))
    implementation(libs.gson)
}