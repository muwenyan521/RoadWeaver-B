package net.shiroha233.roadweaver.features.decoration.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.Optional;

public final class WoodSelector {
    private WoodSelector() {}

    public static Records.WoodAssets forBiome(WorldGenLevel world, BlockPos pos) {
        Holder<Biome> biome = world.getBiome(pos);
        Optional<ResourceKey<Biome>> optKey = biome.unwrapKey();
        if (optKey.isPresent()) {
            ResourceKey<Biome> key = optKey.get();
            if (key == Biomes.BAMBOO_JUNGLE) {
                return new Records.WoodAssets(Blocks.BAMBOO_FENCE, Blocks.BAMBOO_HANGING_SIGN, Blocks.BAMBOO_PLANKS);
            } else if (biome.is(BiomeTags.IS_JUNGLE)) {
                return new Records.WoodAssets(Blocks.JUNGLE_FENCE, Blocks.JUNGLE_HANGING_SIGN, Blocks.JUNGLE_PLANKS);
            } else if (biome.is(BiomeTags.IS_SAVANNA)) {
                return new Records.WoodAssets(Blocks.ACACIA_FENCE, Blocks.ACACIA_HANGING_SIGN, Blocks.ACACIA_PLANKS);
            } else if (key == Biomes.DARK_FOREST) {
                return new Records.WoodAssets(Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_HANGING_SIGN, Blocks.DARK_OAK_PLANKS);
            } else if (key == Biomes.CHERRY_GROVE) {
                return new Records.WoodAssets(Blocks.CHERRY_FENCE, Blocks.CHERRY_HANGING_SIGN, Blocks.CHERRY_PLANKS);
            } else if (key == Biomes.BIRCH_FOREST || key == Biomes.OLD_GROWTH_BIRCH_FOREST) {
                return new Records.WoodAssets(Blocks.BIRCH_FENCE, Blocks.BIRCH_HANGING_SIGN, Blocks.BIRCH_PLANKS);
            } else if (biome.is(BiomeTags.IS_TAIGA)) {
                return new Records.WoodAssets(Blocks.SPRUCE_FENCE, Blocks.SPRUCE_HANGING_SIGN, Blocks.SPRUCE_PLANKS);
            } else {
                return new Records.WoodAssets(Blocks.OAK_FENCE, Blocks.OAK_HANGING_SIGN, Blocks.OAK_PLANKS);
            }
        }
        return new Records.WoodAssets(Blocks.OAK_FENCE, Blocks.OAK_HANGING_SIGN, Blocks.OAK_PLANKS);
    }
}
