package net.countered.settlementroads.config;

public class NeoForgeModConfigAdapter implements IModConfig {
    @Override
    public String structureToLocate() {
        return ModConfig.structureToLocate();
    }

    @Override
    public int structureSearchRadius() {
        return ModConfig.structureSearchRadius();
    }

    @Override
    public int initialLocatingCount() {
        return ModConfig.initialLocatingCount();
    }

    @Override
    public int maxConcurrentRoadGeneration() {
        return ModConfig.maxConcurrentRoadGeneration();
    }

    @Override
    public int averagingRadius() {
        return ModConfig.averagingRadius();
    }

    @Override
    public boolean allowArtificial() {
        return ModConfig.allowArtificial();
    }

    @Override
    public boolean allowNatural() {
        return ModConfig.allowNatural();
    }

    @Override
    public boolean placeWaypoints() {
        return ModConfig.placeWaypoints();
    }

    @Override
    public boolean placeRoadFences() {
        return ModConfig.placeRoadFences();
    }

    @Override
    public boolean placeSwings() {
        return ModConfig.placeSwings();
    }

    @Override
    public boolean placeBenches() {
        return ModConfig.placeBenches();
    }

    @Override
    public boolean placeGloriettes() {
        return ModConfig.placeGloriettes();
    }

    @Override
    public int structureDistanceFromRoad() {
        return ModConfig.structureDistanceFromRoad();
    }

    @Override
    public int maxHeightDifference() {
        return ModConfig.maxHeightDifference();
    }

    @Override
    public int maxTerrainStability() {
        return ModConfig.maxTerrainStability();
    }

    @Override
    public int manualMaxHeightDifference() {
        return ModConfig.manualMaxHeightDifference();
    }

    @Override
    public int manualMaxTerrainStability() {
        return ModConfig.manualMaxTerrainStability();
    }
}
