package net.countered.settlementroads.config;

public interface IModConfig {
    // Structures
    String structureToLocate();
    int structureSearchRadius();

    // Pre-generation
    int initialLocatingCount();
    int maxConcurrentRoadGeneration();

    // Roads
    int averagingRadius();
    boolean allowArtificial();
    boolean allowNatural();
    boolean placeWaypoints();
    boolean placeRoadFences();
    boolean placeSwings();
    boolean placeBenches();
    boolean placeGloriettes();
    int structureDistanceFromRoad();
    int maxHeightDifference();
    int maxTerrainStability();

    // 手动连接时更激进的阈值
    int manualMaxHeightDifference();
    int manualMaxTerrainStability();
}
