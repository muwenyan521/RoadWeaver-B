package net.shiroha233.roadweaver.features.decoration.system;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.BlockTags;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.config.PresetService;
import net.shiroha233.roadweaver.features.config.RoadFeatureConfig;
import net.shiroha233.roadweaver.features.decoration.Decoration;
import net.shiroha233.roadweaver.features.decoration.DistanceSignDecoration;
import net.shiroha233.roadweaver.features.decoration.FenceWaypointDecoration;
import net.shiroha233.roadweaver.features.decoration.LamppostDecoration;
import net.shiroha233.roadweaver.features.decoration.RoadStructures;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public final class RoadDecorationSystem {
    private RoadDecorationSystem() {}
    private static final int SIGN_INDEX_OFFSET = 65;
    private static final int SIDE_OFFSET = 2;

    public static List<BlockState> selectMaterials(RandomSource rnd, RoadFeatureConfig cfg) {
        return selectMaterialsFromPresets(rnd);
    }

    public static List<BlockState> selectMaterialsFromPresets(RandomSource rnd) {
        List<List<String>> combos = PresetService.getMaterialCombos();
        if (combos == null || combos.isEmpty()) {
            return java.util.List.of(Blocks.STONE_BRICKS.defaultBlockState(), Blocks.POLISHED_ANDESITE.defaultBlockState());
        }
        List<String> chosen = combos.get(rnd.nextInt(combos.size()));
        List<BlockState> parsed = parseMaterials(chosen);
        if (parsed.isEmpty()) {
            return java.util.List.of(Blocks.STONE_BRICKS.defaultBlockState());
        }
        return parsed;
    }

    private static List<BlockState> parseMaterials(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<BlockState> out = new ArrayList<>(ids.size());
        for (String s : ids) {
            try {
                ResourceLocation rl = new ResourceLocation(s);
                Block b = BuiltInRegistries.BLOCK.get(rl);
                if (b != null && b != Blocks.AIR) out.add(b.defaultBlockState());
            } catch (Throwable ignored) {}
        }
        return out;
    }

    public static void placeOnSurface(WorldGenLevel world, BlockPos placePos, List<BlockState> material, int roadType, RandomSource random, ModConfig cfg) {
        double naturalBlockChance = 0.5;
        BlockPos surfacePos = placePos;
        if (roadType == 1 || Math.max(0, cfg.averagingRadius()) == 0) {
            int topY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, placePos.getX(), placePos.getZ());
            surfacePos = new BlockPos(placePos.getX(), topY, placePos.getZ());
        }
        int topYForBelow = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, surfacePos.getX(), surfacePos.getZ());
        BlockPos belowTop = new BlockPos(surfacePos.getX(), topYForBelow - 1, surfacePos.getZ());
        BlockState blockStateAtPos = world.getBlockState(belowTop);
        if (roadType == 0 || random.nextDouble() < naturalBlockChance) {
            placeRoadBlock(world, blockStateAtPos, surfacePos, material, random, cfg);
        }
    }

    public static void placeRoadBlock(WorldGenLevel world, BlockState blockBelow, BlockPos surfacePos, List<BlockState> materials, RandomSource random, ModConfig cfg) {
        if (!placeAllowedCheck(blockBelow.getBlock())) return;
        BlockState chosen = materials.get(random.nextInt(materials.size()));

        final int MAX_CAUSEWAY_DEPTH = 12;
        BlockPos below1 = surfacePos.below();
        BlockPos below2 = surfacePos.below(2);
        boolean sturdy1 = world.getBlockState(below1).isFaceSturdy(world, below1, Direction.UP);
        boolean sturdy2 = world.getBlockState(below2).isFaceSturdy(world, below2, Direction.UP);

        if (!sturdy1 && !sturdy2) {
            // Scan downward to find a sturdy base within max depth; if found, fill up to below1. Otherwise, fill a limited pillar.
            BlockPos cursor = below2;
            int depth = 0;
            BlockPos base = null;
            while (cursor.getY() > world.getMinBuildHeight() && depth < MAX_CAUSEWAY_DEPTH) {
                if (world.getBlockState(cursor).isFaceSturdy(world, cursor, Direction.UP)) {
                    base = cursor;
                    break;
                }
                cursor = cursor.below();
                depth++;
            }

            BlockPos fillStart = (base != null) ? base.above() : below1.below(Math.min(MAX_CAUSEWAY_DEPTH - 1, Math.max(0, below1.getY() - world.getMinBuildHeight())));
            // Clamp start to world min height
            if (fillStart.getY() < world.getMinBuildHeight()) {
                fillStart = new BlockPos(fillStart.getX(), world.getMinBuildHeight(), fillStart.getZ());
            }
            BlockPos pos = fillStart;
            while (pos.getY() <= below1.getY()) {
                world.setBlock(pos, chosen, 3);
                pos = pos.above();
            }
        } else {
            // Has immediate or near support; place the road block
            world.setBlock(below1, chosen, 3);
        }

        // Clear up to 3 blocks above for headroom (skip logs/fences)
        for (int i = 0; i < 3; i++) {
            BlockPos up = surfacePos.above(i);
            BlockState blockStateUp = world.getBlockState(up);
            if (!blockStateUp.isAir() && !blockStateUp.is(BlockTags.LOGS) && !blockStateUp.is(BlockTags.FENCES)) {
                world.setBlock(up, Blocks.AIR.defaultBlockState(), 3);
            } else {
                break;
            }
        }

        // Beautify: convert grass two blocks below into dirt to reduce side exposure
        BlockPos belowPos1 = surfacePos.below(2);
        BlockState belowState1 = world.getBlockState(belowPos1);
        if (belowState1.is(Blocks.GRASS_BLOCK)) {
            world.setBlock(belowPos1, Blocks.DIRT.defaultBlockState(), 3);
        }
    }

    public static void addDecoration(WorldGenLevel world,
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
        int dx = nextPos.getX() - prevPos.getX();
        int dz = nextPos.getZ() - prevPos.getZ();
        double len = Math.sqrt((double) dx * dx + (double) dz * dz);
        int nx = len != 0 ? (int) Math.round(dx / len) : 0;
        int nz = len != 0 ? (int) Math.round(dz / len) : 0;
        Vec3i dir = new Vec3i(nx, 0, nz);
        Vec3i ortho = new Vec3i(-dir.getZ(), 0, dir.getX());
        int halfWidth = Math.max(1, roadWidth / 2);
        int sideOffset = Math.max(SIDE_OFFSET, halfWidth + 1);
        boolean isStart = (segmentIndex == SIGN_INDEX_OFFSET);
        BlockPos shifted;
        if (segmentIndex == SIGN_INDEX_OFFSET || segmentIndex == middlePositions.size() - SIGN_INDEX_OFFSET) {
            shifted = isStart ? placePos.offset(ortho.getX() * sideOffset, 0, ortho.getZ() * sideOffset)
                    : placePos.offset(-ortho.getX() * sideOffset, 0, -ortho.getZ() * sideOffset);
            int dist = computeApproxDistanceMeters(world, shifted, isStart, middlePositions);
            out.add(new DistanceSignDecoration(shifted, ortho, world, isStart, String.valueOf(dist)));
        } else {
            int interval = Math.max(1, cfg.lampInterval());
            if (segmentIndex % interval == 0) {
                boolean left = random.nextBoolean();
                shifted = left ? placePos.offset(ortho.getX() * sideOffset, 0, ortho.getZ() * sideOffset)
                        : placePos.offset(-ortho.getX() * sideOffset, 0, -ortho.getZ() * sideOffset);
                shifted = new BlockPos(shifted.getX(), world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, shifted.getX(), shifted.getZ()), shifted.getZ());
                if (Math.abs(shifted.getY() - placePos.getY()) > 1) return;
                if (roadType == 0) {
                    out.add(new LamppostDecoration(shifted, ortho, world, left));
                } else {
                    out.add(new FenceWaypointDecoration(shifted, world));
                }
            }
        }
    }

    public static void finalizeDecorations(Set<Decoration> decorations) {
        RoadStructures.tryPlaceDecorations(decorations);
    }

    public static int computeApproxDistanceMeters(WorldGenLevel world, BlockPos fromPos, boolean isStart, java.util.List<BlockPos> middlePositions) {
        BlockPos target = isStart ? middlePositions.get(middlePositions.size() - 1) : middlePositions.get(0);
        long dx = (long) target.getX() - fromPos.getX();
        long dz = (long) target.getZ() - fromPos.getZ();
        double d = Math.sqrt((double) dx * dx + (double) dz * dz);
        return (int) Math.round(d);
    }

    private static boolean placeAllowedCheck(net.minecraft.world.level.block.Block block) {
        return !(net.shiroha233.roadweaver.features.decoration.RoadFeatureCompat.dontPlaceHere(block)
                || block.defaultBlockState().is(BlockTags.LEAVES)
                || block.defaultBlockState().is(BlockTags.LOGS)
                || block.defaultBlockState().is(BlockTags.UNDERWATER_BONEMEALS)
                || block.defaultBlockState().is(BlockTags.WOODEN_FENCES)
                || block.defaultBlockState().is(BlockTags.PLANKS)
        );
    }
}
