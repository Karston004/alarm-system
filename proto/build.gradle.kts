import com.google.protobuf.gradle.*

plugins {
    `java-library`
    id("com.google.protobuf") version "0.9.4"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api("com.google.protobuf:protobuf-java:4.35.1")

    api("io.grpc:grpc-protobuf:1.62.2")
    api("io.grpc:grpc-stub:1.62.2")

    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}


// =========================================================
// Nanopb source set
//
// Uses the SAME protobuf definitions as main, but generates
// nanopb C output instead of Java.
// =========================================================

sourceSets {
    create("nanopb") {
        proto {
            setSrcDirs(
                listOf("src/main/proto")
            )
        }
    }
}


val nanopbPlugin =
    System.getenv("NANOPB_PLUGIN")


// =========================================================
// Protobuf generation
// =========================================================

protobuf {

    protoc {
        artifact =
            "com.google.protobuf:protoc:3.25.3"
    }


    plugins {

        // -------------------------------------------------
        // Java gRPC
        // -------------------------------------------------

        id("grpc") {
            artifact =
                "io.grpc:protoc-gen-grpc-java:1.62.2"
        }


        // -------------------------------------------------
        // Nanopb
        //
        // Merely configuring :proto does NOT require this
        // environment variable anymore.
        // -------------------------------------------------

        id("nanopb") {

            if (!nanopbPlugin.isNullOrBlank()) {
                path = nanopbPlugin
            }
        }
    }


    generateProtoTasks {

        // =================================================
        // Main
        //
        // Normal Java + gRPC generation.
        // Used by server/app/repos.
        // =================================================

        ofSourceSet("main").forEach { task ->

            task.plugins {
                id("grpc") { }
            }
        }


        // =================================================
        // Nanopb
        //
        // Explicit embedded/C generation.
        // =================================================

        ofSourceSet("nanopb").forEach { task ->

            // Java is automatically generated for Java
            // source sets, so remove it here.
            task.builtins {
                remove(
                    getByName("java")
                )
            }


            task.plugins {
                id("nanopb") { }
            }


            // Only complain about nanopb when somebody
            // ACTUALLY requests nanopb generation.
            task.doFirst {

                if (nanopbPlugin.isNullOrBlank()) {

                    throw GradleException(
                        """
                        NANOPB_PLUGIN is not set.

                        Install nanopb:
                            py -m pip install nanopb==0.4.9.1

                        Then set NANOPB_PLUGIN to the
                        protoc-gen-nanopb executable.
                        """.trimIndent()
                    )
                }
            }
        }
    }
}


// =========================================================
// Friendly nanopb task name
// =========================================================

tasks.register("generateNanopb") {
    group = "protobuf"

    description =
        "Generates nanopb C sources from the project protobuf schema."

    dependsOn(
        "generateNanopbProto"
    )
}