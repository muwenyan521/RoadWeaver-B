package net.countered.settlementroads.events;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.countered.settlementroads.config.ConfigProvider;
import net.countered.settlementroads.config.IModConfig;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.countered.settlementroads.features.roadlogic.Road;
import net.countered.settlementroads.features.roadlogic.RoadPathCalculator;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureConnector;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


/**
 * 使用 Architectury 事件的通用事件处理器（Common）。
 * 平台端无需各自实现，主类直接调用 ModEventHandler.register() 即可。
 */
public class ModEventHandler {

    private static final int THREAD_COUNT = 7;
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    private static ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    private static final ConcurrentHashMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    public static void register() {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();

        // 世界加载
        LifecycleEvent.SERVER_LEVEL_LOAD.register(ModEventHandler::onWorldLoad);

        // 世界卸载
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(level -> {
            if (!level.dimension().equals(Level.OVERWORLD)) return;
            Future<?> task = runningTasks.remove(level.dimension().location().toString());
            if (task != null && !task.isDone()) {
                task.cancel(true);
                LOGGER.debug("Aborted running road task for world: {}", level.dimension().location());
            }
        });

        // 服务器 Tick（遍历所有世界）
        TickEvent.SERVER_PRE.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.dimension().equals(Level.OVERWORLD)) {
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
            LOGGER.debug("RoadWeaver: ExecutorService shut down.");
        });
    }

    private static void onWorldLoad(ServerLevel level) {
        restartExecutorIfNeeded();
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(level);

        // 恢复未完成的道路生成任务
        restoreUnfinishedRoads(level);

        IModConfig config = ConfigProvider.get();
        if (structureLocationData.structureLocations().size() < config.initialLocatingCount()) {
            LOGGER.info("Initializing world with {} structures", config.initialLocatingCount());
            
            // 只搜寻结构，不立即生成道路（由 tick 事件处理）
            for (int i = 0; i < config.initialLocatingCount(); i++) {
                StructureConnector.cacheNewConnection(level, false);
            }
            
            LOGGER.info("Initial structure search completed, queue size: {}", 
                StructureConnector.cachedStructureConnections.size());
        }
    }

    private static void tryGenerateNewRoads(ServerLevel level, Boolean async, int steps) {
        IModConfig config = ConfigProvider.get();
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();

        // 清理已完成的任务（包括异常终止的）
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

        // 并发上限检查
        int currentRunning = runningTasks.size();
        if (currentRunning >= config.maxConcurrentRoadGeneration()) {
            return;
        }

        if (!StructureConnector.cachedStructureConnections.isEmpty()) {
            Records.StructureConnection structureConnection = StructureConnector.cachedStructureConnections.poll();
            
            if (structureConnection == null) {
                return; // 队列为空（并发情况）
            }
            
            LOGGER.info("🚧 Starting road generation: {} -> {} (running: {}/{}, queue: {})", 
                structureConnection.from(), structureConnection.to(), 
                currentRunning + 1, config.maxConcurrentRoadGeneration(),
                StructureConnector.cachedStructureConnections.size());
            
            ConfiguredFeature<?, ?> feature = level.registryAccess()
                    .registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(RoadFeature.ROAD_FEATURE_KEY);

            if (feature != null && feature.config() instanceof RoadFeatureConfig roadConfig) {
                if (async) {
                    String taskId = level.dimension().location().toString() + "_" + System.nanoTime();
                    Future<?> future = executor.submit(() -> {
                        try {
                            LOGGER.debug("🔨 Generating road: {} -> {}", 
                                structureConnection.from(), structureConnection.to());
                            new Road(level, structureConnection, roadConfig).generateRoad(steps);
                            LOGGER.info("✅ Road generation completed: {} -> {}", 
                                structureConnection.from(), structureConnection.to());
                        } catch (Exception e) {
                            LOGGER.error("❌ Error generating road {} -> {}: {}", 
                                structureConnection.from(), structureConnection.to(), 
                                e.getMessage(), e);
                            
                            // 异常时标记为 FAILED，避免重试
                            try {
                                markConnectionAsFailed(level, structureConnection);
                            } catch (Exception ex) {
                                LOGGER.error("Failed to mark connection as failed", ex);
                            }
                        } finally {
                            runningTasks.remove(taskId);
                        }
                    });
                    runningTasks.put(taskId, future);
                } else {
                    try {
                        new Road(level, structureConnection, roadConfig).generateRoad(steps);
                    } catch (Exception e) {
                        LOGGER.error("❌ Error generating road: {}", e.getMessage(), e);
                        markConnectionAsFailed(level, structureConnection);
                    }
                }
            } else {
                LOGGER.warn("❌ RoadFeature or config not found!");
            }
        }
    }

    /**
     * 标记连接为失败状态
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

    private static void restartExecutorIfNeeded() {
        if (executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newFixedThreadPool(THREAD_COUNT);
            LOGGER.debug("RoadWeaver: ExecutorService restarted.");
        }
    }

    /**
     * 恢复未完成的道路生成任务
     * 在世界加载时调用，将所有 PLANNED 和 GENERATING 状态的连接重新加入队列
     * FAILED 和 COMPLETED 状态不处理
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
                    StructureConnector.cachedStructureConnections.add(resetConnection);
                    needsUpdate = true;
                } else {
                    // PLANNED 状态直接加入队列
                    StructureConnector.cachedStructureConnections.add(connection);
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
                restoredCount, StructureConnector.cachedStructureConnections.size());
        }
    }
}
