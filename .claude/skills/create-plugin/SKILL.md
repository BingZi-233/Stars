---
name: create-plugin
description: Scaffold a new Stars plugin as a Gradle composite build. Mirrors sample-plugin layout (build.gradle.kts, plugin.json, JavaPlugin main class) and registers it via includeBuild in the root settings.gradle.kts.
---

# create-plugin

Scaffold a new Stars plugin module under `<repo-root>/plugin/<plugin-name>/` and register it as a composite build.

## Arguments

The user invokes this skill via `/create-plugin <args>`. Parse `args` as a free-form description; extract:

- **plugin name** (required) — the module directory name AND the `name` field in `plugin.json`. PascalCase preferred (e.g., `EchoPlugin`, `WeatherBot`).
- **package** (optional) — Kotlin package for the main class. Default: `online.bingzi.<lowercase-pluginname-without-suffix>`.
- **main class** (optional) — class name extending `JavaPlugin`. Default: same as plugin name.
- **author** (optional) — string for `authors` array. Default: `bingzi`.
- **description** (optional) — string for `description` field. Default: empty.

If the arguments are ambiguous (e.g., user only typed `/create-plugin`), ask one consolidated `AskUserQuestion` before scaffolding.

## Preconditions

Before writing files:

1. Confirm CWD is the Stars repo root. Look for `settings.gradle.kts` containing `rootProject.name = "Stars"`. If not found, abort with an error.
2. Verify target directory `<repo-root>/plugin/<plugin-name>/` does not already exist. If it does, abort and tell the user.
3. Read existing `plugin/sample-plugin/build.gradle.kts` to mirror its current shape — the canonical template is whatever sample-plugin currently uses (compileOnly deps, archiveBaseName, deploy task). Note paths use `rootDir.parentFile.parentFile` to escape `plugin/<name>/` back to repo root.

## Files to create

All paths relative to the repo root.

### 1. `plugin/<plugin-name>/settings.gradle.kts`

```kotlin
rootProject.name = "<plugin-name>"
```

### 2. `plugin/<plugin-name>/build.gradle.kts`

Mirror `plugin/sample-plugin/build.gradle.kts` exactly, only changing:
- `group` — set to the chosen package (without the trailing class segment).
- `version` — `"1.0.0"`.
- `archiveBaseName` — set to `<plugin-name>`.

The `compileOnly` dependencies, `tasks.register<Copy>("deploy")`, and `tasks.named("build") { finalizedBy("deploy") }` block must be copied verbatim. Do not change the `stars-plugin-api` JAR coordinate or the `com.mikuac:shiro` version unless explicitly asked.

### 3. `plugin/<plugin-name>/src/main/resources/plugin.json`

```json
{
  "name": "<plugin-name>",
  "version": "1.0.0",
  "main": "<package>.<main-class>",
  "authors": ["<author>"],
  "depend": [],
  "softdepend": [],
  "loadbefore": [],
  "apiVersion": "1.0",
  "description": "<description>"
}
```

### 4. `plugin/<plugin-name>/src/main/kotlin/<package-path>/<main-class>.kt`

```kotlin
package <package>

import online.bingzi.stars.plugin.api.JavaPlugin

class <main-class> : JavaPlugin() {

    override fun onEnable() {
        logger.info("enabled")
    }

    override fun onDisable() {
        logger.info("disabled")
    }
}
```

`<package-path>` = `<package>` with `.` replaced by `/`.

## Register the composite build

Append `includeBuild("plugin/<plugin-name>")` to the root `settings.gradle.kts`. Use the `Edit` tool to add it after the existing `includeBuild(...)` lines (or after the `include(...)` line if no `includeBuild` exists yet). Do not duplicate if already present.

## Verification

After scaffolding, run `./gradlew :<plugin-name>:build` in the background to confirm the new module compiles and the deploy task copies the JAR to `./plugins/`. If the build fails, show the user the failing task and the first error.

## What this skill does NOT do

- Does not add event handlers (`@OnPrivateMessage`, etc.) — leave the main class minimal.
- Does not modify `stars-plugin-api` or `stars-core`.
- Does not commit the new module.
- Does not run `./gradlew :stars-plugin-api:build` first; that is the user's responsibility if the API JAR is stale.
