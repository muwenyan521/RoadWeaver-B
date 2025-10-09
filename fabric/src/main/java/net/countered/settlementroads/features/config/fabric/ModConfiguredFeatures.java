package net.countered.settlementroads.features.config.fabric;

import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ModConfiguredFeatures {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModConfiguredFeatures.class);

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        LOGGER.info("Bootstrapping configured features for Fabric...");
        
        List<List<BlockState>> artificialMaterials = List.of(
                List.of(Blocks.MUD_BRICKS.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState()),
                List.of(Blocks.POLISHED_ANDESITE.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState()),
                List.of(Blocks.STONE_BRICKS.defaultBlockState(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState())
        );

        List<List<BlockState>> naturalMaterials = List.of(
                List.of(Blocks.COARSE_DIRT.defaultBlockState(), Blocks.ROOTED_DIRT.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState()),
                List.of(Blocks.COBBLESTONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState()),
                List.of(Blocks.DIRT_PATH.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState())
        );

        List<Integer> widths = List.of(3);
        List<Integer> qualities = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);

        context.register(RoadFeature.ROAD_FEATURE_KEY, new ConfiguredFeature<>(
                RoadFeature.ROAD_FEATURE,
                new RoadFeatureConfig(artificialMaterials, naturalMaterials, widths, qualities)
        ));

        LOGGER.info("Configured features bootstrapped successfully for Fabric.");
    }
}
