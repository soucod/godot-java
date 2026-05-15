# Getting Started

Set up a Godot desktop project that runs Java game code with `godot-java`.

## Prerequisites

| Tool | Minimum Version | Verification |
|---|---:|---|
| JDK | 25+ | `java -version` |
| Godot | 4.6+ | `godot --version` |
| Maven | 4.0+ | `./mvnw --version` or `mvn --version` |

Java 25 is required because godot-java uses the Panama Foreign Function & Memory API (`java.lang.foreign`) for native calls into Godot.

## Recommended Path: Start from the Template

Application developers should start from `godot-java-template` instead of cloning the `godot-java` framework repository.

The template already contains:

- a Maven 4 Java project
- a Godot project under `godot/`
- `godot/godot-java.gdextension`
- a Maven build that creates `target/app.jar`
- automatic sync of `app.jar` and the matching native library into `godot/godot-java/`

```bash
git clone https://github.com/youngledo/godot-java-template.git my-godot-game
cd my-godot-game
./mvnw package
./mvnw verify -Pgodot-run
```

After `./mvnw package`, the Godot project contains:

```text
godot/
  godot-java.gdextension
  godot-java/
    app.jar
    libgodot-java.dylib    # macOS
    libgodot-java.so       # Linux
    libgodot-java.dll      # Windows
    VERSION
```

The Maven property `godot-java.version` controls both Java and native artifacts. Keep `godot-java-core` and `godot-java-native` on the same version:

```xml
<properties>
    <godot-java.version>LATEST_VERSION</godot-java.version> <!-- check Maven Central for latest -->
    <maven.compiler.release>25</maven.compiler.release>
</properties>
```

Use `godot-java-core` in application dependencies:

```xml
<dependency>
    <groupId>io.github.youngledo</groupId>
    <artifactId>godot-java-core</artifactId>
    <version>${godot-java.version}</version>
</dependency>
```

Do not use `io.github.youngledo:godot-java` as an application dependency. Older
releases exposed that root POM on Maven Central, but new releases publish the
developer-facing `godot-java-core` jar instead.

Supported native classifiers:

- `macos-universal`
- `linux-x86_64`
- `windows-x86_64`

## Write a Java Godot Class

Create or edit a Java class in the application source tree:

```java
package com.example;

import org.godot.annotation.Export;
import org.godot.annotation.GodotClass;
import org.godot.annotation.GodotMethod;
import org.godot.annotation.Signal;
import org.godot.node.Node;

@GodotClass(name = "HealthComponent", parent = "Node")
public class HealthComponent extends Node {

    @Export
    private int maxHealth = 100;

    private int currentHealth;

    @Signal(name = "health_changed")
    private void onHealthChanged(int newHealth) {}

    @Signal(name = "died")
    private void onDied() {}

    @Override
    public void _ready() {
        currentHealth = maxHealth;
    }

    @GodotMethod
    public void takeDamage(int amount) {
        currentHealth -= amount;
        emitSignal("health_changed", currentHealth);
        if (currentHealth <= 0) {
            emitSignal("died");
        }
    }

    @GodotMethod
    public int getHealth() {
        return currentHealth;
    }
}
```

Key points:

- `@GodotClass` registers the class with Godot's ClassDB.
- `@Export` exposes a field to the Godot Inspector.
- `@Signal` declares a Godot signal.
- `@GodotMethod` exposes a Java method through Godot's object system.
- Virtual methods like `_ready()` are overridden normally with `@Override`.

Rebuild and run:

```bash
./mvnw package
./mvnw verify -Pgodot-run
```

`./mvnw package` is the primary build and sync command. The optional
`godot-run` profile runs the same Maven lifecycle and then launches Godot with
the template's `godot/` project. If Godot is not on `PATH`, override the
executable:

```bash
./mvnw verify -Pgodot-run -Dgodot.executable=/Applications/Godot.app/Contents/MacOS/Godot
```

For local diagnostics, run:

```bash
./mvnw verify -Pgodot-doctor
```

The doctor profile checks the Maven JDK version, Godot project files, synced
`app.jar`, the generated Java class registry inside the jar, and the current
platform native library.

## Use Java Nodes in Godot

Once the JVM starts, annotated Java classes are registered as Godot classes. A scene can use the Java class directly:

```ini
[gd_scene load_steps=1 format=3]

[node name="HealthComponent" type="HealthComponent"]
```

You can also create Java nodes from another Java node:

```java
HealthComponent health = new HealthComponent();
addChild(health);
health.takeDamage(30);
```

## Manual Integration

Use this path only if you are integrating godot-java into an existing project or writing your own template.

Add the Java dependency:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.youngledo</groupId>
        <artifactId>godot-java-core</artifactId>
        <version>LATEST_VERSION</version>
    </dependency>
</dependencies>
```

`io.github.youngledo:godot-java` may appear in Maven Central search results for
older releases, but it is a `pom` artifact rather than the runtime jar.
Application code should depend on `godot-java-core`.

Your build must produce a fat application jar and populate the Godot runtime directory:

```text
your-godot-project/
  godot-java.gdextension
  godot-java/
    app.jar
    libgodot-java.dylib    # or .so / .dll
    VERSION
```

Use this `.gdextension` file in the Godot project root:

```ini
[configuration]
entry_symbol = "godot_java_init"
compatibility_minimum = 4.6

[libraries]
macos.debug = "res://godot-java/libgodot-java.dylib"
macos.release = "res://godot-java/libgodot-java.dylib"
linux.debug = "res://godot-java/libgodot-java.so"
linux.release = "res://godot-java/libgodot-java.so"
windows.debug = "res://godot-java/libgodot-java.dll"
windows.release = "res://godot-java/libgodot-java.dll"
```

The native library is distributed as:

```text
io.github.youngledo:godot-java-native:LATEST_VERSION:zip:<classifier>
```

The native artifact version must match the `godot-java-core` version inside `app.jar`.

## Framework Examples

This repository also contains `godot-java-examples/`. Those examples are for framework development, regression checks, and learning individual APIs. They are not the recommended starting point for a new game project.

## Common Issues

### "JVM not found"

Set `JAVA_HOME` to your JDK 25 installation:

```bash
export JAVA_HOME=/path/to/jdk-25
```

### "Class not found"

- Run `./mvnw package`.
- Confirm `godot/godot-java/app.jar` was updated.
- Verify the class is annotated with `@GodotClass`.

### Native Library Load Failure

- Verify `.gdextension` paths match the runtime directory.
- Confirm the native library exists in `godot-java/`.
- Confirm the native classifier matches the platform running Godot.

## Next Steps

- [API Reference](api.md) -- Full annotation and type documentation
- [Development Guide](guide.md) -- In-depth walkthroughs
- [Troubleshooting](troubleshooting.md) -- Detailed error resolution
