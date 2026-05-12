## Context

The current repository builds the Java runtime, the generated API bindings, native GDExtension libraries, and example jars. The examples can run in Godot when the correct native library and jar are placed in the expected locations. The missing piece is a contract for what a distributable desktop project looks like and a CI check that proves an exported game can start with Java code loaded.

## Goals / Non-Goals

**Goals:**
- Define one canonical desktop project layout for jars, native libraries, and `.gdextension` files.
- Provide automation that prepares a Godot project for editor execution and desktop export.
- Add CI coverage for exported desktop smoke runs on macOS, Linux, and Windows where practical.
- Keep the workflow compatible with official Godot binaries.

**Non-Goals:**
- Do not introduce Android, iOS, Web, or GraalVM native-image export support.
- Do not require a custom Godot editor build.
- Do not solve cross-Godot-version compatibility in this change.
- Do not build a full editor plugin.

## Decisions

- Use a project-local distribution directory rather than environment-only classpath configuration.
  - Rationale: exported games cannot depend on developer shell state such as `GODOT_JAVA_CLASSPATH`.
  - Alternative considered: continue documenting environment variables only. Rejected because it is fragile for exported artifacts.

- Keep the first automation layer scriptable and Maven-friendly.
  - Rationale: the repository is Maven-based and the fastest path is a deterministic `package` plus sync/export flow.
  - Alternative considered: create a Godot editor plugin first. Rejected because export correctness can be proven without UI work.

- Publish godot-java native libraries as Maven-resolvable artifacts.
  - Rationale: `godot-java-core` already reaches users through Maven, and the native GDExtension library must be version-aligned with that Java runtime. Maven artifacts or classifiers let the sync workflow resolve the correct native library through the same dependency and cache model as the Java code.
  - Preferred shape: publish platform-specific native zip artifacts or classifiers under the `io.github.youngledo` group, with the same version as `godot-java-core`.
  - Alternative considered: GitHub Release-only native zips. Rejected as the primary automated path because it creates a second version-resolution mechanism and makes CI/enterprise caching less reproducible. GitHub Release assets can still exist as a manual fallback.

- Treat export validation as a smoke test, not a full integration suite.
  - Rationale: the purpose is to prove packaging and startup, while detailed API behavior remains covered by runtime integration tests.
  - Alternative considered: run every demo after export. Rejected because it increases CI time and failure surface before the packaging contract is stable.

## Risks / Trade-offs

- Platform export behavior differs across Godot targets -> keep the initial smoke scene minimal and platform-specific script steps explicit.
- Maven Central publishing for native zip/classifier artifacts is more complex than Java-only publishing -> isolate native publishing configuration and verify release-style consumption in CI before documenting it as the default path.
- JDK/JRE discovery may vary on user machines -> document required runtime placement and fail with actionable diagnostics.
- macOS app signing/quarantine can complicate exported binary execution -> start with local CI smoke execution and document signing as a later release concern.
- Windows path escaping can break `.gdextension` or classpath handling -> include Windows CI coverage early.

## Migration Plan

1. Add the canonical layout and update examples to use it.
2. Add Maven-resolvable native artifact packaging for supported desktop platforms.
3. Add sync automation that resolves the platform native artifact and copies it into the Godot project.
4. Add export smoke tests to CI after local scripts are stable.
5. Keep existing manual environment-variable workflow as a fallback during transition.

## Open Questions

- Should the first automation be a shell script, Maven plugin goal, or both?
