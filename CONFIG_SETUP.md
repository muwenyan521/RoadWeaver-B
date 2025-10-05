# RoadWeaver NeoForge 配置界面设置

## 📋 已完成的功能

### 1. Cloth Config API 集成
- ✅ 添加 Cloth Config API 依赖 (v15.0.140)
- ✅ Maven 仓库配置 (maven.shedaniel.me)
- ✅ 在 `mods.toml` 中声明可选依赖

### 2. 配置界面实现
- ✅ 创建 `ClothConfigScreen.java` - 完整的配置界面
- ✅ 四个配置分类：
  - **结构设置** - 结构定位和搜索半径
  - **预生成设置** - 初始定位数量和并发限制
  - **道路设置** - 地形平滑、道路类型、高度限制
  - **装饰设置** - 路标、栏杆、秋千、长椅、凉亭

### 3. UI 集成
- ✅ 在调试地图右上角添加"配置"按钮
- ✅ 点击按钮打开 Cloth Config 配置界面
- ✅ 配置自动保存到 NeoForge 配置文件

### 4. 多语言支持
- ✅ 中文翻译 (`zh_cn.json`)
- ✅ 英文翻译 (`en_us.json`)
- ✅ 所有配置项都有详细的工具提示

## 🎮 使用方法

### 方式 1: 从调试地图打开
1. 按 `H` 键打开调试地图
2. 点击右上角的"配置"按钮
3. 在 Cloth Config 界面中修改设置
4. 点击"完成"保存配置

### 方式 2: 直接调用
```java
Minecraft.getInstance().setScreen(
    ClothConfigScreen.createConfigScreen(currentScreen)
);
```

## 📝 配置项说明

### 结构设置
- **要定位的结构**: 支持标签 (`#minecraft:village`) 或具体结构 ID
- **结构搜寻半径**: 50-200 区块，默认 100

### 预生成设置
- **初始定位数量**: 1-20 个结构，默认 7
- **同时生成道路数量上限**: 1-10 个任务，默认 3

### 道路设置
- **地形平均半径**: 0-5，默认 1
- **允许人工道路**: 石砖、石板等材料
- **允许自然道路**: 泥土、砂砾等材料
- **结构与道路距离**: 3-8 格，默认 4
- **最大高度差**: 3-10，默认 5
- **地形稳定性检查**: 2-10，默认 4

### 装饰设置
- **放置路标而非道路**: 测试模式
- **生成路边栏杆**: 间断式栏杆装饰
- **生成秋千**: 道路旁的秋千
- **生成长椅**: 休息用长椅
- **生成凉亭**: 大型凉亭结构

## 🔧 技术细节

### 依赖配置
```gradle
repositories {
    maven { 
        url "https://maven.shedaniel.me/" 
        name "Shedaniel Maven"
    }
}

dependencies {
    implementation "me.shedaniel.cloth:cloth-config-neoforge:15.0.140"
}
```

### 配置保存机制
- 配置通过 `ModConfig.SERVER` 访问
- 使用 `setSaveConsumer()` 自动保存
- 保存到 NeoForge 配置文件系统

### 界面特性
- 使用滑块控制数值范围
- 布尔值使用切换开关
- 字符串使用文本输入框
- 所有选项都有工具提示说明

## 🐛 故障排除

### 配置界面无法打开
1. 确保 Cloth Config API 已正确安装
2. 检查依赖版本是否兼容 (需要 15.0+)
3. 查看日志中的错误信息

### 配置不保存
1. 确保有写入权限
2. 检查配置文件路径是否正确
3. 重启游戏后验证配置

### 按钮不显示
1. 确保调试地图正确初始化
2. 检查 `init()` 方法是否被调用
3. 验证按钮位置计算是否正确

## 📚 相关文件

### 核心文件
- `ClothConfigScreen.java` - 配置界面实现
- `RoadDebugScreen.java` - 调试地图（包含配置按钮）
- `ModConfig.java` - 配置数据结构

### 资源文件
- `assets/roadweaver/lang/zh_cn.json` - 中文翻译
- `assets/roadweaver/lang/en_us.json` - 英文翻译
- `META-INF/neoforge.mods.toml` - 模组元数据

### 构建文件
- `neoforge/build.gradle` - Gradle 配置

## 🚀 下一步

1. **测试配置界面**
   ```bash
   .\gradlew.bat :neoforge:runClient
   ```

2. **验证功能**
   - 打开调试地图 (H 键)
   - 点击配置按钮
   - 修改各项设置
   - 确认配置保存

3. **可选增强**
   - 添加配置重置按钮
   - 添加配置导入/导出功能
   - 添加配置预设模板
