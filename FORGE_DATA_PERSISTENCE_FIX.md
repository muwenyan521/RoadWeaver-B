# Forge 数据持久化修复说明

## 🐛 问题描述

**症状**: Forge 版本的道路数据无法保存到磁盘，重启世界后所有道路数据丢失。

**影响范围**:
- 结构连接数据 (`connections`)
- 道路数据列表 (`roadDataList`)
- 结构位置数据部分可能也受影响

## 🔍 问题根源

在 `ForgeWorldDataProvider.java` 的 `save()` 和 `load()` 方法中，对 NBT 数据类型的处理有误。

### 错误的代码（save方法）

```java
// ❌ 错误：List 编码为 ListTag，不是 CompoundTag
Codec.list(Records.RoadData.CODEC).encodeStart(ops, roadDataList)
    .result()
    .ifPresent(nbt -> { 
        if (nbt instanceof CompoundTag ct) tag.put(KEY_ROAD_DATA, ct); 
    });
```

**问题**：
1. `Codec.list()` 编码的结果是 `ListTag`，而不是 `CompoundTag`
2. `instanceof CompoundTag` 判断永远失败
3. 数据根本没有被写入到 NBT 中

### 错误的代码（load方法）

```java
// ❌ 错误：强制转换为 CompoundTag，但实际是 ListTag
if (tag.contains(KEY_ROAD_DATA)) {
    CompoundTag roadsTag = tag.getCompound(KEY_ROAD_DATA);  // 返回空 CompoundTag
    DataResult<List<Records.RoadData>> res = Codec.list(Records.RoadData.CODEC).parse(new Dynamic<>(ops, roadsTag));
    res.result().ifPresent(val -> data.roadDataList = val);
}
```

**问题**：
1. `tag.getCompound()` 对 ListTag 返回空的 CompoundTag
2. 解析失败，数据无法读取

## ✅ 修复方案

### 修复 save() 方法

```java
// ✅ 正确：直接保存 Tag，不做类型判断
Codec.list(Records.RoadData.CODEC).encodeStart(ops, roadDataList)
    .result()
    .ifPresent(nbt -> tag.put(KEY_ROAD_DATA, nbt));
```

### 修复 load() 方法

```java
// ✅ 正确：使用 tag.get() 获取原始 Tag
if (tag.contains(KEY_ROAD_DATA)) {
    Tag roadsTag = tag.get(KEY_ROAD_DATA);  // 获取 ListTag
    DataResult<List<Records.RoadData>> res = Codec.list(Records.RoadData.CODEC).parse(new Dynamic<>(ops, roadsTag));
    res.result().ifPresent(val -> data.roadDataList = val);
}
```

## 📝 修复详情

### 修改文件
`forge/src/main/java/net/countered/settlementroads/persistence/forge/ForgeWorldDataProvider.java`

### 修改内容

#### 1. save() 方法（第71-91行）
- **结构位置**: 移除 `instanceof CompoundTag` 判断
- **结构连接**: 移除 `instanceof CompoundTag` 判断
- **道路数据**: 移除 `instanceof CompoundTag` 判断

#### 2. load() 方法（第43-69行）
- **结构位置**: `tag.getCompound()` → `tag.get()`
- **结构连接**: `tag.getCompound()` → `tag.get()`
- **道路数据**: `tag.getCompound()` → `tag.get()`

## 🧪 验证方法

### 1. 编译测试
```bash
./gradlew :forge:build
```

### 2. 游戏内测试
1. 启动 Forge 客户端
2. 创建新世界并生成道路
3. 退出世界
4. 重新进入世界
5. 检查道路数据是否保留

### 3. 数据文件检查
查看世界存档中的数据文件：
```
saves/<世界名>/data/roadweaver_world_data.dat
```

使用 NBT 查看器检查文件内容：
- `structure_locations`: CompoundTag
- `connections`: ListTag（应包含连接数据）
- `road_data_list`: ListTag（应包含道路数据）

## 📊 NBT 数据类型对照

| 数据类型 | Codec 类型 | NBT 类型 | 说明 |
|---------|-----------|---------|------|
| `StructureLocationData` | `Record` | `CompoundTag` | 单个记录对象 |
| `List<StructureConnection>` | `Codec.list()` | `ListTag` | 列表数据 |
| `List<RoadData>` | `Codec.list()` | `ListTag` | 列表数据 |

## 🎯 关键要点

1. **Codec 编码规则**:
   - `Record` → `CompoundTag`
   - `List` → `ListTag`
   - 不要假设所有数据都是 `CompoundTag`

2. **NBT 读写最佳实践**:
   - 保存时：直接使用 `tag.put(key, nbt)`，不要做类型转换
   - 读取时：使用 `tag.get(key)` 获取原始 Tag，让 Codec 自己解析

3. **SavedData 机制**:
   - `setDirty()` 标记数据需要保存
   - 游戏会在适当时机调用 `save()` 方法
   - 数据保存在 `saves/<世界名>/data/` 目录

## 🔄 与 Fabric 版本对比

Fabric 版本使用 Attachment API，不存在这个问题：
```java
// Fabric: 直接使用 Codec，由 API 自动处理序列化
public static final AttachmentType<List<Records.RoadData>> ROAD_DATA_LIST = 
    AttachmentRegistry.createPersistent(
        new ResourceLocation(MOD_ID, "road_chunk_data_map"),
        Codec.list(Records.RoadData.CODEC)  // ✅ API 自动处理
    );
```

## 📅 修复日期
2025-10-11

## ✅ 状态
已修复，待测试验证

---

**注意**: 此修复不影响已有的错误数据文件。如果之前的世界已经保存了空数据，需要重新生成道路。
