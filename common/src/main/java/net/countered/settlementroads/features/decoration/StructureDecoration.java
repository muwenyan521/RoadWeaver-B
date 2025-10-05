package net.countered.settlementroads.features.decoration;

import net.countered.settlementroads.features.decoration.util.BiomeWoodAware;
import net.countered.settlementroads.helpers.Records;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;

/**
 * 结构装饰基类，用于处理NBT结构文件的加载和放置
 * 注意：具体的NBT结构加载实现需要在平台特定模块中完成
 */
public abstract class StructureDecoration extends OrientedDecoration implements BiomeWoodAware {
    private Records.WoodAssets wood;

    public StructureDecoration(BlockPos pos, Vec3i direction, WorldGenLevel world) {
        super(pos, direction, world);
    }

    @Override
    public void setWoodType(Records.WoodAssets assets) {
        this.wood = assets;
    }

    protected Records.WoodAssets getWood() {
        return wood;
    }

    // Abstract method for loading and placing structures
    // Platform-specific implementations will handle NBT loading
    protected abstract void loadAndPlaceStructure();
}
