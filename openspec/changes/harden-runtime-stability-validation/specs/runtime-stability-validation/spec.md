## ADDED Requirements

### Requirement: Lifecycle validation
The system SHALL validate Java-backed Godot object creation, ready/process callbacks, node removal, free/queueFree behavior, and cleanup of Java-to-native object mappings.

#### Scenario: Java node lifecycle completes
- **WHEN** an integration test creates, attaches, removes, and frees a Java-backed node
- **THEN** lifecycle callbacks run in the expected order and the object mapping is cleaned up

### Requirement: RefCounted validation
The system SHALL validate RefCounted-derived objects across Java and Godot ownership boundaries.

#### Scenario: RefCounted object is released
- **WHEN** a RefCounted Java-backed object is passed through Godot and released
- **THEN** reference bookkeeping remains consistent and cleanup runs exactly once

### Requirement: Dispatch validation
The system SHALL validate GDScript-to-Java calls, Java-to-Godot calls, Godot virtual method overrides, exported properties, signals, ptrcall dispatch, and Variant dispatch.

#### Scenario: Mixed dispatch path succeeds
- **WHEN** an integration test invokes Java methods through Godot and emits signals across the boundary
- **THEN** arguments and return values are marshaled correctly and callbacks execute once

### Requirement: Complex Variant validation
The system SHALL validate conversion and cleanup for complex Godot types including `GodotString`, `GodotStringName`, `GodotArray`, `GodotDictionary`, `Callable`, object references, RefCounted references, and math structs.

#### Scenario: Complex values cross boundary
- **WHEN** complex values are passed from Java to Godot and back
- **THEN** the returned values preserve type and content and temporary native allocations are cleaned up

### Requirement: Failure-mode diagnostics
The system SHALL validate that common runtime failures produce actionable diagnostics without silently corrupting runtime state.

#### Scenario: Java callback throws
- **WHEN** a Java method invoked by Godot throws an exception
- **THEN** the error is reported with class and method context and subsequent runtime calls remain usable

### Requirement: Stress validation
The system SHALL provide stress scenarios for repeated method calls, node creation/destruction, signal emission, Variant conversion, and scene reload.

#### Scenario: Short stress run completes
- **WHEN** CI runs the short stress profile
- **THEN** the Godot process completes without crash, deadlock, or tracked native memory growth beyond accepted thresholds

### Requirement: Memory accounting diagnostics
The system SHALL record tracked godot-java native allocation diagnostics before, during, and after stability scenarios without changing runtime allocation semantics.

#### Scenario: Scoped allocation diagnostics are captured
- **WHEN** a test performs nested Java-to-Godot calls using scoped arenas
- **THEN** the test output includes NativeMemoryTracker diagnostics that can be inspected for unexpected growth
