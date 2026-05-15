## Why

Recent changes (v0.1.4) introduced significant API changes and infrastructure improvements, but documentation has not been updated:

- README and docs reference old manual setup steps (e.g., manually copying native libs)
- Version numbers are hardcoded in docs — should use a maintainable approach given rapid iteration
- New features (typed signals, enum types, export DSL) are not documented
- gdextension path changed from `res://native/` to `res://godot-java/` but docs may still reference old paths
- Native lib is now Maven-managed, making manual steps obsolete

## What

### 1. README.md overhaul

- Update quick start guide to reflect Maven-based native lib management
- Remove manual native lib copy instructions
- Update version references: use `VERSION` placeholder or link to Maven Central badge instead of hardcoded version
- Add v0.1.4 feature highlights (typed signals, enums, export DSL, typed ptrcall)
- Update gdextension path references to `res://godot-java/`

### 2. API documentation

- Document `@Signal` + `TypedSignal` usage with examples
- Document enum type usage (replacing int params)
- Document `@ExportGroup` / `@ExportSubgroup` / `PropertyUsage`
- Document `Godot.instantiate()` API
- Update `GodotArray` usage (indexed loop, not for-each)

### 3. Version strategy

- Replace hardcoded version numbers with Maven Central badge or `VERSION` placeholder
- Add version compatibility table (godot-java version ↔ Godot version ↔ Java version)

### 4. Build & setup docs

- Update native lib management instructions (Maven auto-sync, no manual copy)
- Document OS-specific profiles in pom.xml
- Update example project setup instructions

### Scope

- README.md (root)
- docs/ directory
- Example project READMEs (if any)

### Out of scope

- API javadoc (separate effort)
- Website / landing page
- Tutorial series
