package net.shiroha233.roadweaver.planning;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.*;

/**
 * 增量最小生成树更新系统
 * 支持动态添加/删除节点，避免全图重算
 */
public final class IncrementalMST {
    private IncrementalMST() {}

    /**
     * 增量更新MST，添加新节点
     */
    public static List<Records.StructureConnection> incrementalAdd(
            List<BlockPos> existingNodes,
            List<Records.StructureConnection> existingMST,
            BlockPos newNode,
            int maxEdgeLenBlocks) {
        
        if (existingNodes == null || existingNodes.isEmpty()) {
            return List.of();
        }

        long maxD2 = maxEdgeLenBlocks > 0 ? (long) maxEdgeLenBlocks * (long) maxEdgeLenBlocks : Long.MAX_VALUE;
        
        // 找到新节点到现有MST的最小边
        Records.StructureConnection minEdge = null;
        long minDist2 = Long.MAX_VALUE;
        
        for (BlockPos node : existingNodes) {
            long d2 = distanceSquared(node, newNode);
            if (d2 <= maxD2 && d2 < minDist2) {
                minDist2 = d2;
                minEdge = new Records.StructureConnection(node, newNode);
            }
        }
        
        if (minEdge == null) {
            return existingMST;
        }
        
        // 构建新MST
        List<Records.StructureConnection> newMST = new ArrayList<>(existingMST);
        newMST.add(minEdge);
        
        return newMST;
    }

    /**
     * 增量更新MST，删除节点
     */
    public static List<Records.StructureConnection> incrementalRemove(
            List<BlockPos> remainingNodes,
            List<Records.StructureConnection> existingMST,
            BlockPos removedNode,
            int maxEdgeLenBlocks) {
        
        if (remainingNodes.size() < 2) {
            return List.of();
        }

        // 移除与被删除节点相关的边
        List<Records.StructureConnection> filteredMST = new ArrayList<>();
        for (Records.StructureConnection edge : existingMST) {
            if (!edge.from().equals(removedNode) && !edge.to().equals(removedNode)) {
                filteredMST.add(edge);
            }
        }
        
        // 检查连通性，如果图不连通则重新连接
        return reconnectComponents(remainingNodes, filteredMST, maxEdgeLenBlocks);
    }

    /**
     * 重新连接不连通的组件
     */
    private static List<Records.StructureConnection> reconnectComponents(
            List<BlockPos> nodes,
            List<Records.StructureConnection> baseEdges,
            int maxEdgeLenBlocks) {
        
        int n = nodes.size();
        if (n < 2) return baseEdges;

        long maxD2 = maxEdgeLenBlocks > 0 ? (long) maxEdgeLenBlocks * (long) maxEdgeLenBlocks : Long.MAX_VALUE;
        
        // 构建邻接表
        Map<BlockPos, List<BlockPos>> adj = new HashMap<>();
        for (BlockPos node : nodes) {
            adj.put(node, new ArrayList<>());
        }
        
        for (Records.StructureConnection edge : baseEdges) {
            adj.get(edge.from()).add(edge.to());
            adj.get(edge.to()).add(edge.from());
        }
        
        // 查找连通分量
        List<Set<BlockPos>> components = findConnectedComponents(nodes, adj);
        if (components.size() <= 1) {
            return baseEdges;
        }
        
        // 找到连接不同组件的最小边
        List<Records.StructureConnection> newEdges = new ArrayList<>(baseEdges);
        Set<Long> existingKeys = new HashSet<>();
        for (Records.StructureConnection edge : baseEdges) {
            existingKeys.add(edgeKey(edge.from(), edge.to()));
        }
        
        for (int i = 0; i < components.size(); i++) {
            for (int j = i + 1; j < components.size(); j++) {
                Records.StructureConnection bridge = findMinBridge(components.get(i), components.get(j), maxD2);
                if (bridge != null) {
                    long key = edgeKey(bridge.from(), bridge.to());
                    if (!existingKeys.contains(key)) {
                        newEdges.add(bridge);
                        existingKeys.add(key);
                    }
                }
            }
        }
        
        return newEdges;
    }

    /**
     * 查找连通分量
     */
    private static List<Set<BlockPos>> findConnectedComponents(List<BlockPos> nodes, Map<BlockPos, List<BlockPos>> adj) {
        List<Set<BlockPos>> components = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        
        for (BlockPos node : nodes) {
            if (!visited.contains(node)) {
                Set<BlockPos> component = new HashSet<>();
                dfs(node, adj, visited, component);
                components.add(component);
            }
        }
        
        return components;
    }

    private static void dfs(BlockPos node, Map<BlockPos, List<BlockPos>> adj, Set<BlockPos> visited, Set<BlockPos> component) {
        visited.add(node);
        component.add(node);
        
        for (BlockPos neighbor : adj.get(node)) {
            if (!visited.contains(neighbor)) {
                dfs(neighbor, adj, visited, component);
            }
        }
    }

    /**
     * 找到连接两个组件的最小边
     */
    private static Records.StructureConnection findMinBridge(Set<BlockPos> comp1, Set<BlockPos> comp2, long maxD2) {
        Records.StructureConnection minEdge = null;
        long minDist2 = Long.MAX_VALUE;
        
        for (BlockPos node1 : comp1) {
            for (BlockPos node2 : comp2) {
                long d2 = distanceSquared(node1, node2);
                if (d2 <= maxD2 && d2 < minDist2) {
                    minDist2 = d2;
                    minEdge = new Records.StructureConnection(node1, node2);
                }
            }
        }
        
        return minEdge;
    }

    private static long distanceSquared(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static long edgeKey(BlockPos a, BlockPos b) {
        long ka = pos2dKey(a);
        long kb = pos2dKey(b);
        long lo = Math.min(ka, kb);
        long hi = Math.max(ka, kb);
        return (hi << 1) ^ lo;
    }

    private static long pos2dKey(BlockPos p) {
        long x = p.getX();
        long z = p.getZ();
        return (x << 32) ^ (z & 0xffffffffL);
    }
}
