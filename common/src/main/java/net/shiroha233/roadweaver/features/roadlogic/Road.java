package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.shiroha233.roadweaver.features.config.RoadFeatureConfig;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.ModConfig;
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
        RandomSource random = RandomSource.create();
        int width = ConfigService.get().roadWidth() > 0 ? ConfigService.get().roadWidth() : getRandomWidth(random, config);
        ModConfig cfg = ConfigService.get();
        if (!cfg.allowArtificial()) return;
        int type = 0;
        List<BlockState> materials = RoadDecorationSystem.selectMaterials(random, config);

        BlockPos start = connection.from();
        BlockPos end = connection.to();
        List<Records.RoadSegmentPlacement> segments = RoadPathCalculator.calculateAStarRoadPath(start, end, width, level, maxSteps);
        if (segments == null || segments.size() < 5) return;
        List<Records.RoadSpan> spans = RoadPathCalculator.extractSpans(segments, level);

        WorldDataProvider provider = WorldDataProvider.getInstance();
        List<Records.RoadData> list = new ArrayList<>(provider.getRoadDataList(level));
        list.add(new Records.RoadData(width, type, materials, segments, spans));
        provider.setRoadDataList(level, list);
    }

    

    private static int getRandomWidth(RandomSource rnd, RoadFeatureConfig cfg) {
        return 3;
    }
    
}
