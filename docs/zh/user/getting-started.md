# 快速上手

使用 `godot-java` 创建一个能运行 Java 游戏逻辑的 Godot 桌面项目。

## 前置条件

| 工具 | 最低版本 | 验证命令 |
|---|---:|---|
| JDK | 25+ | `java -version` |
| Godot | 4.6+ | `godot --version` |
| Maven | 4.0+ | `./mvnw --version` 或 `mvn --version` |

Java 25 是硬性要求，因为 godot-java 使用 Panama Foreign Function & Memory API（`java.lang.foreign`）调用 Godot 原生 API。

## 推荐方式：从模板项目开始

应用开发者应该从 `godot-java-template` 开始，而不是直接使用 `godot-java` 框架源码仓库。

模板已经包含：

- Maven 4 Java 项目
- `godot/` 目录下的 Godot 项目
- `godot/godot-java.gdextension`
- 构建时生成 `target/app.jar`
- 构建时自动把 `app.jar` 和匹配平台的原生库同步到 `godot/godot-java/`

```bash
git clone https://github.com/youngledo/godot-java-template.git my-godot-game
cd my-godot-game
./mvnw package
./mvnw verify -Pgodot-run
```

执行 `./mvnw package` 后，Godot 项目里会出现：

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

Maven 属性 `godot-java.version` 同时控制 Java 和 native 发布物版本。`godot-java-core` 与 `godot-java-native` 必须保持同一版本：

```xml
<properties>
    <godot-java.version>0.1.3</godot-java.version>
    <maven.compiler.release>25</maven.compiler.release>
</properties>
```

应用项目依赖的是 `godot-java-core`：

```xml
<dependency>
    <groupId>io.github.youngledo</groupId>
    <artifactId>godot-java-core</artifactId>
    <version>${godot-java.version}</version>
</dependency>
```

不要把 `io.github.youngledo:godot-java` 当作应用依赖使用。旧版本曾把这个
根 POM 发布到 Maven Central；新的发布应只提供开发者实际使用的
`godot-java-core` jar。

当前发布的 native classifiers：

- `macos-universal`
- `linux-x86_64`
- `windows-x86_64`

## 编写 Java Godot 类

在应用项目源码中创建或修改一个 Java 类：

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

要点：

- `@GodotClass` 将类注册到 Godot ClassDB。
- `@Export` 将字段暴露到 Godot Inspector。
- `@Signal` 声明 Godot 信号。
- `@GodotMethod` 将 Java 方法暴露给 Godot 对象系统。
- `_ready()` 等虚方法直接用 `@Override` 覆盖。

重新构建并运行：

```bash
./mvnw package
./mvnw verify -Pgodot-run
```

`./mvnw package` 是主要的构建和同步命令。可选的 `godot-run` profile
会先执行同一套 Maven 生命周期，然后启动 Godot 并加载模板中的 `godot/`
项目。如果本机命令行找不到 `godot`，可以覆盖可执行文件路径：

```bash
./mvnw verify -Pgodot-run -Dgodot.executable=/Applications/Godot.app/Contents/MacOS/Godot
```

本地诊断可以执行：

```bash
./mvnw verify -Pgodot-doctor
```

doctor profile 会检查 Maven 使用的 JDK 版本、Godot 项目文件、已同步的
`app.jar`、jar 内生成的 Java 类注册表，以及当前平台的 native 库。

## 在 Godot 中使用 Java 节点

JVM 启动后，带注解的 Java 类会注册为 Godot 类。场景可以直接使用这个 Java 类型：

```ini
[gd_scene load_steps=1 format=3]

[node name="HealthComponent" type="HealthComponent"]
```

也可以从另一个 Java 节点中创建：

```java
HealthComponent health = new HealthComponent();
addChild(health);
health.takeDamage(30);
```

## 手动集成

只有在接入已有项目或编写自己的模板时，才需要走手动集成路径。

添加 Java 依赖：

```xml
<dependencies>
    <dependency>
        <groupId>io.github.youngledo</groupId>
        <artifactId>godot-java-core</artifactId>
        <version>0.1.3</version>
    </dependency>
</dependencies>
```

Maven Central 搜索结果中可能也会出现旧版本的
`io.github.youngledo:godot-java`，但它是 `pom` artifact，不是运行时 jar。
应用代码应该依赖 `godot-java-core`。

你的构建需要生成 fat application jar，并填充 Godot 运行目录：

```text
your-godot-project/
  godot-java.gdextension
  godot-java/
    app.jar
    libgodot-java.dylib    # 或 .so / .dll
    VERSION
```

Godot 项目根目录中的 `.gdextension` 文件：

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

native 库发布坐标为：

```text
io.github.youngledo:godot-java-native:0.1.3:zip:<classifier>
```

native artifact 版本必须与 `app.jar` 中的 `godot-java-core` 版本一致。

## 框架示例

本仓库里的 `godot-java-examples/` 用于框架开发、回归验证和学习单个 API。新游戏项目不建议以它作为起点，推荐使用 `godot-java-template`。

## 常见问题

### "JVM not found"

确认 `JAVA_HOME` 指向 JDK 25：

```bash
export JAVA_HOME=/path/to/jdk-25
```

### "Class not found"

- 执行 `./mvnw package`
- 确认 `godot/godot-java/app.jar` 已更新
- 确认类上有 `@GodotClass` 注解

### 原生库加载失败

- 检查 `.gdextension` 文件路径是否与运行目录一致
- 确认原生库文件存在于 `godot-java/`
- 确认 native classifier 与运行 Godot 的平台匹配

## 下一步

- [API 参考](api.md) -- 完整的注解和 API 文档
- [使用指南](guide.md) -- 核心概念和进阶用法
- [常见问题](troubleshooting.md) -- 错误排查
