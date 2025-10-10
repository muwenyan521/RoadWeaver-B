# NeoForge 数据丢失修复说明

## 🐛 问题描述

**症状**: NeoForge 版本退出重进世界后，调试地图中的结构点数据完全丢失

**影响范围**: 
- 结构位置数据丢失
- 结构类型信息丢失
- 导致无法正确显示和管理道路网络

## 🔍 问题根源

### 数据结构定义
`Records.StructureLocationData` 包含两个字段：

```java
public record StructureLocationData(
    List<BlockPos> structureLocations,      // 结构位置列表
    List<StructureInfo> structureInfos      // 结构信息列表（位置 + 类型）
)
```

### 错误的加载逻辑

**修复前** (`WorldDataHelper.java` 第 121-127 行):
```java
public static StructureLocationsData load(CompoundTag tag) {
    // ❌ 只加载了 structureLocations，丢失了 structureInfos
    List<BlockPos> locations = Records.StructureLocationData.CODEC
            .parse(NbtOps.INSTANCE, tag.get("data"))
            .resultOrPartial(error -> ...)
            .map(Records.StructureLocationData::structureLocations)  // ❌ 只提取位置
            .orElse(new ArrayList<>());
    return new StructureLocationsData(locations);  // ❌ 只传入位置列表
}
```

**问题分析**:
1. 虽然 `save()` 方法正确保存了完整的 `StructureLocationData` 对象
2. 但 `load()` 方法只提取了 `structureLocations` 字段
3. `structureInfos` 字段被丢弃，导致结构类型信息丢失
4. 重新创建的 `StructureLocationData` 只包含位置，不包含类型信息

## ✅ 修复方案

### 修复后的代码

```java
public static StructureLocationsData load(CompoundTag tag) {
    // ✅ 直接解析完整的 StructureLocationData 对象（包含 structureLocations 和 structureInfos）
    Records.StructureLocationData data = Records.StructureLocationData.CODEC
            .parse(NbtOps.INSTANCE, tag.get("data"))
            .resultOrPartial(error -> SettlementRoads.getLogger().error("Failed to load structure locations: {}", error))
            .orElse(new Records.StructureLocationData(new ArrayList<>()));
    return new StructureLocationsData(data);  // ✅ 传入完整对象
}
```

### 修复要点

1. **移除 `.map()` 调用**: 不再只提取 `structureLocations` 字段
2. **直接使用解析结果**: 保留完整的 `StructureLocationData` 对象
3. **保持数据完整性**: `structureLocations` 和 `structureInfos` 都被正确加载

## 🔄 数据流对比

### 修复前（数据丢失）
```
保存时:
StructureLocationData {
    structureLocations: [pos1, pos2, pos3]
    structureInfos: [info1, info2, info3]
}
    ↓ save() ✅ 正确保存
NBT 文件 (完整数据)
    ↓ load() ❌ 只加载位置
StructureLocationData {
    structureLocations: [pos1, pos2, pos3]
    structureInfos: []  // ❌ 丢失
}
```

### 修复后（数据完整）
```
保存时:
StructureLocationData {
    structureLocations: [pos1, pos2, pos3]
    structureInfos: [info1, info2, info3]
}
    ↓ save() ✅ 正确保存
NBT 文件 (完整数据)
    ↓ load() ✅ 完整加载
StructureLocationData {
    structureLocations: [pos1, pos2, pos3]
    structureInfos: [info1, info2, info3]  // ✅ 保留
}
```

## 🧪 验证方法

### 测试步骤
1. 启动 NeoForge 版本的游戏
2. 进入世界，等待结构搜寻完成
3. 按 `H` 键打开调试地图，记录结构数量
4. 退出世界并重新进入
5. 再次打开调试地图，点击"刷新"按钮
6. ✅ 确认所有结构点都还在

### 预期结果
- ✅ 结构位置正确保存和加载
- ✅ 结构类型信息正确保存和加载
- ✅ 调试地图显示完整的结构网络
- ✅ 结构颜色编码正确（不同类型显示不同颜色）

## 📊 与 Fabric 版本对比

### Fabric 实现 (正确)
```java
// Fabric 使用 Attachment API，直接使用 Codec 序列化/反序列化
public static final AttachmentType<Records.StructureLocationData> STRUCTURE_LOCATIONS = 
    AttachmentRegistry.createPersistent(
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "village_locations"),
        Records.StructureLocationData.CODEC  // ✅ 完整的 Codec
    );
```

### NeoForge 实现 (已修复)
```java
// NeoForge 使用 SavedData，需要手动实现 save/load
public static StructureLocationsData load(CompoundTag tag) {
    Records.StructureLocationData data = Records.StructureLocationData.CODEC
            .parse(NbtOps.INSTANCE, tag.get("data"))
            .orElse(new Records.StructureLocationData(new ArrayList<>()));
    return new StructureLocationsData(data);  // ✅ 现在与 Fabric 一致
}
```

## 🎯 修复影响

### 修复的功能
1. ✅ **结构位置持久化**: 退出重进后结构点不丢失
2. ✅ **结构类型持久化**: 结构 ID 信息正确保存
3. ✅ **调试地图显示**: 所有结构正确显示
4. ✅ **颜色编码**: 不同结构类型显示不同颜色
5. ✅ **连接关系**: 基于结构类型的连接逻辑正常工作

### 不受影响的功能
- ✅ 结构连接数据 (`ConnectedStructuresData`) - 本来就正确
- ✅ 道路数据 (`RoadDataStorage`) - 本来就正确
- ✅ 道路生成逻辑 - 不受影响

## 🔧 技术细节

### Codec 工作原理
```java
// StructureLocationData 的 Codec 定义
public static final Codec<StructureLocationData> CODEC = RecordCodecBuilder.create(instance ->
    instance.group(
        BlockPos.CODEC.listOf().optionalFieldOf("structure_locations", new ArrayList<>())
            .forGetter(StructureLocationData::structureLocations),
        StructureInfo.CODEC.listOf().optionalFieldOf("structure_infos", new ArrayList<>())
            .forGetter(StructureLocationData::structureInfos)
    ).apply(instance, StructureLocationData::new)
);
```

### 序列化格式 (NBT)
```
data: {
    structure_locations: [
        {X: 100, Y: 64, Z: 200},
        {X: 300, Y: 70, Z: 400}
    ],
    structure_infos: [
        {pos: {X: 100, Y: 64, Z: 200}, structure_id: "minecraft:village_plains"},
        {pos: {X: 300, Y: 70, Z: 400}, structure_id: "minecraft:village_desert"}
    ]
}
```

## 📝 总结

### 问题原因
- NeoForge 的 `load()` 方法实现错误，只加载了部分数据

### 修复方法
- 移除 `.map()` 调用，直接使用完整的解析结果

### 修复效果
- 数据持久化功能现在与 Fabric 版本完全一致
- 所有结构信息在世界重新加载后完整保留

### 建议
- 定期测试数据持久化功能
- 对比 Fabric 和 NeoForge 的实现，确保一致性
- 使用 Codec 时注意保持数据结构的完整性

---

**修复时间**: 2025-10-10  
**影响版本**: NeoForge 1.0.0  
**修复文件**: `WorldDataHelper.java`  
**修复行数**: 第 121-127 行
