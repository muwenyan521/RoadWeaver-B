# RoadWeaver 项目概览

## 📋 项目基本信息

- **项目名称**: RoadWeaver (道路编织者)
- **Minecraft版本**: 1.20.1
- **模组架构**: Architectury 多加载器项目
- **支持平台**: Fabric + Forge
- **Java版本**: 17+
- **构建工具**: Gradle
- **当前版本**: 1.0.0

## 🏗️ 项目架构

### 模块结构
```
RoadWeaver/
├── common/          # 通用逻辑模块（平台无关）
├── fabric/          # Fabric 平台实现
├── forge/           # Forge 平台实现
├── gradle/          # Gradle 构建配置
└── 文档/            # 各类技术文档
```

### 核心依赖
- **Architectury API**: 9.2.14 (已内嵌到模组中)
- **Fabric API**: 0.92.2+1.20.1
- **Forge**: 47.3.0
- **Cloth Config**: 11.1.106 (Fabric) / 11.1.136 (Forge)
- **Yarn Mappings**: 1.20.1+build.10

## 🎯 核心功能

### 1. 智能道路生成系统
- **A* 寻路算法**: 自动规划最优路径
- **地形感知**: 考虑高度差、生物群系、地形稳定性
- **多线程生成**: 7个工作线程异步处理
- **高度缓存**: 优化性能，减少重复计算

### 2. 道路类型
- **人工道路**: 石砖、石板等材料
- **自然道路**: 泥土、砂砾等材料
- **自适应材料**: 根据生物群系自动选择

### 3. 装饰系统
- **路灯**: 红石灯 + 阳光检测器（夜晚自动点亮）
- **路边栏杆**: 间断式随机栏杆
- **距离标志**: 显示道路长度信息
- **大型装饰**: 秋千、长椅、凉亭（可配置）

### 4. 可视化调试工具
- **实时道路网络地图**: 查看所有道路和结构位置
- **状态颜色编码**: PLANNED、GENERATING、COMPLETED、FAILED
- **交互功能**: 拖拽、缩放、点击传送
- **详细统计**: 道路数量、长度、状态统计

## 📂 代码结构详解

### Common 模块 (平台无关核心逻辑)

#### 主要包结构
```
net.countered.settlementroads/
├── client/                    # 客户端相关
│   └── gui/                   # GUI界面
│       └── StructureColorManager.java
├── config/                    # 配置系统
│   ├── ConfigProvider.java   # 配置提供者（平台桥接）
│   └── IModConfig.java        # 配置接口
├── events/                    # 事件处理
│   └── ModEventHandler.java  # 通用事件处理器
├── features/                  # 核心功能
│   ├── RoadFeature.java       # 道路特性主类
│   ├── config/                # 特性配置
│   │   ├── RoadFeatureConfig.java
│   │   └── RoadFeatureRegistry.java
│   ├── decoration/            # 装饰系统
│   │   ├── BenchDecoration.java       # 长椅
│   │   ├── GlorietteDecoration.java   # 凉亭
│   │   ├── SwingDecoration.java       # 秋千
│   │   ├── LamppostDecoration.java    # 路灯
│   │   ├── RoadFenceDecoration.java   # 路边栏杆
│   │   ├── DistanceSignDecoration.java # 距离标志
│   │   └── util/              # 装饰工具类
│   │       ├── BiomeWoodAware.java
│   │       └── WoodSelector.java
│   └── roadlogic/             # 道路逻辑
│       ├── Road.java          # 道路生成主逻辑
│       ├── RoadDirection.java # 道路方向
│       └── RoadPathCalculator.java # A*路径计算
├── helpers/                   # 辅助工具
│   ├── Records.java           # 数据记录类
│   ├── StructureConnector.java # 结构连接器
│   ├── StructureLocator.java  # 结构定位器接口
│   └── StructureLocatorImpl.java # 平台实现
└── persistence/               # 数据持久化
    └── WorldDataProvider.java # 世界数据提供者（平台桥接）
```

### Fabric 模块 (Fabric平台实现)

#### 主要类
```
net.countered.settlementroads/
├── SettlementRoads.java       # 主入口类
├── SettlementRoadsDataGenerator.java # 数据生成
├── client/
│   ├── SettlementRoadsClient.java # 客户端入口
│   ├── ModMenuIntegration.java    # ModMenu集成
│   └── gui/                   # GUI实现
├── config/fabric/             # Fabric配置实现
│   ├── FabricModConfig.java
│   ├── FabricJsonConfig.java
│   └── ConfigProviderImpl.java
├── features/config/           # 特性配置
│   └── FabricBiomeInjection.java # 生物群系注入
├── persistence/               # 数据持久化
│   ├── attachments/
│   │   └── WorldDataAttachment.java # Fabric Attachment API
│   └── fabric/
│       └── WorldDataProviderImpl.java
└── helpers/fabric/
    └── StructureLocatorImpl.java # Fabric结构定位实现
```

### Forge 模块 (Forge平台实现)

#### 主要类
```
net.countered.settlementroads/
├── SettlementRoads.java       # 主入口类（Forge）
├── SettlementRoadsDataGenerator.java # 数据生成
├── client/
│   ├── SettlementRoadsClient.java # 客户端入口
│   └── gui/
│       └── ClothConfigScreen.java # 配置界面
├── config/forge/              # Forge配置实现
│   ├── ForgeJsonConfig.java
│   ├── ForgeModConfigAdapter.java
│   └── ConfigProviderImpl.java
├── features/config/forge/     # 特性配置
│   ├── ModConfiguredFeatures.java
│   └── ModPlacedFeatures.java
├── persistence/forge/         # 数据持久化
│   ├── WorldDataHelper.java
│   └── ForgeWorldDataProvider.java
└── helpers/forge/
    └── StructureLocatorImpl.java # Forge结构定位实现
```

## 🔧 核心技术实现

### 1. 平台桥接机制
使用 Architectury 的 `@ExpectPlatform` 注解实现平台特定功能：
- `ConfigProvider`: 配置系统桥接
- `WorldDataProvider`: 数据持久化桥接
- `StructureLocator`: 结构定位桥接

### 2. 数据持久化
- **Fabric**: 使用 Fabric Attachment API
- **Forge**: 使用 SavedData 系统
- **存储内容**:
  - 结构位置列表 (`StructureLocationData`)
  - 结构连接关系 (`StructureConnection`)
  - 道路数据列表 (`RoadData`)

### 3. 配置系统
- **统一JSON配置**: `config/roadweaver.json`
- **Cloth Config GUI**: 可视化配置界面
- **平台集成**:
  - Fabric: ModMenu 集成
  - Forge: 模组菜单集成

### 4. 事件系统
使用 Architectury Events API 统一事件处理：
- `ServerWorldEvents.LOAD`: 世界加载事件
- `ServerTickEvents.END_WORLD_TICK`: 世界Tick事件
- `ServerLifecycleEvents.SERVER_STOPPED`: 服务器停止事件

## 📊 道路生成流程

### 1. 结构搜寻阶段
```
RoadFeature.place()
  ↓
tryFindNewStructureConnection()
  ↓
StructureLocator.locateStructures()
  ↓
保存到 WorldDataProvider
```

### 2. 连接规划阶段
```
StructureConnector.connectStructures()
  ↓
创建 StructureConnection (PLANNED状态)
  ↓
保存到 WorldDataProvider
```

### 3. 道路生成阶段
```
ModEventHandler.onWorldTick()
  ↓
检查并发上限
  ↓
Road.generateRoad()
  ↓
RoadPathCalculator.calculateAStarRoadPath()
  ↓
更新状态为 GENERATING → COMPLETED/FAILED
```

### 4. 装饰放置阶段
```
RoadFeature.runRoadLogic()
  ↓
addDecoration() (收集装饰)
  ↓
RoadStructures.tryPlaceDecorations()
  ↓
实际放置装饰方块
```

## 🎨 装饰系统详解

### 装饰类型层级
```
Decoration (接口)
  ├── OrientedDecoration (抽象类 - 有方向的装饰)
  │   ├── LamppostDecoration (路灯)
  │   ├── FenceWaypointDecoration (栅栏路标)
  │   └── DistanceSignDecoration (距离标志)
  ├── StructureDecoration (抽象类 - 大型结构装饰)
  │   ├── NbtStructureDecoration (NBT结构装饰)
  │   │   ├── SwingDecoration (秋千)
  │   │   ├── BenchDecoration (长椅)
  │   │   └── GlorietteDecoration (凉亭)
  └── RoadFenceDecoration (路边栏杆 - 直接实现)
```

### 装饰放置规则
- **路灯**: 每16格放置一个，两侧交替
- **栏杆**: 1-3格随机长度，间断式放置
- **距离标志**: 道路起点和终点
- **大型装饰**: 距离道路4格，随机选择类型

## 📝 配置项说明

### 结构设置
- `structuresToLocate`: 要连接的结构类型列表
- `structureSearchRadius`: 结构搜寻半径（区块）
- `structureSearchTriggerDistance`: 触发搜寻的距离

### 道路设置
- `averagingRadius`: 地形平均半径
- `allowArtificial`: 允许人工道路
- `allowNatural`: 允许自然道路
- `maxHeightDifference`: 最大高度差
- `maxTerrainStability`: 地形稳定性检查

### 装饰设置
- `placeWaypoints`: 生成路标
- `placeRoadFences`: 生成路边栏杆
- `placeSwings`: 生成秋千
- `placeBenches`: 生成长椅
- `placeGloriettes`: 生成凉亭
- `structureDistanceFromRoad`: 大型装饰距离道路的距离

### 性能设置
- `initialLocatingCount`: 初始定位结构数量
- `maxConcurrentRoadGeneration`: 同时生成道路数量上限

## 🔍 调试工具

### 调试地图功能
- **快捷键**: R 键（默认）
- **功能**:
  - 实时显示所有结构位置
  - 显示道路连接状态
  - 颜色编码状态显示
  - 点击传送功能
  - 统计信息面板

### 日志系统
- **日志级别**: INFO, DEBUG, WARN, ERROR
- **关键日志点**:
  - 结构搜寻: `🔍 Triggering new structure search`
  - 道路生成: `🛣️ Starting road generation`
  - 状态更新: `Connection status updated`
  - 错误处理: `❌ Road generation failed`

## 🚀 构建与运行

### 构建命令
```bash
# 构建所有平台
./gradlew build

# 仅构建 Fabric
./gradlew :fabric:build

# 仅构建 Forge
./gradlew :forge:build
```

### 运行客户端
```bash
# Fabric 客户端
./gradlew :fabric:runClient

# Forge 客户端
./gradlew :forge:runClient
```

### 数据生成
```bash
# Fabric 数据生成
./gradlew :fabric:runDatagen

# Forge 数据生成
./gradlew :forge:runData
```

## 📚 重要文档

### 技术文档
- `TECHNICAL_OVERVIEW.md`: 完整技术详解
- `FORGE_MIGRATION_SUMMARY.md`: Forge迁移总结
- `MIGRATION_DIFFERENCES.md`: 迁移差异对比
- `CONCURRENCY_FIX.md`: 并发控制修复说明

### 功能文档
- `路边装饰系统完善说明.md`: 装饰系统详解
- `距离标志国际化说明.md`: 国际化实现
- `配置默认值变更说明.md`: 配置变更说明

### 开发文档
- `DECORATION_UPDATE_CHECKLIST.md`: 装饰更新检查清单
- `FINAL_FIX_SUMMARY.md`: 最终修复总结

## 🐛 已知问题与解决方案

### 已修复问题
1. ✅ 结构搜寻功能失效 → 修复平台实现和参数
2. ✅ 不可变集合异常 → 创建可变副本
3. ✅ 并发控制问题 → 完善任务恢复逻辑
4. ✅ 模组图标不显示 → 调整为正方形512x512

### 待完善功能
1. 大型装饰系统（秋千、长椅、凉亭）- 默认关闭
2. 更多道路材料类型
3. 自定义装饰配置

## 🌍 国际化支持

### 支持语言
- 🇨🇳 简体中文 (`zh_cn.json`)
- 🇺🇸 English (`en_us.json`)

### 翻译键示例
```json
{
  "sign.roadweaver.distance.to": "距离",
  "sign.roadweaver.distance.blocks": "格",
  "config.roadweaver.title": "RoadWeaver 配置"
}
```

## 📦 发布配置

### Modrinth
- 项目ID: `countereds-settlement-roads`
- 支持版本: 1.20.1
- 加载器: Fabric / Forge

### CurseForge
- 项目ID: `1140708`
- 分类: 世界生成
- 标签: 结构, 道路, 装饰

## 🤝 贡献指南

### 开发环境要求
- Java 17 或更高版本
- IntelliJ IDEA 或 Eclipse
- Git

### 代码规范
- 遵循 Java 命名规范
- 添加适当的注释和文档
- 考虑性能优化
- 确保客户端-服务器兼容性

### 提交流程
1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 创建 Pull Request

## 📄 许可证

本项目采用 [MIT](LICENSE) 许可证。

---

**最后更新**: 2025-10-11
**维护者**: shiroha-233
**项目地址**: https://github.com/shiroha-233/RoadWeaver
