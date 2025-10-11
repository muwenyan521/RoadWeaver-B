# Forge 版本修复记录

## 🔧 已应用的修复

### 1. ✅ 添加 pack.mcmeta 文件（修复汉化失效）

**问题**: 游戏警告 "Mod 文件缺少 ResourcePackInfo"，导致资源（包括语言文件）无法加载

**修复**: 为所有模块添加 `pack.mcmeta` 文件

#### 创建的文件：
- `forge/src/main/resources/pack.mcmeta`
- `fabric/src/main/resources/pack.mcmeta`
- `common/src/main/resources/pack.mcmeta`

#### 文件内容（Forge）：
```json
{
  "pack": {
    "description": "RoadWeaver Resources",
    "pack_format": 15,
    "forge:resource_pack_format": 15,
    "forge:data_pack_format": 12
  }
}
```

**说明**:
- `pack_format: 15` - Minecraft 1.20.1 的资源包格式
- `forge:resource_pack_format: 15` - Forge 资源包格式
- `forge:data_pack_format: 12` - Forge 数据包格式（1.20.1）

### 2. ✅ 简化 Forge 数据生成器（避免重复定义）

**问题**: Forge 数据生成器通过代码生成 configured/placed features，与 Common 模块的 JSON 定义冲突

**修复**: 移除代码生成逻辑，完全依赖 Common 模块的 JSON 定义

#### 修改的文件：
- `forge/src/main/java/net/countered/settlementroads/datagen/SettlementRoadsDataGenerator.java`

**变更**:
- ❌ 删除了 `ModConfiguredFeatures::bootstrap`
- ❌ 删除了 `ModPlacedFeatures::bootstrap`
- ✅ 保留事件监听器（用于未来扩展）
- ✅ 添加了详细注释说明架构

### 3. ⚠️ 待手动执行：删除重复的 worldgen 文件

**问题**: Forge 模块有重复的 worldgen JSON 文件，与 Common 模块冲突

**需要删除的目录**:
```
forge/src/main/resources/data/roadweaver/worldgen/
```

**执行方式**:
```powershell
# 方式 1: 运行修复脚本
.\fix_forge_duplicates.ps1

# 方式 2: 手动删除
Remove-Item -Recurse -Force "forge\src\main\resources\data\roadweaver\worldgen"
```

## 📊 修复后的架构

### 资源文件结构
```
Common 模块（主要定义）:
  resources/
    ├── pack.mcmeta  ✅ 新增
    ├── assets/roadweaver/
    │   └── lang/
    │       ├── en_us.json
    │       └── zh_cn.json
    └── data/roadweaver/
        ├── structures/
        └── worldgen/
            ├── configured_feature/
            │   └── road_feature.json  ← 主定义
            └── placed_feature/
                └── road_feature_placed.json  ← 主定义

Fabric 模块:
  resources/
    ├── pack.mcmeta  ✅ 新增
    ├── fabric.mod.json
    ├── assets/roadweaver/
    │   └── lang/  ← 平台特定翻译（如果有）
    └── data/roadweaver/
        └── structures/

Forge 模块:
  resources/
    ├── pack.mcmeta  ✅ 新增
    ├── META-INF/mods.toml
    ├── assets/roadweaver/
    │   └── lang/  ← 平台特定翻译（如果有）
    └── data/roadweaver/
        ├── forge/
        │   └── biome_modifier/
        │       └── road_feature.json  ← Forge 特定
        ├── structures/
        └── worldgen/  ⚠️ 需要删除（重复）
```

### 特性注册流程

#### Fabric 平台
```
1. RoadFeatureRegistry.registerFeatures()
   ↓ 注册 Feature 到注册表
2. FabricBiomeInjection.inject()
   ↓ 使用 BiomeModifications API
3. 特性被添加到所有生物群系
```

#### Forge 平台
```
1. ForgeRoadFeatureRegistry.register(modEventBus)
   ↓ 使用 Forge DeferredRegister
2. biome_modifier JSON 被加载
   ↓ forge:add_features
3. 特性被添加到 #minecraft:is_overworld 生物群系
```

## 🎯 验证清单

### 修复后需要验证的功能

- [ ] **汉化显示**
  - [ ] 主菜单中文显示正常
  - [ ] 配置界面中文显示正常
  - [ ] 游戏内提示中文显示正常

- [ ] **道路生成**
  - [ ] 创建新世界
  - [ ] 找到村庄
  - [ ] 检查村庄之间是否生成道路
  - [ ] 检查道路装饰（路灯、栏杆等）

- [ ] **配置功能**
  - [ ] 配置文件正确生成（config/roadweaver.json）
  - [ ] 配置界面可以打开
  - [ ] 配置修改可以保存

- [ ] **调试功能**
  - [ ] R 键打开调试地图
  - [ ] 地图显示结构位置
  - [ ] 地图显示道路连接

## 🚀 下一步操作

### 立即执行
1. **删除重复文件**:
   ```powershell
   .\fix_forge_duplicates.ps1
   ```

2. **清理并重新构建**:
   ```powershell
   $env:JAVA_HOME="C:\Program Files\Zulu\zulu-17"
   ./gradlew :forge:clean :forge:build
   ```

3. **运行游戏测试**:
   ```powershell
   ./gradlew :forge:runClient --no-daemon
   ```

### 测试步骤
1. 启动游戏，检查主菜单语言
2. 进入 Mods 列表，找到 RoadWeaver
3. 打开配置界面，检查中文显示
4. 创建新世界（种子：任意）
5. 使用 `/locate structure minecraft:village_plains` 找村庄
6. 传送到村庄附近
7. 等待区块加载，观察道路生成
8. 按 R 键打开调试地图

## 📝 已知问题

### 仍需观察的问题
1. **道路生成频率**: 可能需要调整配置
2. **性能影响**: 多线程生成是否正常工作
3. **装饰放置**: 大型装饰（秋千、长椅等）默认关闭

### 如果问题仍然存在

#### 汉化仍然失效
1. 检查 JAR 文件是否包含 `pack.mcmeta`
2. 检查语言文件路径是否正确
3. 查看游戏日志中的资源加载信息

#### 道路仍然不生成
1. 检查 `biome_modifier` 是否被加载
2. 查看日志中的特性注册信息
3. 使用 `/locate` 命令确认结构存在
4. 检查配置文件中的设置

## 🔍 调试命令

### 检查特性注册
```
/reload  # 重新加载数据包
```

### 检查生物群系修饰符
查看日志中是否有：
```
[Forge] Loading biome modifier: roadweaver:road_feature
```

### 检查特性加载
查看日志中是否有：
```
[RoadWeaver] Configured features bootstrapped successfully
[RoadWeaver] Placed features bootstrapped successfully
```

## 📚 参考资料

- [Minecraft 1.20.1 Pack Format](https://minecraft.fandom.com/wiki/Pack_format)
- [Forge Biome Modifiers](https://docs.minecraftforge.net/en/1.20.x/worldgen/biomemodifiers/)
- [Architectury Documentation](https://docs.architectury.dev/)

---

**修复时间**: 2025-10-11 15:52
**修复者**: AI Assistant
**状态**: 部分完成，需要手动删除重复文件并测试
