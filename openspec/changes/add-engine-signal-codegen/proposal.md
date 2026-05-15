## Why

godot-java's signal API only covers user-defined signals via `@Signal` + APT-generated `*Signals` facade. Engine class signals (Node.ready, Button.pressed, Area2D.body_entered, etc.) have no typed access — users must use stringly-typed `call("connect", "ready", callable)` with no compile-time checking.

gdext code-generates per-class signal types from `extension_api.json`, providing full autocomplete, type-safe emit/connect, inherited signal access, and `ConnectHandle` lifecycle management.

## What

Extend `godot-java-code-generator` to generate typed signal accessors for all engine classes, and enhance the signal runtime API.

### Phase 1: Engine signal codegen

1. **Parse signals from extension_api.json**: Each class has a `signals` array with name and arguments
2. **Generate `SignalsOf{ClassName}` class per engine class**: Contains methods returning typed signal instances for each declared signal
3. **Generate signal accessor on engine classes**: Add `signals()` method to generated engine classes (e.g., `Node.signals().ready()`)
4. **Inheritance chain**: Subclass signal collections include parent signals through delegation

### Phase 2: Signal type enhancements

5. **Extend parameter type support**: Beyond primitives + String to include Vector2/3, Color, enums, GodotArray, etc.
6. **Increase max arity**: Support beyond 5 parameters (extension_api.json has signals with up to ~8 params)

### Phase 3: Connect API improvements

7. **Type-safe connect callbacks**: Accept typed lambda/method reference instead of opaque `Callable`
8. **`ConnectHandle` with disconnect/isConnected**: Return a handle from `connect()` instead of `boolean`
9. **Builder pattern**: `connect().flags(...).deferred().build()` for advanced configuration

### Reference implementation (gdext)

- `godot-codegen/src/generator/signals.rs`: Per-class `SignalsOf{Class}` with `Sig{SignalName}` structs
- `TypedSignal<'c, C, Ps>`: Parameterized by owner type and param tuple
- `Deref` chain: Signal collection upcasts to parent for inherited signal access
- `ConnectBuilder`: Type-state builder for flags, named callables, cross-thread connections
- `ConnectHandle`: Tracks connection with `disconnect()` and `is_connected()`

### Scope

- godot-java-code-generator: Signal codegen from extension_api.json
- godot-java-core: `TypedSignal` enhancements, `ConnectHandle`, builder pattern
- APT processor: Extend user signal parameter type support

### Out of scope

- Thread-safe signals (gdext feature behind `experimental-threads`)
- Async/await on signals (gdext has `.to_future().await`)
- Internal/hidden signals
