## ADDED Requirements

### Requirement: Project template
The system SHALL provide a minimal Java-backed Godot project template that includes build configuration, Godot project files, `.gdextension` configuration, and documented source locations.

#### Scenario: User creates a project from template
- **WHEN** a user copies or generates the Java Godot project template
- **THEN** the project contains all required files to build Java code and run it from the Godot editor after sync

### Requirement: Build and sync command
The system SHALL provide a documented Maven workflow that builds Java sources and syncs the resulting artifacts into the Godot project.

#### Scenario: User updates Java code
- **WHEN** the user changes a Java class and runs the build and sync workflow
- **THEN** the Godot project receives the updated jar in the expected project-local location

### Requirement: Editor run workflow
The system SHALL provide a documented Maven workflow for launching the Godot editor or project with the correct Java runtime assumptions.

#### Scenario: User runs project from command line
- **WHEN** the user runs the documented command for the local platform
- **THEN** Godot starts with the godot-java native library and Java project artifacts available

### Requirement: Diagnostics
The system SHALL provide Maven-native diagnostics for common editor workflow failures, including missing JDK 25, missing native library, missing jar, wrong platform architecture, missing generated class registry, and invalid `.gdextension` paths.

#### Scenario: Missing jar is detected
- **WHEN** the user runs the diagnostic workflow and the project jar is missing
- **THEN** the diagnostic output identifies the missing jar and the build or sync command needed to create it

### Requirement: Restart guidance
The system SHALL document when Godot must be restarted or reopened after Java artifacts change.

#### Scenario: Java artifact changed
- **WHEN** the user rebuilds and syncs the Java jar
- **THEN** the workflow documentation states whether the current editor session can continue or must be restarted
