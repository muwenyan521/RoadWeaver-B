# RoadWeaver 更新日志

## [未发布] - 2025-10-11

### 📦 依赖内嵌优化
- **内嵌 Architectury API (13.0.8)**: 减少玩家需要下载的前置模组数量
  - Fabric: 前置从 4 个减少到 3 个
  - NeoForge: 前置从 2 个减少到 1 个
  - 使用 JiJ (Jar-in-Jar) 技术，不会与手动安装的版本冲突
  - 模组文件大小增加约 500KB

### 🎮 NeoForge 模组菜单集成
- **新增 NeoForge 配置界面集成**: 现在可以在 NeoForge 的模组列表中直接打开配置界面
  - 使用 `ModContainer.registerExtensionPoint` 注册配置屏幕
  - 与 Fabric 版本功能对等
  - 无需额外依赖（Cloth Config 为可选依赖）
  - 访问方式: 主菜单 → Mods → RoadWeaver → Config 按钮

### 🖼️ 模组图标修复
- **修复模组图标不显示问题**: 将图标调整为正方形（512x512）
  - 图标已正确放置在 common/fabric/neoforge 三个模块
  - 符合 Mod Menu 的正方形要求
  - 在两个平台的模组列表中正常显示

### 🎨 新增装饰功能
- **长椅装饰 (BenchDecoration)**: 新增道路旁长椅装饰，支持 NBT 结构加载
- **大型装饰系统完善**: 实现了秋千、长椅、凉亭的完整放置逻辑
  - 随机选择装饰类型
  - 地形高度检查
  - 生物群系感知的木材适配
  - NBT 结构文件整合到 Common 模块

### 🌍 国际化改进
- **距离标志国际化**: 将硬编码文本改为翻译键
  - 新增 4 个翻译键：`sign.roadweaver.distance.*`
  - 支持中文和英文自动切换
  - 使用 `Component.translatable()` API

### ⚙️ 配置变更
- **大型装饰默认关闭**: 将 `placeSwings`, `placeBenches`, `placeGloriettes` 默认值改为 `false`
  - 原因：这些功能还在完善中
  - 玩家可在配置文件中手动启用
  - 不影响已有配置文件

### 📚 文档
- **路边装饰系统完善说明.md**: 详细的技术文档和使用说明
- **距离标志国际化说明.md**: 国际化实现细节
- **配置默认值变更说明.md**: 配置变更说明和启用指南
- **DECORATION_UPDATE_CHECKLIST.md**: 更新检查清单

### 🔧 技术改进
- 完善 `RoadFeature.addDecoration()` 方法
- 更新 `RoadStructures.tryPlaceDecorations()` 支持新装饰
- 统一 Fabric 和 NeoForge 配置默认值

---

## [未发布] - 2025-10-09

### 🐛 修复
- **修复结构搜寻功能失效问题** ⭐
  - 修复 `StructureLocatorImpl` 平台实现缺失
  - 修复 `skipKnownStructures` 参数错误（从 false 改为 true）
  - 修复不可变集合异常（`Road.java` 和 `updateConnectionStatus`）
  - 修复日志格式化参数顺序错误
  - 结构搜寻现在正常工作，数量可以从初始值增长到 `maxLocatingCount`

- **修复并发控制问题**
  - 修复世界重新加载时任务恢复逻辑
  - 只恢复 GENERATING 状态的连接（意外中断），不恢复 PLANNED（避免失败任务无限重试）
  - 异常时正确标记连接为 FAILED 状态
  - 移除世界加载时的立即生成调用，由 tick 事件统一处理
  - 增强并发上限检查和日志

### ✨ 新增
- **新增配置项 `maxLocatingCount`**: 限制模组搜寻的最大结构数量（默认 50）
  - 防止结构列表无限增长
  - 控制内存占用和性能
  - 避免道路网络过于密集
  - 支持 Fabric 和 NeoForge 平台
  - 包含中英文配置说明

- **新增 `WorldDataProvider.addStructureLocation()` 便捷方法**
  - 简化结构位置添加逻辑
  - 自动处理不可变集合问题

### 📚 文档
- **`TECHNICAL_OVERVIEW.md`**: 完整技术详解（推荐阅读）
  - 结构搜寻机制详解
  - 道路生成序列详解
  - A* 路径算法说明
  - 数据流转图
  - 性能优化策略

- **`MIGRATION_DIFFERENCES.md`**: 迁移差异对比
  - 原项目 vs Architectury 项目
  - 所有发现的差异和修复方案

- **`CONCURRENCY_FIX.md`**: 并发控制修复说明
  - 任务恢复机制
  - 失败处理策略
  - 并发上限控制

- **`FINAL_FIX_SUMMARY.md`**: 最终修复总结
  - 所有问题和修复清单
  - 验证结果

### ✅ 验证
- **Fabric**: 结构搜寻正常，数量从 6 增加到 18，连接和道路正常生成
- **NeoForge**: 编译成功，待测试

### 🏗️ 架构
- 完成 Architectury 多平台架构迁移
  - 核心逻辑统一到 `common` 模块
  - Fabric 和 NeoForge 平台各自实现平台特定功能
  - 使用 Architectury Events 统一事件处理
  - 使用 `@ExpectPlatform` 实现平台桥接

## [1.0.0] - 初始版本

### ✨ 功能
- 自动在结构（如村庄）之间生成道路
- A* 路径算法，地形感知
- 多种道路类型（人工/自然）
- 丰富的装饰系统（路灯、栏杆、标志、秋千、长椅、凉亭）
- 可视化调试界面
- 多线程异步道路生成
- 完整的配置系统
- 多语言支持（中文/英文）
