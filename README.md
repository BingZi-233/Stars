<div style="text-align: center;">
  <img src="assets/logo.png" alt="Stars Logo" style="width: 160px;" />
</div>

# Stars

基于 Spring Boot 4.x + Kotlin + [Shiro](https://github.com/MisakaTAT/Shiro) 的 OneBot 插件化机器人框架。核心提供插件加载、事件总线、共享数据库、Bot 桥接等基础设施，业务功能以独立 Gradle Composite Build 插件形式注入。

## 模块结构

| 模块 | 类型 | 说明 |
| :--- | :--- | :--- |
| `stars-plugin-api` | 子项目 | 插件 SDK：`Plugin`/`PluginContext`/事件注解/数据库 Facade。插件 `compileOnly` 引用。 |
| `stars-core` | 子项目（Spring Boot 应用） | 主程序。负责加载 `plugins/` 目录下的插件 jar、暴露 actuator 端点、托管 Shiro Bot。 |
| `plugin/sample-plugin` | 包含构建（includeBuild） | 示例插件 `HelloPlugin`。 |

插件构建产物会被自身的 `deploy` 任务复制到根目录 `plugins/`，主程序启动时从该目录扫描加载。

## 环境要求

- JDK 21（Gradle Toolchain 已声明，缺失会自动 provisioning）
- Gradle Wrapper（仓库自带 `./gradlew`）

## 构建

### 一键构建核心 + 全部插件

```bash
./gradlew buildAll
```

该任务分两阶段执行：

1. `buildPluginApi` —— 先执行 `stars-plugin-api:publishToMavenLocal`，将 SDK 发布到本地 Maven 仓库（插件通过 GAV 坐标 `compileOnly` 引用，必须先存在）。
2. `buildCoreAndPlugins` —— 在子 Gradle 进程中并行执行 `:stars-core:bootJar` 与所有插件的 `:deploy`。

构建完成后：

- `stars-core/build/libs/stars-core-0.0.1-SNAPSHOT.jar` —— 可执行 Spring Boot fat jar
- `plugins/*.jar` —— 已部署的插件 jar（`HelloPlugin`、`NicknameGuard`、`BanQueryPlugin`）

### 单独构建

```bash
# 仅 SDK（发布到 mavenLocal）
./gradlew :stars-plugin-api:publishToMavenLocal

# 仅核心 fat jar（需先发布 SDK）
./gradlew :stars-plugin-api:publishToMavenLocal :stars-core:bootJar

# 单个插件部署到 plugins/（需先发布 SDK）
./gradlew :stars-plugin-api:publishToMavenLocal :NicknameGuard:deploy
```

### 清理

```bash
./gradlew clean
rm -rf plugin/*/build plugins/*.jar
```

## 运行

```bash
java -jar stars-core/build/libs/stars-core-0.0.1-SNAPSHOT.jar
```

主进程默认监听 `8080` 端口，并在 `/ws/shiro` 暴露 OneBot WebSocket 服务端，等待 OneBot 实现（如 NapCat、Lagrange）反向连接。

### 主要环境变量

| 变量 | 默认 | 说明 |
| :--- | :--- | :--- |
| `STARS_HTTP_PORT` | `8080` | HTTP 端口 |
| `STARS_DB_URL` | `jdbc:h2:file:./data/stars;AUTO_SERVER=TRUE;MODE=LEGACY` | JDBC URL |
| `STARS_DB_DRIVER` | `org.h2.Driver` | JDBC Driver |
| `STARS_DB_USER` / `STARS_DB_PASS` | `sa` / — | DB 凭据 |
| `STARS_PLUGINS_DIR` | `./plugins` | 插件扫描目录 |
| `STARS_AUTO_ENABLE` | `true` | 启动时是否自动启用所有插件 |
| `STARS_ADMINS` | — | 全局管理员 QQ 列表（逗号分隔） |
| `SHIRO_TOKEN` | — | OneBot 反向 WS 鉴权 token |
| `SHIRO_TIMEOUT` | `10` | WS 连接超时（秒） |
| `SHIRO_SERVER_ENABLE` | `true` | 启用 WS 服务端模式 |
| `SHIRO_SERVER_URL` | `/ws/shiro` | OneBot WS 路径 |
| `SHIRO_CLIENT_ENABLE` | `false` | 启用 WS 客户端模式（主动连接） |
| `SHIRO_CLIENT_URL` | — | WS 客户端目标地址 |

Actuator 暴露 `health`、`info`、`plugins` 端点，可通过 `/actuator/plugins` 查询当前已加载插件状态。

## 插件开发

新建插件最快路径：使用 Claude Code Skill `create-plugin`，会按 `plugin/sample-plugin` 模板生成 Composite Build 骨架，并在根 `settings.gradle.kts` 自动注册 `includeBuild`。

手动新增时需要：

1. 在 `plugin/<YourPlugin>/` 下创建独立 Gradle build（含 `settings.gradle.kts` + `build.gradle.kts`）。
2. 添加 `mavenLocal()` 仓库，并 `compileOnly` 引用 SDK：
   ```kotlin
   repositories {
       mavenLocal()
       mavenCentral()
   }

   dependencies {
       compileOnly("online.bingzi:stars-plugin-api:0.0.1-SNAPSHOT")
   }
   ```
3. 提供 `tasks.register<Copy>("deploy")`，目标目录为根 `plugins/`。
4. 在 `src/main/resources/plugin.json` 声明元数据：
   ```json
   {
     "name": "YourPlugin",
     "version": "1.0.0",
     "main": "com.example.YourPlugin",
     "authors": ["yourname"],
     "apiVersion": "1.0",
     "depend": [],
     "softdepend": [],
     "loadbefore": []
   }
   ```
   其中 `main` 为继承 `JavaPlugin` 的入口类全限定名，`depend`/`softdepend`/`loadbefore` 填插件名列表。
5. 主类继承 `online.bingzi.stars.plugin.api.JavaPlugin`，重写 `onLoad` / `onEnable` / `onDisable`。
6. 在根 `settings.gradle.kts` 末尾添加 `includeBuild("plugin/<YourPlugin>")`。
7. 同步将 `<YourPlugin>` 加入根 `build.gradle.kts` 的 `pluginBuildNames` 列表，使其纳入 `buildAll`。

插件可通过 `PluginContext` 访问：

- `eventBus` —— 注册 `@EventHandler` / `@MessageHandler` 监听 OneBot 事件
- `database` —— 共享 H2/外部数据库的 prefixed `JdbcOperations` Facade
- `bot` —— 主 Bot 实例，发送主动消息
- `config` —— 当前插件 `application.yaml` 命名空间下的配置

## ❤️ Sponsor

> [Want to appear here?](mailto:lhby233@outlook.com)

<table>
<tr>
<td style="width: 180px;"><a href="https://www.packyapi.com/register?aff=r9cg"><img src="assets/partners/logos/packycode.png" alt="PackyCode" style="width: 150px;"></a></td>
<td>Thanks to PackyCode for sponsoring this project! PackyCode is a reliable and efficient API relay service provider, offering relay services for Claude Code, Codex, Gemini, and more. Register using <a href="https://www.packyapi.com/register?aff=r9cg">this link</a> to get started.</td>
</tr>
</table>

## License

见仓库内单独声明（如有）。
