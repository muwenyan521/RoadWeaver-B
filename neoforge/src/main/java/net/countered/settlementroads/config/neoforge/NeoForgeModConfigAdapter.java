package net.countered.settlementroads.config.neoforge;

import net.countered.settlementroads.config.IModConfig;

public class NeoForgeModConfigAdapter implements IModConfig {
    private static final NeoForgeModConfig CONFIG = new NeoForgeModConfig();
    
    @Override
    public String structureToLocate() {
        return CONFIG.structureToLocate();
    }

    @Override
    public int structureSearchRadius() {
        return CONFIG.structureSearchRadius();
    }

    @Override
    public int initialLocatingCount() {
        return CONFIG.initialLocatingCount();
    }

    @Override
    public int maxConcurrentRoadGeneration() {
        return CONFIG.maxConcurrentRoadGeneration();
    }

    @Override
    public int averagingRadius() {
        return CONFIG.averagingRadius();
    }

    @Override
    public boolean allowArtificial() {
        return CONFIG.allowArtificial();
    }

    @Override
    public boolean allowNatural() {
        return CONFIG.allowNatural();
    }

    @Override
    public boolean placeWaypoints() {
        return CONFIG.placeWaypoints();
    }

    @Override
    public boolean placeRoadFences() {
        return CONFIG.placeRoadFences();
    }

    @Override
    public boolean placeSwings() {
        return CONFIG.placeSwings();
    }

    @Override
    public boolean placeBenches() {
        return CONFIG.placeBenches();
    }

    @Override
    public boolean placeGloriettes() {
        return CONFIG.placeGloriettes();
    }

    @Override
    public int structureDistanceFromRoad() {
        return CONFIG.structureDistanceFromRoad();
    }

    @Override
    public int maxHeightDifference() {
        return CONFIG.maxHeightDifference();
    }

    @Override
    public int maxTerrainStability() {
        return CONFIG.maxTerrainStability();
    }

    @Override
    public int manualMaxHeightDifference() {
        return CONFIG.manualMaxHeightDifference();
    }

    @Override
    public int manualMaxTerrainStability() {
        return CONFIG.manualMaxTerrainStability();
    }
}
