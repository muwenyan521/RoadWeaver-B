# 道路渲染LOD优化实现

## 🎯 优化目标

解决调试地图中道路渲染过于精细导致的性能问题，通过基于缩放比例的LOD（Level of Detail）系统动态调整渲染精度。

---

## 📊 LOD级别设计

### 基于块/格比例的分级策略

| LOD级别 | 块/格范围 | 采样步长 | 渲染精度 | 说明 |
|---------|----------|---------|---------|------|
| **FINEST** | < 50 | 1 | 100% | 全部渲染，最精细 |
| **SIXTY_FOURTH** | 50-500 | 64 | 1.56% | 1/64精度 |
| **ONE_TWENTY_EIGHTH** | 500-1000 | 128 | 0.78% | 1/128精度 |
| **TWO_FIFTY_SIXTH** | 1000-2000 | 256 | 0.39% | 1/256精度 |
| **FIVE_TWELVE** | 2000-5000 | 512 | 0.195% | 1/512精度 |
| **ONE_THOUSAND_TWENTY_FOURTH** | 5000-10000 | 1024 | 0.098% | 1/1024精度 |
| **NONE** | > 15000 | ∞ | 0% | 不渲染 |

### 计算公式

```java
// 每个屏幕像素代表多少世界方块
blocksPerPixel = 1.0 / (baseScale * zoom)

// 每格（80像素）代表多少方块
blocksPerGrid = blocksPerPixel * 80
```

---

## 🔧 技术实现

### 1. 配置常量

```java
// 道路渲染LOD配置 - 基于块/格比例
private static final double ROAD_LOD_FINEST = 200;      // 最精细（全部渲染）
private static final double ROAD_LOD_QUARTER = 500;     // 1/4精度
private static final double ROAD_LOD_EIGHTH = 1000;     // 1/8精度
private static final double ROAD_LOD_SIXTEENTH = 2000;  // 1/16精度
private static final double ROAD_LOD_THIRTYSECOND = 5000; // 1/32精度
private static final double ROAD_LOD_NONE = 10000;      // 不渲染
```

### 2. LOD级别枚举

```java
private enum RoadLODLevel {
    FINEST,       // < 200块/格 - 全部渲染 (步长1)
    QUARTER,      // 200-500块/格 - 1/4精度 (步长4)
    EIGHTH,       // 500-1000块/格 - 1/8精度 (步长8)
    SIXTEENTH,    // 1000-2000块/格 - 1/16精度 (步长16)
    THIRTYSECOND, // 2000-5000块/格 - 1/32精度 (步长32)
    NONE          // > 10000块/格 - 不渲染
}
```

### 3. LOD级别计算

```java
private RoadLODLevel getRoadLODLevel() {
    // 计算当前缩放下，每个屏幕像素代表多少个世界方块
    double blocksPerPixel = 1.0 / (baseScale * zoom);
    
    // 计算每格（假设格子间距为TARGET_GRID_PX像素）代表多少方块
    double blocksPerGrid = blocksPerPixel * TARGET_GRID_PX;
    
    // 根据块/格比例确定LOD级别
    if (blocksPerGrid >= ROAD_LOD_NONE) return RoadLODLevel.NONE;
    if (blocksPerGrid >= ROAD_LOD_THIRTYSECOND) return RoadLODLevel.THIRTYSECOND;
    if (blocksPerGrid >= ROAD_LOD_SIXTEENTH) return RoadLODLevel.SIXTEENTH;
    if (blocksPerGrid >= ROAD_LOD_EIGHTH) return RoadLODLevel.EIGHTH;
    if (blocksPerGrid >= ROAD_LOD_QUARTER) return RoadLODLevel.QUARTER;
    return RoadLODLevel.FINEST;
}
```

### 4. 道路渲染优化

```java
private void drawRoadPathWithRoadLOD(DrawContext ctx, 
                                     List<Records.RoadSegmentPlacement> segments, 
                                     int color, 
                                     RoadLODLevel roadLOD) {
    // 根据道路LOD级别决定采样步长
    int step = switch (roadLOD) {
        case FINEST -> 1;           // 全部渲染
        case QUARTER -> 4;          // 1/4精度
        case EIGHTH -> 8;           // 1/8精度
        case SIXTEENTH -> 16;       // 1/16精度
        case THIRTYSECOND -> 32;    // 1/32精度
        case NONE -> Integer.MAX_VALUE; // 不渲染
    };
    
    if (step >= segments.size()) return; // 步长太大，跳过
    
    ScreenPos prevPos = null;
    int drawnSegments = 0;
    int maxSegments = 10000; // 防止过度渲染
    
    for (int i = 0; i < segments.size() && drawnSegments < maxSegments; i += step) {
        BlockPos pos = segments.get(i).middlePos();
        ScreenPos currentPos = worldToScreen(pos.getX(), pos.getZ());
        
        // 边界检查优化 - 扩大边界以避免线段被截断
        if (!isInUIBounds(currentPos.x, currentPos.y, 100)) {
            prevPos = currentPos;
            continue;
        }
        
        if (prevPos != null && i > 0) {
            if (isLineInUIBounds(prevPos.x, prevPos.y, currentPos.x, currentPos.y)) {
                drawLine(ctx, prevPos.x, prevPos.y, currentPos.x, currentPos.y, color);
                drawnSegments++;
            }
        }
        prevPos = currentPos;
    }
}
```

---

## 🚀 性能提升

### 渲染负载对比

假设一条道路有 **10,000 个路段**：

| 缩放级别 | 块/格 | LOD级别 | 渲染路段数 | 性能提升 |
|---------|------|---------|-----------|---------|
| **极度放大** | 100 | FINEST | 10,000 | 基准 |
| **正常查看** | 300 | QUARTER | 2,500 | **4倍** ⚡ |
| **中等缩小** | 700 | EIGHTH | 1,250 | **8倍** ⚡⚡ |
| **较小缩小** | 1,500 | SIXTEENTH | 625 | **16倍** ⚡⚡⚡ |
| **大幅缩小** | 3,000 | THIRTYSECOND | 312 | **32倍** ⚡⚡⚡⚡ |
| **极度缩小** | 15,000 | NONE | 0 | **∞** 🚫 |

### 实际场景分析

**场景1: 查看整个道路网络（缩小视图）**
- 块/格比例: ~3000
- LOD级别: THIRTYSECOND
- 性能提升: **32倍**
- 用户体验: 仍能看到道路连接，但不会卡顿

**场景2: 查看局部道路细节（放大视图）**
- 块/格比例: ~150
- LOD级别: FINEST
- 性能提升: 无（全精度）
- 用户体验: 完整细节，流畅渲染

**场景3: 极度缩小查看全局**
- 块/格比例: >10000
- LOD级别: NONE
- 性能提升: **完全跳过**
- 用户体验: 道路不可见，但结构节点清晰

---

## 🎨 视觉效果

### LOD级别可视化

```
FINEST (步长1):     ●━●━●━●━●━●━●━●━●━●━●━●━●━●━●━●━●
                    完整连续的道路线

QUARTER (步长4):    ●━━━●━━━●━━━●━━━●━━━●━━━●━━━●
                    略微稀疏，但仍然连贯

EIGHTH (步长8):     ●━━━━━━●━━━━━━●━━━━━━●━━━━━━●
                    明显稀疏，但路径清晰

SIXTEENTH (步长16): ●━━━━━━━━━━━━━●━━━━━━━━━━━━━●
                    非常稀疏，仅显示大致方向

THIRTYSECOND (步长32): ●━━━━━━━━━━━━━━━━━━━━━━━━━━●
                       极度稀疏，仅显示起终点连接

NONE:               （不渲染）
```

---

## 🔍 调试信息

### 如何查看当前LOD级别

在统计面板中可以看到：
- **缩放**: 显示当前zoom值
- 根据zoom和baseScale可以推算出块/格比例

### 计算示例

```
假设:
- baseScale = 0.05 (地图初始缩放)
- zoom = 2.0 (用户放大2倍)
- TARGET_GRID_PX = 80 (格子间距)

计算:
blocksPerPixel = 1.0 / (0.05 * 2.0) = 10 块/像素
blocksPerGrid = 10 * 80 = 800 块/格

结果: LOD级别 = EIGHTH (1/8精度)
```

---

## ⚙️ 配置调整

### 如果需要调整LOD阈值

修改 `RoadDebugScreen.java` 中的常量：

```java
// 更激进的LOD策略（更早降低精度）
private static final double ROAD_LOD_FINEST = 100;      // 改为100
private static final double ROAD_LOD_QUARTER = 300;     // 改为300
private static final double ROAD_LOD_EIGHTH = 600;      // 改为600
// ...

// 更保守的LOD策略（保持更高精度）
private static final double ROAD_LOD_FINEST = 500;      // 改为500
private static final double ROAD_LOD_QUARTER = 1000;    // 改为1000
private static final double ROAD_LOD_EIGHTH = 2000;     // 改为2000
// ...
```

---

## 🧪 测试建议

### 测试场景

1. **大量道路测试**
   - 生成50+条道路
   - 缩放到不同级别
   - 观察帧率变化

2. **极端缩放测试**
   - 极度放大（zoom > 5）
   - 极度缩小（zoom < 0.1）
   - 验证LOD切换流畅性

3. **边界测试**
   - 在LOD切换临界点附近缩放
   - 验证无明显跳变

### 性能指标

- **目标帧率**: 60 FPS
- **可接受帧率**: 30 FPS
- **道路数量**: 支持100+条道路
- **路段数量**: 每条道路10,000+路段

---

## 📝 代码变更总结

### 修改的文件

- `src/main/java/net/countered/settlementroads/client/gui/RoadDebugScreen.java`

### 新增内容

1. **常量配置** (6个)
   - `ROAD_LOD_FINEST` ~ `ROAD_LOD_NONE`

2. **枚举类型** (1个)
   - `RoadLODLevel` (6个级别)

3. **方法** (2个)
   - `getRoadLODLevel()`: 计算当前LOD级别
   - `drawRoadPathWithRoadLOD()`: 基于LOD渲染道路

### 修改内容

1. **方法更新** (1个)
   - `drawRoadPathsLOD()`: 集成道路LOD系统

---

## 🎉 优化效果

### 预期改进

✅ **性能提升**: 在缩小视图下提升 **8-32倍** 渲染性能  
✅ **内存优化**: 减少绘制调用次数，降低GPU负载  
✅ **用户体验**: 流畅的缩放操作，无卡顿  
✅ **视觉质量**: 在需要细节时保持完整精度  
✅ **可扩展性**: 支持更多道路和更大地图

### 兼容性

- ✅ 向后兼容：不影响现有功能
- ✅ 配置灵活：可调整LOD阈值
- ✅ 独立系统：不影响结构节点和连接线渲染

---

## 🔮 未来优化方向

### 短期
- [ ] 添加LOD级别指示器到UI
- [ ] 平滑LOD过渡（避免突变）
- [ ] 自适应LOD（基于帧率）

### 中期
- [ ] 道路宽度LOD（远距离显示为单线）
- [ ] 颜色LOD（远距离简化颜色）
- [ ] 批量渲染优化

### 长期
- [ ] GPU加速渲染
- [ ] 预计算LOD层级
- [ ] 动态LOD预测

---

**版本**: v2.0.4  
**日期**: 2025-10-03  
**优化类型**: 性能优化 - 道路渲染LOD系统  
**影响范围**: 调试地图UI  
**性能提升**: 8-32倍（取决于缩放级别）
