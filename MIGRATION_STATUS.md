# Architectury 迁移状态

## ✅ 已完成

### 1. 项目配置
- ✅ 根项目 `build.gradle` - Architectury 插件配置
- ✅ `settings.gradle` - 仓库和模块配置  
- ✅ `common/build.gradle` - Common 模块配置
- ✅ `fabric/build.gradle` - Fabric 平台配置
- ✅ `neoforge/build.gradle` - NeoForge 平台配置

### 2. 已迁移到 common 的代码
- ✅ `helpers/Records.java` - 数据记录类
- ✅ `features/roadlogic/RoadDirection.java` - 方向枚举

## 🔄 进行中

### 当前任务：验证 common 模块编译

运行命令：
```bash
.\gradlew.bat :common:build
```

## 📋 待迁移代码清单

### 优先级 1：纯逻辑类（无平台依赖）
这些类可以直接复制到 common：

1. **算法类**
   - `features/roadlogic/RoadPathCalculator.java` (269行)
   - `features/roadlogic/Road.java` (113行)

2. **装饰系统基类**
   - `features/decoration/Decoration.java`
   - `features/decoration/OrientedDecoration.java`
   - `features/decoration/BiomeWoodAware.java` (接口)
   - `features/decoration/util/WoodSelector.java`

3. **具体装饰类**
   - `features/decoration/LamppostDecoration.java`
   - `features/decoration/RoadFenceDecoration.java`
   - `features/decoration/DistanceSignDecoration.java`
   - `features/decoration/FenceWaypointDecoration.java`
   - `features/decoration/StructureDecoration.java`
   - `features/decoration/SwingDecoration.java`
   - `features/decoration/NbtStructureDecoration.java`
   - `features/decoration/RoadStructures.java`

### 优先级 2：需要抽象的平台特定代码

这些需要创建接口或使用 Architectury API：

1. **配置系统** - `config/ModConfig.java`
   - 需要创建接口，Fabric 用 MidnightLib，NeoForge 用 Config API

2. **数据持久化** - `persistence/WorldDataHelper.java`
   - Fabric 用 Attachment API
   - NeoForge 用 SavedData
   - 需要创建统一接口

3. **事件处理** - `events/ModEventHandler.java`
   - 使用 Architectury Events API 替代

4. **注册系统** - `features/config/RoadFeatureRegistry.java`
   - 使用 Architectury Registry API

5. **辅助类**
   - `helpers/StructureLocator.java`
   - `helpers/StructureConnector.java`

### 优先级 3：平台特定实现

保留在各自模块：

1. **主类**
   - `fabric/SettlementRoads.java`
   - `neoforge/SettlementRoads.java`

2. **客户端**
   - `client/SettlementRoadsClient.java`
   - `client/gui/RoadDebugScreen.java`

3. **数据生成器**
   - `fabric/SettlementRoadsDataGenerator.java`

## 🎯 下一步操作

### 步骤 1：验证 common 模块
```bash
.\gradlew.bat :common:build
```

如果成功，继续步骤 2。

### 步骤 2：迁移算法类
```bash
# 复制 RoadPathCalculator
cp fabric/src/main/java/net/countered/settlementroads/features/roadlogic/RoadPathCalculator.java \
   common/src/main/java/net/countered/settlementroads/features/roadlogic/

# 复制 Road
cp fabric/src/main/java/net/countered/settlementroads/features/roadlogic/Road.java \
   common/src/main/java/net/countered/settlementroads/features/roadlogic/
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
