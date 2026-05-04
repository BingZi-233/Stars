<div align="center">

![logo.png](assets/logo.png)

**基于 Spring Boot 4.x + Kotlin 的 OneBot 热插拔插件化机器人框架**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-purple.svg)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-green.svg)](https://spring.io/projects/spring-boot)
[![Version](https://img.shields.io/badge/version-1.0.0-orange.svg)](https://github.com/BingZi-233/Stars/releases)

[快速开始](#快速开始) • [模块结构](#模块结构) • [插件开发](#插件开发) • [Wiki](https://github.com/BingZi-233/Stars/wiki)

</div>

# Stars

基于 Spring Boot 4.x + Kotlin + [Shiro](https://github.com/MisakaTAT/Shiro) 的 OneBot **热插拔**插件化机器人框架。核心提供插件加载、事件总线、共享数据库、Bot 桥接等基础设施，业务功能以独立 Gradle Composite Build 插件形式注入，支持运行时热插拔。

## 快速开始

**环境要求：** JDK 21、Gradle Wrapper（仓库自带）

```bash
# 构建核心 + 全部插件
./gradlew buildAll

# 运行
java -jar stars-core/build/libs/stars-core-*.jar
```

主进程监听 `8080` 端口，在 `/ws/shiro` 暴露 OneBot WebSocket 服务端。

详细构建、配置、插件开发说明见 **[Wiki](https://github.com/BingZi-233/Stars/wiki)**。

## 模块结构

| 模块 | 说明 |
| :--- | :--- |
| `stars-plugin-api` | 插件 SDK，以 compileOnly 方式引用 |
| `stars-core` | 主程序，负责插件加载、事件总线、Bot 托管 |
| `plugin/` 子目录 | 各业务插件（Gradle Composite Build，自动扫描加载） |

## 插件开发

在 `plugin/<YourPlugin>/` 下创建独立 Gradle build，框架自动扫描，**无需**修改根项目配置。

完整步骤见 [Wiki · 插件开发指南](https://github.com/BingZi-233/Stars/wiki/%E6%8F%92%E4%BB%B6%E5%BC%80%E5%8F%91%E6%8C%87%E5%8D%97)。

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
