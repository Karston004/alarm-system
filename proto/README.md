# Shared Protocol Buffers

Module `:proto` contains shared Protobuf schema and generator configuration for Java, gRPC and embedded nanopb C output. Its Gradle toolchain targets Java 21.

From the repository root:

```powershell
.\gradlew.bat :proto:generateProto
```

For nanopb:

```powershell
py -m pip install nanopb==0.4.9.1
```

Set `NANOPB_PLUGIN` to the location of the installed `protoc-gen-nanopb` executable in your **current shell**, then:

```powershell
.\gradlew.bat :proto:generateNanopb
```

The build script intentionally requires `NANOPB_PLUGIN` only for nanopb generation. The nanopb options file is resolved relative to `proto/src/main/proto`; generation uses `--error-on-unmatched`, so stale `.options` entries can fail generation.

When embedded controller development resumes, the PlatformIO integration expects generated nanopb sources under:

```text
proto/build/generated/source/proto/nanopb/nanopb/
```

If that directory is missing or the generator output layout changes, check the Gradle generation task before troubleshooting unrelated ESP32 source files. Code generation and firmware compilation are separate steps at present.

---

*Note: Generative Ai was used to aid the development of this document.*
