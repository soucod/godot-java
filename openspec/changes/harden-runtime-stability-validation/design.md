## Context

The runtime contains sensitive boundaries: Java objects mapped to Godot native pointers, Panama memory segments, scoped arenas, GDExtension callbacks, generated dispatch, RefCounted behavior, and Godot lifecycle callbacks. Existing tests cover many units and one integration path, but stability work should deliberately target failure modes and repeated lifecycle transitions.

## Goals / Non-Goals

**Goals:**
- Add integration tests for lifecycle and ownership boundaries.
- Add stress tests that can run in short CI mode and longer local/nightly mode.
- Add memory accounting assertions where the project already has tracking hooks.
- Ensure exceptions and invalid configurations produce actionable diagnostics.

**Non-Goals:**
- Do not introduce a cross-Godot-version matrix.
- Do not require external profilers or heavyweight sanitizer infrastructure for the first implementation.
- Do not guarantee hot reload support.
- Do not benchmark every API method.

## Decisions

- Use the existing Godot integration-test project as the first stability harness.
  - Rationale: it already exercises real Godot startup and Java registration.
  - Alternative considered: build a separate harness immediately. Rejected because it would duplicate setup before the stability scenarios are proven.

- Separate short CI tests from long local/nightly stress tests.
  - Rationale: every PR needs fast feedback, while long-run tests are useful but expensive.
  - Alternative considered: run all stress tests on every CI job. Rejected because it would slow routine development.

- Prefer observable runtime assertions over log-only validation.
  - Rationale: memory and lifecycle bugs need hard pass/fail signals.
  - Alternative considered: rely on manual inspection of Godot logs. Rejected because it is not repeatable.

- Keep NativeMemoryTracker semantics unchanged in this change.
  - Rationale: the runtime has already been validated by large demos, and changing Bridge scoped-allocation accounting could affect diagnostics or hide ownership ambiguity. This change records memory stats during stability scenarios but defers strict baseline assertions until the tracker model is specified separately.

## Risks / Trade-offs

- Some lifecycle failures may be timing-sensitive -> repeat selected scenarios and keep deterministic counters.
- Godot process crashes can hide assertion details -> write progress markers before risky operations.
- Native allocation accounting can produce false positives if Godot owns memory beyond Java cleanup -> scope assertions to allocations tracked by godot-java.
- Stress tests can become flaky in CI -> keep CI thresholds modest and move heavy loops to opt-in long mode.

## Migration Plan

1. Add deterministic lifecycle and failure-mode tests to the existing integration project.
2. Add memory accounting assertions around tracked allocations and cleanup.
3. Add short stress scenarios to CI.
4. Add opt-in long-run stress tasks for local or scheduled execution.

## Open Questions

- Should long-run tests be wired to a scheduled GitHub Actions workflow or remain local-only initially?
- Which counters should be considered authoritative for native memory leak assertions? Current implementation records diagnostics only and defers strict assertions.
- Should Java exceptions crossing Godot callbacks be surfaced as Godot errors, Java logs, or both?
