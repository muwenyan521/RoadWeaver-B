package net.countered.settlementroads.api.materials;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 道路材料接口
 * 定义自定义道路材料的行为
 */
public interface RoadMaterial {
    
    /**
     * 获取材料的唯一标识符
     * @return 材料ID
     */
    ResourceLocation getId();
    
    /**
     * 获取材料类型（人工/自然）
     * @return 材料类型
     */
    MaterialType getType();
    
    /**
     * 获取在指定生物群系中的材料方块状态列表
     * @param biome 生物群系
     * @param pos 位置
     * @param world 世界
     * @return 方块状态列表
     */
    List<BlockState> getBlockStates(Biome biome, BlockPos pos, WorldGenLevel world);
    
    /**
     * 获取材料在指定生物群系中的权重
     * @param biome 生物群系
     * @return 权重值，0表示不使用
     */
    int getWeight(Biome biome);
    
    /**
     * 检查材料是否可以在指定位置使用
     * @param pos 位置
     * @param biome 生物群系
     * @param world 世界
     * @return 是否可以使用
     */
    boolean canUseAt(BlockPos pos, Biome biome, WorldGenLevel world);
    
    /**
     * 获取材料的耐久度（影响道路维护频率）
     * @return 耐久度值
     */
    default int getDurability() {
        return 100;
    }
    
    /**
     * 获取材料的生成成本（影响路径计算）
     * @return 成本值
     */
    default double getCost() {
        return 1.0;
    }
    
    /**
     * 是否需要基础方块（如石头基座）
     * @return 是否需要基础
     */
    default boolean requiresFoundation() {
        return false;
    }
    
    /**
     * 获取基础方块状态
     * @param biome 生物群系
     * @param pos 位置
     * @param world 世界
     * @return 基础方块状态
     */
    default BlockState getFoundationBlock(Biome biome, BlockPos pos, WorldGenLevel world) {
        return null;
    }
}
