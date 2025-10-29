package net.shiroha233.roadweaver.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.features.config.RoadFeatureConfig;
import net.shiroha233.roadweaver.features.decoration.Decoration;
import net.shiroha233.roadweaver.features.decoration.system.RoadDecorationSystem;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;

import java.util.*;

public class RoadFeature extends Feature<RoadFeatureConfig> {
    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RoadFeatureConfig> ctx) {
        WorldGenLevel world = ctx.level();
        Level lvl = world.getLevel();
        if (!(lvl instanceof ServerLevel server)) return false;

        List<Records.RoadData> roadDataList = WorldDataProvider.getInstance().getRoadDataList(server);
        if (roadDataList == null || roadDataList.isEmpty()) return false;

        ChunkPos currentChunk = new ChunkPos(ctx.origin());
        Set<BlockPos> processedMiddle = new HashSet<>();
        RandomSource random = ctx.random();
        ModConfig cfg = ConfigService.get();
        int averagingRadius = Math.max(0, cfg.averagingRadius());

        Set<Decoration> decorations = new HashSet<>();
        for (Records.RoadData data : roadDataList) {
            int roadType = data.roadType();
            int roadWidth = Math.max(1, data.width());
            List<BlockState> materials = data.materials();
            List<Records.RoadSegmentPlacement> segments = data.roadSegmentList();
            if (segments == null || segments.size() < 5) continue;

            List<BlockPos> middlePositions = segments.stream().map(Records.RoadSegmentPlacement::middlePos).toList();
            int segmentIndex = 0;
            for (int i = 2; i < segments.size() - 2; i++) {
                BlockPos middle = middlePositions.get(i);
                if (!processedMiddle.add(middle)) continue;
                segmentIndex++;
                if (segmentIndex < 60 || segmentIndex > segments.size() - 60) continue;
                ChunkPos middleChunk = new ChunkPos(middle);
                if (!middleChunk.equals(currentChunk)) continue;

                BlockPos prev = middlePositions.get(i - 2);
                BlockPos next = middlePositions.get(i + 2);

                List<Integer> heights = new ArrayList<>();
                for (int j = i - averagingRadius; j <= i + averagingRadius; j++) {
                    if (j >= 0 && j < middlePositions.size()) {
                        BlockPos sample = middlePositions.get(j);
                        int yTop = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, sample.getX(), sample.getZ());
                        heights.add(yTop);
                    }
                }
                int averageY = heights.isEmpty() ? middle.getY() : (int) Math.round(heights.stream().mapToInt(Integer::intValue).average().orElse(middle.getY()));
                int topYCenter = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, middle.getX(), middle.getZ());
                BlockPos averaged = new BlockPos(middle.getX(), topYCenter, middle.getZ());

                Records.RoadSegmentPlacement seg = segments.get(i);
                for (BlockPos widthBlock : seg.positions()) {
                    BlockPos pos = new BlockPos(widthBlock.getX(), averageY, widthBlock.getZ());
                    placeOnSurface(world, pos, materials, roadType, random, cfg);
                }

                addDecoration(world, decorations, averaged, segmentIndex, next, prev, middlePositions, roadType, roadWidth, random, cfg);
            }
        }
        RoadDecorationSystem.finalizeDecorations(decorations);
        return true;
    }

    private static void placeOnSurface(WorldGenLevel world, BlockPos placePos, List<BlockState> material, int roadType, RandomSource random, ModConfig cfg) {
        RoadDecorationSystem.placeOnSurface(world, placePos, material, roadType, random, cfg);
    }

    private static void addDecoration(WorldGenLevel world,
                                      Set<Decoration> out,
                                      BlockPos placePos,
                                      int segmentIndex,
                                      BlockPos nextPos,
                                      BlockPos prevPos,
                                      List<BlockPos> middlePositions,
                                      int roadType,
                                      int roadWidth,
                                      RandomSource random,
                                      ModConfig cfg) {
        RoadDecorationSystem.addDecoration(world, out, placePos, segmentIndex, nextPos, prevPos, middlePositions, roadType, roadWidth, random, cfg);
    }

    
}
