## Why

godot-java users can write annotated Java classes, but the editor workflow still depends on manual jar deployment, classpath setup, and restart discipline. A smoother workflow should let users build, sync, and diagnose Java project state without needing to understand the binding internals.

## What Changes

- Define a project-template workflow for Java-backed Godot projects.
- Add Maven-first automation for project sync, run, and diagnostics.
- Improve editor-facing project behavior by using stable project-local assets and clear generated files.
- Defer a full Godot editor plugin until the command workflow is reliable.

## Capabilities

### New Capabilities
- `java-editor-workflow`: Defines how users create, build, sync, run, and diagnose Java-backed Godot projects during editor development.

### Modified Capabilities

## Impact

- Affects examples, docs, project templates, and Maven build profiles.
- Keeps the first implementation outside a custom Godot editor plugin.
- Does not introduce hot reload as a guaranteed feature in this change.
