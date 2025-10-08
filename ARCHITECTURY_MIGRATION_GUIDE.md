# Architectury API 迁移指南

## ✅ 已完成的配置

### 1. 项目结构更新
- ✅ 根项目 `build.gradle` - 添加 Architectury 插件
- ✅ `settings.gradle` - 添加 Architectury 仓库和 common 模块
- ✅ `common/build.gradle` - 创建通用代码模块
- ✅ `fabric/build.gradle` - 配置为依赖 common 模块
- ✅ `neoforge/build.gradle` - 配置为依赖 common 模块
- ✅ `fabric.mod.json` - 添加 Architectury API 依赖
- ✅ `neoforge.mods.toml` - 添加 Architectury API 依赖

### 2. Architectury 版本
- **Architectury Loom**: 1.11-SNAPSHOT
- **Architectury Plugin**: 3.4-SNAPSHOT
- **Architectury API**: 13.0.8

## 当前状态同步
- 配置桥接、世界数据桥接、事件系统已统一到 `common/`（`ConfigProvider` / `WorldDataProvider` / `ModEventHandler`）。
- 注册系统仍在平台侧（Fabric/NeoForge）维护；可选将 `RoadFeature` 注册统一到 `common/` 使用 Architectury Registry。
- 已清理重复实现：NeoForge 端 `WorldDataProviderImpl` 统一返回 `NeoForgeWorldDataProvider`。

---

## 📋 代码迁移步骤

### 阶段 1: 识别通用代码

需要移动到 `common` 模块的代码：

#### ✅ 完全平台无关的代码
1. **数据结构** (`helpers/Records.java`)
   - `StructureLocationData`
   - `StructureConnection`
   - `RoadData`
   - `ConnectionStatus`

2. **算法逻辑** (`features/roadlogic/`)
   - `RoadPathCalculator.java` - A* 路径算法
   - `Road.java` - 道路生成逻辑
   - `RoadDirection.java` - 方向枚举

3. **装饰系统** (`features/decoration/`)
   - `Decoration.java` - 装饰基类
   - `OrientedDecoration.java` - 方向感知装饰
   - `BiomeWoodAware.java` - 接口
   - `WoodSelector.java` - 木材选择器
   - 所有具体装饰类（LamppostDecoration, RoadFenceDecoration 等）

4. **配置数据** (不含平台特定实现)
   - 配置常量定义

#### ⚠️ 需要抽象的平台特定代码

以下代码需要创建抽象接口，然后在 Fabric/NeoForge 中实现：

1. **配置系统** (`config/ModConfig.java`)
   ```java
   // common 模块 - 定义接口
   public interface ModConfig {
       String structureToLocate();
       int structureSearchRadius();
       // ... 其他配置方法
   }
   
   // fabric/neoforge - 平台实现
   public class FabricModConfig implements ModConfig { ... }
   public class NeoForgeModConfig implements ModConfig { ... }
   ```

2. **数据持久化** (`persistence/WorldDataHelper.java`)
   ```java
   // common 模块 - 定义接口
   public interface WorldDataProvider {
       StructureLocationData getStructureLocations(ServerLevel level);
       void setStructureLocations(ServerLevel level, StructureLocationData data);
       // ...
   }
   
   // fabric - Attachment API 实现
   // neoforge - SavedData 实现
   ```

3. **事件处理** (`events/ModEventHandler.java`)
   - 使用 Architectury Events API 替代平台特定事件
   ```java
   // common 模块
   import dev.architectury.event.events.common.LifecycleEvent;
   import dev.architectury.event.events.common.TickEvent;
   
   LifecycleEvent.SERVER_LEVEL_LOAD.register(this::onWorldLoad);
   TickEvent.SERVER_PRE.register(this::onServerTick);
   ```

4. **注册系统** (`features/config/RoadFeatureRegistry.java`)
   - 使用 Architectury Registry API
   ```java
   // common 模块
   import dev.architectury.registry.registries.DeferredRegister;
   import dev.architectury.registry.registries.RegistrySupplier;
   
   public class RoadFeatureRegistry {
       private static final DeferredRegister<Feature<?>> FEATURES = 
           DeferredRegister.create("roadweaver", Registries.FEATURE);
       
       public static final RegistrySupplier<Feature<RoadFeatureConfig>> ROAD_FEATURE = 
           FEATURES.register("road", () -> new RoadFeature(RoadFeatureConfig.CODEC));
       
       public static void register() {
           FEATURES.register();
       }
   }
   ```

---

## 🔧 具体迁移示例

### 示例 1: 迁移纯逻辑类

**`RoadPathCalculator.java`** - 直接移动到 common

```bash
# 从 fabric 或 neoforge 移动到 common
mv fabric/src/main/java/net/countered/settlementroads/features/roadlogic/RoadPathCalculator.java \
   common/src/main/java/net/countered/settlementroads/features/roadlogic/RoadPathCalculator.java
```

### 示例 2: 配置系统抽象

**步骤：**

1. 在 `common` 创建接口：
```java
// common/src/main/java/.../config/ModConfig.java
package net.countered.settlementroads.config;

public interface ModConfig {
    // 结构配置
    String structureToLocate();
    int structureSearchRadius();
    
    // 预生成配置
    int initialLocatingCount();
    int maxConcurrentRoadGeneration();
    
    // 道路配置
    int averagingRadius();
    boolean allowArtificial();
    boolean allowNatural();
    // ... 其他配置
    
    // 获取当前平台实例
    static ModConfig getInstance() {
        return ModConfigImpl.INSTANCE;
    }
}
```

2. 在 `common` 创建服务加载器：
```java
// common/src/main/java/.../config/ModConfigImpl.java
package net.countered.settlementroads.config;

import dev.architectury.platform.Platform;

public class ModConfigImpl {
    static ModConfig INSTANCE;
    
    static {
        // Architectury 会在运行时注入正确的实现
        INSTANCE = Platform.getConfigFolder().resolve("roadweaver-config.toml");
    }
}
```

3. 在 Fabric 实现：
```java
// fabric/src/main/java/.../config/FabricModConfig.java
package net.countered.settlementroads.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class FabricModConfig extends MidnightConfig implements ModConfig {
    @Entry public static String structureToLocate = "#minecraft:village";
    @Entry public static int structureSearchRadius = 100;
    // ... 其他字段
    
    @Override
    public String structureToLocate() {
        return structureToLocate;
    }
    
    // ... 实现其他方法
}
```

4. 在 NeoForge 实现：
```java
// neoforge/src/main/java/.../config/NeoForgeModConfig.java
package net.countered.settlementroads.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class NeoForgeModConfig implements ModConfig {
    private final ServerConfig config;
    
    // 保持现有的 NeoForge 配置实现
    
    @Override
    public String structureToLocate() {
        return config.structureToLocate.get();
    }
    
    // ... 实现其他方法
}
```

### 示例 3: 事件系统迁移

**原 NeoForge 代码：**
```java
@SubscribeEvent
public static void onWorldLoad(LevelEvent.Load event) {
    if (!(event.getLevel() instanceof ServerLevel serverWorld)) return;
    // ...
}
```

**迁移到 Architectury (common 模块)：**
```java
import dev.architectury.event.events.common.LifecycleEvent;

public class ModEventHandler {
    public static void register() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register(ModEventHandler::onWorldLoad);
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(ModEventHandler::onWorldUnload);
        TickEvent.SERVER_PRE.register(ModEventHandler::onServerTick);
    }
    
    private static void onWorldLoad(ServerLevel level) {
        // 原有逻辑
    }
}
```

---

## 📦 推荐的迁移顺序

### 第 1 步：迁移纯数据类 (最简单)
1. `Records.java`
2. `RoadDirection.java`

### 第 2 步：迁移算法逻辑
1. `RoadPathCalculator.java`
2. `Road.java`

### 第 3 步：迁移装饰系统
1. `Decoration.java`
2. `OrientedDecoration.java`
3. `BiomeWoodAware.java`
4. `WoodSelector.java`
5. 所有装饰实现类

### 第 4 步：抽象平台特定代码
1. 配置系统接口化
2. 数据持久化接口化
3. 事件系统使用 Architectury Events
4. 注册系统使用 Architectury Registry

### 第 5 步：迁移辅助类
1. `StructureLocator.java` (可能需要调整)
2. `StructureConnector.java`

### 第 6 步：测试和验证
1. 编译 common 模块
2. 编译 fabric 模块
3. 编译 neoforge 模块
4. 运行客户端测试

---

## 🚀 Architectury API 常用替代

### 平台检测
```java
import dev.architectury.platform.Platform;

if (Platform.isFabric()) {
    // Fabric 特定代码
} else if (Platform.isNeoForge()) {
    // NeoForge 特定代码
}
```

### 注册系统
```java
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;

DeferredRegister<Block> BLOCKS = DeferredRegister.create("modid", Registries.BLOCK);
RegistrySupplier<Block> MY_BLOCK = BLOCKS.register("my_block", () -> new Block());
```

### 事件系统
```java
import dev.architectury.event.events.common.*;

// 生命周期事件
LifecycleEvent.SERVER_STARTING.register(server -> {});
LifecycleEvent.SERVER_STOPPING.register(server -> {});

// Tick 事件
TickEvent.SERVER_PRE.register(server -> {});
TickEvent.PLAYER_PRE.register(player -> {});

// 方块/物品交互
InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
    return EventResult.pass();
});
```

### 配置文件夹
```java
import dev.architectury.platform.Platform;

Path configPath = Platform.getConfigFolder().resolve("roadweaver.toml");
```

---

## ⚠️ 注意事项

1. **不要在 common 模块使用平台特定 API**
   - ❌ `net.fabricmc.*`
   - ❌ `net.neoforged.*`
   - ✅ `dev.architectury.*`
   - ✅ `net.minecraft.*` (原版 API)

2. **ExpectPlatform 注解**
   - 对于必须使用平台特定实现的方法，使用 `@ExpectPlatform`
   ```java
   // common 模块
   public class PlatformHelper {
       @ExpectPlatform
       public static String getPlatformName() {
           throw new AssertionError();
       }
   }
   
   // fabric 模块
   public class PlatformHelperImpl {
       public static String getPlatformName() {
           return "Fabric";
       }
   }
   ```

3. **资源文件迁移**
   - 语言文件：每个平台保留独立副本
   - NBT 结构文件：移到 common
   - 纹理/模型：移到 common

4. **Mixin**
   - Mixin 类通常保留在各平台模块
   - 如果两个平台的 Mixin 完全相同，可以移到 common

---

## 🧪 测试命令

```bash
# 清理构建
.\gradlew.bat clean

# 构建所有模块
.\gradlew.bat build

# 运行 Fabric 客户端
.\gradlew.bat :fabric:runClient

# 运行 NeoForge 客户端
.\gradlew.bat :neoforge:runClient

# 生成源码 JAR
.\gradlew.bat :common:sourcesJar
```

---

## 📝 下一步操作

1. **创建分支**: `git checkout -b feature/architectury-migration`
2. **按顺序迁移代码**: 从简单到复杂
3. **逐步测试**: 每完成一个模块就测试编译
4. **更新文档**: 记录遇到的问题和解决方案

---

## 🆘 常见问题

### Q: 编译错误 "找不到符号"
A: 检查 common 模块是否正确添加到依赖中，运行 `.\gradlew.bat :common:build`

### Q: 资源文件加载失败
A: 确保 access widener 正确配置，检查 `roadweaver.accesswidener` 文件

### Q: 运行时找不到类
A: 检查 shadowJar 配置，确保 common 模块被正确打包

### Q: 平台特定功能如何实现
A: 使用 `@ExpectPlatform` 注解或 Architectury Platform API

---

**祝迁移顺利！** 🎉
