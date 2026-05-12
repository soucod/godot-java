# Getting Started

Set up godot-java in 5 minutes.

## Prerequisites

| Tool | Minimum Version | Verification |
|---|-----------------|---|
| JDK | 25+             | `java -version` |
| Godot | 4.6+            | `godot --version` |
| Maven | 4.0.x (Wrapper included) | `mvn -version` or `./mvnw --version` |

Java 25 is required because godot-java uses the Panama Foreign Function & Memory API (`java.lang.foreign`) for all native calls to the Godot C API.

### Install JDK 25

**macOS:**
```bash
brew install openjdk@25 maven
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 25)' >> ~/.zshrc
source ~/.zshrc
```

**Linux (Ubuntu/Debian):**
```bash
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh
sdk install java 25.0.0-tem
sudo apt install maven
```

**Windows:**
1. Download JDK 25 from [Adoptium](https://adoptium.net/)
2. Install Maven: `choco install maven`

Verify:
```bash
java -version    # must show 25+
./mvnw --version # First-time: auto-downloads Maven 4.0+
# or use system Maven (requires 4.0+ pre-installed)
mvn -version
```

## Step 1: Create a Maven Project

### Option A: IntelliJ IDEA (recommended)

1. **File > New > Project**, select **Maven Archetype**
2. Set Group ID: `com.example`, Artifact ID: `my-godot-game`
3. Select JDK 25+
4. Click **Create**

### Option B: Command Line

```bash
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=my-godot-game \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
cd my-godot-game
```

## Step 2: Add the Dependency

### Maven

> Check the latest version on [Maven Central](https://central.sonatype.com/artifact/io.github.youngledo/godot-java-core)

Edit `pom.xml` and add godot-java-core inside `<dependencies>`:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.youngledo</groupId>
        <artifactId>godot-java-core</artifactId>
        <version>VERSION</version> <!-- replace with latest -->
    </dependency>
</dependencies>
```

Set the Java 25 compiler target:

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
</properties>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.youngledo:godot-java-core:VERSION") // replace with latest
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'io.github.youngledo:godot-java-core:VERSION' // replace with latest
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```

## Step 3: Write Your First Godot Class

Create `src/main/java/com/example/HealthComponent.java`:

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

- `@GodotClass` registers the class with Godot's ClassDB (auto-discovered at startup).
- `@Export` exposes a field to the Godot editor Inspector panel.
- `@Signal` declares a signal; the method body is unused, only the signature matters.
- `@GodotMethod` makes a method callable through Godot's object system.
- Virtual methods like `_ready()` are overridden normally with `@Override`.

## Step 4: Compile

```bash
mvn compile
```

On success, class files appear in `target/classes/`.

## Step 5: Deploy to Your Godot Project

### 5.1 Package Your Java App Jar

Godot loads the native GDExtension first, then godot-java starts a JVM and loads your Java application jar. In a real project this should be a fat/shaded jar that contains your game classes, `godot-java-core`, third-party dependencies, and the annotation-processor generated registry.

The standard runtime directory is:

```
your-godot-project/
  godot-java/
    app.jar
    libgodot-java.dylib    # or .so / .dll for the current platform
```

In an application project, this should be part of the project build. The
`godot-java-template` Maven build runs this during `mvn package`: it creates
`target/app.jar`, resolves the matching `godot-java-native` classifier artifact,
and writes both files into the Godot project.

For repository examples and framework release verification, the source
repository also includes a sync helper:

```bash
scripts/sync-godot-java.sh \
  --project godot-java-examples/examples/it-test \
  --app-jar godot-java-examples/target/godot-java-examples.jar
```

By default, the helper resolves the matching `godot-java-native` Maven artifact for the current platform. During local framework development, pass `--native-lib` or `--native-zip` to use a freshly built native library. Application developers should not need the source repository helper when using the template.

### 5.2 Create the .gdextension File

Create `godot-java.gdextension` in your Godot project root:

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

### 5.3 Final Directory Layout

```
your-godot-project/
  godot-java.gdextension
  godot-java/
    app.jar
    libgodot-java.dylib    # or .so / .dll
  scenes/
```

### 5.4 Enable in Godot

1. Open your Godot project.
2. Go to **Project > Project Settings > GDExtensions**.
3. Click **Add** and select the `godot-java.gdextension` file.
4. Restart the Godot editor.

## Step 6: Use Java Nodes in a Godot Scene

Create a scene that uses the registered Java class as a node type:

```ini
[gd_scene load_steps=1 format=3]

[node name="HealthComponent" type="HealthComponent"]
```

Set this scene as the main scene in `project.godot`, or instantiate the Java
class from another Java node:

```java
HealthComponent health = new HealthComponent();
addChild(health);
health.takeDamage(30);
```

Press **F5** to run. Godot loads the GDExtension, godot-java starts the JVM,
and the Java node is created by Godot's ClassDB.

## Common Issues

### "JVM not found"

Set `JAVA_HOME` to your JDK 25 installation:
```bash
export JAVA_HOME=/path/to/jdk-25
```

### "Class not found"

- Run `mvn package` to build the application jar.
- Run the sync workflow so `godot-java/app.jar` is updated.
- Verify the class is annotated with `@GodotClass`.

### Native library load failure

- Verify the `.gdextension` file paths match the actual file locations.
- Confirm the native library exists in the `godot-java/` runtime directory.
- Confirm the library architecture matches your Godot editor (x64 vs ARM64).

## Next Steps

- [API Reference](api.md) -- Full annotation and type documentation
- [Development Guide](guide.md) -- In-depth walkthroughs
- [Troubleshooting](troubleshooting.md) -- Detailed error resolution
