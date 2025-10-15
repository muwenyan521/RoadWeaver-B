package net.countered.settlementroads.helpers;

import com.mojang.datafixers.util.Pair;
import net.countered.settlementroads.config.ConfigProvider;
import net.countered.settlementroads.config.IModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步结构定位管理器
 * 参考 MC-249136 的实现，将耗时的 locateStructure 操作移到异步线程
 */
public class AsyncStructureLocator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    
    // 多线程执行器，支持并行搜索多个结构
    // 线程池大小从配置文件读取
    private static ExecutorService EXECUTOR = createExecutor();
    
    /**
     * 创建新的执行器
     */
    private static ExecutorService createExecutor() {
        IModConfig config = ConfigProvider.get();
        
        // 检查是否启用异步搜索
        if (!config.enableAsyncStructureSearch()) {
            LOGGER.info("RoadWeaver: 异步结构搜索已禁用（配置项 enableAsyncStructureSearch = false）");
            return null;
        }
        
        int threadPoolSize = config.structureSearchThreads();
        
        LOGGER.info("RoadWeaver: 创建结构搜索线程池，大小: {}", threadPoolSize);
        
        return Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread thread = new Thread(r, "RoadWeaver-StructureLocator-" + System.currentTimeMillis());
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
            // 只在成功创建执行器时才打印日志
            if (EXECUTOR != null) {
                LOGGER.info("RoadWeaver: 异步结构定位器已重启");
            }
        }
    }
    
    // 存储异步搜索任务的结果
    // Key: 任务ID, Value: 搜索结果
    public static final ConcurrentHashMap<String, StructureLocateResult> LOCATE_RESULTS = new ConcurrentHashMap<>();
    
    // 正在进行的搜索任务计数
    private static int pendingTaskCount = 0;
    
    /**
     * 异步搜索结构
     * 
     * @param level 服务器世界
     * @param targetStructures 目标结构集合
     * @param center 搜索中心点
     * @param radius 搜索半径
     * @param taskId 任务ID（用于追踪结果）
     */
    public static void locateStructureAsync(
            ServerLevel level,
            HolderSet<Structure> targetStructures,
            BlockPos center,
            int radius,
            String taskId) {
        
        // 确保执行器可用
        ensureExecutorAvailable();
        
        // 如果异步搜索被禁用，直接在主线程执行
        if (EXECUTOR == null) {
            LOGGER.debug("RoadWeaver: 在主线程执行结构搜索任务 {} (异步已禁用)", taskId);
            executeStructureSearch(level, targetStructures, center, radius, taskId);
            return;
        }
        
        synchronized (AsyncStructureLocator.class) {
            pendingTaskCount++;
        }
        
        LOGGER.debug("RoadWeaver: 提交异步结构搜索任务 {} (中心: {}, 半径: {}, 待处理: {})", 
            taskId, center, radius, pendingTaskCount);
        
        EXECUTOR.submit(() -> {
            executeStructureSearch(level, targetStructures, center, radius, taskId);
        });
    }
    
    /**
     * 执行结构搜索（可在主线程或异步线程执行）
     */
    private static void executeStructureSearch(
            ServerLevel level,
            HolderSet<Structure> targetStructures,
            BlockPos center,
            int radius,
            String taskId) {
        try {
                LOGGER.debug("RoadWeaver: 开始执行结构搜索任务 {}", taskId);
                
                // 执行耗时的结构定位操作
                Pair<BlockPos, Holder<Structure>> result = level.getChunkSource()
                        .getGenerator()
                        .findNearestMapStructure(level, targetStructures, center, radius, true);
                
                if (result != null) {
                    BlockPos structurePos = result.getFirst();
                    Holder<Structure> structureHolder = result.getSecond();
                    
                    String structureId = structureHolder.unwrapKey()
                            .map(key -> key.location().toString())
                            .orElse("unknown");
                    
                    StructureLocateResult locateResult = new StructureLocateResult(
                        structurePos, 
                        structureId, 
                        true
                    );
                    
                    LOCATE_RESULTS.put(taskId, locateResult);
                    LOGGER.info("RoadWeaver: ✅ 结构搜索成功 {} - 找到 {} 于 {}", 
                        taskId, structureId, structurePos);
                } else {
                    // 未找到结构
                    StructureLocateResult locateResult = new StructureLocateResult(
                        null, 
                        null, 
                        true
                    );
                    LOCATE_RESULTS.put(taskId, locateResult);
                    LOGGER.info("RoadWeaver: ⚠️ 结构搜索完成 {} - 未找到结构", taskId);
                }
        } catch (Exception e) {
            LOGGER.error("RoadWeaver: ❌ 结构搜索异常 {}: {}", taskId, e.getMessage(), e);
            
            // 标记为失败
            StructureLocateResult errorResult = new StructureLocateResult(
                null, 
                null, 
                true
            );
            LOCATE_RESULTS.put(taskId, errorResult);
        } finally {
            // 只在异步模式下减少计数器
            if (EXECUTOR != null) {
                synchronized (AsyncStructureLocator.class) {
                    pendingTaskCount--;
                }
                LOGGER.debug("RoadWeaver: 结构搜索任务 {} 完成 (剩余待处理: {})", 
                    taskId, pendingTaskCount);
            }
        }
    }
    
    /**
     * 检查任务是否完成
     */
    public static boolean isTaskComplete(String taskId) {
        StructureLocateResult result = LOCATE_RESULTS.get(taskId);
        return result != null && result.completed();
    }
    
    /**
     * 获取任务结果并移除
     */
    public static StructureLocateResult getAndRemoveResult(String taskId) {
        return LOCATE_RESULTS.remove(taskId);
    }
    
    /**
     * 获取待处理任务数量
     */
    public static int getPendingTaskCount() {
        return pendingTaskCount;
    }
    
    /**
     * 清理所有结果
     */
    public static void clearAllResults() {
        LOCATE_RESULTS.clear();
        LOGGER.debug("RoadWeaver: 清理所有异步结构搜索结果");
    }
    
    /**
     * 关闭执行器
     */
    public static synchronized void shutdown() {
        if (EXECUTOR != null && !EXECUTOR.isShutdown()) {
            EXECUTOR.shutdown(); // 优雅关闭，等待当前任务完成
            try {
                // 等待最多 5 秒让任务完成
                if (!EXECUTOR.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
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
    
    /**
     * 结构定位结果
     */
    public record StructureLocateResult(
        BlockPos position,      // 结构位置（null 表示未找到）
        String structureId,     // 结构ID
        boolean completed       // 是否完成
    ) {}
}
