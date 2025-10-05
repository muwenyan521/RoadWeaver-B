package net.countered.settlementroads.config;

/**
 * Abstract interface for mod configuration across different platforms.
 * This allows the common module to work with both Fabric's MidnightConfig and NeoForge's ModConfigSpec.
 */
public interface ModConfig {
    
    // Structure configuration
    String structureToLocate();
    int structureSearchRadius();
    
    // Pre-generation configuration
    int initialLocatingCount();
    int maxConcurrentRoadGeneration();
    
    // Road generation configuration
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
    
    // Get platform-specific implementation
    static ModConfig getInstance() {
        return ModConfigImpl.INSTANCE;
    }
}
