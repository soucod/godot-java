# godot-java-examples

中文 | [ENGLISH](README.md)

[godot-java](https://github.com/youngledo/godot-java) 渐进式教程示例 — 基于 Java 25 + Panama FFI 的 Godot 绑定。

## 前置要求

- **JDK 25+**（Project Panama FFI 所需）
- **Godot 4.6+**
- **Maven 4.0+**

## 配置

这些示例位于框架源码仓库中。新游戏项目建议从 `godot-java-template`
开始；模板会在 `mvn package` 时打包 Java 应用并同步 native runtime。

### 1. 构建示例 jar

```bash
./mvnw package -pl godot-java-examples -am -DskipTests
```

生成 fat JAR：`target/godot-java-examples.jar`。

### 2. 同步运行时文件到示例项目

标准项目本地运行目录为 `godot-java/`，里面放 `app.jar` 和原生 GDExtension 库：

如果你在试用已发布 tag，可以同步同版本的已发布 native artifact：

```bash
./scripts/sync-godot-java.sh \
  --project godot-java-examples/examples/it-test \
  --app-jar godot-java-examples/target/godot-java-examples.jar \
  --version 0.1.2
```

如果你在 `main` 上开发框架源码，应先从源码构建 native 库，并显式传入，
避免 Java 与 native runtime 版本不一致：

```bash
./godot-java-core/native/build-macos.sh
./scripts/sync-godot-java.sh \
  --project godot-java-examples/examples/it-test \
  --app-jar godot-java-examples/target/godot-java-examples.jar \
  --native-lib godot-java-core/native/build/libgodot-java.dylib
```

Linux 或 Windows 请使用 `build-linux.sh` / `build-windows.bat` 和对应库文件。

### 3. 在 Godot 中打开示例

用 Godot 打开 `examples/` 下的任意示例目录：

```bash
# macOS
open -a Godot.app ./examples/01-hello-world
```

或者以无头模式运行：
```shell
# 步骤 1
/Applications/Godot.app/Contents/MacOS/Godot \                                                                                                                                                               
    --path ./examples/01-hello-world \                                                                                                                                                             
    --editor --quit-after 2

# 步骤 2
/Applications/Godot.app/Contents/MacOS/Godot \                                                                                                                                                                     
    --path ./examples/01-hello-world \                                                                                                                                                             
    --headless --quit
```

## 示例列表

| # | 名称 | 概念 | 说明 |
|---|------|------|------|
| 01 | [hello-world](examples/01-hello-world) | `@GodotClass`、`_ready()` | 节点进入场景树时打印消息 |
| 02 | [export-properties](examples/02-export-properties) | `@Export` | 将字段暴露给 Godot Inspector 编辑 |
| 03 | [godot-methods](examples/03-godot-methods) | `@GodotMethod` | 从 GDScript 调用 Java 方法，支持参数和返回值 |
| 04 | [signals](examples/04-signals) | `@Signal` | 在 Java 中声明和发射信号，在 GDScript 中连接 |
| 05 | [process-loop](examples/05-process-loop) | `_process(delta)` | 每帧更新 — 圆形轨道运动 |
| 06 | [node-tree](examples/06-node-tree) | `getChildren()`、`addChild()`、`getNode()` | 场景树遍历和动态创建节点 |
| 07 | [physics-2d](examples/07-physics-2d) | `CharacterBody2D`、`_physicsProcess()`、`move_and_slide()` | 2D 物理运动：重力与移动 |
| 08 | [math-types](examples/08-math-types) | `Vector2`、`Vector3`、`Color` | 数学类型运算（纯计算，无运动） |

## 工作原理

较早的功能示例遵循相同的模式：

1. **Java 类** — 使用 `@GodotClass` 注解，继承 Godot 节点类型（如 `Node`、`Node2D`、`CharacterBody2D`）
2. **GDScript** — 通过 `ClassDB.instantiate("ClassName")` 创建 Java 对象并加入场景树
3. **Godot 项目** — 包含 `project.godot`、`godot-java.gdextension` 和场景文件的最小项目

`godot-java.gdextension` 文件告诉 Godot 加载原生桥接库（`libgodot-java.so`/`.dylib`），该库启动 JVM 并注册 classpath 上所有带 `@GodotClass` 注解的类。

## 核心注解

```java
@GodotClass(name = "MyNode", parent = "Node2D")  // 注册为 Godot 类
public class MyNode extends Node2D {

    @Export                                       // 在 Godot Inspector 中可见
    public double speed = 200.0;

    @Override
    public void _ready() { ... }                  // 生命周期：加入场景树时调用一次

    @Override
    public void _process(double delta) { ... }    // 生命周期：每帧调用

    @GodotMethod                                  // 可从 GDScript 调用
    public int compute(int x) { return x * 2; }

    @Signal                                       // 声明信号
    public void onHit(int damage) {}
}
```

从 Java 发射信号：
```java
call("emit_signal", "onHit", 42);
```

在 GDScript 中连接信号：
```gdscript
var node = ClassDB.instantiate("MyNode")
node.connect("onHit", _on_hit)
```

## 项目结构

```
godot-java-examples/
├── pom.xml                       # Maven 构建（shade 打 fat jar）
├── src/main/java/examples/       # Java 示例类
└── examples/
    ├── it-test/
    │   ├── godot-java/           # 同步后的运行目录（已忽略）
    │   │   ├── app.jar
    │   │   └── libgodot-java.dylib   # 或当前平台的 .so / .dll
    │   ├── godot-java.gdextension
    │   └── test_runner.tscn
    ├── 01-hello-world/
    │   ├── native/               # 旧版本地运行目录
    │   ├── godot-java.gdextension
    │   └── main.tscn
    ├── 02-export-properties/
    └── ...
```

`it-test` 项目已经使用标准 `godot-java/` 运行布局。其它功能示例仍保留原有
`native/` 布局，后续会逐步迁移。

## 许可证

Apache-2.0
