# Desktop Distribution Release Notes

`godot-java` desktop projects use a project-local runtime directory so the same Godot project layout works in the editor and in exported builds.

## Runtime Layout

Place `godot-java.gdextension` in the Godot project root and keep runtime files under `res://godot-java/`:

```text
godot-java/
  app.jar
  libgodot-java.dylib
  libgodot-java.so
  libgodot-java.dll
  VERSION
```

The reusable `.gdextension` template is available at `templates/godot-java.gdextension`.

## Maven Artifacts

Java projects depend on:

```xml
<dependency>
    <groupId>io.github.youngledo</groupId>
    <artifactId>godot-java-core</artifactId>
    <version>${godot-java.version}</version>
</dependency>
```

Native desktop libraries are published through `io.github.youngledo:godot-java-native` with platform classifiers:

- `macos-universal`
- `linux-x86_64`
- `linux-aarch64`
- `windows-x86_64`

The native artifact version must match the `godot-java-core` version embedded in the application jar.

## Application Project Flow

Application projects should make the build self-contained. The recommended
template configures Maven so `mvn package` creates `target/app.jar`, resolves
the matching `godot-java-native` classifier artifact, and writes the runtime
layout into the Godot project:

```bash
mvn package
```

The `scripts/sync-godot-java.sh` helper in this repository is for repository
examples, CI, and framework release verification. It is not the normal
developer-facing entry point for applications that consume released artifacts.

## Verification

Before a release, run the release-style verifier after building the native library for the current platform:

```bash
scripts/verify-release-style-distribution.sh --template ../godot-java-template
```

The verifier installs artifacts into a temporary Maven repository and builds the external template against that repository. The template build must populate the Godot runtime by resolving `godot-java-native` from Maven coordinates instead of source-tree paths.
