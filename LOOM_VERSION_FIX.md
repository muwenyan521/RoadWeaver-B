# Loom 版本问题修复指南

## 问题
无法找到 Architectury Loom 的正确版本用于 Minecraft 1.20.1

## 解决方案

### 方案 1: 使用已测试的版本组合
对于 Minecraft 1.20.1，推荐使用以下版本组合：

```gradle
plugins {
    id 'java'
    id 'dev.architectury.loom' version '1.2.413' apply false
    id 'architectury-plugin' version '3.4-SNAPSHOT'
}
```

### 方案 2: 如果方案 1 失败，尝试其他稳定版本

```gradle
// 选项 A: 使用 1.2.377
id 'dev.architectury.loom' version '1.2.377' apply false

// 选项 B: 使用 1.2.368
id 'dev.architectury.loom' version '1.2.368' apply false

// 选项 C: 使用 1.3.358
id 'dev.architectury.loom' version '1.3.358' apply false
```

### 方案 3: 降级到更早的 Architectury 版本

如果 Loom 版本问题持续，可以考虑使用更早但稳定的 Architectury 组合：

```gradle
plugins {
    id 'java'
    id 'dev.architectury.loom' version '1.0.308' apply false
    id 'architectury-plugin' version '3.4-SNAPSHOT'
}
```

同时需要调整 Architectury API 版本：
```gradle
// 在 common/build.gradle 和各平台的 build.gradle 中
modImplementation("dev.architectury:architectury:8.3.128")  // 适用于 1.20.1
```

### 方案 4: 使用 Fabric Loom + Architectury Plugin

Architectury 3.4 也支持直接使用 Fabric Loom：

```gradle
plugins {
    id 'java'
    id 'fabric-loom' version '1.2-SNAPSHOT' apply false
    id 'architectury-plugin' version '3.4-SNAPSHOT'
}
```

但这需要在子项目中做额外配置。

## 测试步骤

1. 修改 `build.gradle` 中的版本
2. 删除 `.gradle` 缓存：
   ```powershell
   Remove-Item -Recurse -Force .gradle
   ```
3. 刷新依赖：
   ```powershell
   .\gradlew.bat clean --refresh-dependencies
   ```
4. 尝试构建：
   ```powershell
   $env:JAVA_HOME="C:\Program Files\Zulu\zulu-17"
   .\gradlew.bat build
   ```

## 当前尝试的版本

已尝试的版本（按顺序）：
- ❌ `1.4-SNAPSHOT` - 不存在
- ❌ `1.3-SNAPSHOT` - 无法解析
- ❌ `1.3.392` - 不存在
- ❌ `1.2-SNAPSHOT` - 无法解析
- ⏳ `1.2.413` - 当前测试中

## 推荐：查看工作中的项目配置

如果以上都不行，最可靠的方法是：

1. 找一个已知可以工作的 Architectury 1.20.1 项目
2. 复制其 `build.gradle` 和 `gradle.properties` 配置
3. 参考项目：
   - Architectury 官方模板：https://github.com/architectury/architectury-templates
   - 其他 1.20.1 Architectury 模组

## 备选方案：保持 1.21.1

如果 1.20.1 的配置问题太多，可以考虑：
1. 保持项目在 1.21.1
2. 或者只降级 Fabric 版本到 1.20.1，Forge 保持在 1.21.1（分别维护）

这样可以避免大量的兼容性问题。
