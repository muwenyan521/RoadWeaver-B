package net.shiroha233.roadweaver.features;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public final class RoadClearanceService {
    private static final Set<Block> TREE_BLOCKS = Set.of(
        Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
        Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG,
        Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
        Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
        Blocks.MANGROVE_LEAVES, Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES
    );

    private static final Set<Block> CLEARANCE_BLOCKS = new HashSet<>();
    
    static {
        CLEARANCE_BLOCKS.addAll(TREE_BLOCKS);
        CLEARANCE_BLOCKS.addAll(Set.of(
            Blocks.VINE, Blocks.GLOW_LICHEN, Blocks.TALL_GRASS, Blocks.LARGE_FERN,
            Blocks.SWEET_BERRY_BUSH, Blocks.ROSE_BUSH, Blocks.PEONY, Blocks.LILAC,
            Blocks.SUNFLOWER, Blocks.DEAD_BUSH, Blocks.FERN, Blocks.GRASS
        ));
    }

    public void clearRoadCorridor(LevelAccessor level, BlockPos center, int roadWidth, int clearanceHeight) {
        if (roadWidth <= 0 || clearanceHeight <= 0) {
            return;
        }

        int halfWidth = roadWidth / 2;
        Set<BlockPos> clearedPositions = new HashSet<>();

        // 清理道路走廊区域
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                BlockPos basePos = center.offset(dx, 0, dz);
                clearVerticalColumn(level, basePos, clearanceHeight, clearedPositions);
            }
        }

        // 清理悬垂的树叶和藤蔓
        clearOverhangingVegetation(level, center, roadWidth, clearanceHeight);
    }

    private void clearVerticalColumn(LevelAccessor level, BlockPos basePos, int clearanceHeight, Set<BlockPos> clearedPositions) {
        // 从地面向上清理
        for (int dy = 0; dy <= clearanceHeight; dy++) {
            BlockPos pos = basePos.above(dy);
            
            if (clearedPositions.contains(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (shouldClearBlock(state)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                clearedPositions.add(pos);
            }
        }

        // 清理悬垂的树叶（从顶部向下检查）
        for (int dy = clearanceHeight + 1; dy <= clearanceHeight + 5; dy++) {
            BlockPos pos = basePos.above(dy);
            
            if (clearedPositions.contains(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (isLeavesBlock(state) && !hasLogSupport(level, pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                clearedPositions.add(pos);
            }
        }
    }

    private void clearOverhangingVegetation(LevelAccessor level, BlockPos center, int roadWidth, int clearanceHeight) {
        int extendedWidth = roadWidth + 2; // 扩展清理范围以处理悬垂
        
        for (int dx = -extendedWidth; dx <= extendedWidth; dx++) {
            for (int dz = -extendedWidth; dz <= extendedWidth; dz++) {
                // 只清理扩展区域但不在主要道路区域内的悬垂物
                if (Math.abs(dx) <= roadWidth / 2 && Math.abs(dz) <= roadWidth / 2) {
                    continue;
                }

                BlockPos checkPos = center.offset(dx, clearanceHeight + 1, dz);
                BlockState state = level.getBlockState(checkPos);
                
                if (isLeavesBlock(state) && isOverRoad(level, checkPos, center, roadWidth)) {
                    level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private boolean isOverRoad(LevelAccessor level, BlockPos pos, BlockPos roadCenter, int roadWidth) {
        // 检查这个位置是否在道路正上方
        BlockPos belowPos = pos.below();
        int dx = belowPos.getX() - roadCenter.getX();
        int dz = belowPos.getZ() - roadCenter.getZ();
        
        return Math.abs(dx) <= roadWidth / 2 && Math.abs(dz) <= roadWidth / 2;
    }

    private boolean shouldClearBlock(BlockState state) {
        Block block = state.getBlock();
        return CLEARANCE_BLOCKS.contains(block) || 
               block.defaultDestroyTime() < 0.5f; // 清理容易破坏的植物
    }

    private boolean isLeavesBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.OAK_LEAVES || block == Blocks.SPRUCE_LEAVES ||
               block == Blocks.BIRCH_LEAVES || block == Blocks.JUNGLE_LEAVES ||
               block == Blocks.ACACIA_LEAVES || block == Blocks.DARK_OAK_LEAVES ||
               block == Blocks.MANGROVE_LEAVES || block == Blocks.AZALEA_LEAVES ||
               block == Blocks.FLOWERING_AZALEA_LEAVES;
    }

    private boolean hasLogSupport(LevelAccessor level, BlockPos pos) {
        // 检查树叶块是否有原木支撑（避免清理整棵树）
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    
                    BlockPos neighbor = pos.offset(dx, dy, dz);
                    BlockState neighborState = level.getBlockState(neighbor);
                    Block neighborBlock = neighborState.getBlock();
                    
                    if (neighborBlock == Blocks.OAK_LOG || neighborBlock == Blocks.SPRUCE_LOG ||
                        neighborBlock == Blocks.BIRCH_LOG || neighborBlock == Blocks.JUNGLE_LOG ||
                        neighborBlock == Blocks.ACACIA_LOG || neighborBlock == Blocks.DARK_OAK_LOG ||
                        neighborBlock == Blocks.MANGROVE_LOG) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void clearSingleTree(LevelAccessor level, BlockPos treeBase) {
        // 清理单个树（用于精确清理）
        Set<BlockPos> cleared = new HashSet<>();
        clearTreeRecursive(level, treeBase, cleared, 0);
    }

    private void clearTreeRecursive(LevelAccessor level, BlockPos pos, Set<BlockPos> cleared, int depth) {
        if (depth > 32 || cleared.contains(pos)) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!isTreeBlock(state)) {
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        cleared.add(pos);

        // 递归清理相邻的树块
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) { // 主要向上和水平方向
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    
                    BlockPos neighbor = pos.offset(dx, dy, dz);
                    clearTreeRecursive(level, neighbor, cleared, depth + 1);
                }
            }
        }
    }

    private boolean isTreeBlock(BlockState state) {
        Block block = state.getBlock();
        return TREE_BLOCKS.contains(block);
    }
}
