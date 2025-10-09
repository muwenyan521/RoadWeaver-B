package net.countered.settlementroads.features.config.fabric;

import net.countered.settlementroads.features.RoadFeature;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ModPlacedFeatures {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ModPlacedFeatures.class);
    
    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        LOGGER.info("Bootstrapping placed features for Fabric...");
        
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        
        List<PlacementModifier> roadPlacements = List.of(
                HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG)
        );
        
        context.register(RoadFeature.ROAD_FEATURE_PLACED_KEY, new PlacedFeature(
                configuredFeatures.getOrThrow(RoadFeature.ROAD_FEATURE_KEY), 
                roadPlacements
        ));
        
        LOGGER.info("Placed features bootstrapped successfully for Fabric.");
    }
}
