## Context

The binding already has annotations, an annotation processor, generated class registry, native bootstrap, and examples. The main editor friction is operational: users must know where jars and native libraries go, how to rebuild, how to refresh Godot state, and how to diagnose missing classes or wrong JDK/native architecture.

## Goals / Non-Goals

**Goals:**
- Provide a repeatable Java project workflow that works from the command line and IDE terminals.
- Make examples and templates use the same conventions.
- Add diagnostics for common editor-time failures.
- Establish a foundation for a later Godot editor plugin.

**Non-Goals:**
- Do not build a full Godot editor plugin in the first step.
- Do not guarantee live class hot reload.
- Do not replace Maven with Gradle or require a new build system.
- Do not handle mobile export.

## Decisions

- Start with a Maven-first workflow before editor UI.
  - Rationale: Maven automation is easier to test, works in CI, fits Java IDE terminals, and keeps developers focused on Java project operations.
  - Alternative considered: implement a Godot editor plugin first. Rejected because UI cannot compensate for unclear project layout and packaging contracts.

- Use templates rather than one-off examples as the user-facing starting point.
  - Rationale: examples demonstrate features, while templates encode the expected project structure.
  - Alternative considered: ask users to copy an example manually. Rejected because examples contain tutorial-specific code.

- Provide diagnostics as first-class commands.
  - Rationale: most early user failures are environment and layout errors, not API bugs.
  - Alternative considered: document troubleshooting only. Rejected because users need fast feedback in their local project.

- Do not introduce shell workflow scripts for application developers.
  - Rationale: the project template should make `mvn package` the primary build and sync loop. Optional run and diagnostic entry points should remain Maven profiles so users do not learn a second automation surface.

## Risks / Trade-offs

- A Maven workflow may feel less native than a Godot editor button -> document this as phase one and keep the workflow editor-compatible.
- Too much generated project content can confuse users -> keep generated files under predictable directories and mark them clearly.
- Restart requirements may be surprising -> diagnostics and docs must state when a Godot restart is required.

## Migration Plan

1. Normalize examples around the template layout.
2. Add Maven automation for sync, run, and doctor-style checks.
3. Update docs to make the automated workflow the default path.
4. Evaluate a Godot editor plugin after the command workflow stabilizes.

## Open Questions

- Should future workflow commands move from Maven profiles into a dedicated Maven plugin?
- Should generated Godot-side files be committed by users or regenerated during sync?
- What exact restart/reopen behavior is required after jar changes on each platform?
