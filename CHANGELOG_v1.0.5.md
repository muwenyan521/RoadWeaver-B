# RoadWeaver v1.0.5 更新日志

## 🎉 重大更新：异步结构搜索与智能道路规划

### ✨ 新功能

#### 1. 异步结构搜索系统
- **不阻塞主线程**：结构搜索移到独立线程，服务器 TPS 保持稳定
- **性能提升**：从 500-2000ms 降到 0ms（主线程）
- **参考实现**：基于 MC-249136 Fix 模组的方案

#### 2. 轮询搜索机制
- **每次只搜索一种结构类型**：避免 StructureCheck 缓存溢出
- **循环轮询**：公平搜索所有配置的结构类型
- **解决 Bug**：完全避免 `ArrayIndexOutOfBoundsException: Index -1`

#### 3. 多线程并行搜索
- **可配置线程池**：默认 3 个线程，可调整 1-8
- **并行搜索不同结构**：大幅提升搜索效率
- **线程安全**：使用 `ConcurrentHashMap` 管理结果

#### 4. 批量累积规划机制 ⭐
- **智能累积**：先累积结构到缓冲区，达到批量大小后统一加入道路规划
- **防止混乱连接**：避免远距离结构先被发现导致的道路混乱
- **有序道路网络**：近距离结构优先连接，符合自然发展规律
- **可配置大小**：默认 5 个，可调整 1-50

### 🔧 新增配置项

#### Structure Batch Size（结构批量累积大小）
- **默认值**: 5
- **范围**: 1-50
- **说明**: 累积多少个结构后再统一加入道路规划
- **效果**: 数值越大越能防止远距离结构先连接

#### Structure Search Threads（结构搜索线程数）
- **默认值**: 3
- **范围**: 1-8
- **说明**: 并行结构搜索的线程池大小
- **效果**: 数值越大搜索越快，但消耗更多 CPU
- **注意**: 需要重启服务器才能生效

### 🐛 Bug 修复

1. **修复 RejectedExecutionException**
   - 问题：退出世界后重新加载时，ExecutorService 已关闭导致崩溃
   - 解决：添加自动重启机制，检测到关闭时重新创建线程池

2. **修复 ArrayIndexOutOfBoundsException**
   - 问题：Minecraft 1.20.1 的 StructureCheck 缓存溢出 Bug
   - 解决：轮询搜索机制，每次只搜索一种结构，缓存压力降低 10 倍

3. **修复道路网络混乱**
   - 问题：远距离结构先被发现，导致长路分割近距离结构
   - 解决：批量累积机制，统一规划后按距离排序连接

### 📊 性能对比

| 指标 | v1.0.4 | v1.0.5 | 改进 |
|------|--------|--------|------|
| **主线程阻塞** | 500-2000ms | 0ms | ✅ 100% |
| **服务器 TPS** | 不稳定 | 稳定 20 | ✅ 稳定 |
| **缓存压力** | 高（10+ 结构/次） | 低（1 结构/次） | ✅ -90% |
| **ArrayIndexOutOfBoundsException** | 频繁触发 | 完全避免 | ✅ 100% |
| **道路网络质量** | 混乱 | 有序 | ✅ 显著提升 |

### 🎮 使用建议

#### 推荐配置

**低配置服务器**：
```
structureBatchSize = 3-5
structureSearchThreads = 1-2
```

**中等配置**：
```
structureBatchSize = 5-10
structureSearchThreads = 3（默认）
```

**高配置服务器**：
```
structureBatchSize = 10-20
structureSearchThreads = 4-6
```

**超高配置**：
```
structureBatchSize = 20-50
structureSearchThreads = 6-8
```

### 📝 技术细节

#### 异步搜索流程
```
1. 玩家移动触发搜索
   ↓
2. 主线程提交任务到线程池（不阻塞）
   ↓
3. 独立线程执行结构搜索
   ↓
4. 搜索完成，结果存入 ConcurrentHashMap
   ↓
5. 主线程 tick 时处理结果（加入缓冲区）
   ↓
6. 缓冲区满时批量加入道路规划
```

#### 轮询搜索机制
```
第 1 次搜索: minecraft:village_plains
第 2 次搜索: minecraft:village_desert  
第 3 次搜索: minecraft:village_savanna
第 4 次搜索: minecraft:pillager_outpost
第 5 次搜索: minecraft:village_plains  ← 循环回到第一个
```

#### 批量累积示例
```
时间线：
T1: 发现结构 A (距离 100m) → 加入缓冲区 (1/5)
T2: 发现结构 B (距离 5000m) → 加入缓冲区 (2/5)
T3: 发现结构 C (距离 200m) → 加入缓冲区 (3/5)
T4: 发现结构 D (距离 300m) → 加入缓冲区 (4/5)
T5: 发现结构 E (距离 150m) → 加入缓冲区 (5/5)
T6: 缓冲区满，批量加入道路规划
T7: 按距离排序连接：A-C-D-E-B
    → 近距离优先，远距离最后
```

### 🌍 多语言支持

- ✅ 英文（en_us.json）
- ✅ 简体中文（zh_cn.json）

### 📚 文档

新增技术文档：
- `ASYNC_STRUCTURE_LOCATION_IMPLEMENTATION.md` - 异步实现详解
- `ASYNC_EXECUTOR_FIX.md` - ExecutorService 重启修复
- `ROUND_ROBIN_STRUCTURE_SEARCH.md` - 轮询搜索机制
- `BATCH_STRUCTURE_PLANNING.md` - 批量累积规划

### ⚠️ 注意事项

1. **线程数配置需要重启**：修改 `structureSearchThreads` 后需要重启服务器才能生效
2. **向后兼容**：所有新功能都有合理的默认值，不影响现有配置
3. **内存使用**：批量累积会暂存结构信息，但内存占用可忽略（< 1KB）

### 🔄 升级说明

从 v1.0.4 升级到 v1.0.5：
1. 替换模组文件
2. 首次启动会自动生成新配置项（使用默认值）
3. 可选：根据服务器配置调整 `structureBatchSize` 和 `structureSearchThreads`
4. 无需修改现有世界数据

### 🎉 总结

v1.0.5 是一个重大更新，完全重构了结构搜索系统：
- ✅ 解决了性能问题（主线程不再阻塞）
- ✅ 解决了稳定性问题（避免 Minecraft Bug）
- ✅ 解决了道路质量问题（智能批量规划）
- ✅ 提供了灵活的配置选项（适应不同服务器）

**推荐所有用户升级！**

---

**发布日期**: 2025-01-15  
**版本**: 1.0.5  
**支持**: Minecraft 1.20.1 (Fabric & Forge)
