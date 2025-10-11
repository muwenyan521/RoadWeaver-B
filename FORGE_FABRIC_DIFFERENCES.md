# Forge 版与 Fabric 版功能差异分析

## 🔍 问题总结

Forge 版本存在以下功能缺失：
1. ❌ **汉化失效** - 中文翻译不显示
2. ❌ **无法生成道路** - 道路特性未正确注册到世界生成
3. ❌ **部分功能缺失** - 需要进一步验证

## 📊 详细差异对比

### 1. 特性注册方式差异

#### Fabric 版本 ✅
```java
// Fabric 使用 BiomeModifications API 动态注入
public static void inject() {
    BiomeModifications.addFeature(
        BiomeSelectors.all(),
        GenerationStep.Decoration.LOCAL_MODIFICATIONS,
        RoadFeature.ROAD_FEATURE_PLACED_KEY
    );
}
```

**调用位置**: `SettlementRoads.onInitialize()` 中调用 `FabricBiomeInjection.inject()`

#### Forge 版本 ⚠️
```json
// Forge 使用数据驱动的 biome_modifier
{
  "type": "forge:add_features",
  "biomes": "#minecraft:is_overworld",
  "features": "roadweaver:road_feature_placed",
  "step": "local_modifications"
}
```

**问题**: 
- ✅ biome_modifier 文件存在于 `forge/src/main/resources/data/roadweaver/forge/biome_modifier/road_feature.json`
- ❓ 但可能未被正确加载或引用的特性不存在

### 2. 特性定义差异

#### Common 模块（共享）
- `common/src/main/resources/data/roadweaver/worldgen/configured_feature/road_feature.json`
- `common/src/main/resources/data/roadweaver/worldgen/placed_feature/road_feature_placed.json`

#### Forge 特定
- `forge/src/main/resources/data/roadweaver/worldgen/` - **重复定义！**
  - `configured_feature/road_feature.json`
  - `placed_feature/road_feature_placed.json`

**问题**: Forge 版本有重复的特性定义文件，可能导致冲突

### 3. 特性注册代码差异

#### Fabric 版本
```java
// 使用 Architectury DeferredRegister（Common 模块）
private static final DeferredRegister<Feature<?>> FEATURES =
    DeferredRegister.create(MOD_ID, Registries.FEATURE);

public static final RegistrySupplier<Feature<RoadFeatureConfig>> ROAD_FEATURE =
    FEATURES.register("road_feature", () -> RoadFeature.ROAD_FEATURE);
```

#### Forge 版本
```java
// 使用 Forge 原生 DeferredRegister
private static final DeferredRegister<Feature<?>> FEATURES =
    DeferredRegister.create(Registries.FEATURE, MOD_ID);

public static final RegistryObject<Feature<RoadFeatureConfig>> ROAD_FEATURE =
    FEATURES.register("road_feature", () -> RoadFeature.ROAD_FEATURE);
```

**状态**: ✅ 已修复，使用 Forge 原生注册器

### 4. 资源文件结构

#### 语言文件
- ✅ Fabric: `fabric/src/main/resources/assets/roadweaver/lang/zh_cn.json`
- ✅ Forge: `forge/src/main/resources/assets/roadweaver/lang/zh_cn.json`

**状态**: 两个平台都有语言文件，但 Forge 版可能未正确加载

#### 数据文件
```
Common:
  data/roadweaver/
    ├── structures/
    ├── worldgen/
    │   ├── configured_feature/
    │   └── placed_feature/

Fabric:
  data/roadweaver/
    └── structures/

Forge:
  data/roadweaver/
    ├── forge/
    │   └── biome_modifier/
    ├── structures/
    └── worldgen/  ⚠️ 重复！
        ├── configured_feature/
        └── placed_feature/
```

### 5. 数据生成器差异

#### Fabric 版本
```java
public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
    FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
    pack.addProvider(ModWorldGenerator::new);
}
```

#### Forge 版本
```java
public static void gatherData(GatherDataEvent event) {
    generator.addProvider(true, new DatapackBuiltinEntriesProvider(
        output,
        lookupProvider,
        new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
            .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap),
        Set.of(SettlementRoads.MOD_ID)
    ));
}
```

**问题**: Forge 版使用代码生成特性定义，而 Common 模块有 JSON 文件，可能导致冲突

## 🔧 需要修复的问题

### 问题 1: 重复的特性定义文件
**位置**: `forge/src/main/resources/data/roadweaver/worldgen/`

**原因**: 
- Common 模块已经有 JSON 定义
- Forge 模块又有重复的 JSON 文件
- Forge 数据生成器还通过代码生成定义

**解决方案**: 删除 Forge 模块中重复的 worldgen 文件，使用 Common 模块的定义

### 问题 2: 特性引用不一致
**原因**: 
- Forge 的 `biome_modifier` 引用 `roadweaver:road_feature_placed`
- 但这个特性可能未正确注册或被重复定义覆盖

**解决方案**: 确保 biome_modifier 引用的特性与实际注册的特性一致

### 问题 3: 数据生成器冲突
**原因**: 
- Forge 数据生成器通过代码生成特性
- 但 build.gradle 排除了这些文件的使用

**解决方案**: 
- 选项 A: 完全使用 Common 的 JSON 文件，删除 Forge 的代码生成
- 选项 B: 完全使用 Forge 的代码生成，删除 Common 的 JSON 文件

### 问题 4: 汉化文件未加载
**可能原因**:
1. 资源包未正确注册
2. Mod ID 不匹配
3. 文件路径错误
4. 资源重载问题

**需要检查**:
- `mods.toml` 中的 mod ID 是否为 `roadweaver`
- 资源文件是否正确打包到 JAR 中
- 是否有资源包冲突

## 📝 推荐修复方案

### 方案 A: 使用 Common 的 JSON 定义（推荐）

1. **删除 Forge 重复文件**:
   ```
   删除: forge/src/main/resources/data/roadweaver/worldgen/
   ```

2. **删除 Forge 数据生成器**:
   ```java
   // 删除或注释掉 SettlementRoadsDataGenerator 中的特性生成代码
   ```

3. **确保 biome_modifier 正确引用**:
   ```json
   {
     "type": "forge:add_features",
     "biomes": "#minecraft:is_overworld",
     "features": "roadweaver:road_feature_placed",
     "step": "local_modifications"
   }
   ```

4. **验证特性注册**:
   - 确保 `ForgeRoadFeatureRegistry` 正确注册了特性
   - 特性 ID 必须与 JSON 文件中的一致

### 方案 B: 完全使用 Forge 代码生成

1. **删除 Common 的 JSON 文件**
2. **保留 Forge 的数据生成器**
3. **更新 build.gradle** 不排除生成的文件

## 🔍 需要进一步检查的项目

1. ✅ 特性是否正确注册到注册表
2. ❓ biome_modifier 是否被游戏加载
3. ❓ 资源文件是否正确打包
4. ❓ 配置文件是否正确加载
5. ❓ 事件处理器是否正常工作

## 🎯 下一步行动

1. **立即修复**: 删除重复的特性定义文件
2. **验证**: 运行游戏并检查日志
3. **测试**: 创建新世界，验证道路生成
4. **调试**: 如果仍有问题，添加详细日志

---

**创建时间**: 2025-10-11
**分析者**: AI Assistant
**状态**: 待修复
