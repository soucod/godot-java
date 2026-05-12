# godot-java templates

Copy `godot-java.gdextension` into a Godot project root and keep the runtime files under `res://godot-java/`.

Expected runtime layout:

```text
godot-java/
  app.jar
  libgodot-java.dylib
  libgodot-java.so
  libgodot-java.dll
  VERSION
```

Application templates should populate this directory from their own build, for
example by resolving `godot-java-native` from Maven and copying the shaded Java
application jar during `mvn package`.

The repository sync script can populate this directory for framework examples
and release verification:

```bash
scripts/sync-godot-java.sh --project path/to/godot/project --app-jar path/to/app.jar
```
