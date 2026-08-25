Import("env")

import os

project_dir = env.subst("$PROJECT_DIR")
pio_environment = env.subst("$PIOENV")
project_libdeps_dir = env.subst("$PROJECT_LIBDEPS_DIR")

generated_proto_dir = os.path.abspath(
    os.path.join(
        project_dir,
        "..",
        "..",
        "proto",
        "build",
        "generated",
        "source",
        "proto",
        "nanopb",
        "nanopb"
    )
)

nanopb_include_dir = os.path.join(
    project_libdeps_dir,
    pio_environment,
    "Nanopb"
)

env.Append(
    CPPPATH=[
        generated_proto_dir,
        nanopb_include_dir
    ]
)

env.BuildSources(
    os.path.join(
        env.subst("$BUILD_DIR"),
        "generated_proto"
    ),
    generated_proto_dir
)