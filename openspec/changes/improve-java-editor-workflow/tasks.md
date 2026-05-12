## 1. Template Definition

- [x] 1.1 Define the minimal project template structure for Java source, Maven build files, Godot project files, and native artifacts.
- [x] 1.2 Extract common template files from the examples where possible.
- [x] 1.3 Add template documentation for Maven and IDE users.
- [x] 1.4 Add a smoke check that validates the template can build.

## 2. Workflow Commands

- [x] 2.1 Add an init or copy-template workflow for new projects.
- [x] 2.2 Add a sync workflow that copies jar and native artifacts into the Godot project.
- [x] 2.3 Add a run workflow that launches Godot with project-local artifacts.
- [x] 2.4 Add a doctor workflow that validates JDK, native library, jar, `.gdextension`, and generated registry state.

## 3. Documentation

- [x] 3.1 Update getting-started docs to use the template and workflow commands.
- [x] 3.2 Update troubleshooting docs with diagnostic output examples.
- [x] 3.3 Document restart requirements after jar and native library changes.
- [x] 3.4 Keep manual setup docs as an advanced fallback.

## 4. Editor Plugin Readiness

- [x] 4.1 Identify the command hooks a future Godot editor plugin would call.
- [x] 4.2 Keep workflow output stable enough for editor UI integration.
- [x] 4.3 Record remaining editor-plugin-specific requirements for a later change.
