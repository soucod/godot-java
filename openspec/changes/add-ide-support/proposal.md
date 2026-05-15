## Why

godot-java has no dedicated IDE support. Developers currently work with plain Maven + any IDE, with no validation for annotation parameters, no autocomplete for engine class names/signal names, and no gdextension file generation. The development workflow requires manual `mvn package` + Godot restart on every code change.

godot-kotlin-jvm provides an IntelliJ plugin with annotation validation and a Gradle plugin for build automation. gdext benefits from rust-analyzer's excellent IDE integration.

## What

### Phase 1: IntelliJ plugin (core)

1. **Annotation validation**: Real-time checking of `@GodotClass(name, parent)` — verify parent is a valid engine class name; `@Export` hint values; `@Signal` parameter types
2. **Code completion**: Engine class names in `@GodotClass(parent=...)`, signal names in `connect()`, method names in `call()`
3. **Quick fixes**: Auto-generate `@GodotMethod` override stubs for virtual methods, generate gdextension file
4. **Gdextension file template**: New project/file wizard for `.gdextension` files

### Phase 2: Build tooling

5. **Maven archetype**: Project template for new godot-java projects
6. **Gradle plugin** (optional): Alternative build system with simpler configuration

### Phase 3: Hot reload (experimental)

7. **GDExtension reload**: Leverage Godot's `GDExtensionManager::unload_extension` / `load_extension` to reload the native library
8. **JVM state management**: Strategy for preserving/rebuilding JVM state (statics, singletons) across reloads
9. **Dev server mode**: Watch for source changes, auto-compile + trigger reload

### Scope

- IntelliJ plugin project (separate repository or module)
- Maven archetype
- Hot reload requires changes in godot-java-core (reload hooks) and godot-java-native

### Out of scope

- Full visual editor integration (inspector, scene tree — requires engine module approach like godot-kotlin-jvm)
- Android Studio support (blocked by Android platform support)
- LSP server for non-IntelliJ IDEs
