package net.countered.settlementroads.events;

import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureConnector;
import net.countered.settlementroads.persistence.WorldDataHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static net.countered.settlementroads.SettlementRoads.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ModEventHandler {

    private static final int THREAD_COUNT = 7;
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    private static final ConcurrentHashMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    public static void register(IEventBus modEventBus) {
        // NeoForge 使用注解自动注册事件
        LOGGER.info("Registering event handlers");
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverWorld)) return;
        
        restartExecutorIfNeeded();
        if (!serverWorld.dimension().equals(Level.OVERWORLD)) return;
        
        Records.StructureLocationData structureLocationData = WorldDataHelper.getStructureLocations(serverWorld);
        if (structureLocationData == null) {
            structureLocationData = new Records.StructureLocationData(new ArrayList<>());
            WorldDataHelper.setStructureLocations(serverWorld, structureLocationData);
        }

        // 🆕 恢复未完成的道路生成任务
        restoreUnfinishedRoads(serverWorld);

        if (structureLocationData.structureLocations().size() < ModConfig.initialLocatingCount()) {
            for (int i = 0; i < ModConfig.initialLocatingCount(); i++) {
                StructureConnector.cacheNewConnection(serverWorld, false);
                tryGenerateNewRoads(serverWorld, true, 5000);
            }
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverWorld)) return;
        if (!serverWorld.dimension().equals(Level.OVERWORLD)) return;
        
        Future<?> task = runningTasks.remove(serverWorld.dimension().location().toString());
        if (task != null && !task.isDone()) {
            task.cancel(true);
            LOGGER.debug("Aborted running road task for world: {}", serverWorld.dimension().location());
        }
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel serverWorld)) return;
        if (!serverWorld.dimension().equals(Level.OVERWORLD)) return;
        
        tryGenerateNewRoads(serverWorld, true, 5000);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        net.countered.settlementroads.features.roadlogic.RoadPathCalculator.heightCache.clear();
        runningTasks.values().forEach(future -> future.cancel(true));
        runningTasks.clear();
        executor.shutdownNow();
        LOGGER.debug("RoadWeaver: ExecutorService shut down.");
    }

    private static void tryGenerateNewRoads(ServerLevel serverWorld, Boolean async, int steps) {
        // 清理已完成的任务
        runningTasks.entrySet().removeIf(entry -> entry.getValue().isDone());
        
        // 检查是否达到并发上限
        if (runningTasks.size() >= ModConfig.maxConcurrentRoadGeneration()) {
            return;
        }
        
        if (!StructureConnector.cachedStructureConnections.isEmpty()) {
            Records.StructureConnection structureConnection = StructureConnector.cachedStructureConnections.poll();
            
            // 获取 RoadFeature 配置
            net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?> feature = serverWorld.registryAccess()
                    .registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(net.countered.settlementroads.features.config.ModConfiguredFeatures.ROAD_FEATURE_KEY);

            if (feature != null && feature.config() instanceof net.countered.settlementroads.features.config.RoadFeatureConfig roadConfig) {
                if (async) {
                    // 使用唯一的任务ID而不是世界ID，允许多个任务并发
                    String taskId = serverWorld.dimension().location().toString() + "_" + System.nanoTime();
                    Future<?> future = executor.submit(() -> {
                        try {
                            new net.countered.settlementroads.features.roadlogic.Road(serverWorld, structureConnection, roadConfig).generateRoad(steps);
                        } catch (Exception e) {
                            LOGGER.error("Error generating road", e);
                        } finally {
                            runningTasks.remove(taskId);
                        }
                    });
                    runningTasks.put(taskId, future);
                }
                else {
                    new net.countered.settlementroads.features.roadlogic.Road(serverWorld, structureConnection, roadConfig).generateRoad(steps);
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
    private static void restoreUnfinishedRoads(ServerLevel serverWorld) {
        List<Records.StructureConnection> connections = WorldDataHelper.getConnectedStructures(serverWorld);
        
        int restoredCount = 0;
        for (Records.StructureConnection connection : connections) {
            // 只恢复计划中或生成中的连接
            if (connection.status() == Records.ConnectionStatus.PLANNED || 
                connection.status() == Records.ConnectionStatus.GENERATING) {
                
                // 如果是生成中状态，重置为计划中（因为之前的生成被中断了）
                if (connection.status() == Records.ConnectionStatus.GENERATING) {
                    Records.StructureConnection resetConnection = new Records.StructureConnection(
                            connection.from(), 
                            connection.to(), 
                            Records.ConnectionStatus.PLANNED
                    );
                    StructureConnector.cachedStructureConnections.add(resetConnection);
                    
                    // 更新世界数据中的状态
                    List<Records.StructureConnection> updatedConnections = new ArrayList<>(connections);
                    int index = updatedConnections.indexOf(connection);
                    if (index >= 0) {
                        updatedConnections.set(index, resetConnection);
                        WorldDataHelper.setConnectedStructures(serverWorld, updatedConnections);
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
