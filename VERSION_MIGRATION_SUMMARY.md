# 🔄 版本迁移完成总结

## 迁移信息
- **原版本**: Minecraft 1.21.1 + NeoForge 21.1.169
- **新版本**: Minecraft 1.20.1 + Forge 47.3.0
- **迁移日期**: 2025-10-11

---

## ✅ 已完成的自动修改

### 1. 根项目配置
- ✅ `build.gradle`
  - Minecraft: 1.21.1 → 1.20.1
  - Java: 21 → 17
  - Architectury Loom: 1.11-SNAPSHOT → 1.4-SNAPSHOT
  
- ✅ `gradle.properties`
  - minecraft_version: 1.20.1
  - fabric_version: 0.92.2+1.20.1
  - forge_version: 47.3.0
  - loader_version: 0.15.11
  - yarn_mappings: 1.20.1+build.10
  - midnightlib_version: 1.4.1

- ✅ `settings.gradle`
  - NeoForge 仓库 → Forge 仓库
  - include 'neoforge' → include 'forge'

### 2. Common 模块
- ✅ `common/build.gradle`
  - Architectury API: 13.0.8 → 9.2.14
  - common("fabric", "neoforge") → common("fabric", "forge")

- ✅ API 兼容性修改
  - `RoadFeature.java`: ResourceLocation.fromNamespaceAndPath → new ResourceLocation
  - `StructureDecoration.java`: ResourceLocation.fromNamespaceAndPath → new ResourceLocation

### 3. Fabric 模块
- ✅ `fabric/build.gradle`
  - Architectury API: 9.2.14
  - Cloth Config: 11.1.106
  - ModMenu: 7.2.2
  - 游戏版本: 1.20.1

- ✅ `fabric/src/main/resources/fabric.mod.json`
  - Minecraft: ~1.20.1
  - Java: >=17
  - Fabric Loader: >=0.15.0

- ✅ Fabric 平台代码
  - `WorldDataAttachment.java`: API 更新
  - `RoadFeatureRegistry.java`: API 更新
  - `RoadFeature.java`: API 更新

### 4. Forge 模块准备
- ✅ 创建 `forge_build.gradle.new` (新的 Forge 配置文件)
- ✅ 创建 `MIGRATION_TO_1.20.1_GUIDE.md` (详细迁移指南)

### 5. 文档更新
- ✅ `README.md`: 版本号更新
- ✅ 创建迁移指南文档

---

## 🔧 需要手动完成的步骤

### 步骤 1: 重命名 neoforge 文件夹 ⚠️
```bash
# 在项目根目录
mv neoforge forge
```
或在 Windows 文件管理器中手动重命名 `neoforge` 文件夹为 `forge`

### 步骤 2: 替换 build.gradle ⚠️
```bash
# 删除旧文件
rm forge/build.gradle
# 重命名新文件
mv forge_build.gradle.new forge/build.gradle
```

### 步骤 3: 修改 Forge 模块代码 ⚠️

需要修改以下包名和导入：

#### 3.1 包名重命名
```
forge/src/main/java/net/countered/settlementroads/
├── config/neoforge/        → config/forge/
├── features/config/neoforge/ → features/config/forge/
├── helpers/neoforge/       → helpers/forge/
└── persistence/neoforge/   → persistence/forge/
```

#### 3.2 主类修改
**文件**: `forge/src/main/java/net/countered/settlementroads/SettlementRoads.java`

需要将 NeoForge API 改为 Forge API：
```java
// 旧导入 (NeoForge)
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

// 新导入 (Forge)
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
```

#### 3.3 配置屏幕注册
```java
// 旧代码 (NeoForge)
modContainer.registerExtensionPoint(
    net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
    (client, parent) -> ClothConfigScreen.createConfigScreen(parent)
);

// 新代码 (Forge)
ModLoadingContext.get().registerExtensionPoint(
    ConfigScreenHandler.ConfigScreenFactory.class,
    () -> new ConfigScreenHandler.ConfigScreenFactory(
        (client, parent) -> ClothConfigScreen.createConfigScreen(parent)
    )
);
```

#### 3.4 数据生成类
**文件**: `forge/src/main/java/net/countered/settlementroads/datagen/SettlementRoadsDataGenerator.java`

```java
// 旧导入
import net.neoforged.neoforge.data.event.GatherDataEvent;

// 新导入
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
```

### 步骤 4: 创建 mods.toml ⚠️

**删除**: `forge/src/main/resources/META-INF/neoforge.mods.toml`

**创建**: `forge/src/main/resources/META-INF/mods.toml`
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
```

### 步骤 5: 清理和构建 ⚠️
```bash
# 清理旧构建
./gradlew clean

# 重新构建
./gradlew build

# 测试 Fabric
./gradlew :fabric:runClient

# 测试 Forge
./gradlew :forge:runClient
```

---

## 📋 版本对照表

| 组件 | 1.21.1 版本 | 1.20.1 版本 |
|------|------------|------------|
| Minecraft | 1.21.1 | 1.20.1 |
| Java | 21 | 17 |
| Fabric Loader | 0.16.10 | 0.15.11 |
| Fabric API | 0.115.1+1.21.1 | 0.92.2+1.20.1 |
| Forge/NeoForge | NeoForge 21.1.169 | Forge 47.3.0 |
| Architectury | 13.0.8 | 9.2.14 |
| Architectury Loom | 1.11-SNAPSHOT | 1.4-SNAPSHOT |
| Cloth Config | 15.0.140 | 11.1.106 |
| ModMenu | 11.0.2 | 7.2.2 |
| MidnightLib | 1.6.9+1.21-fabric | 1.4.1 |

---

## 🔍 主要 API 变更

### ResourceLocation 构造方法
```java
// 1.21.1
ResourceLocation.fromNamespaceAndPath("namespace", "path")

// 1.20.1
new ResourceLocation("namespace", "path")
```

### 事件系统
```java
// NeoForge 1.21.1
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

// Forge 1.20.1
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
```

---

## ⚠️ 重要注意事项

1. **必须手动重命名 neoforge 文件夹为 forge**
2. **必须修改所有 Forge 模块中的 NeoForge API 调用**
3. **必须创建新的 mods.toml 文件**
4. **Java 版本从 21 降级到 17**
5. **所有 ResourceLocation.fromNamespaceAndPath 已自动替换**

---

## 📂 已创建的文件

1. ✅ `forge_build.gradle.new` - 新的 Forge 构建配置
2. ✅ `MIGRATION_TO_1.20.1_GUIDE.md` - 详细迁移指南
3. ✅ `VERSION_MIGRATION_SUMMARY.md` - 本文件

---

## 🎯 下一步操作

1. **立即执行**: 重命名 `neoforge` 文件夹为 `forge`
2. **立即执行**: 替换 `forge/build.gradle`
3. **重要**: 按照 `MIGRATION_TO_1.20.1_GUIDE.md` 修改 Forge 模块代码
4. **测试**: 运行 `./gradlew clean build` 检查编译错误
5. **调试**: 分别测试 Fabric 和 Forge 版本

---

## 📞 遇到问题？

参考详细指南：`MIGRATION_TO_1.20.1_GUIDE.md`

**祝迁移顺利！** 🚀
