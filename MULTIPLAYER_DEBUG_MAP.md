# 多人服务器调试地图功能实现说明

## 📦 新增文件

### Common模块
1. **`network/RoadWeaverNetworkManager.java`** - 网络管理器
   - 注册C2S和S2C数据包处理器
   - 环境检测：S2C仅在客户端注册

2. **`network/DebugDataPacket.java`** - 数据包编解码
   - 结构信息：位置 + 类型ID
   - 连接信息：起点 + 终点 + 状态 + 手动标记
   - 道路信息：宽度 + 类型 + 段列表（每段包含中点位置）

3. **`network/PacketHandler.java`** - 业务逻辑处理
   - 服务器端：获取世界数据并发送
   - 客户端：接收并缓存数据

## 🔧 修改文件

### Fabric
- `SettlementRoads.java` - 在初始化时注册网络包
- `client/SettlementRoadsClient.java` - 多人服务器支持

### Forge
- `SettlementRoads.java` - 在commonSetup中注册网络包
- `client/SettlementRoadsClient.java` - 多人服务器支持

### 语言文件（Fabric & Forge）
- `roadweaver.debug_map.loading` - 加载提示
- `roadweaver.debug_map.timeout` - 超时提示

## 🎮 使用方式

### 单人游戏
按H键直接打开调试地图

### 多人服务器
1. **普通玩家**：显示"没有权限"
2. **管理员（OP≥2）**：
   - 按H键发送请求
   - 等待数据加载（最多5秒）
   - 自动打开调试地图

## 🔐 权限要求
- OP等级 ≥ 2
- 命令：`/op <玩家名>`

## 📊 数据流程
```
客户端 → REQUEST_DEBUG_DATA → 服务器
服务器 → 权限验证 → 获取数据
服务器 → SEND_DEBUG_DATA → 客户端
客户端 → 解码数据 → 打开界面
```

## ⚠️ 注意事项

### 环境隔离
- S2C接收器仅在客户端环境注册
- 避免服务器端注册客户端专用代码

### 数据优化
- 道路段只传输中点位置（用于绘制路径）
- 不传输每段的详细方块列表（减少网络负载）
- 不传输材料列表（客户端仅用于可视化）

### 超时处理
- 5秒超时机制
- 使用异步线程等待，不阻塞主线程

## 🐛 已解决的问题

1. ✅ 文件名错误（NetworkManager.java → RoadWeaverNetworkManager.java）
2. ✅ Records类字段访问方法不匹配
3. ✅ Forge环境下S2C注册导致NoSuchMethodError
4. ✅ 数据包编解码与Records定义对齐
5. ✅ 道路数据传输问题（需要传输段列表才能显示道路）

## 🧪 测试清单

- [ ] Fabric单人游戏
- [ ] Fabric多人服务器（管理员）
- [ ] Fabric多人服务器（普通玩家）
- [ ] Forge单人游戏
- [ ] Forge多人服务器（管理员）
- [ ] Forge多人服务器（普通玩家）
- [ ] 网络延迟测试
- [ ] 超时处理测试
