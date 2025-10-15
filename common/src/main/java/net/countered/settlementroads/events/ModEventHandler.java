package net.countered.settlementroads.events;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.countered.settlementroads.config.ConfigProvider;
import net.countered.settlementroads.config.IModConfig;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.countered.settlementroads.features.roadlogic.Road;
import net.countered.settlementroads.features.roadlogic.RoadPathCalculator;
import net.countered.settlementroads.helpers.AsyncStructureLocator;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureConnector;
import net.countered.settlementroads.helpers.StructureLocator;
import net.countered.settlementroads.helpers.StructureLocatorImpl;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;


/**
 * 使用 Architectury 事件的通用事件处理器（Common）。
 * 平台端无需各自实现，主类直接调用 ModEventHandler.register() 即可。
 */
public class ModEventHandler {

    // 使用CPU核心数的2倍作为线程池大小，避免资源浪费
    // 在大多数机器上，这会创建8-32个线程，而非128个
    private static final int THREAD_COUNT = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    private static ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    private static final ConcurrentHashMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();
    
    // 添加初始化延迟机制
    private static final ConcurrentHashMap<String, Integer> worldInitDelay = new ConcurrentHashMap<>();
    private static final int INIT_DELAY_TICKS = 100; // 5秒延迟，确保注册表完全加载

    /**
     * 注册模组事件处理器
     * <p>
     * 此方法应在模组初始化时调用一次，用于注册所有必要的生命周期事件监听器。
     * 包括世界加载/卸载、服务器Tick和服务器停止事件。
     * </p>
     * <p>
     * 事件处理流程：
     * <ol>
     *   <li>世界加载时：初始化延迟机制，恢复未完成任务，必要时搜索结构</li>
     *   <li>世界卸载时：取消运行中的任务，清理队列</li>
     *   <li>服务器Tick时：处理异步结构搜索结果，尝试生成新道路</li>
     *   <li>服务器停止时：清理缓存，关闭线程池</li>
     * </ol>
     * </p>
     * 
     * @see #onWorldLoad(ServerLevel)
     * @see #tryGenerateNewRoads(ServerLevel, Boolean, int)
     */
    public static void register() {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();

        // 世界加载
        LifecycleEvent.SERVER_LEVEL_LOAD.register(ModEventHandler::onWorldLoad);

        // 世界卸载
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(level -> {
            if (!level.dimension().equals(Level.OVERWORLD)) return;
            String worldKey = level.dimension().location().toString();
            Future<?> task = runningTasks.remove(worldKey);
            if (task != null && !task.isDone()) {
                task.cancel(true);
                LOGGER.debug("Aborted running road task for world: {}", level.dimension().location());
            }
            // 清理延迟计数器和队列
            worldInitDelay.remove(worldKey);
            StructureConnector.clearQueueForWorld(level);
            // 清理虚拟结构记录
            VirtualStructureManager.clearVirtualStructures(worldKey);
        });

        // 服务器 Tick（遍历所有世界）
        TickEvent.SERVER_PRE.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.dimension().equals(Level.OVERWORLD)) {
                    // 处理异步结构搜索结果
                    processAsyncStructureResults(level);
                    // 尝试生成新道路
                    tryGenerateNewRoads(level, true, 5000);
                }
            }
        });

        // 服务器停止
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            RoadPathCalculator.heightCache.clear();
            runningTasks.values().forEach(future -> future.cancel(true));
            runningTasks.clear();
            executor.shutdownNow();
            // 关闭异步结构定位器
            AsyncStructureLocator.shutdown();
            LOGGER.debug("RoadWeaver: ExecutorService shut down.");
        });
    }

    /**
     * 处理世界加载事件
     * <p>
     * 世界加载时执行以下操作：
     * <ul>
     *   <li>重启线程池（如果已关闭）</li>
     *   <li>设置初始化延迟计数器（{@value #INIT_DELAY_TICKS} ticks）</li>
     *   <li>恢复未完成的道路生成任务</li>
     *   <li>如果结构数量不足，提交异步搜索任务</li>
     * </ul>
     * </p>
     * 
     * @param level 加载的服务器世界
     */
    private static void onWorldLoad(ServerLevel level) {
        restartExecutorIfNeeded();
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        // 初始化世界延迟计数器，确保注册表完全加载后再开始生成
        String worldKey = level.dimension().location().toString();
        worldInitDelay.put(worldKey, INIT_DELAY_TICKS);
        LOGGER.info("RoadWeaver: 世界 {} 已加载，将在 {} ticks 后开始道路生成", worldKey, INIT_DELAY_TICKS);

        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(level);

        // 恢复未完成的道路生成任务
        restoreUnfinishedRoads(level);

        IModConfig config = ConfigProvider.get();
        if (structureLocationData.structureLocations().size() < config.initialLocatingCount()) {
            int needed = config.initialLocatingCount() - structureLocationData.structureLocations().size();
            LOGGER.info("RoadWeaver: 初始化世界，需要搜索 {} 个结构（使用异步搜索）", needed);
            
            // 使用异步搜索，避免阻塞主线程和缓存溢出
            // 轮询机制会自动分散搜索，每次只搜索一种结构
            StructureLocator.locateConfiguredStructure(level, needed, false);
            
            LOGGER.info("RoadWeaver: 已提交 {} 个异步结构搜索任务", needed);
        }
    }

    /**
     * 尝试生成新的道路
     * 职责：协调道路生成的整体流程
     */
    private static void tryGenerateNewRoads(ServerLevel level, Boolean async, int steps) {
        String worldKey = level.dimension().location().toString();
        
        // 检查是否仍在初始化延迟期
        if (!checkInitializationDelay(worldKey)) {
            return;
        }
        
        IModConfig config = ConfigProvider.get();
        
        // 清理已完成的任务
        cleanupCompletedTasks();
        
        // 检查并发限制
        if (!checkConcurrencyLimit(config)) {
            return;
        }
        
        // 尝试从队列获取并处理道路连接
        processNextRoadConnection(level, async, steps, config);
    }
    
    /**
     * 检查世界初始化延迟
     * @return true 如果可以继续生成，false 如果仍在延迟期
     */
    private static boolean checkInitializationDelay(String worldKey) {
        Integer delayTicks = worldInitDelay.get(worldKey);
        if (delayTicks != null) {
            if (delayTicks > 0) {
                worldInitDelay.put(worldKey, delayTicks - 1);
                return false; // 还在延迟期内
            } else {
                // 延迟结束
                worldInitDelay.remove(worldKey);
                LOGGER.info("RoadWeaver: 世界 {} 初始化延迟结束，开始道路生成", worldKey);
            }
        }
        return true;
    }
    
    /**
     * 清理已完成的任务
     */
    private static void cleanupCompletedTasks() {
        runningTasks.entrySet().removeIf(entry -> {
            Future<?> future = entry.getValue();
            if (future.isDone()) {
                try {
                    future.get(); // 检查是否有异常
                } catch (Exception e) {
                    LOGGER.warn("Task {} completed with error: {}", entry.getKey(), e.getMessage());
                }
                return true;
            }
            return false;
        });
    }
    
    /**
     * 检查是否达到并发上限
     * @return true 如果可以继续生成，false 如果已达上限
     */
    private static boolean checkConcurrencyLimit(IModConfig config) {
        int currentRunning = runningTasks.size();
        return currentRunning < config.maxConcurrentRoadGeneration();
    }
    
    /**
     * 处理队列中的下一个道路连接
     */
    private static void processNextRoadConnection(ServerLevel level, Boolean async, int steps, IModConfig config) {
        Queue<Records.StructureConnection> queue = StructureConnector.getQueueForWorld(level);
        if (queue.isEmpty()) {
            return;
        }
        
        // 窥视队列，确保资源就绪后再弹出
        Records.StructureConnection structureConnection = queue.peek();
        if (structureConnection == null) {
            return; // 并发情况下可能为 null
        }
        
        // 检查注册表是否就绪
        final RoadFeatureConfig roadConfig = getRoadFeatureConfig(level);
        if (roadConfig == null) {
            LOGGER.debug("RoadWeaver: 注册表未就绪，等待下一个 tick（队列大小: {})", queue.size());
            return;
        }
        
        // 资源就绪，弹出队列并开始生成
        queue.poll();
        logRoadGenerationStart(level, structureConnection, config);
        
        if (async) {
            submitAsyncRoadGeneration(level, structureConnection, roadConfig, steps);
        } else {
            executeSyncRoadGeneration(level, structureConnection, roadConfig, steps);
        }
    }
    
    /**
     * 记录道路生成开始日志
     */
    private static void logRoadGenerationStart(ServerLevel level, Records.StructureConnection connection, IModConfig config) {
        int currentRunning = runningTasks.size();
        Queue<Records.StructureConnection> queue = StructureConnector.getQueueForWorld(level);
        LOGGER.info("🚧 Starting road generation: {} -> {} (running: {}/{}, queue: {})",
            connection.from(), connection.to(),
            currentRunning + 1, config.maxConcurrentRoadGeneration(),
            queue.size());
    }
    
    /**
     * 提交异步道路生成任务
     * <p>
     * 创建唯一的任务ID，提交到线程池执行，并添加到运行中任务映射。
     * 任务完成后会自动从映射中移除。
     * </p>
     * 
     * @param level 服务器世界
     * @param connection 道路连接信息（起点和终点）
     * @param config 道路特性配置
     * @param steps 最大寻路步数
     */
    private static void submitAsyncRoadGeneration(ServerLevel level, Records.StructureConnection connection,
                                                   RoadFeatureConfig config, int steps) {
        String taskId = level.dimension().location().toString() + "_" + System.nanoTime();
        Future<?> future = executor.submit(() -> executeRoadGeneration(level, connection, config, steps));
        runningTasks.put(taskId, future);
    }
    
    /**
     * 执行同步道路生成
     */
    private static void executeSyncRoadGeneration(ServerLevel level, Records.StructureConnection connection,
                                                   RoadFeatureConfig config, int steps) {
        executeRoadGeneration(level, connection, config, steps);
    }
    
    /**
     * 执行道路生成的核心逻辑
     * <p>
     * 此方法执行实际的道路生成，包括：
     * <ul>
     *   <li>使用A*算法计算道路路径</li>
     *   <li>放置道路方块</li>
     *   <li>添加装饰物（路灯、标志等）</li>
     *   <li>更新世界数据</li>
     * </ul>
     * </p>
     * <p>
     * 如果生成失败，会自动标记连接为FAILED状态，避免重复尝试。
     * </p>
     * 
     * @param level 服务器世界
     * @param connection 道路连接（起点到终点）
     * @param config 道路特性配置（材料、宽度等）
     * @param steps 最大A*寻路步数
     */
    private static void executeRoadGeneration(ServerLevel level, Records.StructureConnection connection,
                                               RoadFeatureConfig config, int steps) {
        try {
            LOGGER.debug("🔨 Generating road: {} -> {}", connection.from(), connection.to());
            new Road(level, connection, config).generateRoad(steps);
            LOGGER.info("✅ Road generation completed: {} -> {}", connection.from(), connection.to());
        } catch (Exception e) {
            LOGGER.error("❌ Error generating road {} -> {}: {}",
                connection.from(), connection.to(), e.getMessage(), e);
            handleRoadGenerationFailure(level, connection);
        }
    }
    
    /**
     * 处理道路生成失败
     * <p>
     * 当道路生成过程中发生异常时调用此方法，将连接状态标记为FAILED。
     * 这样可以避免系统反复尝试生成失败的道路，浪费资源。
     * </p>
     * 
     * @param level 服务器世界
     * @param connection 失败的道路连接
     */
    private static void handleRoadGenerationFailure(ServerLevel level, Records.StructureConnection connection) {
        try {
            markConnectionAsFailed(level, connection);
        } catch (Exception ex) {
            LOGGER.error("Failed to mark connection as failed", ex);
        }
    }

    /**
     * 获取道路特性配置，包含健壮的注册表检查
     * @param level 服务器世界
     * @return 配置对象，如果注册表未就绪则返回 null
     */
    private static RoadFeatureConfig getRoadFeatureConfig(ServerLevel level) {
        try {
            // 检查注册表是否可用
            if (level.registryAccess() == null) {
                LOGGER.debug("RoadWeaver: RegistryAccess is null");
                return null;
            }
            
            var registry = level.registryAccess().registry(Registries.CONFIGURED_FEATURE);
            if (registry.isEmpty()) {
                LOGGER.debug("RoadWeaver: ConfiguredFeature registry is not available");
                return null;
            }
            
            ConfiguredFeature<?, ?> feature = registry.get().get(RoadFeature.ROAD_FEATURE_KEY);
            if (feature != null && feature.config() instanceof RoadFeatureConfig cfg) {
                LOGGER.debug("RoadWeaver: Using registered RoadFeatureConfig");
                return cfg;
            } else {
                // 使用 fallback 配置
                LOGGER.debug("RoadWeaver: ConfiguredFeature {} missing or invalid, using fallback", 
                    RoadFeature.ROAD_FEATURE_KEY.location());
                return defaultRoadConfig();
            }
        } catch (Exception e) {
            LOGGER.debug("RoadWeaver: Exception while getting RoadFeatureConfig: {}", e.getMessage());
            return null; // 注册表未就绪，返回 null 等待下一个 tick
        }
    }

    /**
     * 创建默认的道路特性配置
     * <p>
     * 当注册表中的配置无法获取时，使用此fallback配置。
     * 配置内容与datagen中的默认配置保持一致，确保功能正常。
     * </p>
     * <p>
     * 包含的配置：
     * <ul>
     *   <li>人工材料：泥砖、磨制安山岩、石砖等</li>
     *   <li>自然材料：粗泥、卵石、土径等</li>
     *   <li>道路宽度：3格</li>
     *   <li>道路质量：1-9级</li>
     * </ul>
     * </p>
     * 
     * @return 默认的道路特性配置
     */
    private static RoadFeatureConfig defaultRoadConfig() {
        // 与 datagen 中的默认配置保持一致
        List<List<BlockState>> artificialMaterials = List.of(
                List.of(Blocks.MUD_BRICKS.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState()),
                List.of(Blocks.POLISHED_ANDESITE.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState()),
                List.of(Blocks.STONE_BRICKS.defaultBlockState(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState())
        );

        List<List<BlockState>> naturalMaterials = List.of(
                List.of(Blocks.COARSE_DIRT.defaultBlockState(), Blocks.ROOTED_DIRT.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState()),
                List.of(Blocks.COBBLESTONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState()),
                List.of(Blocks.DIRT_PATH.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState())
        );

        List<Integer> widths = List.of(3);
        List<Integer> qualities = List.of(1,2,3,4,5,6,7,8,9);
        return new RoadFeatureConfig(artificialMaterials, naturalMaterials, widths, qualities);
    }

    /**
     * 标记连接为失败状态
     * <p>
     * 更新世界数据中的连接状态为FAILED，避免系统重复尝试生成失败的道路。
     * 支持双向连接检查（A→B 和 B→A）。
     * </p>
     * 
     * @param level 服务器世界
     * @param structureConnection 需要标记为失败的连接
     */
    private static void markConnectionAsFailed(ServerLevel level, Records.StructureConnection structureConnection) {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> connections = dataProvider.getStructureConnections(level);
        List<Records.StructureConnection> mutableConnections = new ArrayList<>(connections != null ? connections : new ArrayList<>());
        
        for (int i = 0; i < mutableConnections.size(); i++) {
            Records.StructureConnection conn = mutableConnections.get(i);
            if ((conn.from().equals(structureConnection.from()) && conn.to().equals(structureConnection.to())) ||
                (conn.from().equals(structureConnection.to()) && conn.to().equals(structureConnection.from()))) {
                mutableConnections.set(i, new Records.StructureConnection(
                    conn.from(), conn.to(), Records.ConnectionStatus.FAILED, conn.manual()));
                dataProvider.setStructureConnections(level, mutableConnections);
                LOGGER.info("Marked connection as FAILED: {} -> {}", conn.from(), conn.to());
                break;
            }
        }
    }

    /**
     * 在需要时重启线程池
     * <p>
     * 检查线程池是否已关闭或终止，如果是则创建新的线程池。
     * 这在服务器重启或世界重载时很有用。
     * </p>
     */
    private static void restartExecutorIfNeeded() {
        if (executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newFixedThreadPool(THREAD_COUNT);
            LOGGER.debug("RoadWeaver: ExecutorService restarted.");
        }
    }

    /**
     * 恢复未完成的道路生成任务
     * <p>
     * 在世界加载时调用，扫描所有保存的连接，将未完成的任务重新加入队列。
     * </p>
     * <p>
     * 处理逻辑：
     * <ul>
     *   <li>PLANNED状态：直接加入队列</li>
     *   <li>GENERATING状态：重置为PLANNED后加入队列（意外中断的任务）</li>
     *   <li>COMPLETED状态：跳过（已完成）</li>
     *   <li>FAILED状态：跳过（避免重复失败）</li>
     * </ul>
     * </p>
     * 
     * @param level 服务器世界
     */
    private static void restoreUnfinishedRoads(ServerLevel level) {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> connections = dataProvider.getStructureConnections(level);

        int restoredCount = 0;
        List<Records.StructureConnection> updatedConnections = new ArrayList<>(connections);
        boolean needsUpdate = false;
        
        for (int i = 0; i < updatedConnections.size(); i++) {
            Records.StructureConnection connection = updatedConnections.get(i);
            
            // 恢复 PLANNED 和 GENERATING 状态的连接
            if (connection.status() == Records.ConnectionStatus.PLANNED ||
                connection.status() == Records.ConnectionStatus.GENERATING) {

                // 将 GENERATING 状态重置为 PLANNED（意外中断的任务）
                if (connection.status() == Records.ConnectionStatus.GENERATING) {
                    Records.StructureConnection resetConnection = new Records.StructureConnection(
                            connection.from(),
                            connection.to(),
                            Records.ConnectionStatus.PLANNED,
                            connection.manual()
                    );
                    updatedConnections.set(i, resetConnection);
                    StructureConnector.getQueueForWorld(level).add(resetConnection);
                    needsUpdate = true;
                } else {
                    // PLANNED 状态直接加入队列
                    StructureConnector.getQueueForWorld(level).add(connection);
                }
                restoredCount++;
            }
            // COMPLETED 和 FAILED 状态不处理
        }

        // 批量更新连接状态
        if (needsUpdate) {
            dataProvider.setStructureConnections(level, updatedConnections);
        }

        if (restoredCount > 0) {
            LOGGER.info("RoadWeaver: 恢复了 {} 个未完成的道路生成任务（队列大小: {}）", 
                restoredCount, StructureConnector.getQueueForWorld(level).size());
        }
    }
    
    /**
     * 处理异步结构搜索结果
     * <p>
     * 在每个服务器tick中调用，检查异步结构搜索是否完成，
     * 如果发现新结构则自动创建道路连接。
     * </p>
     * <p>
     * 处理流程：
     * <ol>
     *   <li>调用StructureLocatorImpl处理异步搜索结果</li>
     *   <li>检查结构数量是否增加</li>
     *   <li>如果增加则创建新的道路连接</li>
     * </ol>
     * </p>
     * 
     * @param level 服务器世界
     */
    private static void processAsyncStructureResults(ServerLevel level) {
        // 调用 StructureLocatorImpl 处理异步结果
        StructureLocatorImpl.processAsyncResults(level);
        
        // 检查是否有新结构被发现，如果有则创建连接
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        Records.StructureLocationData locationData = dataProvider.getStructureLocations(level);
        
        if (locationData != null && locationData.structureLocations().size() >= 2) {
            // 检查是否需要创建新连接
            List<Records.StructureConnection> connections = dataProvider.getStructureConnections(level);
            int existingConnectionCount = connections != null ? connections.size() : 0;
            int structureCount = locationData.structureLocations().size();
            
            // 如果结构数量增加了，尝试创建新连接
            if (structureCount > existingConnectionCount + 1) {
                StructureConnector.createNewStructureConnection(level);
            }
        }
    }
}
