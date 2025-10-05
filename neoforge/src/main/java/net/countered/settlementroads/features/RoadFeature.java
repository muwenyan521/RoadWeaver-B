package net.countered.settlementroads.features;

import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.countered.settlementroads.features.decoration.*;
import net.countered.settlementroads.features.roadlogic.RoadPathCalculator;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureConnector;
import net.countered.settlementroads.persistence.WorldDataHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoadFeature extends Feature<RoadFeatureConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    public static final Set<Block> dontPlaceHere = new HashSet<>();
    static {
        dontPlaceHere.add(Blocks.PACKED_ICE);
        dontPlaceHere.add(Blocks.ICE);
        dontPlaceHere.add(Blocks.BLUE_ICE);
        dontPlaceHere.add(Blocks.TALL_SEAGRASS);
        dontPlaceHere.add(Blocks.MANGROVE_ROOTS);
    }

    public static int chunksForLocatingCounter = 1;

    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RoadFeatureConfig> context) {
        if (RoadPathCalculator.heightCache.size() > 100_000){
            RoadPathCalculator.heightCache.clear();
        }
        ServerLevel serverWorld = context.level().getLevel();
        WorldGenLevel worldGenLevel = context.level();
        Records.StructureLocationData structureLocationData = WorldDataHelper.getStructureLocations(serverWorld);
        if (structureLocationData == null) {
            return false;
        }
        List<BlockPos> villageLocations = structureLocationData.structureLocations();;
        tryFindNewStructureConnection(villageLocations, serverWorld);
        Set<Decoration> roadDecorationCache = new HashSet<>();
        runRoadLogic(worldGenLevel, context, roadDecorationCache);
        RoadStructures.tryPlaceDecorations(roadDecorationCache);
        return true;
    }

    private void tryFindNewStructureConnection(List<BlockPos> villageLocations, ServerLevel serverWorld) {
        // 移除数量限制，改为基于距离的智能搜寻
        chunksForLocatingCounter++;
        if (chunksForLocatingCounter > 300) {
            List<Records.StructureConnection> connectionList= WorldDataHelper.getConnectedStructures(serverWorld);
            serverWorld.getServer().execute(() -> {
                StructureConnector.cacheNewConnection(serverWorld, true);
            });
            chunksForLocatingCounter = 1;
        }
    }

    private void runRoadLogic(WorldGenLevel worldGenLevel, FeaturePlaceContext<RoadFeatureConfig> context, Set<Decoration> roadDecorationPlacementPositions) {
        int averagingRadius = ModConfig.averagingRadius();
        List<Records.RoadData> roadDataList = WorldDataHelper.getRoadDataList(worldGenLevel.getLevel());
        if (roadDataList == null) return;
        ChunkPos currentChunkPos = new ChunkPos(context.origin());

        Set<BlockPos> posAlreadyContainsSegment = new HashSet<>();
        for (Records.RoadData data : roadDataList) {
            int roadType = data.roadType();
            List<BlockState> materials = data.materials();
            List<Records.RoadSegmentPlacement> segmentList = data.roadSegmentList();

            List<BlockPos> middlePositions = segmentList.stream().map(Records.RoadSegmentPlacement::middlePos).toList();
            int segmentIndex = 0;
            for (int i = 2; i < segmentList.size() - 2; i++) {
                if (posAlreadyContainsSegment.contains(middlePositions.get(i))) continue;
                segmentIndex++;
                Records.RoadSegmentPlacement segment = segmentList.get(i);
                BlockPos segmentMiddlePos = segment.middlePos();
                // offset to structure
                if (segmentIndex < 60 || segmentIndex > segmentList.size() - 60) continue;
                ChunkPos middleChunkPos = new ChunkPos(segmentMiddlePos);
                if (!middleChunkPos.equals(currentChunkPos)) continue;

                BlockPos prevPos = middlePositions.get(i - 2);
                BlockPos nextPos = middlePositions.get(i + 2);
                List<Double> heights = new ArrayList<>();
                for (int j = i - averagingRadius; j <= i + averagingRadius; j++) {
                    if (j >= 0 && j < middlePositions.size()) {
                        BlockPos samplePos = middlePositions.get(j);
                        double y = worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, samplePos.getX(), samplePos.getZ());
                        heights.add(y);
                    }
                }

                int averageY = (int) Math.round(heights.stream().mapToDouble(Double::doubleValue).average().orElse(segmentMiddlePos.getY()));
                BlockPos averagedPos = new BlockPos(segmentMiddlePos.getX(), averageY, segmentMiddlePos.getZ());

                RandomSource random = context.random();
                if (!ModConfig.placeWaypoints()) {
                    for (BlockPos widthBlock : segment.positions()) {
                        BlockPos correctedYPos = new BlockPos(widthBlock.getX(), averageY, widthBlock.getZ());
                        placeOnSurface(worldGenLevel, correctedYPos, materials, roadType, random);
                    }
                }
                addDecoration(worldGenLevel, roadDecorationPlacementPositions, averagedPos, segmentIndex, nextPos, prevPos, middlePositions, roadType, random);
                posAlreadyContainsSegment.add(segmentMiddlePos);
            }
        }
    }

    private void addDecoration(WorldGenLevel worldGenLevel, Set<Decoration> roadDecorationPlacementPositions,
                               BlockPos placePos, int segmentIndex, BlockPos nextPos, BlockPos prevPos, List<BlockPos> middleBlockPositions, int roadType, RandomSource random) {
        BlockPos surfacePos = placePos.atY(worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, placePos.getX(), placePos.getZ()));
        BlockState blockStateAtPos = worldGenLevel.getBlockState(surfacePos.below());
        // Water surface handling is now done in placeOnSurface method
        if (ModConfig.placeWaypoints()) {
            if (segmentIndex % 25 == 0) {
                roadDecorationPlacementPositions.add(new FenceWaypointDecoration(surfacePos, worldGenLevel));
            }
            return;
        }
        int dx = nextPos.getX() - prevPos.getX();
        int dz = nextPos.getZ() - prevPos.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        int normDx = length != 0 ? (int) Math.round(dx / length) : 0;
        int normDz = length != 0 ? (int) Math.round(dz / length) : 0;
        Vec3i directionVector = new Vec3i(normDx, 0, normDz);

        Vec3i orthogonalVector = new Vec3i(-directionVector.getZ(), 0, directionVector.getX());
        boolean isEnd = segmentIndex != middleBlockPositions.size() - 65;
        BlockPos shiftedPos;
        if (segmentIndex == 65 || segmentIndex == middleBlockPositions.size() - 65) {
            shiftedPos = isEnd ? placePos.offset(orthogonalVector.multiply(2)) : placePos.subtract(orthogonalVector.multiply(2));
            roadDecorationPlacementPositions.add(new DistanceSignDecoration(shiftedPos, orthogonalVector, worldGenLevel, isEnd, String.valueOf(middleBlockPositions.size())));
        }
        else if (segmentIndex % 59 == 0) {
            boolean leftRoadSide = random.nextBoolean();
            shiftedPos = leftRoadSide ? placePos.offset(orthogonalVector.multiply(2)) : placePos.subtract(orthogonalVector.multiply(2));
            shiftedPos = shiftedPos.atY(worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, shiftedPos.getX(), shiftedPos.getZ()));
            if (Math.abs(shiftedPos.getY() - placePos.getY()) > 1) {
                return;
            }
            if (roadType == 0) {
                roadDecorationPlacementPositions.add(new LamppostDecoration(shiftedPos, orthogonalVector, worldGenLevel, leftRoadSide));
            }
            else {
                roadDecorationPlacementPositions.add(new FenceWaypointDecoration(shiftedPos, worldGenLevel));
            }
        }
        // 添加间断栏杆装饰
        else if (ModConfig.placeRoadFences() && segmentIndex % 15 == 0) {
            // 随机选择道路一侧
            boolean leftRoadSide = random.nextBoolean();
            shiftedPos = leftRoadSide ? placePos.offset(orthogonalVector.multiply(2)) : placePos.subtract(orthogonalVector.multiply(2));
            shiftedPos = shiftedPos.atY(worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, shiftedPos.getX(), shiftedPos.getZ()));
            
            // 检查高度差
            if (Math.abs(shiftedPos.getY() - placePos.getY()) > 1) {
                return;
            }
            
            // 随机栏杆长度（1-3个方块）
            int fenceLength = random.nextInt(1, 4);
            roadDecorationPlacementPositions.add(new RoadFenceDecoration(shiftedPos, orthogonalVector, worldGenLevel, leftRoadSide, fenceLength));
        }
        // 添加大型装饰结构（秋千、长椅、凉亭等）
        else if (segmentIndex % 80 == 0) {
            // 随机选择装饰类型
            java.util.List<String> availableStructures = new java.util.ArrayList<>();
            
            if (ModConfig.placeSwings()) availableStructures.add("swing");
            if (ModConfig.placeBenches()) availableStructures.add("bench");
            if (ModConfig.placeGloriettes()) availableStructures.add("gloriette");
            
            if (availableStructures.isEmpty()) {
                return;
            }
            
            // 随机选择一个结构
            String selectedStructure = availableStructures.get(random.nextInt(availableStructures.size()));
            
            // 大型结构需要更多空间，选择道路一侧并远离道路中心
            boolean leftRoadSide = random.nextBoolean();
            shiftedPos = leftRoadSide ? placePos.offset(orthogonalVector.multiply(ModConfig.structureDistanceFromRoad())) : placePos.subtract(orthogonalVector.multiply(ModConfig.structureDistanceFromRoad()));
            shiftedPos = shiftedPos.atY(worldGenLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, shiftedPos.getX(), shiftedPos.getZ()));
            
            // 对地形要求更严格
            if (Math.abs(shiftedPos.getY() - placePos.getY()) > 1) {
                return;
            }
            
            // 根据选择的结构类型创建装饰
            switch (selectedStructure) {
                case "swing":
                    roadDecorationPlacementPositions.add(new SwingDecoration(shiftedPos, orthogonalVector, worldGenLevel));
                    break;
                case "bench":
                    roadDecorationPlacementPositions.add(new NbtStructureDecoration(shiftedPos, orthogonalVector, worldGenLevel, "bench", new Vec3i(3, 3, 3)));
                    break;
                case "gloriette":
                    roadDecorationPlacementPositions.add(new NbtStructureDecoration(shiftedPos, orthogonalVector, worldGenLevel, "gloriette", new Vec3i(5, 5, 5)));
                    break;
            }
        }
    }

    private void placeOnSurface(WorldGenLevel worldGenLevel, BlockPos placePos, List<BlockState> material, int natural, RandomSource random) {
        double naturalBlockChance = 0.5;
        BlockPos surfacePos = placePos;
        if (natural == 1 || ModConfig.averagingRadius() == 0) {
            surfacePos = worldGenLevel.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, placePos);
        }
        BlockPos topPos = worldGenLevel.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, surfacePos);
        BlockState blockStateAtPos = worldGenLevel.getBlockState(topPos.below());
        
        // Check if this is water surface - place unlit campfire instead of regular road
        if (blockStateAtPos.equals(Blocks.WATER.defaultBlockState())) {
            worldGenLevel.setBlock(topPos, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false), 3);
            return;
        }
        
        // place road
        if (natural == 0 || random.nextDouble() < naturalBlockChance) {
            placeRoadBlock(worldGenLevel, blockStateAtPos, surfacePos, material, random);
        }
    }

    private void placeRoadBlock(WorldGenLevel worldGenLevel, BlockState blockStateAtPos, BlockPos surfacePos, List<BlockState> materials, RandomSource deterministicRandom) {
        // If not water, just place the road
        if (!placeAllowedCheck(blockStateAtPos.getBlock())
                || (!worldGenLevel.getBlockState(surfacePos.below()).canOcclude())
                && !worldGenLevel.getBlockState(surfacePos.below(2)).canOcclude()
                //&& !worldGenLevel.getBlockState(surfacePos.below(3)).canOcclude())
                //|| worldGenLevel.getBlockState(surfacePos.above(3)).canOcclude()
        ) {
            return;
        }
        BlockState material = materials.get(deterministicRandom.nextInt(materials.size()));
        setBlock(worldGenLevel, surfacePos.below(), material);

        for (int i = 0; i < 3; i++) {
            BlockState blockStateUp = worldGenLevel.getBlockState(surfacePos.above(i));
            if (!blockStateUp.getBlock().equals(Blocks.AIR) && !blockStateUp.is(BlockTags.LOGS) && !blockStateUp.is(BlockTags.FENCES)) {
                setBlock(worldGenLevel, surfacePos.above(i), Blocks.AIR.defaultBlockState());
            }
            else {
                break;
            }
        }

        BlockPos belowPos1 = surfacePos.below(2);
        BlockState belowState1 = worldGenLevel.getBlockState(belowPos1);

        if (belowState1.getBlock().equals(Blocks.GRASS_BLOCK)) {
            setBlock(worldGenLevel, belowPos1, Blocks.DIRT.defaultBlockState());
        }
    }

    private boolean placeAllowedCheck (Block blockToCheck) {
        return !(dontPlaceHere.contains(blockToCheck)
                || blockToCheck.defaultBlockState().is(BlockTags.LEAVES)
                || blockToCheck.defaultBlockState().is(BlockTags.LOGS)
                || blockToCheck.defaultBlockState().is(BlockTags.UNDERWATER_BONEMEALS)
                || blockToCheck.defaultBlockState().is(BlockTags.WOODEN_FENCES)
                || blockToCheck.defaultBlockState().is(BlockTags.PLANKS)
        );
    }
}
