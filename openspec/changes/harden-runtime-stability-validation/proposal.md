## Why

godot-java has meaningful tests and demos, but production confidence depends on proving lifecycle, memory, error handling, and long-running behavior under Godot. The runtime needs a focused stability validation layer beyond simple API and demo coverage.

## What Changes

- Add targeted runtime integration tests for lifecycle, object mapping, RefCounted handling, signals, properties, virtual methods, and complex Variant conversions.
- Add failure-mode tests that ensure errors are diagnosable and do not silently corrupt runtime state.
- Add stress and long-run smoke scenarios for high-frequency calls, node creation/destruction, signal storms, and scene reloads.
- Add memory/accounting assertions around native allocations, scoped arenas, and cleanup.

## Capabilities

### New Capabilities
- `runtime-stability-validation`: Defines the runtime stability, lifecycle, memory, stress, and failure-mode validation expected for godot-java.

### Modified Capabilities

## Impact

- Affects `godot-java-core/src/test`, `godot-java-examples/examples/it-test`, CI runtime, and runtime diagnostics.
- May require test-only Godot scenes and Java classes.
- Does not add cross-version CI or mobile export testing in this change.
