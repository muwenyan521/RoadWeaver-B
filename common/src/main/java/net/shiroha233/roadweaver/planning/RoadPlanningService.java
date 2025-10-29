package net.shiroha233.roadweaver.planning;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaver.client.map.MapDataCollector;
import net.shiroha233.roadweaver.client.map.MapSnapshot;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RoadPlanningService {
    private RoadPlanningService() {}

    private static final ConcurrentHashMap<Level, Set<Long>> PLANNED_TILES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Level, java.util.concurrent.ConcurrentHashMap<Long, Long>> PLANNED_TILE_CENTERS = new ConcurrentHashMap<>();

    public static void initialPlan(ServerLevel level) {
        if (!Level.OVERWORLD.equals(level.dimension())) return;
        ModConfig cfg = ConfigService.get();
        int radiusChunks = Math.max(1, cfg.initialPlanRadiusChunks());
        BlockPos spawn = level.getSharedSpawnPos();
        int cx = spawn.getX() >> 4;
        int cz = spawn.getZ() >> 4;
        int minX = (cx - radiusChunks) * 16;
        int maxX = (cx + radiusChunks) * 16;
        int minZ = (cz - radiusChunks) * 16;
        int maxZ = (cz + radiusChunks) * 16;
        planRect(level, minX, minZ, maxX, maxZ);
    }

    public static void planAroundPlayer(ServerPlayer player) {
        if (player == null) return;
        ServerLevel level = player.serverLevel();
        if (!Level.OVERWORLD.equals(level.dimension())) return;
        ModConfig cfg = ConfigService.get();
        if (!cfg.dynamicPlanEnabled()) return;
        int radiusChunks = Math.max(1, cfg.dynamicPlanRadiusChunks());
        int stride = Math.max(1, cfg.dynamicPlanStrideChunks());
        int tile = Math.max(8, Math.min(256, stride));
        int pcx = player.chunkPosition().x;
        int pcz = player.chunkPosition().z;
        int kx = floorDiv(pcx, tile);
        int kz = floorDiv(pcz, tile);
        long key = (((long) kx) << 32) ^ (kz & 0xffffffffL);
        Set<Long> set = PLANNED_TILES.computeIfAbsent(level, l -> ConcurrentHashMap.newKeySet());
        boolean isNewTile = set.add(key);
        // 记录该 tile 的规划中心（玩家当时的区块坐标），用于地图覆盖重建
        java.util.concurrent.ConcurrentHashMap<Long, Long> centers = PLANNED_TILE_CENTERS.computeIfAbsent(level, l -> new java.util.concurrent.ConcurrentHashMap<>());
        centers.putIfAbsent(key, (((long) pcx) << 32) ^ (pcz & 0xffffffffL));
        if (!isNewTile) return; 

        int minX = (pcx - radiusChunks) * 16;
        int maxX = (pcx + radiusChunks) * 16;
        int minZ = (pcz - radiusChunks) * 16;
        int maxZ = (pcz + radiusChunks) * 16;
        planRect(level, minX, minZ, maxX, maxZ);
    }

    private static void planRect(ServerLevel level, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        MapSnapshot snap = MapDataCollector.build(level, minBlockX, minBlockZ, maxBlockX, maxBlockZ);
        List<BlockPos> points = new ArrayList<>();
        HashSet<Long> seenPos = new HashSet<>();
        for (BlockPos p : snap.structures()) {
            BlockPos q = new BlockPos(p.getX(), 0, p.getZ());
            long key = PlanningUtils.pos2dKey(q);
            if (seenPos.add(key)) points.add(q);
        }
        if (points.size() < 2) return;

        List<Records.StructureConnection> primaryEdges;
        ModConfig cfg0 = ConfigService.get();
        if (cfg0.planningAlgorithm() == ModConfig.PlanningAlgorithm.DELAUNAY) {
            primaryEdges = DelaunayPlanner.planDelaunay(points, 2048);
        } else if (cfg0.planningAlgorithm() == ModConfig.PlanningAlgorithm.RNG) {
            primaryEdges = RNGPlanner.planRNG(points, 2048);
        } else {
            primaryEdges = KNNPlanner.planKNN(points, 2, 2048, 1.8, 40.0, 2);
        }
        if (primaryEdges.isEmpty()) return;

        WorldDataProvider provider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> existing = provider.getStructureConnections(level);

        HashSet<BlockPos> inRect = new HashSet<>(points);
        ArrayList<Records.StructureConnection> existingInRect = new ArrayList<>();
        if (existing != null) {
            for (Records.StructureConnection c : existing) {
                if (inRect.contains(c.from()) && inRect.contains(c.to())) existingInRect.add(c);
            }
        }

        ArrayList<Records.StructureConnection> base = new ArrayList<>(existingInRect);
        base.addAll(primaryEdges);

        List<Records.StructureConnection> bridges = KNNPlanner.connectComponents(points, base, 1536, 35.0, 3);

        ArrayList<Records.StructureConnection> incoming = new ArrayList<>(primaryEdges);
        incoming.addAll(bridges);
        List<Records.StructureConnection> merged = mergeConnections(existing, incoming);
        if (merged.size() != existing.size()) {
            provider.setStructureConnections(level, merged);
        }
    }

    private static List<Records.StructureConnection> mergeConnections(List<Records.StructureConnection> existing,
                                                                      List<Records.StructureConnection> incoming) {
        HashSet<Long> seen = new HashSet<>();
        ArrayList<Records.StructureConnection> out = new ArrayList<>();
        if (existing != null) {
            for (Records.StructureConnection c : existing) {
                long k = PlanningUtils.edgeKey(c.from(), c.to());
                if (seen.add(k)) out.add(c);
            }
        }
        for (Records.StructureConnection c : incoming) {
            long k = PlanningUtils.edgeKey(c.from(), c.to());
            if (seen.add(k)) out.add(new Records.StructureConnection(c.from(), c.to(), Records.ConnectionStatus.PLANNED));
        }
        return out;
    }

    private static int floorDiv(int a, int b) {
        int r = a / b;
        if ((a ^ b) < 0 && (r * b != a)) r--;
        return r;
    }

    public static Set<Long> getPlannedTiles(ServerLevel level) {
        Set<Long> s = PLANNED_TILES.get(level);
        return s != null ? java.util.Set.copyOf(s) : java.util.Set.of();
    }

    public static java.util.Map<Long, Long> getPlannedTileCenters(ServerLevel level) {
        var m = PLANNED_TILE_CENTERS.get(level);
        if (m == null || m.isEmpty()) return java.util.Map.of();
        return java.util.Map.copyOf(m);
    }

    public static int getStrideTileSizeChunks() {
        ModConfig cfg = ConfigService.get();
        int stride = Math.max(1, cfg.dynamicPlanStrideChunks());
        return Math.max(8, Math.min(256, stride));
    }

    public static int getDynamicPlanRadiusChunks() {
        ModConfig cfg = ConfigService.get();
        return Math.max(1, cfg.dynamicPlanRadiusChunks());
    }
}
