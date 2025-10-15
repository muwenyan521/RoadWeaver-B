# 配置同步系统

## 🎯 目标

确保在多人游戏中，**所有玩家使用服务端的配置**，而不是各自的本地配置。

## ❌ 原有问题

### 问题场景

```
服务器配置：
- structureBatchSize = 10
- structureSearchThreads = 6

客户端 A 配置：
- structureBatchSize = 5
- structureSearchThreads = 3

客户端 B 配置：
- structureBatchSize = 20
- structureSearchThreads = 1
```

**问题**：
- ❌ 每个客户端读取自己的本地配置
- ❌ 可能导致不一致的行为
- ❌ 服务端配置被忽略

## ✅ 解决方案

### 配置同步机制

```
1. 服务端启动
   ↓
2. 读取服务端配置文件
   ↓
3. 客户端连接服务器
   ↓
4. 服务端发送配置到客户端（通过网络包）
   ↓
5. 客户端接收并缓存服务端配置
   ↓
6. 客户端使用服务端配置（而不是本地配置）
   ↓
7. 客户端断开连接
   ↓
8. 清空缓存的服务端配置
```

## 🔧 实现细节

### 1. ServerConfigHolder

管理服务端配置的同步状态：

```java
public class ServerConfigHolder {
    // 是否在多人游戏的客户端
    private static boolean isMultiplayerClient = false;
    
    // 从服务端同步的配置值
    private static final Map<String, Object> syncedConfig = new HashMap<>();
    
    // 设置多人游戏状态
    public static void setMultiplayerClient(boolean value);
    
    // 从服务端同步配置
    public static void syncFromServer(Map<String, Object> config);
    
    // 获取同步的配置值
    public static <T> T getSynced(String key, T defaultValue);
    
    // 打包配置为 Map（用于发送）
    public static Map<String, Object> packConfig(IModConfig config);
}
```

### 2. SyncedConfigWrapper

配置包装器，自动选择使用本地或同步配置：

```java
public class SyncedConfigWrapper implements IModConfig {
    private final IModConfig localConfig;
    
    @Override
    public int structureBatchSize() {
        // 多人游戏客户端：使用同步配置
        // 单人游戏/服务端：使用本地配置
        return ServerConfigHolder.getSynced(
            "structureBatchSize", 
            localConfig.structureBatchSize()
        );
    }
    
    // ... 其他配置项同理
}
```

### 3. ConfigProvider 修改

使用包装器替代直接返回本地配置：

```java
// Fabric
public class ConfigProviderImpl {
    private static final IModConfig INSTANCE = 
        new SyncedConfigWrapper(new FabricModConfigAdapter());
    
    public static IModConfig get() {
        return INSTANCE;
    }
}

// Forge
public class ConfigProviderImpl {
    private static final IModConfig INSTANCE = 
        new SyncedConfigWrapper(new ForgeModConfigAdapter());
    
    public static IModConfig get() {
        return INSTANCE;
    }
}
```

## 📊 工作流程

### 单人游戏

```
玩家启动单人世界
  ↓
ServerConfigHolder.isMultiplayerClient = false
  ↓
ConfigProvider.get() 返回 SyncedConfigWrapper
  ↓
SyncedConfigWrapper.structureBatchSize()
  ↓
ServerConfigHolder.getSynced("structureBatchSize", localConfig.structureBatchSize())
  ↓
isMultiplayerClient = false → 返回 localConfig.structureBatchSize()
  ↓
使用本地配置 ✅
```

### 多人游戏（客户端）

```
玩家连接服务器
  ↓
服务端发送配置包
  ↓
客户端接收：ServerConfigHolder.syncFromServer(serverConfig)
  ↓
ServerConfigHolder.setMultiplayerClient(true)
  ↓
ConfigProvider.get() 返回 SyncedConfigWrapper
  ↓
SyncedConfigWrapper.structureBatchSize()
  ↓
ServerConfigHolder.getSynced("structureBatchSize", localConfig.structureBatchSize())
  ↓
isMultiplayerClient = true → 返回 syncedConfig.get("structureBatchSize")
  ↓
使用服务端配置 ✅
```

### 多人游戏（服务端）

```
服务器启动
  ↓
ServerConfigHolder.isMultiplayerClient = false
  ↓
ConfigProvider.get() 返回 SyncedConfigWrapper
  ↓
SyncedConfigWrapper.structureBatchSize()
  ↓
ServerConfigHolder.getSynced("structureBatchSize", localConfig.structureBatchSize())
  ↓
isMultiplayerClient = false → 返回 localConfig.structureBatchSize()
  ↓
使用服务端本地配置 ✅
```

## 🌐 网络同步（待实现）

### 需要添加的网络包

```java
// 服务端 → 客户端：同步配置
public class ConfigSyncPacket {
    private final Map<String, Object> config;
    
    // 服务端发送
    public static void sendToClient(ServerPlayer player) {
        IModConfig config = ConfigProvider.get();
        Map<String, Object> configMap = ServerConfigHolder.packConfig(config);
        // 发送网络包...
    }
    
    // 客户端接收
    public static void handleOnClient(Map<String, Object> config) {
        ServerConfigHolder.syncFromServer(config);
        ServerConfigHolder.setMultiplayerClient(true);
    }
}
```

### 触发时机

1. **玩家加入服务器**
   ```java
   @SubscribeEvent
   public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
       if (event.getEntity() instanceof ServerPlayer player) {
           ConfigSyncPacket.sendToClient(player);
       }
   }
   ```

2. **玩家离开服务器**
   ```java
   @SubscribeEvent
   public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
       // 客户端侧
       ServerConfigHolder.setMultiplayerClient(false);
   }
   ```

3. **配置热重载**
   ```java
   public void onConfigReload() {
       // 服务端：重新发送配置到所有在线玩家
       for (ServerPlayer player : server.getPlayerList().getPlayers()) {
           ConfigSyncPacket.sendToClient(player);
       }
   }
   ```

## 📝 配置项列表

需要同步的所有配置项：

### 结构配置
- `structuresToLocate` (List<String>)
- `structureSearchRadius` (int)

### 预生成配置
- `initialLocatingCount` (int)
- `maxConcurrentRoadGeneration` (int)
- `structureSearchTriggerDistance` (int)
- `structureBatchSize` (int)
- `structureSearchThreads` (int)

### 道路配置
- `averagingRadius` (int)
- `allowArtificial` (boolean)
- `allowNatural` (boolean)
- `placeWaypoints` (boolean)
- `placeRoadFences` (boolean)
- `placeSwings` (boolean)
- `placeBenches` (boolean)
- `placeGloriettes` (boolean)
- `structureDistanceFromRoad` (int)
- `maxHeightDifference` (int)
- `maxTerrainStability` (int)

### 手动模式配置
- `manualMaxHeightDifference` (int)
- `manualMaxTerrainStability` (int)
- `manualIgnoreWater` (boolean)

**总计**: 21 个配置项

## ✅ 优势

1. **✅ 配置一致性**
   - 所有玩家使用相同配置
   - 避免不一致行为

2. **✅ 服务器控制**
   - 服务器管理员完全控制配置
   - 客户端无法绕过服务器设置

3. **✅ 向后兼容**
   - 单人游戏不受影响
   - 使用本地配置

4. **✅ 自动清理**
   - 离开服务器时自动清空同步配置
   - 不会污染本地配置

## 🔄 当前状态

### ✅ 已完成
- [x] ServerConfigHolder 类
- [x] SyncedConfigWrapper 类
- [x] ConfigProvider 修改（Fabric & Forge）
- [x] 配置打包方法

### ⏳ 待实现
- [ ] 网络包实现（ConfigSyncPacket）
- [ ] 玩家加入/离开事件处理
- [ ] 配置热重载支持
- [ ] 测试多人游戏同步

## 📚 使用示例

### 服务端代码（自动使用本地配置）

```java
// 在 ServerLevel 中调用
IModConfig config = ConfigProvider.get();
int batchSize = config.structureBatchSize();
// → 服务端：读取服务端本地配置
```

### 客户端代码（自动使用同步配置）

```java
// 在客户端连接服务器后
IModConfig config = ConfigProvider.get();
int batchSize = config.structureBatchSize();
// → 客户端：使用从服务端同步的配置
```

### 检查同步状态

```java
if (ServerConfigHolder.isMultiplayerClient()) {
    // 当前在多人游戏客户端
    // 使用服务端同步的配置
} else {
    // 单人游戏或服务端
    // 使用本地配置
}
```

---

**实现日期**: 2025-01-15  
**状态**: 🟡 部分完成（核心逻辑已实现，网络同步待实现）  
**优先级**: 高（多人游戏必需）
