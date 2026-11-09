# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# 借贷 Demo — CLAUDE.md

## 项目概述
Android 借贷业务演示 App。**当前无后端，所有数据仅存于本地 Room 数据库。**
未来计划接入远程 API，因此 Repository 接口必须设计为可替换实现，
UI 和 Domain 层不得直接依赖 Room 或任何数据源细节。

## 技术栈（不可偏离）
- Kotlin + Jetpack Compose + Material 3（禁止 XML 布局、禁止 View 系统）
- MVVM + UiState（StateFlow），Composable 中不放业务逻辑
- Hilt（依赖注入）
- Room（本地数据库，使用 KSP 而非 KAPT）
- Coroutines + Flow（禁止 LiveData、RxJava、AsyncTask、回调风格）
- **现状说明**：当前项目尚未引入 Hilt/Room/KSP，首次需要时通过
  `gradle/libs.versions.toml` 添加版本与依赖（不要在 build.gradle.kts 里写死版本号）。

## 目录约定
以下路径均相对 `app/src/main/java/com/example/loan/`：
- `data/local/entity/` — Room Entity
- `data/local/dao/` — Room DAO
- `data/local/db/` — AppDatabase
- `domain/repository/` — Repository 接口（纯 Kotlin：禁止 `import androidx.*` 和 `import android.*`）
- `data/repository/` — Repository 实现
- `di/` — Hilt Module
- `ui/` — Compose 页面与 ViewModel

## 验证命令
- 构建：`./gradlew assembleDebug`（Windows 下也可用 `gradlew.bat`）
- 单元测试：`./gradlew test`
- 每次修改后必须运行 `./gradlew assembleDebug`，构建通过才算完成。
- 涉及 data/domain 层的改动必须同时运行 `./gradlew test`。
- 注意：本机 shell 中 `java` 不在 PATH，构建前需设置
  `JAVA_HOME="D:\anzhuo\jbr"`（Android Studio 自带 JBR，OpenJDK 25）。

## 业务边界
- 当前阶段只实现本地数据层和核心业务闭环。
- `UserEntity` 的 `kycStatus`、`creditLimit` 等字段先保留，暂不实现 KYC 流程。
- 贷款状态用 `enum class LoanStatus` 定义，禁止用字符串硬编码。
- 利息计算规则由 Repository 实现，UI 不重复计算。
- 贷款产品、利率等业务常量集中放在 `domain/` 下的常量对象中，不散落在 UI 层。

## 工作方式
- 每个任务完成后，先运行构建验证，再报告结果。
- 不要一次性修改多个无关文件；按数据层 → Domain → UI 的顺序推进。
- 遇到不确定的 API 版本，先查看 `gradle/libs.versions.toml`，不要猜测。
- AGP 9 内置 Kotlin 支持，构建文件中没有 `org.jetbrains.kotlin.android` 插件，属正常现象，不要"修复"。
- `org.gradle.configuration-cache=true` 已开启。
