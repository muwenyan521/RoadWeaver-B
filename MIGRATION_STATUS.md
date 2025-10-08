# Architectury 迁移状态

## ✅ 已完成

### 1. 项目配置
- ✅ `settings.gradle` - 仓库和模块配置  
- ✅ `common/build.gradle` - Common 模块配置
- ✅ `fabric/build.gradle` - Fabric 平台配置
- ✅ `neoforge/build.gradle` - NeoForge 平台配置

### 2. 跨平台桥接（已落地）
- ✅ 配置系统桥接：`common/config/IModConfig.java` + `ConfigProvider`（Fabric `FabricModConfigAdapter` / NeoForge `NeoForgeModConfigAdapter`）
- ✅ 世界数据桥接：`common/persistence/WorldDataProvider`（@ExpectPlatform）→ Fabric 使用 Attachment API，NeoForge 使用 SavedData（`WorldDataHelper`）
- ✅ 事件体系统一：`common/events/ModEventHandler` 使用 Architectury Events（平台主类仅调用 `ModEventHandler.register()`）
- ✅ 重复实现清理：NeoForge 端 `WorldDataProviderImpl` 统一改为返回 `NeoForgeWorldDataProvider`

### 3. 已迁移到 common 的代码
- ✅ `helpers/Records.java` - 数据记录类
- ✅ `features/roadlogic/RoadDirection.java` - 方向枚举
- ✅ `features/roadlogic/RoadPathCalculator.java` - A* 路径算法
- ✅ `features/roadlogic/Road.java` - 道路生成逻辑
- ✅ `features/decoration/*` - 装饰系统（含 `WoodSelector`、`RoadFenceDecoration` 等）
- ✅ `features/RoadFeature.java` - 世界特性（统一实现，平台侧仅做注册）
- ✅ `events/ModEventHandler.java` - 使用 Architectury 事件的通用事件处理器

## 🔄 进行中

### 当前任务：暂无
（注册系统统一已完成，见“已完成”与“下一步操作”更新）

## 📋 待迁移代码清单

### 优先级 1：纯逻辑类（无平台依赖）
已完成迁移（Road/RoadPathCalculator/装饰系统全集合）。

### 优先级 2：需要抽象的平台特定代码

这些项已完成或替换为统一实现：

1. **配置系统** - 已完成（`IModConfig` + `ConfigProvider` + 平台 Adapter）
2. **数据持久化** - 已完成（`WorldDataProvider` + Fabric Attachment / NeoForge SavedData）
3. **事件处理** - 已完成（Architectury Events，common 收敛）
4. **注册系统** - 待办：统一到 Architectury Registry（仍在平台侧注册）

### 优先级 3：平台特定实现

保留在各自模块：

1. **主类**
   - `fabric/SettlementRoads.java`
   - `neoforge/SettlementRoads.java`

2. **客户端（如调试 GUI）**
   - `client/gui/RoadDebugScreen.java`（平台侧维护）

3. **数据生成器**
   - `fabric/SettlementRoadsDataGenerator.java`

## 🎯 下一步操作

### 步骤 1：统一注册系统（已完成）
已在 `common/features/config/RoadFeatureRegistry.java` 使用 Architectury Registry（`DeferredRegister`/`RegistrySupplier`）注册 `RoadFeature`；
平台主类改为调用统一注册入口：
 - Fabric：`SettlementRoads.onInitialize()` 调用 `RoadFeatureRegistry.registerFeatures()` 后通过 `BiomeModifications` 注入 `ROAD_FEATURE_PLACED`
 - NeoForge：`SettlementRoads` 调用 `RoadFeatureRegistry.registerFeatures()`；生物群系注入使用数据驱动（`data/roadweaver/neoforge/biome_modifier/*.json`）

### 步骤 2：验证 common 模块
```bash
.\gradlew.bat :common:build
```

### 步骤 3：迁移装饰系统
按照优先级 1 列表逐个迁移装饰类。

### 步骤 4：抽象平台特定代码
参考 `ARCHITECTURY_MIGRATION_GUIDE.md` 中的示例。

## ⚠️ 注意事项

1. **包名映射**：
   - Fabric: `net.minecraft.util.math.BlockPos`
   - Mojmap: `net.minecraft.core.BlockPos`
   - Common 使用 Mojmap 映射

2. **API 差异**：
   - Fabric: `StructureWorldAccess`
   - Mojmap: `WorldGenLevel`

3. **逐步迁移**：
   - 每迁移一个类就测试编译
   - 确保 common 模块始终能编译通过

## 🔧 快速命令

```bash
# 清理并构建所有模块
.\gradlew.bat clean build

# 只构建 common
.\gradlew.bat :common:build

# 只构建 fabric
.\gradlew.bat :fabric:build

# 只构建 neoforge
.\gradlew.bat :neoforge:build

# 运行 Fabric 客户端
.\gradlew.bat :fabric:runClient

# 运行 NeoForge 客户端
.\gradlew.bat :neoforge:runClient
```

## 📚 参考文档

- `ARCHITECTURY_MIGRATION_GUIDE.md` - 详细迁移指南
- [Architectury 官方文档](https://docs.architectury.dev/)
- [Architectury API JavaDoc](https://maven.architectury.dev/docs/architectury-api/)
