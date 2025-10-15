 package net.countered.settlementroads.helpers;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import net.minecraft.core.BlockPos;
 import net.minecraft.server.level.ServerLevel;
 
 import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
 
import net.countered.settlementroads.persistence.WorldDataProvider;
 
public class StructureConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");

    // 按世界维度区分的队列存储
    private static final ConcurrentHashMap<String, Queue<Records.StructureConnection>> worldQueues = new ConcurrentHashMap<>();
    
    /**
     * 获取指定世界的连接队列
     */
    public static Queue<Records.StructureConnection> getQueueForWorld(ServerLevel level) {
        String worldKey = level.dimension().location().toString();
        return worldQueues.computeIfAbsent(worldKey, k -> new ConcurrentLinkedQueue<>());
    }
    
    /**
     * 清理指定世界的队列
     */
    public static void clearQueueForWorld(ServerLevel level) {
        String worldKey = level.dimension().location().toString();
        Queue<Records.StructureConnection> queue = worldQueues.remove(worldKey);
        if (queue != null) {
            queue.clear();
            LOGGER.debug("Cleared queue for world: {}", worldKey);
        }
    }
 
    public static void cacheNewConnection(ServerLevel serverWorld, boolean locateAtPlayer) {
        LOGGER.debug(" cacheNewConnection called, locateAtPlayer={}", locateAtPlayer);
        
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        int beforeCount = dataProvider.getStructureLocations(serverWorld).structureLocations().size();
        
        StructureLocator.locateConfiguredStructure(serverWorld, 1, locateAtPlayer);
        
        Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(serverWorld);
        if (structureLocationData == null) {
            LOGGER.warn(" structureLocationData is null");
            return;
        }
        
        List<BlockPos> locations = structureLocationData.structureLocations();
        int afterCount = locations.size();
        LOGGER.debug("Structure count: before={}, after={}", beforeCount, afterCount);
        
        if (locations == null || locations.size() < 2) {
            LOGGER.debug(" Not enough structures to create connection (need 2, have {})", locations.size());
            return;
        }
        
        createNewStructureConnection(serverWorld);
    }
    
    public static void createNewStructureConnection(ServerLevel serverWorld) {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(serverWorld);
        if (structureLocationData == null) return;
        List<BlockPos> worldStructureLocations = structureLocationData.structureLocations();
        if (worldStructureLocations == null || worldStructureLocations.size() < 2) return;

        // 获取现有连接
        List<Records.StructureConnection> existingConnections = new ArrayList<>(
                Optional.ofNullable(dataProvider.getStructureConnections(serverWorld)).orElseGet(ArrayList::new)
        );
        
        // 如果没有任何连接，使用完整 MST
        if (existingConnections.isEmpty()) {
            LOGGER.info("RoadWeaver: 初始化道路网络，使用 MST 算法 ({} 个结构)", worldStructureLocations.size());
            List<Records.StructureConnection> mstConnections = generateMinimumSpanningTree(worldStructureLocations);
            
            Queue<Records.StructureConnection> queue = getQueueForWorld(serverWorld);
            for (Records.StructureConnection connection : mstConnections) {
                existingConnections.add(connection);
                queue.add(connection);
                
                double distance = Math.sqrt(connection.from().distSqr(connection.to()));
                LOGGER.info("RoadWeaver: 创建道路连接 {} <-> {} (距离: {} 格)",
                        connection.from(), connection.to(), (int) Math.round(distance));
            }
            
            dataProvider.setStructureConnections(serverWorld, existingConnections);
            LOGGER.info("RoadWeaver: 初始化完成，创建 {} 条连接", mstConnections.size());
            return;
        }
        
        // 增量式添加：找出未连接的结构
        Set<BlockPos> connectedStructures = getConnectedStructures(existingConnections);
        List<BlockPos> unconnectedStructures = new ArrayList<>();
        
        for (BlockPos pos : worldStructureLocations) {
            if (!connectedStructures.contains(pos)) {
                unconnectedStructures.add(pos);
            }
        }
        
        if (unconnectedStructures.isEmpty()) {
            LOGGER.debug("RoadWeaver: 所有结构已连接，无需新连接");
            return;
        }
        
        LOGGER.info("RoadWeaver: 增量添加 {} 个新结构到道路网络", unconnectedStructures.size());
        
        // 为每个未连接的结构找到最近的已连接结构
        Queue<Records.StructureConnection> queue = getQueueForWorld(serverWorld);
        int newConnectionCount = 0;
        
        for (BlockPos newStructure : unconnectedStructures) {
            BlockPos nearestConnected = findNearestStructure(newStructure, new ArrayList<>(connectedStructures));
            
            if (nearestConnected != null && !connectionExists(existingConnections, newStructure, nearestConnected)) {
                Records.StructureConnection newConnection = new Records.StructureConnection(newStructure, nearestConnected);
                existingConnections.add(newConnection);
                queue.add(newConnection);
                connectedStructures.add(newStructure); // 添加到已连接集合
                newConnectionCount++;
                
                double distance = Math.sqrt(newStructure.distSqr(nearestConnected));
                LOGGER.info("RoadWeaver: 创建道路连接 {} <-> {} (距离: {} 格)",
                        newStructure, nearestConnected, (int) Math.round(distance));
            }
        }
        
        // 保存更新后的连接
        if (newConnectionCount > 0) {
            dataProvider.setStructureConnections(serverWorld, existingConnections);
            LOGGER.info("RoadWeaver: 已创建 {} 条新道路连接，总计 {} 条连接",
                    newConnectionCount, existingConnections.size());
        }
    }
    
    /**
     * 获取所有已连接的结构
     */
    private static Set<BlockPos> getConnectedStructures(List<Records.StructureConnection> connections) {
        Set<BlockPos> connected = new HashSet<>();
        for (Records.StructureConnection conn : connections) {
            connected.add(conn.from());
            connected.add(conn.to());
        }
        return connected;
    }
    
    /**
     * 找到最近的结构
     */
    private static BlockPos findNearestStructure(BlockPos target, List<BlockPos> candidates) {
        BlockPos nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (BlockPos candidate : candidates) {
            if (!candidate.equals(target)) {
                double distance = target.distSqr(candidate);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = candidate;
                }
            }
        }
        
        return nearest;
    }
    
    /**
     * 使用 Kruskal 算法生成最小生成树
     * 确保所有结构都能互相连通且总路径最短
     */
    private static List<Records.StructureConnection> generateMinimumSpanningTree(List<BlockPos> structures) {
        List<Records.StructureConnection> result = new ArrayList<>();
        if (structures.size() < 2) return result;
        
        // 1. 生成所有可能的边（结构对）
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < structures.size(); i++) {
            for (int j = i + 1; j < structures.size(); j++) {
                BlockPos from = structures.get(i);
                BlockPos to = structures.get(j);
                double distance = Math.sqrt(from.distSqr(to));
                edges.add(new Edge(from, to, distance));
            }
        }
        
        // 2. 按距离排序（从小到大）
        edges.sort(Comparator.comparingDouble(e -> e.distance));
        
        // 3. 使用并查集（Union-Find）避免环路
        UnionFind uf = new UnionFind(structures);
        
        // 4. Kruskal 算法：逐个添加最短的边，直到所有节点连通
        for (Edge edge : edges) {
            if (uf.union(edge.from, edge.to)) {
                result.add(new Records.StructureConnection(edge.from, edge.to));
                
                // 如果已经有 n-1 条边，所有节点已连通
                if (result.size() == structures.size() - 1) {
                    break;
                }
            }
        }
        
        return result;
    }
    
    /**
     * 边（用于 Kruskal 算法）
     */
    private static class Edge {
        final BlockPos from;
        final BlockPos to;
        final double distance;
        
        Edge(BlockPos from, BlockPos to, double distance) {
            this.from = from;
            this.to = to;
            this.distance = distance;
        }
    }
    
    /**
     * 并查集（Union-Find）数据结构
     * 用于检测和合并集合
     */
    private static class UnionFind {
        private final Map<BlockPos, BlockPos> parent = new HashMap<>();
        
        UnionFind(List<BlockPos> nodes) {
            for (BlockPos node : nodes) {
                parent.put(node, node); // 初始化：每个节点的父节点是自己
            }
        }
        
        /**
         * 查找根节点（带路径压缩）
         */
        BlockPos find(BlockPos node) {
            if (!parent.get(node).equals(node)) {
                parent.put(node, find(parent.get(node))); // 路径压缩
            }
            return parent.get(node);
        }
        
        /**
         * 合并两个集合
         * @return true 如果合并成功（原本不在同一集合）
         */
        boolean union(BlockPos a, BlockPos b) {
            BlockPos rootA = find(a);
            BlockPos rootB = find(b);
            
            if (rootA.equals(rootB)) {
                return false; // 已经在同一集合，不需要合并
            }
            
            parent.put(rootA, rootB); // 合并
            return true;
        }
    }
    
    private static boolean connectionExists(List<Records.StructureConnection> existingConnections, BlockPos a, BlockPos b) {
        for (Records.StructureConnection connection : existingConnections) {
            if ((connection.from().equals(a) && connection.to().equals(b)) ||
                (connection.from().equals(b) && connection.to().equals(a))) {
                return true;
            }
        }
        return false;
    }
    
    // 已移除 findClosestStructure 方法，使用最小生成树算法替代
}
