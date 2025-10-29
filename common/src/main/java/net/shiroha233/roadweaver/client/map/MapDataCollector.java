package net.shiroha233.roadweaver.client.map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaver.search.StructurePredictor;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.planning.RoadPlanningService;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public final class MapDataCollector {
    private MapDataCollector() {}

    public static MapSnapshot build(ServerLevel level) {
        WorldDataProvider provider = WorldDataProvider.getInstance();
        Records.StructureLocationData loc = provider.getStructureLocations(level);
        List<Records.StructureConnection> connections = provider.getStructureConnections(level);
        List<BlockPos> structures = (loc != null) ? new ArrayList<>(loc.structureLocations()) : new ArrayList<>();
        List<Records.StructureConnection> conns = (connections != null) ? new ArrayList<>(connections) : new ArrayList<>();
        List<Records.StructureInfo> infos = (loc != null) ? new ArrayList<>(loc.structureInfos()) : new ArrayList<>();
        List<List<BlockPos>> roads = new ArrayList<>();
        List<Records.RoadData> roadDataList = provider.getRoadDataList(level);
        if (roadDataList != null) {
            for (Records.RoadData rd : roadDataList) {
                List<Records.RoadSegmentPlacement> segs = rd.roadSegmentList();
                if (segs == null || segs.isEmpty()) continue;
                ArrayList<BlockPos> poly = new ArrayList<>(segs.size());
                for (Records.RoadSegmentPlacement sp : segs) poly.add(sp.middlePos());
                if (poly.size() >= 2) roads.add(poly);
            }
        }

        if (Level.OVERWORLD.equals(level.dimension())) {
            ModConfig cfg = ConfigService.get();
            if (cfg.villagePredictionEnabled()) {
                List<Records.StructureInfo> predicted = StructurePredictor.predictOverworldStructuresAroundSpawn(
                        level,
                        cfg.predictRadiusChunks(),
                        cfg.biomePrefilter(),
                        cfg.structureWhitelist(),
                        cfg.structureBlacklist()
                );
                if (!predicted.isEmpty()) {
                    Set<BlockPos> existing = new HashSet<>(structures);
                    for (Records.StructureInfo info : predicted) {
                        BlockPos p = info.pos();
                        if (!existing.contains(p)) {
                            structures.add(p);
                            infos.add(info);
                            existing.add(p);
                        }
                    }
                }
            }
        }
        return new MapSnapshot(structures, conns, infos, roads);
    }

    public static MapSnapshot build(ServerLevel level, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        WorldDataProvider provider = WorldDataProvider.getInstance();
        Records.StructureLocationData loc = provider.getStructureLocations(level);
        List<Records.StructureConnection> connections = provider.getStructureConnections(level);

        List<BlockPos> structures = new ArrayList<>();
        if (loc != null && loc.structureLocations() != null) {
            for (BlockPos p : loc.structureLocations()) {
                int x = p.getX(), z = p.getZ();
                if (x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ) structures.add(p);
            }
        }

        List<Records.StructureConnection> conns = new ArrayList<>();
        if (connections != null) {
            for (Records.StructureConnection c : connections) {
                BlockPos a = c.from();
                BlockPos b = c.to();
                boolean ina = a.getX() >= minBlockX && a.getX() <= maxBlockX && a.getZ() >= minBlockZ && a.getZ() <= maxBlockZ;
                boolean inb = b.getX() >= minBlockX && b.getX() <= maxBlockX && b.getZ() >= minBlockZ && b.getZ() <= maxBlockZ;
                if (ina || inb) conns.add(c);
            }
        }

        List<Records.StructureInfo> infos = new ArrayList<>();
        if (loc != null && loc.structureInfos() != null) {
            for (Records.StructureInfo info : loc.structureInfos()) {
                BlockPos p = info.pos();
                int x = p.getX(), z = p.getZ();
                if (x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ) infos.add(info);
            }
        }

        if (Level.OVERWORLD.equals(level.dimension())) {
            ModConfig cfg = ConfigService.get();
            if (cfg.villagePredictionEnabled()) {
                int cminx = Math.floorDiv(minBlockX, 16);
                int cminz = Math.floorDiv(minBlockZ, 16);
                int cmaxx = Math.floorDiv(maxBlockX, 16);
                int cmaxz = Math.floorDiv(maxBlockZ, 16);
                List<Records.StructureInfo> predicted = StructurePredictor.predictOverworldStructuresInRect(
                        level,
                        cminx, cminz, cmaxx, cmaxz,
                        cfg.biomePrefilter(),
                        cfg.structureWhitelist(),
                        cfg.structureBlacklist()
                );
                if (!predicted.isEmpty()) {
                    Set<BlockPos> existing = new HashSet<>(structures);
                    for (Records.StructureInfo info : predicted) {
                        BlockPos p = info.pos();
                        if (!existing.contains(p)) {
                            int x = p.getX(), z = p.getZ();
                            if (x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ) {
                                structures.add(p);
                                infos.add(info);
                                existing.add(p);
                            }
                        }
                    }
                }
            }
        }

        List<List<BlockPos>> roads = new ArrayList<>();
        List<Records.RoadData> roadDataList = provider.getRoadDataList(level);
        if (roadDataList != null) {
            for (Records.RoadData rd : roadDataList) {
                List<Records.RoadSegmentPlacement> segs = rd.roadSegmentList();
                if (segs == null || segs.isEmpty()) continue;
                ArrayList<BlockPos> poly = new ArrayList<>(segs.size());
                for (Records.RoadSegmentPlacement sp : segs) {
                    BlockPos p = sp.middlePos();
                    int x = p.getX(), z = p.getZ();
                    if (x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ) poly.add(p);
                }
                if (poly.size() >= 2) roads.add(poly);
            }
        }

        return new MapSnapshot(structures, conns, infos, roads);
    }

    public static MapSnapshot build(ServerLevel level,
                                    int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ,
                                    int centerX, int centerZ, int radiusBlocks) {
        WorldDataProvider provider = WorldDataProvider.getInstance();
        Records.StructureLocationData loc = provider.getStructureLocations(level);
        List<Records.StructureConnection> connections = provider.getStructureConnections(level);

        final long r2 = (long) Math.max(0, radiusBlocks) * (long) Math.max(0, radiusBlocks);
        java.util.function.BiPredicate<Integer, Integer> inAOI = (x, z) -> {
            if (r2 <= 0) return true;
            long dx = (long) x - centerX;
            long dz = (long) z - centerZ;
            return dx * dx + dz * dz <= r2;
        };

        // 先构建“已规划端点”集合（任何状态）
        java.util.Set<BlockPos> plannedEndpoints = new java.util.HashSet<>();
        if (connections != null) {
            for (Records.StructureConnection c : connections) {
                plannedEndpoints.add(c.from());
                plannedEndpoints.add(c.to());
            }
        }

        // 计算“已触发规划覆盖”判断：
        // - 初始规划：以出生点为中心、initialPlanRadius
        // - 动态规划：以每个已规划过的 tile 为中心，取 dynamicPlanRadius 构成矩形并取并集（近似重构历史计划矩形）
        // - 兼顾端点近邻（小范围补偿）
        ModConfig cfgAll = ConfigService.get();
        int initialRadiusBlocks = Math.max(1, cfgAll.initialPlanRadiusChunks()) * 16;
        int strideChunks = RoadPlanningService.getStrideTileSizeChunks();
        int dynRadiusChunks = RoadPlanningService.getDynamicPlanRadiusChunks();
        int dynRadiusBlocks = Math.max(1, dynRadiusChunks) * 16;
        long initialR2 = (long) initialRadiusBlocks * (long) initialRadiusBlocks;
        BlockPos spawn = level.getSharedSpawnPos();

        // 预计算已规划 tiles 对应的近似矩形（块坐标）
        java.util.List<int[]> plannedRects = new java.util.ArrayList<>();
        java.util.HashSet<Long> rectKeys = new java.util.HashSet<>();
        java.util.Map<Long, Long> centersMap = RoadPlanningService.getPlannedTileCenters(level);
        for (Long key : RoadPlanningService.getPlannedTiles(level)) {
            int kx = (int) (key >> 32);
            int kz = (int) (key & 0xffffffffL);
            int centerChunkX;
            int centerChunkZ;
            Long cval = centersMap.get(key);
            if (cval != null) {
                centerChunkX = (int) (cval >> 32);
                centerChunkZ = (int) (cval & 0xffffffffL);
            } else {
                centerChunkX = kx * strideChunks + strideChunks / 2;
                centerChunkZ = kz * strideChunks + strideChunks / 2;
            }
            int cxBlocks = centerChunkX * 16;
            int czBlocks = centerChunkZ * 16;
            int minBx = cxBlocks - dynRadiusBlocks;
            int maxBx = cxBlocks + dynRadiusBlocks;
            int minBz = czBlocks - dynRadiusBlocks;
            int maxBz = czBlocks + dynRadiusBlocks;
            plannedRects.add(new int[]{minBx, minBz, maxBx, maxBz});
            rectKeys.add(key);
        }

        java.util.function.BiPredicate<Integer, Integer> inPlannedCoverage = (x, z) -> {
            // 初始覆盖（仅主世界）
            if (Level.OVERWORLD.equals(level.dimension())) {
                long dxs = (long) x - spawn.getX();
                long dzs = (long) z - spawn.getZ();
                if (dxs * dxs + dzs * dzs <= initialR2) return true;
            }
            // 历史动态规划矩形覆盖
            for (int[] r : plannedRects) {
                if (x >= r[0] && x <= r[2] && z >= r[1] && z <= r[3]) return true;
            }
            return false;
        };

        List<BlockPos> structures = new ArrayList<>();
        if (loc != null && loc.structureLocations() != null) {
            for (BlockPos p : loc.structureLocations()) {
                int x = p.getX(), z = p.getZ();
                boolean inRect = x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ;
                if (!inRect) continue;
                // 规则：已规划端点始终显示；已触发规划覆盖内的结构显示；其余仅在 AOI 内显示
                if (plannedEndpoints.contains(p) || inPlannedCoverage.test(x, z) || inAOI.test(x, z)) {
                    structures.add(p);
                }
            }
        }

        // 确保连接端点一定出现在结构点列表中（即使 StructureLocationData 尚未包含该点）
        if (connections != null) {
            java.util.HashSet<BlockPos> existing = new java.util.HashSet<>(structures);
            for (Records.StructureConnection c : connections) {
                BlockPos[] eps = new BlockPos[]{c.from(), c.to()};
                for (BlockPos ep : eps) {
                    int x = ep.getX(), z = ep.getZ();
                    boolean inRect = x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ;
                    if (inRect && !existing.contains(ep)) {
                        structures.add(ep);
                        existing.add(ep);
                    }
                }
            }
        }

        List<Records.StructureConnection> conns = new ArrayList<>();
        if (connections != null) {
            for (Records.StructureConnection c : connections) {
                BlockPos a = c.from();
                BlockPos b = c.to();
                boolean ina = a.getX() >= minBlockX && a.getX() <= maxBlockX && a.getZ() >= minBlockZ && a.getZ() <= maxBlockZ;
                boolean inb = b.getX() >= minBlockX && b.getX() <= maxBlockX && b.getZ() >= minBlockZ && b.getZ() <= maxBlockZ;
                // 规则：连接按矩形过滤，不受 AOI 限制
                if (ina || inb) {
                    conns.add(c);
                }
            }
        }

        List<Records.StructureInfo> infos = new ArrayList<>();
        if (loc != null && loc.structureInfos() != null) {
            for (Records.StructureInfo info : loc.structureInfos()) {
                BlockPos p = info.pos();
                int x = p.getX(), z = p.getZ();
                boolean inRect = x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ;
                if (!inRect) continue;
                if (plannedEndpoints.contains(p) || inPlannedCoverage.test(x, z) || inAOI.test(x, z)) {
                    infos.add(info);
                }
            }
        }

        if (Level.OVERWORLD.equals(level.dimension())) {
            ModConfig cfg = ConfigService.get();
            if (cfg.villagePredictionEnabled()) {
                int cminx = Math.floorDiv(minBlockX, 16);
                int cminz = Math.floorDiv(minBlockZ, 16);
                int cmaxx = Math.floorDiv(maxBlockX, 16);
                int cmaxz = Math.floorDiv(maxBlockZ, 16);
                List<Records.StructureInfo> predicted = StructurePredictor.predictOverworldStructuresInRect(
                        level,
                        cminx, cminz, cmaxx, cmaxz,
                        cfg.biomePrefilter(),
                        cfg.structureWhitelist(),
                        cfg.structureBlacklist()
                );
                if (!predicted.isEmpty()) {
                    Set<BlockPos> existing = new HashSet<>(structures);
                    for (Records.StructureInfo info : predicted) {
                        BlockPos p = info.pos();
                        int x = p.getX(), z = p.getZ();
                        boolean inRect = x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ;
                        if (!inRect) continue;
                        if (!existing.contains(p) && (inPlannedCoverage.test(x, z) || inAOI.test(x, z))) {
                            structures.add(p);
                            infos.add(info);
                            existing.add(p);
                        }
                    }
                }
            }
        }

        List<List<BlockPos>> roads = new ArrayList<>();
        List<Records.RoadData> roadDataList = provider.getRoadDataList(level);
        if (roadDataList != null) {
            for (Records.RoadData rd : roadDataList) {
                List<Records.RoadSegmentPlacement> segs = rd.roadSegmentList();
                if (segs == null || segs.isEmpty()) continue;
                ArrayList<BlockPos> poly = new ArrayList<>(segs.size());
                for (Records.RoadSegmentPlacement sp : segs) {
                    BlockPos p = sp.middlePos();
                    int x = p.getX(), z = p.getZ();
                    // 道路仅按矩形过滤（任何已生成/规划的道路都属于“已触发范围”）
                    if (x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ) {
                        poly.add(p);
                    }
                }
                if (poly.size() >= 2) roads.add(poly);
            }
        }

        return new MapSnapshot(structures, conns, infos, roads);
    }
}

