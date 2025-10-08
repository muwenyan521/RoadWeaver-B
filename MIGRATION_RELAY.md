# RoadWeaver 迁移/构建接力文档

用于跨会话延续 Architectury 多端迁移与构建工作，记录已完成项、当前状态与后续任务清单。

> 最近核对：2025-10-08T13:21:02+08:00（本会话已统一注册系统；尚未重新运行客户端）

---

## 概览
- **目标版本**：Minecraft 1.21.1 / Java 21 / Gradle / Architectury (Loom)
- **模块**：`common` + `fabric` + `neoforge`
- **关键设计**：
  - `common/` 定义跨平台接口与数据模型（如 `IModConfig`、`WorldDataProvider`、数据 `Records`、道路算法与装饰）。
  - 平台端通过 `@ExpectPlatform` 提供 `...Impl` 实现（如 `WorldDataProviderImpl`）。
  - 配置桥接：`ConfigProvider.get()` → 平台侧 `ConfigProviderImpl.get()` 返回 `IModConfig` 实现。

---

## 已完成工作（已核验）
- **依赖与仓库**
  - `neoforge/build.gradle`：已使用 `neoForge "net.neoforged:neoforge:${project.neoforge_version}"` DSL。
  - `neoforge/gradle.properties`：包含 `loom.platform=neoforge`，启用 NeoForge 平台。
  - 根级 `gradle.properties`：
    - `minecraft_version=1.21.1` / `loader_version=0.16.10` / `fabric_version=0.115.1+1.21.1` / `neoforge_version=21.1.169`
    - JVM 参数：`-Djava.net.preferIPv4Stack=true -Dhttps.protocols=TLSv1.2,TLSv1.3`
    - 代理：`127.0.0.1:7890`（http/https）
  - `settings.gradle`：已为 `net.neoforged` 配置 `exclusiveContent` 指向 `https://maven.neoforged.net/releases/`。

- **平台实现 / 桥接**
  - `common/persistence/WorldDataProvider.java`：抽象类 + `@ExpectPlatform getInstance()`。
  - `fabric/persistence/WorldDataProviderImpl.java` → 返回 `FabricWorldDataProvider`；
    `neoforge/persistence/WorldDataProviderImpl.java` → 返回 `NeoForgeWorldDataProvider`。
  - `fabric/persistence/fabric/FabricWorldDataProvider.java`：使用 Attachment API；
    `neoforge/persistence/neoforge/NeoForgeWorldDataProvider.java`：委托 `WorldDataHelper`（SavedData）。

- **配置系统一致性**
  - `common/config/IModConfig.java`：包含“手动连接”阈值 `manualMaxHeightDifference()`、`manualMaxTerrainStability()`。
  - 平台实现：
    - `fabric/config/fabric/FabricModConfig.java`（MidnightLib）。
    - `neoforge/config/neoforge/NeoForgeModConfig.java` + `neoforge/config/NeoForgeModConfigAdapter.java`。
  - 统一入口：`common/config/ConfigProvider.java` + 平台侧 `config/ConfigProviderImpl.java`。

- **核心公共逻辑（已在 common）**
  - 道路算法：`common/.../features/roadlogic/RoadPathCalculator.java`
  - 道路生成：`common/.../features/roadlogic/Road.java`
  - 数据模型：`common/.../helpers/Records.java`
  - 结构连接：`common/.../helpers/StructureConnector.java`
  - 装饰系统：`common/.../features/decoration/*`、`.../decoration/util/WoodSelector.java`
  - 配置模型：`common/.../features/config/RoadFeatureConfig.java`

- **资源与清单**
  - 统一注册：`common/src/main/java/.../features/config/RoadFeatureRegistry.java` 使用 Architectury Registry。
  - 统一数据：在 common 资源新增 `data/roadweaver/worldgen/configured_feature/road_feature.json` 与 `placed_feature/road_feature_placed.json`，两端共享。
  - Fabric 端：新增 `features/config/FabricBiomeInjection.java` 通过 BiomeModifications 注入 placed feature。
  - NeoForge 端：保留 `data/roadweaver/neoforge/biome_modifier/add_road_feature.json` 进行数据驱动注入。
  - Fabric：`fabric/src/main/resources/fabric.mod.json`（依赖 `architectury >= 13.0.0`）。
  - NeoForge：`neoforge/src/main/resources/META-INF/neoforge.mods.toml`（由 `processResources` 展开占位符）。

> 备注：`MIGRATION_STATUS.md` 中“已迁移到 common 的代码”列表未包含 `Road`/`RoadPathCalculator`，但当前已在 `common` 实际存在（文档需后续同步）。

---

## 当前状态（沿用上次记录 + 本次核验）
- **构建链**：上次记录显示项目全量构建成功、`neoforge` 可打包；本会话未重跑构建。
- **桥接对齐**：`ConfigProvider` / `WorldDataProvider` 体系已与 `common` 对齐，平台实现可用。
- **事件/注册**：
  - 事件处理：已在 common 使用 Architectury Events（`common/src/main/java/net/countered/settlementroads/events/ModEventHandler.java`），平台主类仅调用 `ModEventHandler.register()`。
  - 注册（已统一）：两端均调用 `common/.../features/config/RoadFeatureRegistry.registerFeatures()`；
    - Fabric：`SettlementRoads.onInitialize()` 之后调用 `FabricBiomeInjection.inject()` 注入 `ROAD_FEATURE_PLACED_KEY`；
    - NeoForge：通过 `data/neoforge/biome_modifier` 注入。
- **目录提醒**：仓库内存在历史副本 `RoadWeaver/`（包含重复源码与构建文件），不在当前多模块构建链内，避免混淆。

---

## 待办清单（建议顺序）
1. **文档同步与清理**（高优先级）
   - 已更新 `MIGRATION_STATUS.md`，标注“注册系统统一（已完成）”。
   - 评估并处理历史目录 `RoadWeaver/`（确认无用后再做归档/移除，避免误编辑）。

2. **事件抽象（已完成/复核）**（中优先级）
   - 事件已用 Architectury Events；后续仅需复核平台端调用处是否有遗留。

3. **多人手动连接支持**（中优先级）
   - Fabric 端 `client/gui/RoadDebugScreen.java` 已有“手动连接模式”；NeoForge 端 UI 存在同名类，需核实是否含该模式。
   - 增补 C2S 数据包（Fabric/NeoForge 双端）以在多人环境下创建 `Records.StructureConnection`（写入 `WorldDataProvider` 数据源）。

4. **镜像地址微调**（可选）
   - `settings.gradle` 中的阿里云镜像建议统一为 `https://maven.aliyun.com/repository/public/`。

5. **数据生成与资源校验**（可选）
   - 校验 `neoforge.mods.toml` 占位符是否经 `processResources` 正确展开。
   - 如需使用数据生成器：补全并验证 `SettlementRoadsDataGenerator` 与 Fabric `fabricApi.configureDataGeneration` 产物。
   - 如需避免资源重复：可在任一平台端移除与 common 完全相同的 `configured_feature/placed_feature` JSON（可选优化）。

---

## 常用构建命令
```powershell
# 清理并构建 NeoForge 子模块
.\u0067radlew.bat :neoforge:clean :neoforge:build --stacktrace --info

# 构建全项目
.\u0067radlew.bat clean build

# 停止 Gradle 守护进程
.\u0067radlew.bat --stop
```

---

## 关键文件索引（现状）
- **根级**：`build.gradle`、`settings.gradle`、`gradle.properties`
- **common/**：
  - `build.gradle`
  - `persistence/WorldDataProvider.java`
  - `features/roadlogic/Road.java`、`features/roadlogic/RoadPathCalculator.java`
  - `helpers/Records.java`、`helpers/StructureConnector.java`
  - `features/decoration/*`、`features/decoration/util/WoodSelector.java`
  - `features/config/RoadFeatureConfig.java`
  - `features/config/RoadFeatureRegistry.java`
  - `resources/data/roadweaver/worldgen/{configured_feature,placed_feature}/...`
- **fabric/**：
  - `config/fabric/FabricModConfig.java`
  - `config/ConfigProviderImpl.java`
  - `persistence/WorldDataProviderImpl.java`
  - `persistence/fabric/FabricWorldDataProvider.java`
  - `persistence/attachments/WorldDataAttachment.java`
  - `features/config/FabricBiomeInjection.java`
  - `resources/fabric.mod.json`
  - 注：平台侧可能存在历史 `features/RoadFeature.java`/`events/ModEventHandler.java` 文件，但已在 `build.gradle` 中通过 `sourceSets.main.java.exclude` 排除，不参与编译。
- **neoforge/**：
  - `build.gradle`、`gradle.properties`
  - `config/neoforge/NeoForgeModConfig.java`
  - `config/NeoForgeModConfigAdapter.java`
  - `config/ConfigProviderImpl.java`
  - `persistence/WorldDataProviderImpl.java`
  - `persistence/neoforge/NeoForgeWorldDataProvider.java`
  - `persistence/WorldDataHelper.java`
  - `features/config/RoadFeatureRegistry.java`
  - `resources/META-INF/neoforge.mods.toml`

---

## 设计/记忆参考
{{ ... }}
- 项目职责划分：`README.md` + 以上目录结构。

---

## 交接摘要
- 已完成：NeoForge 依赖 DSL、平台标识、仓库解析与代理、配置与世界数据桥接；核心道路逻辑与装饰系统已迁入 `common`；平台侧可注册与运行。
- 待推进：注册与事件的 Architectury 抽象合流、多人的手动连接模式（C2S 包）。
- 注意：避免编辑历史目录 `RoadWeaver/` 内重复代码。

> 如需继续推进，建议从“事件抽象/注册抽象/多人 C2S 包”三条线择一开始；如需我执行具体改造，请在新会话指定优先级。
