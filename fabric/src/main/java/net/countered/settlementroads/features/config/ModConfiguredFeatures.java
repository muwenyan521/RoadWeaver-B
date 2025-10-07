package net.countered.settlementroads.features.config;

import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.features.RoadFeature;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ModConfiguredFeatures {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context){
        LOGGER.info("Bootstrap ConfiguredFeature");
        context.register(RoadFeature.ROAD_FEATURE_KEY,
                new ConfiguredFeature<>(RoadFeature.ROAD_FEATURE,
                        new RoadFeatureConfig(
                                // artificial
                                List.of(List.of(Blocks.MUD_BRICKS.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState()),
                                        List.of(Blocks.POLISHED_ANDESITE.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState()),
                                        List.of(Blocks.STONE_BRICKS.defaultBlockState(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState())),
                                // natural
                                List.of(List.of(Blocks.COARSE_DIRT.defaultBlockState(), Blocks.ROOTED_DIRT.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState()),
                                        List.of(Blocks.COBBLESTONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState()),
                                        List.of(Blocks.DIRT_PATH.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState())),
                                List.of(3),                                                                                                           // width
                                List.of(1,2,3,4,5,6,7,8,9)                                                                                                // quality (not used)
                        )
                )
        );
    }
}