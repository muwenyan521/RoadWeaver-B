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
            for (int i = 0; i < config.initialLocatingCount(); i++) {
                StructureConnector.cacheNewConnection(level, false);
                tryGenerateNewRoads(level, true, 5000);
            }
        }
    }

    private static void tryGenerateNewRoads(ServerLevel level, Boolean async, int steps) {
        IModConfig config = ConfigProvider.get();
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();

        // 清理已完成的任务
        runningTasks.entrySet().removeIf(entry -> entry.getValue().isDone());

        // 并发上限
        if (runningTasks.size() >= config.maxConcurrentRoadGeneration()) {
            return;
        }

        if (!StructureConnector.cachedStructureConnections.isEmpty()) {
            Records.StructureConnection structureConnection = StructureConnector.cachedStructureConnections.poll();
            ConfiguredFeature<?, ?> feature = level.registryAccess()
                    .registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(RoadFeature.ROAD_FEATURE_KEY);

            if (feature != null && feature.config() instanceof RoadFeatureConfig roadConfig) {
                if (async) {
                    String taskId = level.dimension().location().toString() + "_" + System.nanoTime();
                    Future<?> future = executor.submit(() -> {
                        try {
                            new Road(level, structureConnection, roadConfig).generateRoad(steps);
                        } catch (Exception e) {
                            LOGGER.error("Error generating road", e);
                        } finally {
                            runningTasks.remove(taskId);
                        }
                    });
                    runningTasks.put(taskId, future);
                } else {
                    new Road(level, structureConnection, roadConfig).generateRoad(steps);
                }
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
     */
    private static void restoreUnfinishedRoads(ServerLevel level) {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> connections = dataProvider.getStructureConnections(level);

        int restoredCount = 0;
        for (Records.StructureConnection connection : connections) {
            if (connection.status() == Records.ConnectionStatus.PLANNED ||
                connection.status() == Records.ConnectionStatus.GENERATING) {

                if (connection.status() == Records.ConnectionStatus.GENERATING) {
                    Records.StructureConnection resetConnection = new Records.StructureConnection(
                            connection.from(),
                            connection.to(),
                            Records.ConnectionStatus.PLANNED
                    );
                    StructureConnector.cachedStructureConnections.add(resetConnection);

                    List<Records.StructureConnection> updatedConnections = new ArrayList<>(connections);
                    int index = updatedConnections.indexOf(connection);
                    if (index >= 0) {
                        updatedConnections.set(index, resetConnection);
                        dataProvider.setStructureConnections(level, updatedConnections);
                    }
                } else {
                    StructureConnector.cachedStructureConnections.add(connection);
                }
                restoredCount++;
            }
        }

        if (restoredCount > 0) {
            LOGGER.info("RoadWeaver: 恢复了 {} 个未完成的道路生成任务", restoredCount);
        }
    }
}
