## Context

godot-java v0.1.4 introduces typed signals, enum types, export DSL, typed ptrcall, and Maven-based native lib management. README and docs are outdated — they reference manual native lib steps, hardcoded versions, and old gdextension paths.

## Goals / Non-Goals

**Goals:**
- Update README to reflect current project state and v0.1.4 features
- Replace hardcoded version numbers with maintainable approach
- Remove all manual native lib copy instructions (now Maven-managed)
- Document new APIs (signals, enums, export groups)
- Keep docs concise — link to javadoc for full API reference

**Non-Goals:**
- Full API javadoc (separate effort)
- Website or tutorial series
- godot-java-demo-projects or godot-java-3d-demo docs (those repos manage their own)

## Decisions

1. **Version references**: Use Maven Central badge + `[version]` placeholder in code snippets with a note to check latest version on Maven Central. Avoids constant doc updates.
2. **Quick start structure**: Minimal getting-started guide → point to examples for deeper learning.
3. **gdextension path**: Document `res://godot-java/` as the standard convention.

## Risks / Trade-offs

- Version placeholders in code snippets require users to substitute — but this is better than outdated hardcoded versions
- Keeping docs concise means some details are deferred to javadoc
