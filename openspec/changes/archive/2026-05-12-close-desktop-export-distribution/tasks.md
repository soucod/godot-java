## 1. Layout and Templates

- [x] 1.1 Define the canonical `res://` layout for jars, native libraries, and `.gdextension` files.
- [x] 1.2 Update the integration-test example to use the canonical layout.
- [x] 1.3 Add reusable `.gdextension` templates for macOS, Linux, and Windows.
- [x] 1.4 Document the layout in English and Chinese getting-started docs.

## 2. Sync Automation

- [x] 2.1 Add a script or Maven-friendly task that builds and syncs the example jar.
- [x] 2.2 Add Maven-resolvable native artifact coordinates for supported desktop platforms.
- [x] 2.3 Extend the sync flow to resolve and copy the current platform native artifact.
- [x] 2.4 Add validation that reports missing jar, native library, native/core version mismatch, and JDK/JRE prerequisites with actionable errors.
- [x] 2.5 Wire the examples README to use the automated sync flow instead of manual copying.

## 3. Export Smoke Test

- [x] 3.1 Add a minimal export preset for the integration-test project.
- [x] 3.2 Add a smoke scene that exits with a success code after Java registration and one Java method call.
- [x] 3.3 Update CI to export and run the smoke project on Linux.
- [x] 3.4 Extend CI export smoke coverage to macOS and Windows where runner constraints allow.

## 4. Release Packaging

- [x] 4.1 Decide whether native libraries are published as platform artifactIds or platform classifiers.
- [x] 4.2 Configure Maven publication for native desktop artifacts with the same version as `godot-java-core`.
- [x] 4.3 Ensure release notes describe the desktop distribution layout, Maven native artifact resolution, and supported platforms.
- [x] 4.4 Add a final verification step that consumes release-style Maven artifacts instead of build-tree paths.
- [x] 4.5 Optionally attach native libraries to GitHub Releases as a manual fallback without making that the primary automated path.
