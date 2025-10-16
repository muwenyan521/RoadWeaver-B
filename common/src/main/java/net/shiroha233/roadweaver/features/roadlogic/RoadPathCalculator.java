/*
 * This file contains code derived from RoadArchitect
 * Copyright (c) 2024 Shadscure
 * Licensed under the Apache License, Version 2.0
 * 
 * Original source: https://github.com/Shadscure/RoadArchitect
 * Original file: modules/common/src/main/java/net/oxcodsnet/roadarchitect/util/PathFinder.java
 * 
 * Modifications:
 * - Adapted for Minecraft 1.20.1 Architectury project structure
 * - Integrated with RoadWeaver's configuration system
 * - Added manual mode with configurable parameters
 * - Simplified grid system (NEIGHBOR_DISTANCE = 4 vs GRID_STEP = 4)
 * - Added support for cross-water pathfinding
 * 
 * Copyright (c) 2025 shiroha-233 (RoadWeaver modifications)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shiroha233.roadweaver.features.roadlogic;

import net.shiroha233.roadweaver.config.ConfigProvider;
import net.shiroha233.roadweaver.config.IModConfig;
import net.shiroha233.roadweaver.helpers.Records;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 道路路径计算器
 * <p>
 * 使用A*算法计算两点之间的最优道路路径。
 * 考虑地形高度、地形稳定性、生物群系类型等因素。
 * </p>
 * <p>
 * 核心特性：
 * <ul>
 *   <li>A*寻路算法：启发式搜索最短路径</li>
 *   <li>高度缓存：避免重复采样地形高度</li>
 *   <li>地形适应：考虑高度差和地形稳定性</li>
 *   <li>生物群系感知：水域增加额外成本</li>
 * </ul>
 * </p>
 * 
 * @see Records.RoadSegmentPlacement
 * @see Node
 */
public class RoadPathCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");

    // ========== 路径计算常量 ==========
    
    /** 相邻节点之间的距离（方块数） */
    private static final int NEIGHBOR_DISTANCE = 4;
    
    /** 水域生物群系的基础成本倍数 */
    private static final int WATER_BIOME_COST = 50;
    
    /** 高度差的成本倍数 */
    private static final int ELEVATION_COST_MULTIPLIER = 40;
    
    /** 生物群系成本倍数 */
    private static final int BIOME_COST_MULTIPLIER = 8;
    
    /** 海平面高度的成本倍数 */
    private static final int SEA_LEVEL_COST_MULTIPLIER = 8;
    
    /** 地形稳定性成本倍数 */
    private static final int TERRAIN_STABILITY_MULTIPLIER = 16;
    
    /** 对角线移动的成本倍数 */
    private static final double DIAGONAL_COST_MULTIPLIER = 1.5;
    
    /** 启发式估算中的距离倍数 */
    private static final int HEURISTIC_DISTANCE_MULTIPLIER = 30;
    
    /** 启发式估算中的对角线优化系数 */
    private static final double HEURISTIC_DIAGONAL_FACTOR = 0.6;
    
    // ========== 缓存 ==========
    
    /** 高度值缓存：映射哈希后的(x,z)坐标到高度(y) */
    public static final Map<Long, Integer> heightCache = new ConcurrentHashMap<>();

    /**
     * 将x和z坐标哈希为一个long值
     * <p>
     * 用于高度缓存的键值，将2D坐标映射为唯一的long值。
     * </p>
     * 
     * @param x X坐标
     * @param z Z坐标
     * @return 哈希后的long值
     */
    private static long hashXZ(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * 计算A*道路路径（向后兼容重载）
     * <p>
     * 使用配置文件中的默认参数进行路径计算。
     * </p>
     * 
     * @param start 起点坐标
     * @param end 终点坐标
     * @param width 道路宽度
     * @param serverWorld 服务器世界
     * @param maxSteps 最大搜索步数
     * @return 道路段列表，如果未找到路径则返回空列表
     */
    public static List<Records.RoadSegmentPlacement> calculateAStarRoadPath(
            BlockPos start, BlockPos end, int width, ServerLevel serverWorld, int maxSteps
    ) {
        IModConfig cfg = ConfigProvider.get();
        return calculateAStarRoadPath(start, end, width, serverWorld, maxSteps,
                cfg.maxHeightDifference(), cfg.maxTerrainStability(), false);
    }

    /**
     * 计算A*道路路径（不忽略水域）
     * 
     * @param start 起点坐标
     * @param end 终点坐标
     * @param width 道路宽度
     * @param serverWorld 服务器世界
     * @param maxSteps 最大搜索步数
     * @param maxHeightDifference 最大允许高度差
     * @param maxTerrainStability 最大地形稳定性值
     * @return 道路段列表，如果未找到路径则返回空列表
     */
    public static List<Records.RoadSegmentPlacement> calculateAStarRoadPath(
            BlockPos start, BlockPos end, int width, ServerLevel serverWorld, int maxSteps,
            int maxHeightDifference, int maxTerrainStability
    ) {
        return calculateAStarRoadPath(start, end, width, serverWorld, maxSteps,
                maxHeightDifference, maxTerrainStability, false);
    }

    /**
     * 计算A*道路路径（完整版本）
     * <p>
     * 使用A*算法在起点和终点之间寻找最优道路路径。
     * 算法考虑多个因素：
     * <ul>
     *   <li>高度差：陡峭地形增加成本</li>
     *   <li>地形稳定性：周围高度变化大的位置成本高</li>
     *   <li>生物群系：水域（河流、海洋）增加成本</li>
     *   <li>海平面：在海平面高度放置道路增加成本</li>
     * </ul>
     * </p>
     * <p>
     * 算法步骤：
     * <ol>
     *   <li>将起点和终点对齐到网格</li>
     *   <li>初始化开放集和关闭集</li>
     *   <li>使用优先队列按f值排序</li>
     *   <li>扩展当前最优节点的邻居</li>
     *   <li>重建路径并生成道路段</li>
     * </ol>
     * </p>
     * 
     * @param start 起点坐标
     * @param end 终点坐标
     * @param width 道路宽度（方块数）
     * @param serverWorld 服务器世界
     * @param maxSteps 最大搜索步数（防止无限循环）
     * @param maxHeightDifference 最大允许高度差（方块数）
     * @param maxTerrainStability 最大地形稳定性值
     * @param ignoreWater 是否忽略水域成本（用于跨海连接）
     * @return 道路段列表，如果未找到路径则返回空列表
     */
    public static List<Records.RoadSegmentPlacement> calculateAStarRoadPath(
            BlockPos start, BlockPos end, int width, ServerLevel serverWorld, int maxSteps,
            int maxHeightDifference, int maxTerrainStability, boolean ignoreWater
    ) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, List<BlockPos>> interpolatedSegments = new HashMap<>();

        int startX = snapToGrid(start.getX(), NEIGHBOR_DISTANCE);
        int startZ = snapToGrid(start.getZ(), NEIGHBOR_DISTANCE);
        int endX = snapToGrid(end.getX(), NEIGHBOR_DISTANCE);
        int endZ = snapToGrid(end.getZ(), NEIGHBOR_DISTANCE);

        start = new BlockPos(startX, start.getY(), startZ);
        end = new BlockPos(endX, end.getY(), endZ);

        BlockPos startGround = new BlockPos(start.getX(), heightSampler(start.getX(), start.getZ(), serverWorld), start.getZ());
        BlockPos endGround = new BlockPos(end.getX(), heightSampler(end.getX(), end.getZ(), serverWorld), end.getZ());

        Node startNode = new Node(startGround, null, 0.0, heuristic(startGround, endGround));
        openSet.add(startNode);
        allNodes.put(startGround, startNode);

        int d = NEIGHBOR_DISTANCE;
        int[][] neighborOffsets = {
                {d, 0}, {-d, 0}, {0, d}, {0, -d},
                {d, d}, {d, -d}, {-d, d}, {-d, -d}
        };

        while (!openSet.isEmpty() && maxSteps-- > 0) {
            Node current = openSet.poll();

            if (current.pos.offset(0, -current.pos.getY(), 0).distManhattan(endGround.offset(0, -endGround.getY(), 0)) < NEIGHBOR_DISTANCE * 2) {
                LOGGER.debug("Found path! {}", current.pos);
                return reconstructPath(current, width, interpolatedSegments);
            }

            closedSet.add(current.pos);
            allNodes.remove(current.pos);

            for (int[] offset : neighborOffsets) {
                BlockPos neighborXZ = current.pos.offset(offset[0], 0, offset[1]);
                int y = heightSampler(neighborXZ.getX(), neighborXZ.getZ(), serverWorld);
                BlockPos neighborPos = new BlockPos(neighborXZ.getX(), y, neighborXZ.getZ());
                if (closedSet.contains(neighborPos)) continue;

                Holder<Biome> biomeHolder = biomeSampler(neighborPos, serverWorld);
                boolean isWater = biomeHolder.is(BiomeTags.IS_RIVER)
                        || biomeHolder.is(BiomeTags.IS_OCEAN)
                        || biomeHolder.is(BiomeTags.IS_DEEP_OCEAN);
                // 水域成本：如果绕路成本更高，仍会选择穿过水域
                // 手动模式且忽略水域时，水域成本为0（用于跨海连接）
                int biomeCost = (isWater && !ignoreWater) ? WATER_BIOME_COST : 0;
                int elevation = Math.abs(y - current.pos.getY());
                if (elevation > maxHeightDifference) {
                    continue;
                }
                int offsetSum = Math.abs(Math.abs(offset[0])) + Math.abs(offset[1]);
                double stepCost = (offsetSum == 2 * NEIGHBOR_DISTANCE) ? DIAGONAL_COST_MULTIPLIER : 1;
                int terrainStabilityCost = calculateTerrainStability(neighborPos, y, serverWorld);
                if (terrainStabilityCost > maxTerrainStability) {
                    continue;
                }
                int yLevelCost = y == serverWorld.getSeaLevel() ? 20 : 0;
                double tentativeG = current.gScore + stepCost
                        + elevation * ELEVATION_COST_MULTIPLIER
                        + biomeCost * BIOME_COST_MULTIPLIER
                        + yLevelCost * SEA_LEVEL_COST_MULTIPLIER
                        + terrainStabilityCost * TERRAIN_STABILITY_MULTIPLIER;

                Node neighbor = allNodes.get(neighborPos);
                if (neighbor == null || tentativeG < neighbor.gScore) {
                    double h = heuristic(neighborPos, endGround);
                    neighbor = new Node(neighborPos, current, tentativeG, tentativeG + h);
                    allNodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);

                    List<BlockPos> segmentPoints = new ArrayList<>();
                    for (int i = 1; i < NEIGHBOR_DISTANCE; i++) {
                        int interpX = current.pos.getX() + (offset[0] * i) / NEIGHBOR_DISTANCE;
                        int interpZ = current.pos.getZ() + (offset[1] * i) / NEIGHBOR_DISTANCE;
                        BlockPos interpolated = new BlockPos(interpX, current.pos.getY(), interpZ);
                        segmentPoints.add(interpolated);
                    }
                    interpolatedSegments.put(neighborPos, segmentPoints);
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * 启发式函数：估算从位置a到位置b的成本
     * <p>
     * 使用曼哈顿距离的变体，考虑对角线移动的优化。
     * 这个估算值必须小于等于实际成本（admissible heuristic），
     * 以保证A*算法找到最优路径。
     * </p>
     * 
     * @param a 起始位置
     * @param b 目标位置
     * @return 估算的成本值
     */
    private static double heuristic(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        // 曼哈顿距离 - 对角线优化
        double dxzApprox = Math.abs(dx) + Math.abs(dz) - HEURISTIC_DIAGONAL_FACTOR * Math.min(Math.abs(dx), Math.abs(dz));
        return dxzApprox * HEURISTIC_DISTANCE_MULTIPLIER;
    }

    private static int calculateTerrainStability(BlockPos neighborPos, int y, ServerLevel serverWorld) {
        int cost = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos testPos = neighborPos.relative(direction);
            int testY = heightSampler(testPos.getX(), testPos.getZ(), serverWorld);
            int elevation = Math.abs(y - testY);
            cost += elevation;
        }
        return cost;
    }

    private static List<Records.RoadSegmentPlacement> reconstructPath(
            Node endNode, int width, Map<BlockPos, List<BlockPos>> interpolatedPathMap
    ) {
        List<Node> pathNodes = new ArrayList<>();
        Node current = endNode;
        while (current != null) {
            pathNodes.add(current);
            current = current.parent;
        }
        Collections.reverse(pathNodes);

        Map<BlockPos, Set<BlockPos>> roadSegments = new LinkedHashMap<>();
        Set<BlockPos> widthCache = new HashSet<>();

        for (Node node : pathNodes) {
            BlockPos pos = node.pos;
            List<BlockPos> interpolated = interpolatedPathMap.getOrDefault(pos, Collections.emptyList());
            RoadDirection roadDirection = RoadDirection.X_AXIS;
            if (!interpolated.isEmpty()) {
                BlockPos firstInterpolated = interpolated.get(0);
                int dx = pos.getX() - firstInterpolated.getX();
                int dz = pos.getZ() - firstInterpolated.getZ();

                if ((dx < 0 && dz > 0) || (dx > 0 && dz < 0)) {
                    roadDirection = RoadDirection.DIAGONAL_1;
                } else if ((dx < 0 && dz < 0) || (dx > 0 && dz > 0)) {
                    roadDirection = RoadDirection.DIAGONAL_2;
                } else if (dx == 0 && dz != 0) {
                    roadDirection = RoadDirection.Z_AXIS;
                }

                for (BlockPos interp : interpolated) {
                    Set<BlockPos> widthSetInterp = generateWidth(interp, width / 2, widthCache, roadDirection);
                    roadSegments.put(interp, widthSetInterp);
                }
            }

            Set<BlockPos> widthSet = generateWidth(pos, width / 2, widthCache, roadDirection);
            roadSegments.put(pos, widthSet);
        }

        List<Records.RoadSegmentPlacement> result = new ArrayList<>();
        for (Map.Entry<BlockPos, Set<BlockPos>> entry : roadSegments.entrySet()) {
            result.add(new Records.RoadSegmentPlacement(entry.getKey(), new ArrayList<>(entry.getValue())));
        }
        return result;
    }

    // Height sampler method - improved with sea level handling
    private static int heightSampler(int x, int z, ServerLevel serverWorld) {
        long key = hashXZ(x, z);
        return heightCache.computeIfAbsent(key, k -> {
            int seaLevel = serverWorld.getSeaLevel();
            int oceanFloorHeight = serverWorld.getChunkSource()
                    .getGenerator()
                    .getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, serverWorld, serverWorld.getChunkSource().randomState());
            int worldSurfaceHeight = serverWorld.getChunkSource()
                    .getGenerator()
                    .getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, serverWorld, serverWorld.getChunkSource().randomState());

            if (worldSurfaceHeight <= seaLevel && oceanFloorHeight < seaLevel) {
                return seaLevel;
            }
            return worldSurfaceHeight;
        });
    }

    private static Holder<Biome> biomeSampler(BlockPos pos, ServerLevel serverWorld) {
        return serverWorld.getBiome(pos);
    }

    private static class Node {
        BlockPos pos;
        Node parent;
        double gScore, fScore;

        Node(BlockPos pos, Node parent, double gScore, double fScore) {
            this.pos = pos;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    private static int snapToGrid(int value, int gridSize) {
        return Math.floorDiv(value, gridSize) * gridSize;
    }

    private static Set<BlockPos> generateWidth(BlockPos center, int radius, Set<BlockPos> widthPositionsCache, RoadDirection direction) {
        Set<BlockPos> segmentWidthPositions = new HashSet<>();
        int centerX = center.getX();
        int centerZ = center.getZ();
        int y = 0;

        if (direction == RoadDirection.X_AXIS) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = new BlockPos(centerX, y, centerZ + dz);
                if (!widthPositionsCache.contains(pos)) {
                    widthPositionsCache.add(pos);
                    segmentWidthPositions.add(pos);
                }
            }
        } else if (direction == RoadDirection.Z_AXIS) {
            for (int dx = -radius; dx <= radius; dx++) {
                BlockPos pos = new BlockPos(centerX + dx, y, centerZ);
                if (!widthPositionsCache.contains(pos)) {
                    widthPositionsCache.add(pos);
                    segmentWidthPositions.add(pos);
                }
            }
        } else {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (direction == RoadDirection.DIAGONAL_2) {
                        if ((dx == -radius && dz == -radius) || (dx == radius && dz == radius)) {
                            continue;
                        }
                    }
                    if (direction == RoadDirection.DIAGONAL_1) {
                        if ((dx == -radius && dz == radius) || (dx == radius && dz == -radius)) {
                            continue;
                        }
                    }
                    BlockPos pos = new BlockPos(centerX + dx, y, centerZ + dz);
                    if (!widthPositionsCache.contains(pos)) {
                        widthPositionsCache.add(pos);
                        segmentWidthPositions.add(pos);
                    }
                }
            }
        }
        return segmentWidthPositions;
    }
}
