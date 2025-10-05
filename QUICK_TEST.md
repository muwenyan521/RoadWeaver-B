# 🧪 RoadWeaver 配置界面快速测试指南

## ✅ 修复内容

### 问题 1：调试地图中没有配置按钮
**原因**：`render()` 方法缺少 `super.render()` 调用，导致按钮不显示

### 问题 2：按钮模糊、点不到、太大
**原因**：
- 渲染顺序错误，按钮被其他UI元素遮挡
- 按钮尺寸过大，占用空间太多
- 鼠标事件穿透，点击按钮会触发地图操作

### 解决方案
1. ✅ **完全驱除模糊**：使用 PoseStack 将按钮提升到 Z=100 层
2. ✅ **修复点击穿透**：所有鼠标事件优先处理 `super` 方法
3. ✅ 缩小按钮尺寸：宽度 50px，高度 16px（原来 80x20）
4. ✅ 优化按钮位置：右上角 `(width-58, 8)`，紧凑布局

### 技术细节
```java
// 渲染：使用 PoseStack 提升层级
ctx.pose().pushPose();
ctx.pose().translate(0, 0, 100); // Z轴提升
super.render(ctx, mouseX, mouseY, delta);
ctx.pose().popPose();

// 鼠标事件：优先处理按钮
public boolean mouseClicked(...) {
    if (super.mouseClicked(...)) return true; // 按钮优先
    // 然后处理地图点击
}
```

## 🎮 测试步骤

### 1. 编译模组
```bash
.\gradlew.bat :neoforge:build
```

### 2. 运行客户端
```bash
.\gradlew.bat :neoforge:runClient
```

### 3. 测试配置界面
1. **进入游戏**
2. **按 H 键** 打开调试地图
3. **查看右上角** - 应该能看到"配置"按钮
4. **点击配置按钮** - 打开 Cloth Config 配置界面
5. **修改配置** - 测试各项设置
6. **点击完成** - 保存配置

## 🔍 配置按钮位置

```
┌──────────────────────────────────────┐
│  RoadWeaver - 调试地图      [配置] ← │  小巧紧凑
│                                      │
│                                      │
│         (地图内容区域)                │
│                                      │
│                                      │
└──────────────────────────────────────┘
```

**按钮规格**：
- 宽度：50px（紧凑）
- 高度：16px（不占空间）
- 位置：右上角 (width-58, 8)
- 样式：清晰可点击，不遮挡地图

## 📋 配置界面功能检查清单

### 结构设置
- [ ] 要定位的结构（文本输入）
- [ ] 结构搜寻半径（滑块 50-200）

### 预生成设置
- [ ] 初始定位数量（滑块 1-20）
- [ ] 同时生成道路数量上限（滑块 1-10）

### 道路设置
- [ ] 地形平均半径（滑块 0-5）
- [ ] 允许人工道路（开关）
- [ ] 允许自然道路（开关）
- [ ] 结构与道路距离（滑块 3-8）
- [ ] 最大高度差（滑块 3-10）
- [ ] 地形稳定性检查（滑块 2-10）

### 装饰设置
- [ ] 放置路标而非道路（开关）
- [ ] 生成路边栏杆（开关）
- [ ] 生成秋千（开关）
- [ ] 生成长椅（开关）
- [ ] 生成凉亭（开关）

## 🐛 故障排除

### 按钮不显示
- ✅ 已修复：添加了 `super.render()` 调用
- 检查：确保 `init()` 方法被调用
- 检查：查看控制台是否有错误

### 点击按钮无反应
- 检查：Cloth Config API 是否正确加载
- 检查：依赖版本 `cloth-config-neoforge:15.0.140`
- 查看：游戏日志中的异常信息

### 配置不保存
- 检查：配置文件权限
- 检查：`ModConfig.SERVER` 是否正确初始化
- 重启游戏验证

## 📝 关键代码修改

### RoadDebugScreen.java
```java
@Override
public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    // ... 其他渲染代码 ...
    
    // 渲染按钮和其他 widgets（新增）
    super.render(ctx, mouseX, mouseY, delta);
    
    updateHoveredStructure(mouseX, mouseY);
}

@Override
protected void init() {
    super.init();
    
    // 在右上角添加配置按钮（避开标题区域）
    int buttonWidth = 80;
    int buttonHeight = 20;
    int buttonX = this.width - buttonWidth - 15;
    int buttonY = 35; // 在标题下方
    
    this.configButton = Button.builder(
            Component.translatable("gui.roadweaver.config"),
            button -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(ClothConfigScreen.createConfigScreen(this));
                }
            })
            .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
            .build();
    
    this.addRenderableWidget(this.configButton);
}
```

## ✨ 预期效果

1. **调试地图打开** → 右上角显示"配置"按钮
2. **点击配置按钮** → 打开 Cloth Config 界面
3. **修改配置** → 实时预览和保存
4. **点击完成** → 返回调试地图
5. **配置生效** → 新的道路生成使用新配置

## 🎯 测试重点

- ✅ 按钮是否可见
- ✅ 按钮是否可点击
- ✅ 配置界面是否正确打开
- ✅ 所有配置项是否显示
- ✅ 配置是否正确保存
- ✅ 中英文翻译是否正确
- ✅ 工具提示是否显示

测试完成后，配置界面应该完全可用！
