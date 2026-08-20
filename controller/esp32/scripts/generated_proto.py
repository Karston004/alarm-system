Import("env")

import os

project_dir = env.subst("$PROJECT_DIR")

generated_proto_dir = os.path.abspath(
    os.path.join(
        project_dir,
        "..",
        "..",
        "proto",
        "build",
        "generated",
        "sources",
        "proto",
        "main",
        "nanopb"
    )
)

env.Append(
    CPPPATH=[generated_proto_dir]
)

env.BuildSources(
    os.path.join(
        env.subst("$BUILD_DIR"),
        "generated_proto"
    ),
    generated_proto_dir
)