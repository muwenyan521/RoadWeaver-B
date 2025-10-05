package net.countered.settlementroads.features.config;

import net.countered.settlementroads.SettlementRoads;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ModPlacedFeatures {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    public static final ResourceKey<PlacedFeature> ROAD_FEATURE_PLACED_KEY =
            ResourceKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(SettlementRoads.MOD_ID, "road_feature_placed"));

    public static void bootstrap(BootstrapContext<PlacedFeature> context){
        LOGGER.info("Bootstrap PlacedFeature");
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        context.register(ROAD_FEATURE_PLACED_KEY,
                new PlacedFeature(configuredFeatures.getOrThrow(ModConfiguredFeatures.ROAD_FEATURE_KEY),
                        List.of(HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG)))
        );
    }
}
