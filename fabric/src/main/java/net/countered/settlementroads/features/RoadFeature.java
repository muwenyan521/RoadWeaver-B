package net.countered.settlementroads.features;

import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.countered.settlementroads.features.decoration.*;
import net.countered.settlementroads.features.roadlogic.RoadPathCalculator;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureConnector;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
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

    public static final ResourceKey<ConfiguredFeature<?, ?>> ROAD_FEATURE_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(SettlementRoads.MOD_ID, "road_feature"));
    public static final ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> ROAD_FEATURE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(SettlementRoads.MOD_ID, "road_feature_placed"));
    public static final Feature<RoadFeatureConfig> ROAD_FEATURE = new RoadFeature(RoadFeatureConfig.CODEC);
    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RoadFeatureConfig> context) {
        if (RoadPathCalculator.heightCache.size() > 100_000){
            RoadPathCalculator.heightCache.clear();
        }
        WorldGenLevel level = context.level();
        ServerLevel serverLevel = (ServerLevel) level.getLevel();
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        
        Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(serverLevel);
        if (structureLocationData == null) {
            return false;
        }
        List<BlockPos> villageLocations = structureLocationData.structureLocations();
        tryFindNewStructureConnection(villageLocations, serverLevel);
        Set<Decoration> roadDecorationCache = new HashSet<>();
        runRoadLogic(level, context, roadDecorationCache);
        tryPlaceDecorations(roadDecorationCache);
        return true;
    }
    
    private static void tryPlaceDecorations(Set<Decoration> decorations) {
        for (Decoration decoration : decorations) {
            if (decoration.placeAllowed()) {
                decoration.place();
            }
        }
    }

    private void tryFindNewStructureConnection(List<BlockPos> villageLocations, ServerLevel serverLevel) {
        // 移除数量限制，改为基于距离的智能搜寻
        chunksForLocatingCounter++;
        if (chunksForLocatingCounter > 300) {
            serverLevel.getServer().execute(() -> {
                StructureConnector.cacheNewConnection(serverLevel, true);
            });
            chunksForLocatingCounter = 1;
        }
    }

    private void runRoadLogic(WorldGenLevel level, FeaturePlaceContext<RoadFeatureConfig> context, Set<Decoration> roadDecorationPlacementPositions) {
        ModConfig config = ModConfig.getInstance();
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        ServerLevel serverLevel = (ServerLevel) level.getLevel();
        
        int averagingRadius = config.averagingRadius();
        List<Records.RoadData> roadDataList = dataProvider.getRoadDataList(serverLevel);
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
                        double y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, samplePos.getX(), samplePos.getZ());
                        heights.add(y);
                    }
                }

                int averageY = (int) Math.round(heights.stream().mapToDouble(Double::doubleValue).average().orElse(segmentMiddlePos.getY()));
                BlockPos averagedPos = new BlockPos(segmentMiddlePos.getX(), averageY, segmentMiddlePos.getZ());

                RandomSource random = context.random();
                if (!config.placeWaypoints()) {
                    for (BlockPos widthBlock : segment.positions()) {
                        BlockPos correctedYPos = new BlockPos(widthBlock.getX(), averageY, widthBlock.getZ());
                        placeOnSurface(level, correctedYPos, materials, roadType, random);
                    }
                }
                addDecoration(level, roadDecorationPlacementPositions, averagedPos, segmentIndex, nextPos, prevPos, middlePositions, roadType, random, config);
                posAlreadyContainsSegment.add(segmentMiddlePos);
            }
        }
    }

    private void addDecoration(WorldGenLevel level, Set<Decoration> roadDecorationPlacementPositions,
                               BlockPos placePos, int segmentIndex, BlockPos nextPos, BlockPos prevPos, List<BlockPos> middleBlockPositions, int roadType, RandomSource random, ModConfig config) {
        BlockPos surfacePos = placePos.atY(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, placePos.getX(), placePos.getZ()));
        BlockState blockStateAtPos = level.getBlockState(surfacePos.below());
        // Water surface handling is now done in placeOnSurface method
        if (config.placeWaypoints()) {
            if (segmentIndex % 25 == 0) {
                roadDecorationPlacementPositions.add(new FenceWaypointDecoration(surfacePos, level));
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
            shiftedPos = isEnd ? placePos.offset(orthogonalVector.multiply(2)) : placePos.offset(orthogonalVector.multiply(-2));
            roadDecorationPlacementPositions.add(new DistanceSignDecoration(shiftedPos, orthogonalVector, level, isEnd, String.valueOf(middleBlockPositions.size())));
        }
        else if (segmentIndex % 59 == 0) {
            boolean leftRoadSide = random.nextBoolean();
            shiftedPos = leftRoadSide ? placePos.offset(orthogonalVector.multiply(2)) : placePos.offset(orthogonalVector.multiply(-2));
            shiftedPos = shiftedPos.atY(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, shiftedPos.getX(), shiftedPos.getZ()));
            if (Math.abs(shiftedPos.getY() - placePos.getY()) > 1) {
                return;
            }
            if (roadType == 0) {
                roadDecorationPlacementPositions.add(new LamppostDecoration(shiftedPos, orthogonalVector, level, leftRoadSide));
            }
            else {
                roadDecorationPlacementPositions.add(new FenceWaypointDecoration(shiftedPos, level));
            }
        }
        // 添加间断栏杆装饰
        else if (config.placeRoadFences() && segmentIndex % 15 == 0) {
            // 随机选择道路一侧
            boolean leftRoadSide = random.nextBoolean();
            shiftedPos = leftRoadSide ? placePos.offset(orthogonalVector.multiply(2)) : placePos.offset(orthogonalVector.multiply(-2));
            shiftedPos = shiftedPos.atY(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, shiftedPos.getX(), shiftedPos.getZ()));
            
            // 检查高度差
            if (Math.abs(shiftedPos.getY() - placePos.getY()) > 1) {
                return;
            }
            
            // 随机栏杆长度（1-3个方块）
            int fenceLength = random.nextInt(1, 4);
            roadDecorationPlacementPositions.add(new RoadFenceDecoration(shiftedPos, orthogonalVector, level, leftRoadSide, fenceLength));
        }
        // 添加大型装饰结构（秋千、长椅、凉亭等）
        else if (segmentIndex % 80 == 0) {
            // 随机选择装饰类型
            java.util.List<String> availableStructures = new java.util.ArrayList<>();
            
            if (config.placeSwings()) availableStructures.add("swing");
            if (config.placeBenches()) availableStructures.add("bench");
            if (config.placeGloriettes()) availableStructures.add("gloriette");
            
            if (availableStructures.isEmpty()) {
                return;
            }
            
            // 随机选择一个结构
            String selectedStructure = availableStructures.get(random.nextInt(availableStructures.size()));
            
            // 大型结构需要更多空间，选择道路一侧并远离道路中心
            boolean leftRoadSide = random.nextBoolean();
            int distance = config.structureDistanceFromRoad();
            shiftedPos = leftRoadSide ? placePos.offset(orthogonalVector.multiply(distance)) : placePos.offset(orthogonalVector.multiply(-distance));
            shiftedPos = shiftedPos.atY(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, shiftedPos.getX(), shiftedPos.getZ()));
            
            // 对地形要求更严格
            if (Math.abs(shiftedPos.getY() - placePos.getY()) > 1) {
                return;
            }
            
            // TODO: 实现大型装饰结构
            // 这些装饰类需要在 common 模块中实现
            // 暂时跳过这些装饰
        }
    }

    private void placeOnSurface(WorldGenLevel level, BlockPos placePos, List<BlockState> material, int natural, RandomSource random) {
        ModConfig config = ModConfig.getInstance();
        double naturalBlockChance = 0.5;
        BlockPos surfacePos = placePos;
        if (natural == 1 || config.averagingRadius() == 0) {
            surfacePos = new BlockPos(placePos.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, placePos.getX(), placePos.getZ()), placePos.getZ());
        }
        BlockPos topPos = new BlockPos(surfacePos.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, surfacePos.getX(), surfacePos.getZ()), surfacePos.getZ());
        BlockState blockStateAtPos = level.getBlockState(topPos.below());
        
        // Check if this is water surface - place unlit campfire instead of regular road
        if (blockStateAtPos.equals(Blocks.WATER.defaultBlockState())) {
            level.setBlock(topPos, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false), 3);
            return;
        }
        
        // place road
        if (natural == 0 || random.nextDouble() < naturalBlockChance) {
            placeRoadBlock(level, blockStateAtPos, surfacePos, material, random);
        }
    }

    private void placeRoadBlock(WorldGenLevel level, BlockState blockStateAtPos, BlockPos surfacePos, List<BlockState> materials, RandomSource deterministicRandom) {
        // If not water, just place the road
        if (!placeAllowedCheck(blockStateAtPos.getBlock())
                || (!level.getBlockState(surfacePos.below()).canOcclude())
                && !level.getBlockState(surfacePos.below(2)).canOcclude()
        ) {
            return;
        }
        BlockState material = materials.get(deterministicRandom.nextInt(materials.size()));
        level.setBlock(surfacePos.below(), material, 3);

        for (int i = 0; i < 3; i++) {
            BlockState blockStateUp = level.getBlockState(surfacePos.above(i));
            if (!blockStateUp.getBlock().equals(Blocks.AIR) && !blockStateUp.is(BlockTags.LOGS) && !blockStateUp.is(BlockTags.FENCES)) {
                level.setBlock(surfacePos.above(i), Blocks.AIR.defaultBlockState(), 3);
            }
            else {
                break;
            }
        }

        BlockPos belowPos1 = surfacePos.below(2);
        BlockState belowState1 = level.getBlockState(belowPos1);

        if (belowState1.getBlock().equals(Blocks.GRASS_BLOCK)) {
            level.setBlock(belowPos1, Blocks.DIRT.defaultBlockState(), 3);
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

