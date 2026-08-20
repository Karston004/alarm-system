plugins {
    id("java-library")
    id("com.google.protobuf") version "0.10.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    api("com.google.protobuf:protobuf-java:4.35.1")
    api("io.grpc:grpc-stub:1.62.2")

    implementation("io.grpc:grpc-protobuf:1.62.2")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.62.2"
        }

        create("nanopb") {
            val nanopbPlugin =
                System.getenv("NANOPB_PLUGIN")
                    ?: throw GradleException(
                        """
                    NANOPB_PLUGIN is not set.

                    Install nanopb:
                        py -m pip install nanopb==0.4.9.1

                    Then set NANOPB_PLUGIN to the protoc-gen-nanopb executable.
                    """.trimIndent()
                    )

            path = nanopbPlugin
        }
    }

    generateProtoTasks {

        // Keep gRPC generation for all proto tasks.
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }

        // Generate Nanopb C output for the main proto source set.
        ofSourceSet("main").forEach { task ->
            task.plugins {
                create("nanopb") {
                    outputSubDir = "nanopb"
                }
            }
        }
    }
}