package net.shiroha233.roadweaver.generation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.shiroha233.roadweaver.features.config.RoadFeatureConfig;
import net.shiroha233.roadweaver.features.roadlogic.Road;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.shiroha233.roadweaver.planning.PlanningUtils;
import net.shiroha233.roadweaver.config.ConfigService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class RoadGenerationService {
    private RoadGenerationService() {}

    private static volatile ExecutorService EXECUTOR = null;
    private static final ConcurrentHashMap<ServerLevel, ConcurrentLinkedQueue<Records.StructureConnection>> QUEUES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ServerLevel, ConcurrentHashMap<Long, Boolean>> PROCESSED = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ServerLevel, AtomicInteger> RUNNING_COUNT = new ConcurrentHashMap<>();
    private static final Set<Future<?>> ALL_RUNNING = ConcurrentHashMap.newKeySet();

    private static final ResourceLocation ROAD_CF_ID = new ResourceLocation("roadweaver", "road_feature");

    public static void onServerStopping() {
        ALL_RUNNING.forEach(f -> f.cancel(true));
        ALL_RUNNING.clear();
        if (EXECUTOR != null) EXECUTOR.shutdownNow();
        QUEUES.clear();
        PROCESSED.clear();
        RUNNING_COUNT.clear();
    }

    /**
     * 同步生成，用于世界生成前的阻塞阶段（单线程）。
     */
    public static void generateInline(ServerLevel level, Records.StructureConnection conn) {
        if (level == null || conn == null) return;
        WorldDataProvider provider = WorldDataProvider.getInstance();
        try {
            // 标记为 GENERATING
            List<Records.StructureConnection> all0 = new ArrayList<>(provider.getStructureConnections(level));
            for (int i = 0; i < all0.size(); i++) {
                Records.StructureConnection c = all0.get(i);
                if (sameEdge(c, conn)) {
                    all0.set(i, new Records.StructureConnection(c.from(), c.to(), Records.ConnectionStatus.GENERATING));
                }
            }
            if (!all0.isEmpty()) provider.setStructureConnections(level, all0);
            // 立即刷新一次统计，让加载界面能显示“生成中”数量
            InitialGenManager.update(level);
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            // 配置
            var reg = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
            ConfiguredFeature<?, ?> cf = reg.get(ROAD_CF_ID);
            RoadFeatureConfig cfg;
            if (cf != null && cf.config() instanceof RoadFeatureConfig rfc) cfg = rfc; else cfg = defaultConfig();

            // 生成
            new Road(level, conn, cfg).generateRoad(5000);

            // 标记 COMPLETED
            List<Records.StructureConnection> all = new ArrayList<>(provider.getStructureConnections(level));
            for (int i = 0; i < all.size(); i++) {
                Records.StructureConnection c = all.get(i);
                if (sameEdge(c, conn)) {
                    all.set(i, new Records.StructureConnection(c.from(), c.to(), Records.ConnectionStatus.COMPLETED));
                }
            }
            provider.setStructureConnections(level, all);
        } catch (Throwable t) {
            // 标记 FAILED
            List<Records.StructureConnection> all = new ArrayList<>(provider.getStructureConnections(level));
            for (int i = 0; i < all.size(); i++) {
                Records.StructureConnection c = all.get(i);
                if (sameEdge(c, conn)) {
                    all.set(i, new Records.StructureConnection(c.from(), c.to(), Records.ConnectionStatus.FAILED));
                }
            }
            provider.setStructureConnections(level, all);
        }
    }

    public static void onServerStarted() {
        int threads = Math.max(1, ConfigService.get().generationThreads());
        if (EXECUTOR != null && !EXECUTOR.isShutdown() && !EXECUTOR.isTerminated()) {
            EXECUTOR.shutdownNow();
        }
        EXECUTOR = Executors.newFixedThreadPool(threads);
        ALL_RUNNING.clear();
        QUEUES.clear();
        PROCESSED.clear();
        RUNNING_COUNT.clear();
    }

    public static void tick(ServerLevel level) {
        refreshQueue(level);
        ConcurrentLinkedQueue<Records.StructureConnection> q = QUEUES.computeIfAbsent(level, l -> new ConcurrentLinkedQueue<>());
        if (q.isEmpty()) return;
        if (EXECUTOR == null || EXECUTOR.isShutdown() || EXECUTOR.isTerminated()) {
            int threads = Math.max(1, ConfigService.get().generationThreads());
            EXECUTOR = Executors.newFixedThreadPool(threads);
        }
        int limit = Math.max(1, ConfigService.get().maxConcurrentGenerations());
        AtomicInteger cnt = RUNNING_COUNT.computeIfAbsent(level, l -> new AtomicInteger(0));
        java.util.List<ServerPlayer> players = new java.util.ArrayList<>();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p != null && p.serverLevel() == level) players.add(p);
        }
        int sample = Math.max(64, limit * 8);
        while (cnt.get() < limit) {
            Records.StructureConnection conn = pollNearest(q, players, sample);
            if (conn == null) break;
            {
                WorldDataProvider provider = WorldDataProvider.getInstance();
                List<Records.StructureConnection> origin = provider.getStructureConnections(level);
                List<Records.StructureConnection> all = origin != null ? new ArrayList<>(origin) : new ArrayList<>();
                for (int i = 0; i < all.size(); i++) {
                    Records.StructureConnection c = all.get(i);
                    if (sameEdge(c, conn)) {
                        all.set(i, new Records.StructureConnection(c.from(), c.to(), Records.ConnectionStatus.GENERATING));
                    }
                }
                if (!all.isEmpty()) provider.setStructureConnections(level, all);
            }
            final Records.StructureConnection task = conn;
            cnt.incrementAndGet();
            Future<?> fut = EXECUTOR.submit(() -> {
                try {
                    safeGenerate(level, task);
                } finally {
                    cnt.decrementAndGet();
                }
            });
            ALL_RUNNING.add(fut);
        }
    }

    private static void refreshQueue(ServerLevel level) {
        WorldDataProvider provider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> list = provider.getStructureConnections(level);
        if (list == null) return;
        ConcurrentLinkedQueue<Records.StructureConnection> q = QUEUES.computeIfAbsent(level, l -> new ConcurrentLinkedQueue<>());
        ConcurrentHashMap<Long, Boolean> proc = PROCESSED.computeIfAbsent(level, l -> new ConcurrentHashMap<>());
        for (Records.StructureConnection c : list) {
            long key = PlanningUtils.edgeKey(c.from(), c.to());
            if (proc.putIfAbsent(key, Boolean.TRUE) != null) continue;
            if (c.status() != Records.ConnectionStatus.PLANNED && c.status() != Records.ConnectionStatus.GENERATING) continue;
            q.add(c);
        }
    }

    private static void safeGenerate(ServerLevel level, Records.StructureConnection conn) {
        try {
            WorldDataProvider provider = WorldDataProvider.getInstance();
            var reg = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
            ConfiguredFeature<?, ?> cf = reg.get(ROAD_CF_ID);
            RoadFeatureConfig cfg;
            if (cf != null && cf.config() instanceof RoadFeatureConfig rfc) {
                cfg = rfc;
            } else {
                cfg = defaultConfig();
            }
            new Road(level, conn, cfg).generateRoad(5000);
            List<Records.StructureConnection> all = new ArrayList<>(provider.getStructureConnections(level));
            for (int i = 0; i < all.size(); i++) {
                Records.StructureConnection c = all.get(i);
                if (sameEdge(c, conn)) {
                    all.set(i, new Records.StructureConnection(c.from(), c.to(), Records.ConnectionStatus.COMPLETED));
                }
            }
            provider.setStructureConnections(level, all);
        } catch (Throwable t) {
            WorldDataProvider provider = WorldDataProvider.getInstance();
            List<Records.StructureConnection> all = new ArrayList<>(provider.getStructureConnections(level));
            for (int i = 0; i < all.size(); i++) {
                Records.StructureConnection c = all.get(i);
                if (sameEdge(c, conn)) {
                    all.set(i, new Records.StructureConnection(c.from(), c.to(), Records.ConnectionStatus.FAILED));
                }
            }
            provider.setStructureConnections(level, all);
        }
    }

    private static RoadFeatureConfig defaultConfig() {
        return new RoadFeatureConfig();
    }

    private static boolean sameEdge(Records.StructureConnection a, Records.StructureConnection b) {
        BlockPos af = a.from(), at = a.to();
        BlockPos bf = b.from(), bt = b.to();
        return (af.equals(bf) && at.equals(bt)) || (af.equals(bt) && at.equals(bf));
    }

    private static long dist2XZ(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static long playerDistance2(Records.StructureConnection c, java.util.List<ServerPlayer> players) {
        if (players == null || players.isEmpty()) return Long.MAX_VALUE;
        long best = Long.MAX_VALUE;
        int mx = (c.from().getX() + c.to().getX()) >> 1;
        int mz = (c.from().getZ() + c.to().getZ()) >> 1;
        BlockPos mid = new BlockPos(mx, 0, mz);
        for (ServerPlayer p : players) {
            BlockPos pb = p.blockPosition();
            long d = dist2XZ(pb, c.from());
            if (d < best) best = d;
            d = dist2XZ(pb, c.to());
            if (d < best) best = d;
            d = dist2XZ(pb, mid);
            if (d < best) best = d;
        }
        return best;
    }

    private static Records.StructureConnection pollNearest(ConcurrentLinkedQueue<Records.StructureConnection> q,
                                                           java.util.List<ServerPlayer> players,
                                                           int sample) {
        if (q.isEmpty()) return null;
        if (players == null || players.isEmpty()) return q.poll();
        java.util.Iterator<Records.StructureConnection> it = q.iterator();
        Records.StructureConnection best = null;
        long bestd = Long.MAX_VALUE;
        int c = 0;
        while (it.hasNext() && c < Math.max(1, sample)) {
            Records.StructureConnection e = it.next();
            long d = playerDistance2(e, players);
            if (d < bestd) {
                bestd = d;
                best = e;
            }
            c++;
        }
        if (best != null && q.remove(best)) return best;
        return q.poll();
    }
}
