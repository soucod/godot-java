## ADDED Requirements

### Requirement: Canonical desktop project layout
The system SHALL define a canonical Godot project layout for desktop godot-java projects that includes the Java application jar, the godot-java native library for each supported platform, and a `.gdextension` file with relative paths.

#### Scenario: Project layout is present
- **WHEN** a user prepares a Godot project for godot-java desktop execution
- **THEN** the project contains the jar, native library, and `.gdextension` entries in the documented canonical locations

### Requirement: Automated desktop sync
The system SHALL provide an automated application-project workflow that copies the built Java application jar and the resolved platform native library into the canonical Godot project layout without requiring access to the godot-java source repository.

#### Scenario: Sync after package
- **WHEN** the user runs the documented application build workflow
- **THEN** the Godot project contains the latest Java jar and native library without manual copying

### Requirement: Maven-resolvable native artifacts
The system SHALL publish supported desktop godot-java native libraries as Maven-resolvable artifacts whose versions match `godot-java-core`.

#### Scenario: Native artifact resolves by version
- **WHEN** a user project depends on `godot-java-core` version `X` and runs its application build workflow for a supported desktop platform
- **THEN** the workflow resolves the matching native artifact version `X` through Maven and copies the native library into the Godot project

#### Scenario: Native and core version mismatch is detected
- **WHEN** the Java runtime artifact and native library artifact versions do not match
- **THEN** the sync or diagnostic workflow reports the mismatch before launching or exporting the Godot project

### Requirement: Exported desktop startup smoke test
The system SHALL verify at least one exported desktop build can start, load the godot-java GDExtension, register a Java class, execute a Java-backed scene, and exit with success.

#### Scenario: Exported game runs Java scene
- **WHEN** CI exports the smoke-test Godot project for a supported desktop platform
- **THEN** the exported binary starts, loads the Java jar, executes the test scene, and exits successfully

### Requirement: Official Godot binary compatibility
The desktop distribution workflow SHALL work with official Godot desktop binaries and SHALL NOT require a custom Godot editor or export template.

#### Scenario: Official editor opens prepared project
- **WHEN** a user opens the prepared project with an official Godot desktop editor
- **THEN** Godot loads the godot-java extension from the project-local native library
