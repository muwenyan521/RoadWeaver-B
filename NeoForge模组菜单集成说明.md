# NeoForge 模组菜单集成说明

## ✅ 已完成的功能

为 NeoForge 版本添加了与 Fabric 版本相同的模组菜单配置界面集成。

## 🔧 实现方式

### Fabric 实现
Fabric 使用 **Mod Menu** 模组的 API：
```java
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClothConfigScreen::createConfigScreen;
    }
}
```

### NeoForge 实现
NeoForge 使用 **ModContainer.registerExtensionPoint**：
```java
modContainer.registerExtensionPoint(
    net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
    (client, parent) -> ClothConfigScreen.createConfigScreen(parent)
);
```

## 📋 修改的文件

### 1. `SettlementRoads.java` (主类)
**位置**: `neoforge/src/main/java/net/countered/settlementroads/SettlementRoads.java`

**修改内容**:
- 添加 `ClothConfigScreen` 导入
- 在构造函数中注册配置屏幕扩展点

```java
// 注册配置屏幕（NeoForge 模组菜单集成）
modContainer.registerExtensionPoint(
    net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
    (client, parent) -> ClothConfigScreen.createConfigScreen(parent)
);
```

### 2. `SettlementRoadsClient.java` (客户端)
**位置**: `neoforge/src/main/java/net/countered/settlementroads/client/SettlementRoadsClient.java`

**修改内容**:
- 更新注释说明配置屏幕已在主类中注册

## 🎮 使用方法

### 在游戏中访问配置

1. **启动游戏** (NeoForge 1.21.1)
2. **进入主菜单**
3. **点击 "Mods" 按钮** (NeoForge 内置模组列表)
4. **找到 RoadWeaver 模组**
5. **点击模组条目**
6. **点击右下角的 "Config" 按钮** ⚙️
7. **配置界面打开** ✅

### 配置界面功能

与 Fabric 版本完全相同：
- ✅ 结构设置（要定位的结构、搜寻半径）
- ✅ 预生成设置（初始定位数量、并发上限）
- ✅ 道路设置（地形平均、高度差、稳定性）
- ✅ 装饰设置（栏杆、秋千、长椅、凉亭）
- ✅ 手动模式设置（手动连接参数）

## 📊 平台对比

| 功能 | Fabric | NeoForge | 说明 |
|------|--------|----------|------|
| 配置界面 | ✅ Mod Menu | ✅ 内置模组列表 | 都支持 |
| 实现方式 | ModMenuApi | IConfigScreenFactory | 不同API |
| 依赖模组 | Mod Menu (推荐) | 无需额外依赖 | NeoForge内置 |
| 配置文件 | config/roadweaver.json | config/roadweaver.json | 相同 |
| 界面样式 | Cloth Config | Cloth Config | 相同 |

## 🔍 技术细节

### NeoForge 配置屏幕注册机制

NeoForge 1.21.1 使用 **Extension Point** 系统：

1. **IConfigScreenFactory**: 配置屏幕工厂接口
2. **ModContainer**: 模组容器，用于注册扩展点
3. **registerExtensionPoint**: 注册方法

```java
modContainer.registerExtensionPoint(
    ExtensionPointType,
    FactoryFunction
);
```

### 与 Fabric 的区别

| 特性 | Fabric | NeoForge |
|------|--------|----------|
| 入口点 | fabric.mod.json | ModContainer |
| API | ModMenuApi | IConfigScreenFactory |
| 注册时机 | 模组初始化 | 构造函数 |
| 依赖 | Mod Menu 模组 | 内置支持 |

## ⚠️ 注意事项

### 1. Cloth Config API 依赖

NeoForge 版本的 Cloth Config API 是**可选依赖**：
```toml
[[dependencies.roadweaver]]
modId = "cloth_config"
type = "optional"
versionRange = "[15.0,)"
ordering = "NONE"
side = "CLIENT"
```

**如果没有安装 Cloth Config API**:
- ✅ 模组仍然可以运行
- ❌ 无法打开配置界面
- ✅ 可以通过编辑 `config/roadweaver.json` 手动配置

### 2. 推荐安装 Cloth Config

虽然是可选的，但**强烈推荐安装**：
- 📥 下载: https://modrinth.com/mod/cloth-config
- 版本: 15.0.140 for NeoForge 1.21.1

### 3. 配置文件位置

两个平台使用相同的配置文件：
```
.minecraft/config/roadweaver.json
```

可以在不同平台间共享配置！

## 🚀 测试步骤

### 验证配置界面

1. **构建模组**:
```bash
./gradlew :neoforge:build
```

2. **运行客户端**:
```bash
./gradlew :neoforge:runClient
```

3. **测试步骤**:
   - 进入主菜单
   - 点击 "Mods"
   - 找到 "RoadWeaver"
   - 点击 "Config" 按钮
   - ✅ 配置界面应该正常打开

4. **测试配置保存**:
   - 修改任意配置项
   - 点击 "Save" 按钮
   - 重启游戏
   - ✅ 配置应该被保存

## 📝 更新日志

### 2025-10-11
- ✅ 为 NeoForge 添加模组菜单配置界面集成
- ✅ 使用 `ModContainer.registerExtensionPoint` 注册配置屏幕
- ✅ 与 Fabric 版本功能对等
- ✅ 无需额外依赖（Cloth Config 可选）

## 🔗 相关文件

- `SettlementRoads.java` - 主类，注册配置屏幕
- `ClothConfigScreen.java` - 配置界面实现
- `NeoForgeJsonConfig.java` - 配置数据管理
- `neoforge.mods.toml` - 模组元数据（标记 Cloth Config 为可选）

## 📞 常见问题

### Q: 为什么 NeoForge 不需要 Mod Menu？
**A**: NeoForge 内置了模组列表和配置界面支持，不需要额外的 Mod Menu 模组。

### Q: 配置界面打不开怎么办？
**A**: 检查是否安装了 Cloth Config API (NeoForge 版本)。如果没有，请从 Modrinth 或 CurseForge 下载。

### Q: Fabric 和 NeoForge 的配置可以共享吗？
**A**: 可以！两个平台使用相同的 `config/roadweaver.json` 文件，可以直接复制使用。

### Q: 如何手动编辑配置？
**A**: 编辑 `.minecraft/config/roadweaver.json` 文件，然后重启游戏。

---

**完成时间**: 2025-10-11  
**适用版本**: RoadWeaver 1.0.0 for NeoForge 1.21.1
