package net.shiroha233.roadweaver.features.config;

import net.shiroha233.roadweaver.features.RoadFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.level.levelgen.GenerationStep;

public final class FabricBiomeInjection {
    private FabricBiomeInjection() {}

    public static void inject() {
        BiomeModifications.addFeature(
                BiomeSelectors.all(),
                GenerationStep.Decoration.LOCAL_MODIFICATIONS,
                RoadFeature.ROAD_FEATURE_PLACED_KEY
        );
    }
}
