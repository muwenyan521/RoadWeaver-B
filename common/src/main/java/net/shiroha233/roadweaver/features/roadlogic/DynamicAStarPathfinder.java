package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.planning.PathCache;

import java.util.*;

/**
 * 动态A*路径规划器
 * 集成缓存机制和动态路径重计算，避免全图重算
 */
public final class DynamicAStarPathfinder {
    private DynamicAStarPathfinder() {}

    /**
     * 计算动态路径，优先使用缓存
     */
    public static List<Records.RoadSegmentPlacement> calculateDynamicPath(
            BlockPos startGround,
            BlockPos endGround,
            int width,
            ServerLevel level,
            int maxSteps) {
        
        // 首先检查缓存
        List<Records.RoadSegmentPlacement> cachedPath = PathCache.getCachedPath(startGround, endGround, width);
        if (cachedPath != null) {
            return cachedPath;
        }

        // 计算新路径
        List<Records.RoadSegmentPlacement> path = BasicAStarPathfinder.calculateLandPath(
            startGround, endGround, width, level, maxSteps);
        
        if (path != null && !path.isEmpty()) {
            // 缓存结果
            PathCache.cachePath(startGround, endGround, width, path);
        }
        
        return path;
    }

    /**
     * 增量路径更新 - 当环境变化时重新计算受影响的部分
     */
    public static List<Records.RoadSegmentPlacement> incrementalPathUpdate(
            BlockPos startGround,
            BlockPos endGround,
            int width,
            ServerLevel level,
            int maxSteps,
            Set<BlockPos> changedBlocks) {
        
        if (changedBlocks == null || changedBlocks.isEmpty()) {
            // 没有变化，直接返回缓存或计算路径
            return calculateDynamicPath(startGround, endGround, width, level, maxSteps);
        }

        // 检查变化是否影响当前路径
        List<Records.RoadSegmentPlacement> currentPath = PathCache.getCachedPath(startGround, endGround, width);
        if (currentPath == null) {
            // 没有缓存，重新计算完整路径
            return calculateDynamicPath(startGround, endGround, width, level, maxSteps);
        }

        // 检查变化是否影响路径
        if (!isPathAffected(currentPath, changedBlocks)) {
            return currentPath; // 路径未受影响，直接返回缓存
        }

        // 路径受影响，清除缓存并重新计算
        PathCache.clearPathsFrom(startGround);
        PathCache.clearPathsTo(endGround);
        
        return calculateDynamicPath(startGround, endGround, width, level, maxSteps);
    }

    /**
     * 多目标路径规划 - 计算到多个目标的最优路径
     */
    public static Map<BlockPos, List<Records.RoadSegmentPlacement>> calculateMultiTargetPaths(
            BlockPos startGround,
            Set<BlockPos> targetGrounds,
            int width,
            ServerLevel level,
            int maxSteps) {
        
        Map<BlockPos, List<Records.RoadSegmentPlacement>> results = new HashMap<>();
        
        for (BlockPos target : targetGrounds) {
            List<Records.RoadSegmentPlacement> path = calculateDynamicPath(
                startGround, target, width, level, maxSteps);
            
            if (path != null) {
                results.put(target, path);
            }
        }
        
        return results;
    }

    /**
     * 路径分段计算 - 对于长路径进行分段优化
     */
    public static List<Records.RoadSegmentPlacement> calculateSegmentedPath(
            BlockPos startGround,
            BlockPos endGround,
            int width,
            ServerLevel level,
            int maxSteps,
            int segmentLength) {
        
        // 计算总距离
        double totalDistance = Math.hypot(
            endGround.getX() - startGround.getX(),
            endGround.getZ() - startGround.getZ());
        
        if (totalDistance <= segmentLength) {
            // 距离较短，直接计算
            return calculateDynamicPath(startGround, endGround, width, level, maxSteps);
        }

        // 分段计算
        List<BlockPos> waypoints = generateWaypoints(startGround, endGround, segmentLength);
        List<Records.RoadSegmentPlacement> fullPath = new ArrayList<>();
        
        BlockPos currentStart = startGround;
        for (BlockPos waypoint : waypoints) {
            List<Records.RoadSegmentPlacement> segment = calculateDynamicPath(
                currentStart, waypoint, width, level, maxSteps / waypoints.size());
            
            if (segment == null) {
                // 分段失败，回退到直接计算
                return calculateDynamicPath(startGround, endGround, width, level, maxSteps);
            }
            
            // 避免重复添加连接点
            if (!fullPath.isEmpty()) {
                segment = segment.subList(1, segment.size());
            }
            
            fullPath.addAll(segment);
            currentStart = waypoint;
        }
        
        // 添加最后一段到终点
        List<Records.RoadSegmentPlacement> finalSegment = calculateDynamicPath(
            currentStart, endGround, width, level, maxSteps / waypoints.size());
        
        if (finalSegment != null && !finalSegment.isEmpty()) {
            fullPath.addAll(finalSegment.subList(1, finalSegment.size()));
        }
        
        return fullPath;
    }

    /**
     * 检查路径是否受变化影响
     */
    private static boolean isPathAffected(List<Records.RoadSegmentPlacement> path, Set<BlockPos> changedBlocks) {
        for (Records.RoadSegmentPlacement segment : path) {
            // 检查路径点本身
            if (changedBlocks.contains(segment.position())) {
                return true;
            }
            
            // 检查路径宽度区域
            for (BlockPos widthPos : segment.widthPositions()) {
                if (changedBlocks.contains(widthPos)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 生成路径分段点
     */
    private static List<BlockPos> generateWaypoints(BlockPos start, BlockPos end, int segmentLength) {
        List<BlockPos> waypoints = new ArrayList<>();
        
        double dx = end.getX() - start.getX();
        double dz = end.getZ() - start.getZ();
        double distance = Math.hypot(dx, dz);
        
        if (distance <= segmentLength) {
            return waypoints;
        }
        
        int segments = (int) Math.ceil(distance / segmentLength);
        double stepX = dx / segments;
        double stepZ = dz / segments;
        
        for (int i = 1; i < segments; i++) {
            int x = start.getX() + (int) (stepX * i);
            int z = start.getZ() + (int) (stepZ * i);
            waypoints.add(new BlockPos(x, start.getY(), z));
        }
        
        return waypoints;
    }

    /**
     * 获取路径规划统计信息
     */
    public static PathStats getPathStats() {
        PathCache.CacheStats cacheStats = PathCache.getStats();
        return new PathStats(cacheStats.currentSize, cacheStats.maxSize, cacheStats.usagePercentage);
    }

    /**
     * 路径规划统计信息
     */
    public static final class PathStats {
        public final int cacheSize;
        public final int maxCacheSize;
        public final double cacheUsagePercentage;

        public PathStats(int cacheSize, int maxCacheSize, double cacheUsagePercentage) {
            this.cacheSize = cacheSize;
            this.maxCacheSize = maxCacheSize;
            this.cacheUsagePercentage = cacheUsagePercentage;
        }
    }
}
