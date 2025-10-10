# 🔄 RoadWeaver 1.21.1 → 1.20.1 迁移指南

## ✅ 已完成的自动修改

### 1. 根项目配置
- ✅ `build.gradle`: Minecraft 版本改为 1.20.1，Java 改为 17，Loom 改为 1.4-SNAPSHOT
- ✅ `gradle.properties`: 所有版本号已更新
- ✅ `settings.gradle`: NeoForge 仓库改为 Forge 仓库，模块改为 forge

### 2. Common 模块
- ✅ `common/build.gradle`: Architectury API 改为 9.2.14，平台改为 fabric 和 forge

### 3. Fabric 模块
- ✅ `fabric/build.gradle`: 所有依赖版本已更新
  - Architectury API: 9.2.14
  - Cloth Config: 11.1.106
  - ModMenu: 7.2.2
  - 游戏版本: 1.20.1
- ✅ `fabric/src/main/resources/fabric.mod.json`: 依赖版本已更新

---

## 🔧 需要手动完成的步骤

### 步骤 1: 重命名 neoforge 文件夹
```bash
# 在项目根目录执行
mv neoforge forge
# 或者在 Windows 文件管理器中手动重命名
```

### 步骤 2: 替换 forge/build.gradle
```bash
# 删除旧的 build.gradle
rm forge/build.gradle
# 重命名新文件
mv forge_build.gradle.new forge/build.gradle
```

### 步骤 3: 修改 Forge 模块的 Java 代码

#### 3.1 重命名包名
需要将所有 `neoforge` 包名改为 `forge`：
```
forge/src/main/java/net/countered/settlementroads/
├── config/neoforge/        → config/forge/
├── datagen/                (保持不变)
├── features/config/neoforge/ → features/config/forge/
├── helpers/neoforge/       → helpers/forge/
└── persistence/neoforge/   → persistence/forge/
```

#### 3.2 修改主类 `SettlementRoads.java`
**位置**: `forge/src/main/java/net/countered/settlementroads/SettlementRoads.java`

**需要修改的内容**:
```java
// 旧代码 (NeoForge)
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(SettlementRoads.MOD_ID)
public class SettlementRoads {
    public SettlementRoads(IEventBus modEventBus, ModContainer modContainer) {
        // ...
        modContainer.registerExtensionPoint(
            net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
            (client, parent) -> ClothConfigScreen.createConfigScreen(parent)
        );
    }
}

// 新代码 (Forge)
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.ConfigScreenHandler;

@Mod(SettlementRoads.MOD_ID)
public class SettlementRoads {
    public SettlementRoads() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // 加载配置
        ForgeJsonConfig.load();
        
        // 注册配置屏幕
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (client, parent) -> ClothConfigScreen.createConfigScreen(parent)
            )
        );
        
        // 注册事件
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(SettlementRoadsDataGenerator::gatherData);
        
        // 注册特性和事件处理器
        RoadFeatureRegistry.registerFeatures();
        ModEventHandler.register();
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("RoadWeaver common setup completed");
    }
}
```

#### 3.3 修改配置类
**位置**: `forge/src/main/java/net/countered/settlementroads/config/forge/`

将所有 `neoforge` 包名改为 `forge`，并更新导入：
```java
// 旧导入
import net.neoforged...

// 新导入
import net.minecraftforge...
```

#### 3.4 修改数据生成类
**位置**: `forge/src/main/java/net/countered/settlementroads/datagen/SettlementRoadsDataGenerator.java`

```java
// 旧代码 (NeoForge)
import net.neoforged.neoforge.data.event.GatherDataEvent;

public static void gatherData(GatherDataEvent event) {
    // ...
}

// 新代码 (Forge)
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

public static void gatherData(GatherDataEvent event) {
    // ...
}
```

#### 3.5 修改持久化类
**位置**: `forge/src/main/java/net/countered/settlementroads/persistence/forge/`

将所有 `neoforge` 相关的 API 改为 `forge` API。

### 步骤 4: 修改资源文件

#### 4.1 重命名 mods.toml
```bash
# 删除 neoforge.mods.toml
rm forge/src/main/resources/META-INF/neoforge.mods.toml

# 创建新的 mods.toml
```

**新建**: `forge/src/main/resources/META-INF/mods.toml`
```toml
modLoader="javafml"
loaderVersion="[47,)"
license="MIT"

[[mods]]
modId="roadweaver"
version="${mod_version}"
displayName="RoadWeaver"
authors="Countered"
description='''
Automatically generates roads between structures
'''

[[dependencies.roadweaver]]
    modId="forge"
    mandatory=true
    versionRange="[47,)"
    ordering="NONE"
    side="BOTH"

[[dependencies.roadweaver]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.20.1,1.21)"
    ordering="NONE"
    side="BOTH"

[[dependencies.roadweaver]]
    modId="architectury"
    mandatory=true
    versionRange="[9.2.14,)"
    ordering="AFTER"
    side="BOTH"
```

### 步骤 5: 更新 API 调用以兼容 1.20.1

#### 5.1 ResourceLocation API 变更
**1.21.1 → 1.20.1 的主要变更**:
```java
// 1.21.1 (新 API)
ResourceLocation.fromNamespaceAndPath("roadweaver", "structure")

// 1.20.1 (旧 API)
new ResourceLocation("roadweaver", "structure")
```

需要全局搜索并替换：
```bash
# 搜索
ResourceLocation.fromNamespaceAndPath

# 替换为
new ResourceLocation
```

#### 5.2 Component API 变更
某些 Component 方法可能有变化，需要检查编译错误。

#### 5.3 Registry API 变更
1.20.1 的注册系统与 1.21.1 略有不同，需要检查 `RoadFeatureRegistry.java`。

### 步骤 6: 清理和测试

```bash
# 清理旧的构建文件
./gradlew clean

# 重新构建
./gradlew build

# 测试 Fabric
./gradlew :fabric:runClient

# 测试 Forge
./gradlew :forge:runClient
```

---

## 📋 需要修改的文件清单

### Java 代码文件
- [ ] `forge/src/main/java/net/countered/settlementroads/SettlementRoads.java`
- [ ] `forge/src/main/java/net/countered/settlementroads/client/SettlementRoadsClient.java`
- [ ] `forge/src/main/java/net/countered/settlementroads/config/forge/*.java`
- [ ] `forge/src/main/java/net/countered/settlementroads/datagen/*.java`
- [ ] `forge/src/main/java/net/countered/settlementroads/features/config/forge/*.java`
- [ ] `forge/src/main/java/net/countered/settlementroads/helpers/forge/*.java`
- [ ] `forge/src/main/java/net/countered/settlementroads/persistence/forge/*.java`
- [ ] **所有 Common 模块中使用 `ResourceLocation.fromNamespaceAndPath` 的文件**

### 资源文件
- [ ] `forge/src/main/resources/META-INF/mods.toml` (新建)
- [ ] 删除 `forge/src/main/resources/META-INF/neoforge.mods.toml`

### 文档文件
- [ ] `README.md` - 更新版本号
- [ ] `CHANGELOG.md` - 添加迁移记录

---

## ⚠️ 重要注意事项

### 1. API 兼容性
- **1.20.1 使用 Java 17**，不是 Java 21
- **ResourceLocation API 完全不同**，这是最大的变更
- **Forge 1.20.1 与 NeoForge 1.21.1 的事件系统不同**

### 2. 依赖版本
确保使用正确的版本：
- Minecraft: 1.20.1
- Forge: 47.3.0
- Fabric Loader: 0.15.11
- Fabric API: 0.92.2+1.20.1
- Architectury: 9.2.14
- Cloth Config: 11.1.106
- ModMenu: 7.2.2

### 3. 测试重点
- ✅ 结构搜寻功能
- ✅ 道路生成功能
- ✅ 装饰系统
- ✅ 配置界面
- ✅ 数据持久化
- ✅ 调试地图

---

## 🔍 常见问题

### Q: 编译时出现 "cannot find symbol: ResourceLocation.fromNamespaceAndPath"
**A**: 这是 1.21.1 的新 API，1.20.1 不支持。需要改为 `new ResourceLocation(namespace, path)`。

### Q: Forge 模块无法启动
**A**: 检查 `mods.toml` 配置是否正确，确保 modLoader 为 "javafml"。

### Q: 配置界面无法打开
**A**: 检查 Cloth Config 版本是否正确（11.1.106），并确保正确注册了 ConfigScreenFactory。

---

## 📞 需要帮助？

如果遇到问题，请：
1. 检查编译错误日志
2. 确认所有文件都已按照本指南修改
3. 运行 `./gradlew clean build` 清理并重新构建

**祝迁移顺利！** 🚀
