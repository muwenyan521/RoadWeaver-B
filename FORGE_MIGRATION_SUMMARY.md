# Forge 1.20.1 迁移完成总结

## 迁移概述
成功将项目从 **1.21.1 NeoForge** 降级到 **1.20.1 Forge**

## 完成的修改

### 1. 构建配置文件
- ✅ **forge/build.gradle** - 创建了适配Forge 1.20.1的构建脚本
  - 使用 `architectury.forge()` 替代 `architectury.neoForge()`
  - Forge版本: 47.3.0 (1.20.1)
  - Architectury API: 9.2.14 (Forge版本)
  - Cloth Config: 11.1.136 (Forge版本)
  - 修改了Loom配置以适配Forge的数据生成

- ✅ **forge/gradle.properties** - 设置平台为forge
  ```properties
  loom.platform = forge
  ```

### 2. Java代码API迁移

#### 主类 (SettlementRoads.java)
- `net.neoforged.fml` → `net.minecraftforge.fml`
- `net.neoforged.bus.api.IEventBus` → `net.minecraftforge.eventbus.api.IEventBus`
- 构造函数从 `(IEventBus, ModContainer)` 改为无参构造
- 使用 `FMLJavaModLoadingContext.get().getModEventBus()` 获取事件总线
- 配置屏幕注册改用 `ModLoadingContext.get().registerExtensionPoint()`

#### 客户端类 (SettlementRoadsClient.java)
- `@EventBusSubscriber` 注解从 `net.neoforged.fml.common` 改为 `net.minecraftforge.fml.common.Mod`
- 事件总线类型从 `EventBusSubscriber.Bus.GAME` 改为 `Mod.EventBusSubscriber.Bus.FORGE`
- `ClientTickEvent.Post` 改为 `TickEvent.ClientTickEvent` 并检查 `event.phase`
- `net.neoforged.api.distmarker.Dist` → `net.minecraftforge.api.distmarker.Dist`

#### 数据生成类 (SettlementRoadsDataGenerator.java)
- `net.neoforged.neoforge.data.event.GatherDataEvent` → `net.minecraftforge.data.event.GatherDataEvent`
- `net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider` → `net.minecraftforge.common.data.DatapackBuiltinEntriesProvider`

### 3. 包结构重命名
所有 `neoforge` 包重命名为 `forge`：

- ✅ `config.neoforge` → `config.forge`
  - NeoForgeJsonConfig → ForgeJsonConfig
  - NeoForgeModConfigAdapter → ForgeModConfigAdapter
  - ConfigProviderImpl (更新引用)

- ✅ `features.config.neoforge` → `features.config.forge`
  - ModConfiguredFeatures (更新日志和API)
  - ModPlacedFeatures (更新日志和API)

- ✅ `persistence.neoforge` → `persistence.forge`
  - WorldDataHelper (SavedData.Factory API适配)
  - NeoForgeWorldDataProvider → ForgeWorldDataProvider
  - WorldDataProviderImpl (更新引用)

- ✅ `helpers.neoforge` → `helpers.forge`
  - StructureLocatorImpl (ResourceLocation API适配)

### 4. 资源文件修改

#### META-INF配置
- ✅ **neoforge.mods.toml** → **mods.toml**
  - `modLoader = "javafml"` 保持不变
  - `loaderVersion = "[4,)"` → `"[47,)"`
  - 依赖从 `neoforge` 改为 `forge`
  - 版本范围: `[47,48)` (Forge 1.20.1)
  - Minecraft版本: `[1.20.1,1.21)`
  - Cloth Config版本: `[11.0,)` (Forge版本)

#### Biome Modifier
- ✅ **data/roadweaver/neoforge/biome_modifier/** → **data/roadweaver/forge/biome_modifier/**
  - `type: "neoforge:add_features"` → `"forge:add_features"`

### 5. API差异处理

#### ResourceLocation创建
- **1.21.1 NeoForge**: `ResourceLocation.fromNamespaceAndPath(namespace, path)`
- **1.20.1 Forge**: `new ResourceLocation(namespace, path)`

#### SavedData.Factory
- **1.21.1 NeoForge**: `new SavedData.Factory<>(supplier, loader)`
- **1.20.1 Forge**: `new SavedData.Factory<>(supplier, loader, null)` (需要第三个参数)

#### 事件系统
- **1.21.1 NeoForge**: `ClientTickEvent.Post`
- **1.20.1 Forge**: `TickEvent.ClientTickEvent` + `event.phase` 检查

## 验证清单

### 构建验证
```bash
# 清理并构建Forge模组
./gradlew :forge:clean :forge:build

# 运行Forge客户端
./gradlew :forge:runClient

# 生成数据
./gradlew :forge:runData
```

### 功能验证
- [ ] 模组能正常加载
- [ ] 配置文件正确生成 (config/roadweaver.json)
- [ ] 配置界面可以打开 (Mod Menu)
- [ ] 结构搜索功能正常
- [ ] 道路生成功能正常
- [ ] 数据持久化正常
- [ ] 调试界面 (H键) 正常工作

## 注意事项

1. **Architectury版本差异**
   - Forge 1.20.1使用Architectury 9.x
   - NeoForge 1.21.1使用Architectury 13.x
   - API可能有细微差异，需要测试

2. **Mixin配置**
   - 确保 `roadweaver.mixins.json` 在Forge环境下正常工作
   - 检查Mixin目标类在1.20.1中是否存在

3. **依赖版本**
   - Cloth Config: 11.1.136 (Forge 1.20.1)
   - 如需其他依赖，确保使用Forge 1.20.1兼容版本

4. **Common模块兼容性**
   - Common模块代码应该保持平台无关
   - 使用Architectury API的@ExpectPlatform机制

## 下一步

1. 运行 `./gradlew :forge:build` 验证编译
2. 测试所有功能是否正常
3. 如有编译错误，根据错误信息调整API使用
4. 更新README.md说明支持的版本

## 文件清单

### 新增文件
- `forge/build.gradle`
- `forge/gradle.properties`
- `forge/src/main/resources/META-INF/mods.toml`
- `forge/src/main/resources/data/roadweaver/forge/biome_modifier/road_feature.json`
- `forge/src/main/java/net/countered/settlementroads/config/forge/*`
- `forge/src/main/java/net/countered/settlementroads/features/config/forge/*`
- `forge/src/main/java/net/countered/settlementroads/persistence/forge/*`
- `forge/src/main/java/net/countered/settlementroads/helpers/forge/*`

### 修改文件
- `forge/src/main/java/net/countered/settlementroads/SettlementRoads.java`
- `forge/src/main/java/net/countered/settlementroads/client/SettlementRoadsClient.java`
- `forge/src/main/java/net/countered/settlementroads/datagen/SettlementRoadsDataGenerator.java`
- `forge/src/main/java/net/countered/settlementroads/client/gui/ClothConfigScreen.java`

### 删除文件
- `forge/src/main/resources/META-INF/neoforge.mods.toml`
- `forge/src/main/resources/data/roadweaver/neoforge/*`
- `forge/src/main/java/net/countered/settlementroads/config/neoforge/*`
- `forge/src/main/java/net/countered/settlementroads/features/config/neoforge/*`
- `forge/src/main/java/net/countered/settlementroads/persistence/neoforge/*`
- `forge/src/main/java/net/countered/settlementroads/helpers/neoforge/*`

---
**迁移完成时间**: 2025-10-11
**目标版本**: Minecraft 1.20.1 + Forge 47.3.0
