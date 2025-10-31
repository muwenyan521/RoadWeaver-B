package net.shiroha233.roadweaver.planning;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.*;

/**
 * 优化的规划算法
 * 集成Delaunay、KNN、RNG算法性能优化和约束系统
 */
public final class OptimizedPlanner {
    private OptimizedPlanner() {}

    /**
     * 优化的Delaunay三角剖分
     */
    public static List<Records.StructureConnection> optimizedDelaunay(
            List<BlockPos> points, 
            int maxEdgeLenBlocks,
            double minAngleDegrees) {
        
        if (points == null || points.size() < 2) return List.of();

        // 使用优化的Delaunay算法
        List<Records.StructureConnection> delaunayEdges = DelaunayPlanner.planDelaunay(points, maxEdgeLenBlocks);
        
        // 应用Gabriel图约束
        List<Records.StructureConnection> gabrielFiltered = 
            GraphConstraints.applyGabrielConstraint(points, delaunayEdges);
        
        // 应用角度约束优化图结构
        return GraphConstraints.optimizeGraphStructure(points, gabrielFiltered, minAngleDegrees);
    }

    /**
     * 优化的KNN规划
     */
    public static List<Records.StructureConnection> optimizedKNN(
            List<BlockPos> points,
            int k,
            int maxEdgeLenBlocks,
            double alpha,
            double minAngleDegrees,
            int degreeCap) {
        
        if (points == null || points.size() < 2 || k <= 0) return List.of();

        // 使用优化的KNN算法
        List<Records.StructureConnection> knnEdges = KNNPlanner.planKNN(
            points, k, maxEdgeLenBlocks, alpha, minAngleDegrees, degreeCap);
        
        // 应用双重约束
        return GraphConstraints.applyDualConstraints(points, knnEdges, minAngleDegrees);
    }

    /**
     * 优化的RNG规划
     */
    public static List<Records.StructureConnection> optimizedRNG(
            List<BlockPos> points,
            int maxEdgeLenBlocks,
            double minAngleDegrees) {
        
        if (points == null || points.size() < 2) return List.of();

        // 使用优化的RNG算法
        List<Records.StructureConnection> rngEdges = RNGPlanner.planRNG(points, maxEdgeLenBlocks);
        
        // 应用角度约束
        return GraphConstraints.applyAngleConstraint(points, rngEdges, minAngleDegrees);
    }

    /**
     * 混合规划算法 - 结合多种算法的优势
     */
    public static List<Records.StructureConnection> hybridPlan(
            List<BlockPos> points,
            int maxEdgeLenBlocks,
            double minAngleDegrees) {
        
        if (points == null || points.size() < 2) return List.of();

        // 使用Delaunay作为基础
        List<Records.StructureConnection> baseEdges = optimizedDelaunay(points, maxEdgeLenBlocks, minAngleDegrees);
        
        // 使用KNN补充连接
        List<Records.StructureConnection> knnEdges = optimizedKNN(points, 3, maxEdgeLenBlocks, 2.0, minAngleDegrees, 4);
        
        // 合并结果
        Set<Long> edgeKeys = new HashSet<>();
        List<Records.StructureConnection> merged = new ArrayList<>();
        
        addUniqueEdges(merged, edgeKeys, baseEdges);
        addUniqueEdges(merged, edgeKeys, knnEdges);
        
        // 确保连通性
        return ensureConnectivity(points, merged, maxEdgeLenBlocks, minAngleDegrees);
    }

    /**
     * 增量图更新 - 当添加新节点时
     */
    public static List<Records.StructureConnection> incrementalGraphUpdate(
            List<BlockPos> existingNodes,
            List<Records.StructureConnection> existingEdges,
            BlockPos newNode,
            int maxEdgeLenBlocks,
            double minAngleDegrees) {
        
        if (existingNodes == null || existingNodes.isEmpty()) {
            return List.of();
        }

        // 使用增量MST更新
        List<Records.StructureConnection> mstUpdated = IncrementalMST.incrementalAdd(
            existingNodes, existingEdges, newNode, maxEdgeLenBlocks);
        
        // 重新应用约束优化
        List<BlockPos> allNodes = new ArrayList<>(existingNodes);
        allNodes.add(newNode);
        
        return GraphConstraints.optimizeGraphStructure(allNodes, mstUpdated, minAngleDegrees);
    }

    /**
     * 性能优化的批量规划
     */
    public static List<Records.StructureConnection> batchOptimizedPlan(
            List<BlockPos> points,
            int maxEdgeLenBlocks,
            double minAngleDegrees,
            int batchSize) {
        
        if (points == null || points.size() < 2) return List.of();

        // 如果点集较小，直接使用混合规划
        if (points.size() <= batchSize) {
            return hybridPlan(points, maxEdgeLenBlocks, minAngleDegrees);
        }

        // 分批次处理大型点集
        List<Records.StructureConnection> allEdges = new ArrayList<>();
        List<List<BlockPos>> batches = partitionPoints(points, batchSize);
        
        for (List<BlockPos> batch : batches) {
            List<Records.StructureConnection> batchEdges = hybridPlan(batch, maxEdgeLenBlocks, minAngleDegrees);
            allEdges.addAll(batchEdges);
        }
        
        // 连接不同批次
        return connectBatches(batches, allEdges, maxEdgeLenBlocks, minAngleDegrees);
    }

    /**
     * 获取规划统计信息
     */
    public static PlanningStats getPlanningStats(
            List<BlockPos> points,
            List<Records.StructureConnection> edges) {
        
        double avgDegree = GraphConstraints.calculateAverageDegree(points, edges);
        int componentCount = GraphConstraints.countConnectedComponents(points, edges);
        double density = calculateGraphDensity(points, edges);
        
        return new PlanningStats(
            points.size(),
            edges.size(),
            avgDegree,
            componentCount,
            density
        );
    }

    /**
     * 分区点集
     */
    private static List<List<BlockPos>> partitionPoints(List<BlockPos> points, int batchSize) {
        List<List<BlockPos>> batches = new ArrayList<>();
        List<BlockPos> sorted = new ArrayList<>(points);
        
        // 按X坐标排序进行空间分区
        sorted.sort(Comparator.comparingInt(BlockPos::getX));
        
        for (int i = 0; i < sorted.size(); i += batchSize) {
            int end = Math.min(i + batchSize, sorted.size());
            batches.add(sorted.subList(i, end));
        }
        
        return batches;
    }

    /**
     * 连接不同批次
     */
    private static List<Records.StructureConnection> connectBatches(
            List<List<BlockPos>> batches,
            List<Records.StructureConnection> batchEdges,
            int maxEdgeLenBlocks,
            double minAngleDegrees) {
        
        if (batches.size() <= 1) {
            return batchEdges;
        }

        // 收集所有点
        List<BlockPos> allPoints = new ArrayList<>();
        for (List<BlockPos> batch : batches) {
            allPoints.addAll(batch);
        }

        // 使用KNN连接不同批次
        List<Records.StructureConnection> connectorEdges = KNNPlanner.connectComponents(
            allPoints, batchEdges, maxEdgeLenBlocks, minAngleDegrees, 2);
        
        List<Records.StructureConnection> result = new ArrayList<>(batchEdges);
        result.addAll(connectorEdges);
        
        return result;
    }

    /**
     * 确保图连通性
     */
    private static List<Records.StructureConnection> ensureConnectivity(
            List<BlockPos> points,
            List<Records.StructureConnection> edges,
            int maxEdgeLenBlocks,
            double minAngleDegrees) {
        
        int componentCount = GraphConstraints.countConnectedComponents(points, edges);
        if (componentCount <= 1) {
            return edges;
        }

        // 使用KNN连接组件
        List<Records.StructureConnection> connected = KNNPlanner.connectComponents(
            points, edges, maxEdgeLenBlocks, minAngleDegrees, 3);
        
        return connected;
    }

    /**
     * 添加唯一边
     */
    private static void addUniqueEdges(List<Records.StructureConnection> target, 
                                      Set<Long> edgeKeys, 
                                      List<Records.StructureConnection> source) {
        
        for (Records.StructureConnection edge : source) {
            long key = PlanningUtils.edgeKey(edge.from(), edge.to());
            if (edgeKeys.add(key)) {
                target.add(edge);
            }
        }
    }

    /**
     * 计算图密度
     */
    private static double calculateGraphDensity(List<BlockPos> points, List<Records.StructureConnection> edges) {
        if (points.size() < 2) return 0.0;
        int n = points.size();
        int maxPossibleEdges = n * (n - 1) / 2;
        return (double) edges.size() / maxPossibleEdges;
    }

    /**
     * 规划统计信息
     */
    public static final class PlanningStats {
        public final int nodeCount;
        public final int edgeCount;
        public final double averageDegree;
        public final int componentCount;
        public final double graphDensity;

        public PlanningStats(int nodeCount, int edgeCount, double averageDegree, 
                           int componentCount, double graphDensity) {
            this.nodeCount = nodeCount;
            this.edgeCount = edgeCount;
            this.averageDegree = averageDegree;
            this.componentCount = componentCount;
            this.graphDensity = graphDensity;
        }
    }
}
