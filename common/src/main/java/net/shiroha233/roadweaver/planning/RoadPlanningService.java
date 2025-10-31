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

import java.util.*;
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

        ModConfig cfg = ConfigService.get();
        List<Records.StructureConnection> primaryEdges;
        
        if (cfg.useOptimizedPlanning()) {
            // 使用优化的混合规划算法
            primaryEdges = OptimizedPlanner.hybridPlan(points, 2048, 35.0);
        } else {
            // 回退到原有算法
            if (cfg.planningAlgorithm() == ModConfig.PlanningAlgorithm.DELAUNAY) {
                primaryEdges = DelaunayPlanner.planDelaunay(points, 2048);
            } else if (cfg.planningAlgorithm() == ModConfig.PlanningAlgorithm.RNG) {
                primaryEdges = RNGPlanner.planRNG(points, 2048);
            } else {
                primaryEdges = KNNPlanner.planKNN(points, 2, 2048, 1.8, 40.0, 2);
            }
        }
        
        if (primaryEdges.isEmpty()) return;

        WorldDataProvider provider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> existing = provider.getStructureConnections(level);

        // 使用增量MST更新现有连接
        List<Records.StructureConnection> updatedEdges;
        if (cfg.useIncrementalMST() && existing != null && !existing.isEmpty()) {
            // 对于每个新点，使用增量MST添加
            List<Records.StructureConnection> currentEdges = new ArrayList<>(existing);
            for (BlockPos newPoint : points) {
                if (!containsPoint(existing, newPoint)) {
                    currentEdges = IncrementalMST.incrementalAdd(
                        getAllPoints(currentEdges), currentEdges, newPoint, 2048);
                }
            }
            updatedEdges = currentEdges;
        } else {
            updatedEdges = primaryEdges;
        }

        HashSet<BlockPos> inRect = new HashSet<>(points);
        ArrayList<Records.StructureConnection> existingInRect = new ArrayList<>();
        if (existing != null) {
            for (Records.StructureConnection c : existing) {
                if (inRect.contains(c.from()) && inRect.contains(c.to())) existingInRect.add(c);
            }
        }

        ArrayList<Records.StructureConnection> base = new ArrayList<>(existingInRect);
        base.addAll(updatedEdges);

        List<Records.StructureConnection> bridges = KNNPlanner.connectComponents(points, base, 1536, 35.0, 3);

        ArrayList<Records.StructureConnection> incoming = new ArrayList<>(updatedEdges);
        incoming.addAll(bridges);
        List<Records.StructureConnection> merged = mergeConnections(existing, incoming);
        if (merged.size() != existing.size()) {
            provider.setStructureConnections(level, merged);
        }
        
        // 清除相关区域的路径缓存
        PathCache.clearPathsInArea(new BlockPos((minBlockX + maxBlockX) / 2, 0, (minBlockZ + maxBlockZ) / 2), 
                                 Math.max(maxBlockX - minBlockX, maxBlockZ - minBlockZ) / 2);
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

    public static List<BlockPos> findPath(ServerLevel level, BlockPos start, BlockPos end) {
        // 使用PathCache的静态方法
        List<Records.RoadSegmentPlacement> cachedPath = PathCache.getCachedPath(start, end, 3); // 默认宽度3
        if (cachedPath != null) {
            return convertToBlockPosList(cachedPath);
        }

        WorldDataProvider provider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> connections = provider.getStructureConnections(level);
        if (connections == null || connections.isEmpty()) {
            return Collections.emptyList();
        }

        List<BlockPos> path = BasicAStarPathfinder.findPath(start, end, connections);
        if (!path.isEmpty()) {
            PathCache.cachePath(start, end, 3, convertToRoadSegmentPlacements(path));
        }
        return path;
    }

    public static void clearPathCache() {
        PathCache.clearAll();
    }

    public static PathCache.CacheStats getCacheStats() {
        return PathCache.getStats();
    }

    public static void removeStructure(ServerLevel level, BlockPos structurePos) {
        WorldDataProvider provider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> existing = provider.getStructureConnections(level);
        if (existing == null) return;

        // 获取剩余节点
        List<BlockPos> remainingNodes = getAllPoints(existing);
        remainingNodes.remove(structurePos);

        List<Records.StructureConnection> updated = IncrementalMST.incrementalRemove(
            remainingNodes, existing, structurePos, 2048);
        
        if (updated.size() != existing.size()) {
            provider.setStructureConnections(level, updated);
            PathCache.clearPathsInArea(structurePos, 1000);
        }
    }

    // 辅助方法
    private static boolean containsPoint(List<Records.StructureConnection> edges, BlockPos point) {
        for (Records.StructureConnection edge : edges) {
            if (edge.from().equals(point) || edge.to().equals(point)) {
                return true;
            }
        }
        return false;
    }

    private static List<BlockPos> getAllPoints(List<Records.StructureConnection> edges) {
        Set<BlockPos> points = new HashSet<>();
        for (Records.StructureConnection edge : edges) {
            points.add(edge.from());
            points.add(edge.to());
        }
        return new ArrayList<>(points);
    }

    private static List<BlockPos> convertToBlockPosList(List<Records.RoadSegmentPlacement> placements) {
        List<BlockPos> result = new ArrayList<>();
        for (Records.RoadSegmentPlacement placement : placements) {
            result.add(placement.position());
        }
        return result;
    }

    private static List<Records.RoadSegmentPlacement> convertToRoadSegmentPlacements(List<BlockPos> path) {
        List<Records.RoadSegmentPlacement> result = new ArrayList<>();
        for (BlockPos pos : path) {
            result.add(new Records.RoadSegmentPlacement(pos, 3, Records.RoadSegmentType.STRAIGHT));
        }
        return result;
    }
}
