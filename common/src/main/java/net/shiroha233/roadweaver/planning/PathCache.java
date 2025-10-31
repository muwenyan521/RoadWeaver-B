package net.shiroha233.roadweaver.planning;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 路径规划缓存机制
 * 缓存A*算法结果，避免重复计算相同路径
 */
public final class PathCache {
    private PathCache() {}

    private static final ConcurrentHashMap<PathKey, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5分钟

    private static final class CacheEntry {
        final List<Records.RoadSegmentPlacement> path;
        final long timestamp;

        CacheEntry(List<Records.RoadSegmentPlacement> path) {
            this.path = path;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    private static final class PathKey {
        final BlockPos start;
        final BlockPos end;
        final int width;
        final int hash;

        PathKey(BlockPos start, BlockPos end, int width) {
            this.start = start;
            this.end = end;
            this.width = width;
            this.hash = Objects.hash(start, end, width);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PathKey)) return false;
            PathKey other = (PathKey) obj;
            return width == other.width && 
                   start.equals(other.start) && 
                   end.equals(other.end);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /**
     * 获取缓存的路径
     */
    public static List<Records.RoadSegmentPlacement> getCachedPath(BlockPos start, BlockPos end, int width) {
        cleanupExpired();
        
        PathKey key = new PathKey(start, end, width);
        CacheEntry entry = CACHE.get(key);
        
        if (entry != null && !entry.isExpired()) {
            return new ArrayList<>(entry.path);
        }
        
        return null;
    }

    /**
     * 缓存路径
     */
    public static void cachePath(BlockPos start, BlockPos end, int width, List<Records.RoadSegmentPlacement> path) {
        cleanupExpired();
        
        if (CACHE.size() >= MAX_CACHE_SIZE) {
            evictOldest();
        }
        
        PathKey key = new PathKey(start, end, width);
        CACHE.put(key, new CacheEntry(new ArrayList<>(path)));
    }

    /**
     * 清除指定起点的所有缓存路径
     */
    public static void clearPathsFrom(BlockPos start) {
        CACHE.keySet().removeIf(key -> key.start.equals(start));
    }

    /**
     * 清除指定终点的所有缓存路径
     */
    public static void clearPathsTo(BlockPos end) {
        CACHE.keySet().removeIf(key -> key.end.equals(end));
    }

    /**
     * 清除指定区域内的所有缓存路径
     */
    public static void clearPathsInArea(BlockPos center, int radius) {
        CACHE.keySet().removeIf(key -> 
            isInArea(key.start, center, radius) || 
            isInArea(key.end, center, radius)
        );
    }

    /**
     * 清除所有缓存
     */
    public static void clearAll() {
        CACHE.clear();
    }

    /**
     * 获取缓存统计信息
     */
    public static CacheStats getStats() {
        cleanupExpired();
        return new CacheStats(CACHE.size(), MAX_CACHE_SIZE);
    }

    private static void cleanupExpired() {
        CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static void evictOldest() {
        if (CACHE.isEmpty()) return;

        Map.Entry<PathKey, CacheEntry> oldest = null;
        for (Map.Entry<PathKey, CacheEntry> entry : CACHE.entrySet()) {
            if (oldest == null || entry.getValue().timestamp < oldest.getValue().timestamp) {
                oldest = entry;
            }
        }
        
        if (oldest != null) {
            CACHE.remove(oldest.getKey());
        }
    }

    private static boolean isInArea(BlockPos pos, BlockPos center, int radius) {
        return Math.abs(pos.getX() - center.getX()) <= radius && 
               Math.abs(pos.getZ() - center.getZ()) <= radius;
    }

    /**
     * 缓存统计信息
     */
    public static final class CacheStats {
        public final int currentSize;
        public final int maxSize;
        public final double usagePercentage;

        public CacheStats(int currentSize, int maxSize) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.usagePercentage = maxSize > 0 ? (double) currentSize / maxSize * 100.0 : 0.0;
        }
    }
}
