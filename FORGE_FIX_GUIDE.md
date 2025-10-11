# Forge 1.20.1 运行问题修复指南

## 问题诊断

从 NeoForge 1.21.1 降级到 Forge 1.20.1 时遇到的主要问题：

### 问题 1: TransformerRuntime 类找不到
```
错误: 找不到或无法加载主类 dev.architectury.transformer.TransformerRuntime
原因: java.lang.ClassNotFoundException: dev.architectury.transformer.TransformerRuntime
```

### 问题 2: Mixin 兼容性级别错误 ✅ 已修复
```
Mixin config roadweaver.mixins.json specifies compatibility level JAVA_21 which is not recognised
```

这是因为 Minecraft 1.20.1 只支持 Java 17，而不是 Java 21。

## 根本原因

Architectury Loom 1.0.x 版本对 Forge 1.20.1 的支持不完善，导致 Architectury Transformer 运行时类路径配置不正确。

## 解决方案

### 方案 1: 升级 Architectury Loom（推荐）

我已经将 Loom 版本从 `1.0.308` 升级到 `1.6-SNAPSHOT`，这个版本对 Forge 1.20.1 有更好的支持。

**修改的文件**: `build.gradle`
```gradle
plugins {
    id 'architectury-plugin' version '3.4-SNAPSHOT'
    id 'dev.architectury.loom' version '1.6-SNAPSHOT' apply false  // 从 1.0.308 升级
}
```

### 方案 2: 修改依赖配置

**修改的文件**: `forge/build.gradle`
```gradle
dependencies {
    // Common 模块
    common(project(path: ":common", configuration: "namedElements")) { transitive false }
    shadowCommon(project(path: ":common", configuration: "transformProductionForge")) { transitive false }

    // Forge 平台
    forge "net.minecraftforge:forge:${minecraft_version}-${forge_version}"

    // Architectury API - 关键修复
    modApi("dev.architectury:architectury-forge:9.2.14")
    shadowCommon("dev.architectury:architectury-forge:9.2.14") { transitive false }

    // Cloth Config API
    modCompileOnly "me.shedaniel.cloth:cloth-config-forge:11.1.136"
    modRuntimeOnly "me.shedaniel.cloth:cloth-config-forge:11.1.136"
}
```

**关键点**:
- 使用 `modApi` 而不是 `modImplementation`，确保 Architectury API 在开发环境中可用
- 使用 `shadowCommon` 将 Architectury API 打包进最终 JAR
- 不使用 `include`，因为在 Loom 1.6+ 中 `shadowCommon` 已经处理了打包

## 执行步骤

### 1. 清理构建缓存
```powershell
$env:JAVA_HOME="C:\Program Files\Zulu\zulu-17"
./gradlew clean --no-daemon
Remove-Item -Recurse -Force .gradle,.architectury-transformer -ErrorAction SilentlyContinue
```

### 2. 重新下载依赖
```powershell
./gradlew :forge:build --refresh-dependencies --no-daemon
```

### 3. 运行客户端
```powershell
./gradlew :forge:runClient --no-daemon
```

## 如果问题仍然存在

### 备选方案 A: 使用 Loom 1.4.x

如果 Loom 1.6-SNAPSHOT 不稳定，可以尝试 1.4.x 版本：

```gradle
// build.gradle
plugins {
    id 'architectury-plugin' version '3.4-SNAPSHOT'
    id 'dev.architectury.loom' version '1.4-SNAPSHOT' apply false
}
```

### 备选方案 B: 检查 Architectury 版本兼容性

确保所有模块使用相同的 Architectury API 版本：

- `common/build.gradle`: `dev.architectury:architectury:9.2.14`
- `fabric/build.gradle`: `dev.architectury:architectury-fabric:9.2.14`
- `forge/build.gradle`: `dev.architectury:architectury-forge:9.2.14`

### 备选方案 C: 降级到稳定版本

如果以上方案都不行，考虑使用稳定的版本组合：

```properties
# gradle.properties
minecraft_version=1.20.1
architectury_version=9.2.14
loom_version=1.3.8
```

```gradle
// build.gradle
plugins {
    id 'architectury-plugin' version '3.4-SNAPSHOT'
    id 'dev.architectury.loom' version '1.3.8' apply false
}
```

## 验证修复

运行以下命令验证修复是否成功：

```powershell
# 1. 检查依赖是否正确解析
./gradlew :forge:dependencies --configuration runtimeClasspath | Select-String "architectury"

# 2. 检查 Transformer 类是否存在
./gradlew :forge:runClient --debug 2>&1 | Select-String "TransformerRuntime"
```

## 已知问题

1. **Loom 1.6-SNAPSHOT 警告**: "You are using an outdated version..."
   - 这是正常的，因为使用的是 SNAPSHOT 版本
   - 可以忽略，或者等待稳定版本发布

2. **Forge Mod 警告**: "could not find forge mod in modCompileOnly but forcing..."
   - 这些是 Cloth Config 的依赖警告
   - 不影响运行，可以忽略

3. **Gradle 9.0 兼容性警告**
   - 当前使用 Gradle 8.6，与 9.0 有兼容性问题
   - 不影响当前构建，可以忽略

## 参考资料

- [Architectury Loom 文档](https://docs.architectury.dev/loom/)
- [Architectury API 文档](https://docs.architectury.dev/)
- [Forge 1.20.1 迁移指南](https://docs.minecraftforge.net/en/1.20.x/gettingstarted/versioning/)

## 联系支持

如果问题仍未解决，请提供以下信息：

1. 完整的错误堆栈跟踪
2. `./gradlew :forge:dependencies --configuration runtimeClasspath` 的输出
3. Java 版本: `java -version`
4. Gradle 版本: `./gradlew --version`

---

**最后更新**: 2025-10-11
**适用版本**: Minecraft 1.20.1 + Forge 47.3.0
