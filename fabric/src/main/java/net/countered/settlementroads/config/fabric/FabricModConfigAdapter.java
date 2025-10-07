package net.countered.settlementroads.config.fabric;

import net.countered.settlementroads.config.ModConfig;

/**
 * Adapter class to bridge MidnightConfig static fields with ModConfig interface
 */
public class FabricModConfigAdapter implements ModConfig {
    
    @Override
    public String structureToLocate() {
        return FabricModConfig.structureToLocate;
    }

    @Override
    public int structureSearchRadius() {
        return FabricModConfig.structureSearchRadius;
    }

    @Override
    public int initialLocatingCount() {
        return FabricModConfig.initialLocatingCount;
    }

    @Override
    public int maxConcurrentRoadGeneration() {
        return FabricModConfig.maxConcurrentRoadGeneration;
    }

    @Override
    public int averagingRadius() {
        return FabricModConfig.averagingRadius;
    }

    @Override
    public boolean allowArtificial() {
        return FabricModConfig.allowArtificial;
    }

    @Override
    public boolean allowNatural() {
        return FabricModConfig.allowNatural;
    }

    @Override
    public boolean placeWaypoints() {
        return FabricModConfig.placeWaypoints;
    }

    @Override
    public boolean placeRoadFences() {
        return FabricModConfig.placeRoadFences;
    }

    @Override
    public boolean placeSwings() {
        return FabricModConfig.placeSwings;
    }

    @Override
    public boolean placeBenches() {
        return FabricModConfig.placeBenches;
    }

    @Override
    public boolean placeGloriettes() {
        return FabricModConfig.placeGloriettes;
    }

    @Override
    public int structureDistanceFromRoad() {
        return FabricModConfig.structureDistanceFromRoad;
    }

    @Override
    public int maxHeightDifference() {
        return FabricModConfig.maxHeightDifference;
    }

    @Override
    public int maxTerrainStability() {
        return FabricModConfig.maxTerrainStability;
    }
}
