## Why

godot-java can already run Java code inside Godot, but users still need to manually assemble native libraries, jars, `.gdextension` paths, and JVM runtime assumptions before they can ship a desktop build. A production-ready claim needs a repeatable desktop export path that is documented, automated, and verified in CI.

## What Changes

- Add a standard desktop distribution layout for Godot projects using godot-java.
- Provide reusable `.gdextension` and project-side runtime templates for macOS, Linux, and Windows.
- Add build/sync automation that copies the user jar and platform native library into the expected Godot project layout.
- Define a Maven-native distribution path for godot-java native artifacts so the sync workflow can resolve native libraries by version instead of requiring manual downloads.
- Add desktop export smoke tests that export a minimal project and run the exported binary to prove the Java classpath, native library, and registration flow work outside the editor.

## Capabilities

### New Capabilities
- `desktop-export-distribution`: Defines the expected layout, automation, and verification for desktop exported godot-java projects.

### Modified Capabilities

## Impact

- Affects `godot-java-examples`, `.github/workflows/ci.yml`, native build scripts, user documentation, and release packaging.
- May introduce helper scripts, a Maven-oriented sync goal, and Maven-published native artifacts, but does not require a custom Godot editor or export template.
- Does not cover Android, iOS, Web, or GraalVM native-image export in this change.
