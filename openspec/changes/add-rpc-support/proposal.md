## Why

godot-java currently has no declarative RPC registration. Users can only call `Node.rpc()` manually with string method names, with no compile-time validation or automatic configuration. Both gdext (Rust) and godot-kotlin-jvm provide annotation-based RPC config that registers mode, transfer mode, call-local, and channel at `_ready` time.

Adding `@Rpc` annotation support would bring godot-java to parity with other bindings for multiplayer game development.

## What

Add annotation-based RPC configuration for `@GodotMethod` methods, mirroring gdext's `#[rpc]` approach:

1. **New annotations and enums**: `@Rpc`, `RpcMode` (DISABLED, ANY_PEER, AUTHORITY), `TransferMode` (RELIABLE, UNRELIABLE, UNRELIABLE_ORDERED)
2. **APT extension**: `GodotClassProcessor` recognizes `@Rpc` on methods, collects RPC config into generated DispatchIndex
3. **Runtime registration**: At `_ready` notification, iterate `@Rpc`-annotated methods and call `Node.rpc_config(methodName, configDict)` on the instance
4. **Enum codegen**: Generate `MultiplayerAPI.RPCMode` and `MultiplayerPeer.TransferMode` enums from extension_api.json

### Reference implementation (gdext)

- `#[rpc(any_peer, reliable, call_local)]` attribute → `RpcConfig` struct
- Auto-registers in `__before_ready()` via `Node.rpc_config()`
- Config dict keys: `rpc_mode`, `transfer_mode`, `call_local`, `channel`

### Scope

- godot-java-core: `@Rpc` annotation, `RpcMode`/`TransferMode` enums, registration hook
- godot-java-code-generator: generate `MultiplayerAPI` and `MultiplayerPeer` enum types
- APT processor: extend `MethodInfo` with RPC config, generate `_RPC_CONFIGS` map in DispatchIndex
- No changes to godot-java-native or examples

### Out of scope

- High-level multiplayer abstraction (only low-level `rpc_config` registration)
- Network transport layer (uses Godot's built-in multiplayer)
