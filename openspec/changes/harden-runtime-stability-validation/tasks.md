## 1. Lifecycle Tests

- [x] 1.1 Add an integration-test Java class that records lifecycle callback order.
- [x] 1.2 Add a Godot test scene that creates, attaches, removes, and frees Java-backed nodes.
- [x] 1.3 Assert Java object mapping cleanup after node free and scene teardown.
- [x] 1.4 Add RefCounted lifecycle tests for create, pass-through, release, and cleanup.

## 2. Dispatch and Type Tests

- [x] 2.1 Extend integration tests for GDScript-to-Java method calls with primitives and objects.
- [x] 2.2 Add Java-to-Godot call tests covering generated engine wrappers.
- [ ] 2.3 Add virtual method override tests for common lifecycle methods and one non-lifecycle virtual method.
- [x] 2.4 Add signal and exported-property round-trip tests.
- [ ] 2.5 Add complex Variant round-trip tests for strings, arrays, dictionaries, callables, objects, RefCounted values, and math structs.

## 3. Failure Diagnostics

- [ ] 3.1 Add tests for Java callback exceptions.
- [ ] 3.2 Add tests for missing method and wrong argument type failures.
- [ ] 3.3 Add tests for missing generated registry or invalid classpath in a controlled harness.
- [ ] 3.4 Ensure diagnostic messages include class, method, and corrective context where possible.

## 4. Stress and Memory

- [x] 4.1 Add a short CI stress profile for repeated calls, signals, Variant conversion, and node churn.
- [x] 4.2 Add an opt-in long stress profile for local or scheduled runs.
- [x] 4.3 Record NativeMemoryTracker diagnostics around scoped calls and integration teardown without changing runtime semantics.
- [x] 4.4 Add cleanup assertions for `JavaObjectMap` and `RefCountedHelper`.

## 5. CI Integration

- [x] 5.1 Wire the short stability profile into the existing three-platform CI test job.
- [x] 5.2 Keep long stress tests opt-in behind a Maven profile or environment variable.
- [x] 5.3 Publish relevant Godot and Java logs as CI artifacts when stability tests fail.
