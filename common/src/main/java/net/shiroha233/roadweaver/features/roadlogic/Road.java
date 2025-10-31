package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.shiroha233.roadweaver.features.config.RoadFeatureConfig;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.config.BiomeStyleService;
import net.shiroha233.roadweaver.config.RoadStyleConfigEntry;
import net.shiroha233.roadweaver.debug.DebugService;
import net.shiroha233.roadweaver.features.RoadClearanceService;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.shiroha233.roadweaver.features.decoration.system.RoadDecorationSystem;

import java.util.ArrayList;
import java.util.List;

public final class Road {
    private final ServerLevel level;
    private final Records.StructureConnection connection;
    private final RoadFeatureConfig config;

    public Road(ServerLevel level, Records.StructureConnection connection, RoadFeatureConfig config) {
        this.level = level;
        this.connection = connection;
        this.config = config;
    }

    public void generateRoad(int maxSteps) {
        DebugService debugService = DebugService.getInstance();
        long startTime = System.nanoTime();
        
        RandomSource random = RandomSource.create();
        ModConfig cfg = ConfigService.get();
        
        // 检查维度选择器
        if (!cfg.dimensionSelector().isEmpty() && !cfg.dimensionSelector().equals("*")) {
            String dimensionName = level.dimension().location().toString();
            if (!dimensionName.equals(cfg.dimensionSelector())) {
                debugService.logDebug("Road generation skipped: dimension " + dimensionName + " not in selector " + cfg.dimensionSelector());
                return;
            }
        }
        
        if (!cfg.allowArtificial()) return;
        
        // 获取生物群系特定样式
        RoadStyleConfigEntry biomeStyle = BiomeStyleService.getBiomeStyle(level, connection.from());
        int width = cfg.roadWidth() > 0 ? cfg.roadWidth() : getRandomWidth(random, config);
        int type = 0;
        
        // 使用生物群系样式或默认材料
        List<BlockState> materials;
        if (biomeStyle != null && !biomeStyle.surfacePalette().isEmpty()) {
            materials = biomeStyle.surfacePalette();
            debugService.logDebug("Using biome-specific road style for " + level.getBiome(connection.from()).unwrap().location());
        } else {
            materials = RoadDecorationSystem.selectMaterials(random, config);
        }

        BlockPos start = connection.from();
        BlockPos end = connection.to();
        
        // 使用优化的路径查找器
        List<Records.RoadSegmentPlacement> segments;
        if (cfg.enableOptimizedPathfinding()) {
            segments = OptimizedPathFinder.findPath(start, end, width, level, maxSteps, cfg.partialPathAcceptance());
            debugService.logDebug("Using optimized pathfinder with partial acceptance: " + cfg.partialPathAcceptance());
        } else {
            segments = RoadPathCalculator.calculateAStarRoadPath(start, end, width, level, maxSteps);
        }
        
        if (segments == null || segments.size() < 5) {
            debugService.logDebug("Pathfinding failed or insufficient segments: " + (segments == null ? "null" : segments.size()));
            return;
        }
        
        // 执行路线清理
        if (cfg.enableRoadClearance()) {
            RoadClearanceService clearanceService = new RoadClearanceService(level);
            clearanceService.clearRoadCorridor(segments, width);
            debugService.logDebug("Road clearance completed for " + segments.size() + " segments");
        }
        
        List<Records.RoadSpan> spans = RoadPathCalculator.extractSpans(segments, level);

        WorldDataProvider provider = WorldDataProvider.getInstance();
        List<Records.RoadData> list = new ArrayList<>(provider.getRoadDataList(level));
        list.add(new Records.RoadData(width, type, materials, segments, spans));
        provider.setRoadDataList(level, list);
        
        // 记录性能指标
        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        debugService.logPerformance("Road generation completed", durationMs, segments.size());
    }

    

    private static int getRandomWidth(RandomSource rnd, RoadFeatureConfig cfg) {
        return 3;
    }
    
}
