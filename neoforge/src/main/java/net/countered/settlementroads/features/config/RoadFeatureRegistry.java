package net.countered.settlementroads.features.config;

import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.features.RoadFeature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoadFeatureRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES =
            DeferredRegister.create(BuiltInRegistries.FEATURE, SettlementRoads.MOD_ID);

    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, RoadFeature> ROAD_FEATURE =
            FEATURES.register("road_feature", () -> new RoadFeature(RoadFeatureConfig.CODEC));

    public static void registerFeatures(IEventBus modEventBus) {
        LOGGER.info("Registering road features");
        FEATURES.register(modEventBus);
    }
}
