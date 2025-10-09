# RoadWeaver 更新日志

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
