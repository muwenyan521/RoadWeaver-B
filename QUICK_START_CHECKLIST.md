# ✅ 快速操作清单

## 🚨 必须立即手动完成的操作

### 1️⃣ 重命名 neoforge 文件夹
```bash
# Windows PowerShell
Rename-Item -Path "neoforge" -NewName "forge"

# 或者在文件管理器中手动重命名
```
**状态**: ⬜ 未完成

---

### 2️⃣ 替换 Forge 构建文件
```bash
# Windows PowerShell
Remove-Item forge\build.gradle
Rename-Item -Path "forge_build.gradle.new" -NewName "forge\build.gradle"

# 或者手动操作：
# 1. 删除 forge/build.gradle
# 2. 将 forge_build.gradle.new 重命名为 forge/build.gradle
```
**状态**: ⬜ 未完成

---

### 3️⃣ 修改 Forge 主类
**文件**: `forge/src/main/java/net/countered/settlementroads/SettlementRoads.java`

**需要修改**:
- [ ] 将所有 `net.neoforged` 导入改为 `net.minecraftforge`
- [ ] 修改构造函数签名
- [ ] 修改配置屏幕注册代码

**参考**: 查看 `MIGRATION_TO_1.20.1_GUIDE.md` 第 3.2 节

**状态**: ⬜ 未完成

---

### 4️⃣ 重命名 Forge 包名
需要重命名以下目录：
- [ ] `forge/src/main/java/.../config/neoforge/` → `config/forge/`
- [ ] `forge/src/main/java/.../features/config/neoforge/` → `features/config/forge/`
- [ ] `forge/src/main/java/.../helpers/neoforge/` → `helpers/forge/`
- [ ] `forge/src/main/java/.../persistence/neoforge/` → `persistence/forge/`

**状态**: ⬜ 未完成

---

### 5️⃣ 创建 mods.toml
**删除**: `forge/src/main/resources/META-INF/neoforge.mods.toml`
**创建**: `forge/src/main/resources/META-INF/mods.toml`

**内容**: 参考 `MIGRATION_TO_1.20.1_GUIDE.md` 第 4.1 节

**状态**: ⬜ 未完成

---

### 6️⃣ 全局搜索替换
在 `forge/src/` 目录下：
- [ ] 搜索 `net.neoforged` 替换为 `net.minecraftforge`
- [ ] 搜索 `neoforge` 包名替换为 `forge`
- [ ] 检查所有导入语句

**状态**: ⬜ 未完成

---

### 7️⃣ 清理和构建
```bash
# 清理旧构建
./gradlew clean

# 重新构建
./gradlew build
```
**状态**: ⬜ 未完成

---

### 8️⃣ 测试运行
```bash
# 测试 Fabric
./gradlew :fabric:runClient

# 测试 Forge
./gradlew :forge:runClient
```
**状态**: ⬜ 未完成

---

## 📚 参考文档

- **详细指南**: `MIGRATION_TO_1.20.1_GUIDE.md`
- **迁移总结**: `VERSION_MIGRATION_SUMMARY.md`

---

## ⚠️ 常见错误

### 错误 1: "cannot find symbol: ResourceLocation.fromNamespaceAndPath"
**原因**: 1.20.1 不支持此 API
**解决**: 已自动修复，如果还有遗漏请手动替换为 `new ResourceLocation(...)`

### 错误 2: "package net.neoforged does not exist"
**原因**: 忘记修改 Forge 模块的导入
**解决**: 将所有 `net.neoforged` 改为 `net.minecraftforge`

### 错误 3: "Could not find net.neoforged:neoforge"
**原因**: 忘记替换 forge/build.gradle
**解决**: 使用 `forge_build.gradle.new` 替换

---

## 🎯 完成标准

全部勾选后即可运行：
- [ ] neoforge 文件夹已重命名为 forge
- [ ] forge/build.gradle 已替换
- [ ] Forge 主类已修改
- [ ] 所有包名已重命名
- [ ] mods.toml 已创建
- [ ] 所有 NeoForge API 已替换为 Forge API
- [ ] `./gradlew build` 编译成功
- [ ] Fabric 和 Forge 都能正常运行

---

**开始时间**: _____________
**完成时间**: _____________

**祝你好运！** 🚀
