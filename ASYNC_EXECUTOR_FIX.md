# 异步执行器重启修复

## 🐛 问题描述

### 错误信息
```
java.util.concurrent.RejectedExecutionException: Task rejected from 
ThreadPoolExecutor[Terminated, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 72]
```

### 触发场景
1. 玩家进入世界 → 异步结构搜索正常工作
2. 玩家退出世界 → `AsyncStructureLocator.shutdown()` 关闭执行器
3. 玩家重新加载世界 → 尝试提交任务到**已关闭**的执行器
4. **崩溃** → `RejectedExecutionException`

### 根本原因
`ExecutorService` 一旦调用 `shutdown()` 或 `shutdownNow()` 后，就会进入 `SHUTDOWN` 或 `TERMINATED` 状态，**无法再接受新任务**。

原有代码在服务器停止时关闭了执行器，但没有在重新启动时重新创建执行器。

## 🔧 解决方案

### 1. 添加执行器重启机制

#### 修改前
```java
// 单线程执行器，避免过多并发搜索导致性能问题
private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
    Thread thread = new Thread(r, "RoadWeaver-StructureLocator");
    thread.setDaemon(true);
    return thread;
});
```

#### 修改后
```java
// 单线程执行器，避免过多并发搜索导致性能问题
private static ExecutorService EXECUTOR = createExecutor();

/**
 * 创建新的执行器
 */
private static ExecutorService createExecutor() {
    return Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "RoadWeaver-StructureLocator");
        thread.setDaemon(true);
        return thread;
    });
}

/**
 * 确保执行器可用，如果已关闭则重新创建
 */
private static synchronized void ensureExecutorAvailable() {
    if (EXECUTOR == null || EXECUTOR.isShutdown() || EXECUTOR.isTerminated()) {
        EXECUTOR = createExecutor();
        LOGGER.info("RoadWeaver: 异步结构定位器已重启");
    }
}
```

**关键改动**：
- 将 `EXECUTOR` 从 `final` 改为可变
- 添加 `createExecutor()` 工厂方法
- 添加 `ensureExecutorAvailable()` 检查和重启方法

### 2. 在提交任务前检查执行器状态

```java
public static void locateStructureAsync(...) {
    // 确保执行器可用
    ensureExecutorAvailable();
    
    synchronized (AsyncStructureLocator.class) {
        pendingTaskCount++;
    }
    
    EXECUTOR.submit(() -> {
        // 异步搜索逻辑
    });
}
```

### 3. 优雅关闭执行器

#### 修改前
```java
public static void shutdown() {
    EXECUTOR.shutdownNow(); // 强制立即关闭
    LOCATE_RESULTS.clear();
    LOGGER.info("RoadWeaver: 异步结构定位器已关闭");
}
```

#### 修改后
```java
public static synchronized void shutdown() {
    if (EXECUTOR != null && !EXECUTOR.isShutdown()) {
        EXECUTOR.shutdown(); // 优雅关闭，等待当前任务完成
        try {
            // 等待最多 5 秒让任务完成
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow(); // 强制关闭
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    LOCATE_RESULTS.clear();
    LOGGER.info("RoadWeaver: 异步结构定位器已关闭");
}
```

**改进**：
- 先调用 `shutdown()` 优雅关闭（等待任务完成）
- 等待最多 5 秒
- 如果超时，再调用 `shutdownNow()` 强制关闭
- 处理 `InterruptedException`

## 📊 工作流程

### 修复前（有 Bug）
```
世界加载 → 创建 EXECUTOR → 提交任务 ✅
    ↓
世界卸载 → shutdown() → EXECUTOR [TERMINATED]
    ↓
世界重新加载 → 提交任务 → ❌ RejectedExecutionException
```

### 修复后（正常）
```
世界加载 → 创建 EXECUTOR → 提交任务 ✅
    ↓
世界卸载 → shutdown() → EXECUTOR [TERMINATED]
    ↓
世界重新加载 → ensureExecutorAvailable() → 重新创建 EXECUTOR → 提交任务 ✅
```

## ✅ 验证测试

### 测试步骤
1. 启动游戏，创建新世界
2. 观察日志：异步搜索任务正常提交和完成
3. 退出世界（返回主菜单）
4. 重新加载同一个世界
5. 观察日志：应该看到 "异步结构定位器已重启"
6. 验证异步搜索继续正常工作

### 预期日志
```
[Server thread/INFO] RoadWeaver: 世界 minecraft:overworld 已加载
[Server thread/INFO] Initializing world with 50 structures
[Server thread/INFO] RoadWeaver: 已提交 1 个异步结构搜索任务 (x50)
[RoadWeaver-StructureLocator/INFO] RoadWeaver: ✅ 结构搜索成功 ...

// 退出世界
[Server thread/INFO] RoadWeaver: 异步结构定位器已关闭

// 重新加载世界
[Server thread/INFO] RoadWeaver: 世界 minecraft:overworld 已加载
[Server thread/INFO] RoadWeaver: 异步结构定位器已重启  ← 新增日志
[Server thread/INFO] RoadWeaver: 已提交 1 个异步结构搜索任务 (x50)
[RoadWeaver-StructureLocator/INFO] RoadWeaver: ✅ 结构搜索成功 ...
```

## 🔍 技术细节

### ExecutorService 状态转换
```
NEW → RUNNING → SHUTDOWN → TERMINATED
                     ↑
                     └─ shutdown() 调用后不再接受新任务
```

### 为什么需要 synchronized
```java
private static synchronized void ensureExecutorAvailable() {
    if (EXECUTOR == null || EXECUTOR.isShutdown() || EXECUTOR.isTerminated()) {
        EXECUTOR = createExecutor();
    }
}
```

**原因**：
- 多个线程可能同时调用 `locateStructureAsync()`
- 需要确保只创建一个新的 `EXECUTOR` 实例
- 避免竞态条件（race condition）

### 为什么使用 shutdown() 而不是 shutdownNow()

| 方法 | 行为 | 优点 | 缺点 |
|------|------|------|------|
| `shutdown()` | 等待当前任务完成 | 优雅，不丢失数据 | 可能需要等待 |
| `shutdownNow()` | 立即中断所有任务 | 快速 | 可能丢失数据 |

**选择**：先尝试 `shutdown()`，超时后再 `shutdownNow()`，兼顾优雅和效率。

## 🎯 其他改进建议

### 1. 添加健康检查
```java
public static boolean isHealthy() {
    return EXECUTOR != null && !EXECUTOR.isShutdown() && !EXECUTOR.isTerminated();
}
```

### 2. 添加任务队列大小限制
```java
private static final int MAX_PENDING_TASKS = 100;

public static void locateStructureAsync(...) {
    ensureExecutorAvailable();
    
    if (pendingTaskCount >= MAX_PENDING_TASKS) {
        LOGGER.warn("RoadWeaver: 异步任务队列已满，跳过本次搜索");
        return;
    }
    
    // 提交任务
}
```

### 3. 添加任务超时机制
```java
Future<?> future = EXECUTOR.submit(() -> {
    // 搜索逻辑
});

// 在另一个地方检查超时
if (!future.isDone()) {
    future.cancel(true);
}
```

## 📝 相关文件

- `AsyncStructureLocator.java` - 主要修改文件
- `ModEventHandler.java` - 调用 `shutdown()` 的地方
- `StructureLocatorImpl.java` - 调用 `locateStructureAsync()` 的地方

## 🙏 参考资料

- [Java ExecutorService 文档](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html)
- [Graceful Shutdown of ExecutorService](https://www.baeldung.com/java-executor-service-tutorial)
- [MC-249136 Fix](https://github.com/fennifith/MC-249136) - 原始参考项目

---

**修复日期**: 2025-01-15  
**版本**: RoadWeaver 1.0.3+  
**状态**: ✅ 已修复
