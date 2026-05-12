# 快速上手

5 分钟完成 godot-java 安装、配置并运行第一个示例。

## 前置条件

| 工具 | 最低版本     | 说明 |
|------|----------|------|
| JDK | **25+**  | Panama FFI (`java.lang.foreign`) 必需 |
| Godot | **4.6+** | GDExtension 支持 |
| Maven | 4.0.x（项目含 Wrapper） | 构建工具，可用 `./mvnw` 替代 |

> **Java 25 是硬性要求**。godot-java 使用 Panama Foreign Function & Memory API（`java.lang.foreign`），这是 Java 25+ 才稳定可用的特性。

### 安装 JDK

**macOS：**
```bash
brew install openjdk@25 maven
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 25)' >> ~/.zshrc
source ~/.zshrc
```

**Linux（Ubuntu/Debian）：**
```bash
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh
sdk install java 25.0.2-tem
sudo apt install maven
```

**Windows：**
1. 从 [Adoptium](https://adoptium.net/) 下载 JDK 25
2. `choco install maven`

验证安装：
```bash
java -version    # 应显示 25+
./mvnw --version # 首次使用 Maven Wrapper，自动下载 Maven 4.0+
# 或使用系统 Maven（需预装 4.0+）
mvn -version
```

## 第一步：创建 Maven 项目

```bash
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=my-godot-game \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
cd my-godot-game
```

也可以在 IntelliJ IDEA 中选择 **File -> New -> Project -> Maven Archetype** 创建。

## 第二步：添加依赖

### Maven

> 在 [Maven Central](https://central.sonatype.com/artifact/io.github.youngledo/godot-java-core) 查看最新版本号

编辑 `pom.xml`：

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
</properties>

<dependencies>
    <dependency>
        <groupId>io.github.youngledo</groupId>
        <artifactId>godot-java-core</artifactId>
        <version>VERSION</version> <!-- 替换为最新版本 -->
    </dependency>
</dependencies>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.youngledo:godot-java-core:VERSION") // 替换为最新版本
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
    implementation 'io.github.youngledo:godot-java-core:VERSION' // 替换为最新版本
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```

## 第三步：编写 Java 类

创建 `src/main/java/com/example/HealthComponent.java`：

```java
package com.example;

import org.godot.Godot;
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
    private void onHealthChanged(int newHealth) {
        // 方法体不使用，仅声明信号签名
    }

    @Signal(name = "died")
    private void onDied() {}

    @Override
    public void _ready() {
        currentHealth = maxHealth;
    }

    @GodotMethod
    public void takeDamage(int amount) {
        currentHealth -= amount;
        emit("health_changed", currentHealth);

        if (currentHealth <= 0) {
            emit("died");
        }
    }

    @GodotMethod
    public void heal(int amount) {
        currentHealth = Math.min(currentHealth + amount, maxHealth);
        emit("health_changed", currentHealth);
    }

    @GodotMethod
    public int getHealth() {
        return currentHealth;
    }
}
```

要点：
- `@GodotClass` 将类注册到 Godot ClassDB，`name` 为 Godot 中显示的类名
- `@Export` 将字段暴露到 Godot 编辑器的属性检查器
- `@Signal` 声明信号，方法签名定义参数类型
- `@GodotMethod` 将方法暴露给 Godot 对象系统调用
- 虚方法（`_ready`、`_process` 等）通过 `@Override` 实现

## 第四步：编译

```bash
mvn compile -Dcheckstyle.skip=true
```

编译产物在 `target/classes/` 目录下。

## 第五步：配置 Godot 项目

### 5.1 打包 Java 应用 Jar

Godot 会先加载原生 GDExtension，然后 godot-java 启动 JVM 并加载你的 Java 应用 jar。真实项目中，这个 jar 应该是 fat/shaded jar，包含你的游戏代码、`godot-java-core`、第三方依赖，以及注解处理器生成的注册表。

标准运行目录为：

```
your-godot-project/
├── godot-java/
│   ├── app.jar
│   └── libgodot-java.dylib   # 当前平台对应的 .dylib / .so / .dll
```

在应用项目中，这件事应该由项目自己的构建完成。`godot-java-template`
的 Maven 构建会在 `mvn package` 时生成 `target/app.jar`，解析匹配的
`godot-java-native` classifier artifact，并把两者写入 Godot 项目。

仓库示例和框架发布验证仍然可以用源码仓库里的同步脚本生成该布局：

```bash
scripts/sync-godot-java.sh \
  --project godot-java-examples/examples/it-test \
  --app-jar godot-java-examples/target/godot-java-examples.jar
```

默认情况下，同步脚本会解析当前平台匹配的 `godot-java-native` Maven artifact。本地开发框架时，也可以传入 `--native-lib` 或 `--native-zip` 使用刚构建出来的原生库。使用模板的应用开发者不应该依赖源码仓库里的这个脚本。

### 5.2 创建 .gdextension 文件

在 Godot 项目根目录创建 `godot-java.gdextension`：

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

### 5.3 部署目录结构

```
your-godot-project/
├── godot-java.gdextension
├── godot-java/
│   ├── app.jar
│   └── libgodot-java.dylib   # 或 .so / .dll
└── scenes/
```

`app.jar` 与原生库放在同一目录时，godot-java 会自动把该 jar 作为 classpath。`GODOT_JAVA_CLASSPATH` 仍可作为高级调试覆盖项使用。

### 5.4 在 Godot 编辑器中启用

1. 打开 Godot，进入 **项目 -> 项目设置 -> GDExtensions**
2. 点击 **添加** 并选择 `godot-java.gdextension` 文件
3. 重启 Godot 编辑器

## 第六步：在 Godot 场景中使用 Java 节点

创建一个场景，直接使用已注册的 Java 类作为节点类型：

```ini
[gd_scene load_steps=1 format=3]

[node name="HealthComponent" type="HealthComponent"]
```

把这个场景设置为 `project.godot` 的主场景，或者从另一个 Java 节点中实例化它：

```java
HealthComponent health = new HealthComponent();
addChild(health);
health.takeDamage(30);
```

按 **F5** 运行。Godot 会加载 GDExtension，godot-java 启动 JVM，然后由
Godot 的 ClassDB 创建 Java 节点。

## 常见问题

### "JVM not found"

确认 `JAVA_HOME` 指向 JDK 25+：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)  # macOS
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64  # Linux
```

### "Class not found"

- 执行 `mvn package` 构建应用 jar
- 执行同步流程，确保 `godot-java/app.jar` 已更新
- 确认类上标注了 `@GodotClass` 注解

### 原生库加载失败

- 检查 `.gdextension` 文件中的路径是否正确
- 确认原生库文件存在于 `godot-java/` 运行目录中
- 确认库文件架构与 Godot 匹配（x64/ARM64）

## 下一步

- [API 参考](api.md) -- 完整的注解和 API 文档
- [使用指南](guide.md) -- 核心概念和进阶用法
- [常见问题](troubleshooting.md) -- 错误排查
