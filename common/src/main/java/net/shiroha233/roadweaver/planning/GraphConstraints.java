package net.shiroha233.roadweaver.planning;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.*;

/**
 * Gabriel图约束和角度约束智能应用
 * 提供图结构优化和几何约束
 */
public final class GraphConstraints {
    private GraphConstraints() {}

    /**
     * 应用Gabriel图约束过滤边
     */
    public static List<Records.StructureConnection> applyGabrielConstraint(
            List<BlockPos> points,
            List<Records.StructureConnection> candidateEdges) {
        
        if (points == null || candidateEdges == null || candidateEdges.isEmpty()) {
            return candidateEdges;
        }

        List<Records.StructureConnection> filtered = new ArrayList<>();
        Map<BlockPos, Integer> pointIndex = buildPointIndex(points);
        
        for (Records.StructureConnection edge : candidateEdges) {
            if (isGabrielEdge(points, pointIndex, edge.from(), edge.to())) {
                filtered.add(edge);
            }
        }
        
        return filtered;
    }

    /**
     * 应用角度约束过滤边
     */
    public static List<Records.StructureConnection> applyAngleConstraint(
            List<BlockPos> points,
            List<Records.StructureConnection> candidateEdges,
            double minAngleDegrees) {
        
        if (points == null || candidateEdges == null || candidateEdges.isEmpty()) {
            return candidateEdges;
        }

        double minCos = Math.cos(Math.toRadians(Math.max(0.0, Math.min(89.0, minAngleDegrees))));
        Map<BlockPos, List<BlockPos>> adjacency = buildAdjacency(points, candidateEdges);
        List<Records.StructureConnection> filtered = new ArrayList<>();
        
        for (Records.StructureConnection edge : candidateEdges) {
            if (satisfiesAngleConstraint(adjacency, edge.from(), edge.to(), minCos)) {
                filtered.add(edge);
            }
        }
        
        return filtered;
    }

    /**
     * 应用Gabriel和角度双重约束
     */
    public static List<Records.StructureConnection> applyDualConstraints(
            List<BlockPos> points,
            List<Records.StructureConnection> candidateEdges,
            double minAngleDegrees) {
        
        List<Records.StructureConnection> gabrielFiltered = applyGabrielConstraint(points, candidateEdges);
        return applyAngleConstraint(points, gabrielFiltered, minAngleDegrees);
    }

    /**
     * 检查边是否满足Gabriel图条件
     */
    private static boolean isGabrielEdge(List<BlockPos> points, Map<BlockPos, Integer> pointIndex, 
                                        BlockPos a, BlockPos b) {
        long ab2 = distanceSquared(a, b);
        
        for (BlockPos c : points) {
            if (c.equals(a) || c.equals(b)) continue;
            
            long ac2 = distanceSquared(a, c);
            long bc2 = distanceSquared(b, c);
            
            // Gabriel条件：圆盘内不能有其他点
            if (ac2 < ab2 && bc2 < ab2) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 检查边是否满足角度约束
     */
    private static boolean satisfiesAngleConstraint(Map<BlockPos, List<BlockPos>> adjacency, 
                                                   BlockPos from, BlockPos to, double minCos) {
        List<BlockPos> neighbors = adjacency.get(from);
        if (neighbors == null || neighbors.isEmpty()) {
            return true;
        }

        // 计算新边的方向向量
        long abx = (long) to.getX() - from.getX();
        long abz = (long) to.getZ() - from.getZ();
        double abLen = Math.hypot((double) abx, (double) abz);
        if (abLen == 0) return false;
        
        double abxN = abx / abLen;
        double abzN = abz / abLen;

        // 检查与所有现有邻居的角度
        for (BlockPos neighbor : neighbors) {
            if (neighbor.equals(to)) continue;
            
            long acx = (long) neighbor.getX() - from.getX();
            long acz = (long) neighbor.getZ() - from.getZ();
            double acLen = Math.hypot((double) acx, (double) acz);
            if (acLen == 0) continue;
            
            double acxN = acx / acLen;
            double aczN = acz / acLen;
            
            double cos = abxN * acxN + abzN * aczN;
            if (cos > minCos) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 优化图结构，移除冗余边
     */
    public static List<Records.StructureConnection> optimizeGraphStructure(
            List<BlockPos> points,
            List<Records.StructureConnection> edges,
            double minAngleDegrees) {
        
        if (edges == null || edges.isEmpty()) {
            return edges;
        }

        // 按长度排序，优先保留短边
        List<Records.StructureConnection> sortedEdges = new ArrayList<>(edges);
        sortedEdges.sort((e1, e2) -> Long.compare(
            distanceSquared(e1.from(), e1.to()),
            distanceSquared(e2.from(), e2.to())
        ));

        Map<BlockPos, List<BlockPos>> adjacency = new HashMap<>();
        for (BlockPos point : points) {
            adjacency.put(point, new ArrayList<>());
        }

        List<Records.StructureConnection> optimized = new ArrayList<>();
        double minCos = Math.cos(Math.toRadians(Math.max(0.0, Math.min(89.0, minAngleDegrees))));

        for (Records.StructureConnection edge : sortedEdges) {
            if (satisfiesAngleConstraint(adjacency, edge.from(), edge.to(), minCos)) {
                optimized.add(edge);
                adjacency.get(edge.from()).add(edge.to());
                adjacency.get(edge.to()).add(edge.from());
            }
        }

        return optimized;
    }

    /**
     * 计算图的平均度数
     */
    public static double calculateAverageDegree(List<BlockPos> points, List<Records.StructureConnection> edges) {
        if (points == null || edges == null || points.isEmpty()) {
            return 0.0;
        }

        Map<BlockPos, Integer> degree = new HashMap<>();
        for (BlockPos point : points) {
            degree.put(point, 0);
        }

        for (Records.StructureConnection edge : edges) {
            degree.put(edge.from(), degree.get(edge.from()) + 1);
            degree.put(edge.to(), degree.get(edge.to()) + 1);
        }

        int totalDegree = 0;
        for (int deg : degree.values()) {
            totalDegree += deg;
        }

        return (double) totalDegree / points.size();
    }

    /**
     * 计算图的连通分量数量
     */
    public static int countConnectedComponents(List<BlockPos> points, List<Records.StructureConnection> edges) {
        if (points == null || points.isEmpty()) {
            return 0;
        }

        Map<BlockPos, List<BlockPos>> adjacency = buildAdjacency(points, edges);
        Set<BlockPos> visited = new HashSet<>();
        int componentCount = 0;

        for (BlockPos point : points) {
            if (!visited.contains(point)) {
                componentCount++;
                dfs(point, adjacency, visited);
            }
        }

        return componentCount;
    }

    private static Map<BlockPos, Integer> buildPointIndex(List<BlockPos> points) {
        Map<BlockPos, Integer> index = new HashMap<>();
        for (int i = 0; i < points.size(); i++) {
            index.put(points.get(i), i);
        }
        return index;
    }

    private static Map<BlockPos, List<BlockPos>> buildAdjacency(List<BlockPos> points, List<Records.StructureConnection> edges) {
        Map<BlockPos, List<BlockPos>> adjacency = new HashMap<>();
        for (BlockPos point : points) {
            adjacency.put(point, new ArrayList<>());
        }

        for (Records.StructureConnection edge : edges) {
            adjacency.get(edge.from()).add(edge.to());
            adjacency.get(edge.to()).add(edge.from());
        }

        return adjacency;
    }

    private static void dfs(BlockPos node, Map<BlockPos, List<BlockPos>> adjacency, Set<BlockPos> visited) {
        visited.add(node);
        for (BlockPos neighbor : adjacency.get(node)) {
            if (!visited.contains(neighbor)) {
                dfs(neighbor, adjacency, visited);
            }
        }
    }

    private static long distanceSquared(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
