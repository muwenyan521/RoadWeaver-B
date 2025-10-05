package net.countered.settlementroads.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ModConfig {
    
    public static final ServerConfig SERVER;
    public static final ModConfigSpec SERVER_SPEC;
    
    static {
        Pair<ServerConfig, ModConfigSpec> serverPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();
    }
    
    public static class ServerConfig {
        // Structures
        public final ModConfigSpec.ConfigValue<String> structureToLocate;
        public final ModConfigSpec.IntValue structureSearchRadius;
        
        // Pre-generation
        public final ModConfigSpec.IntValue initialLocatingCount;
        public final ModConfigSpec.IntValue maxConcurrentRoadGeneration;
        
        // Roads
        public final ModConfigSpec.IntValue averagingRadius;
        public final ModConfigSpec.BooleanValue allowArtificial;
        public final ModConfigSpec.BooleanValue allowNatural;
        public final ModConfigSpec.BooleanValue placeWaypoints;
        public final ModConfigSpec.BooleanValue placeRoadFences;
        public final ModConfigSpec.BooleanValue placeSwings;
        public final ModConfigSpec.BooleanValue placeBenches;
        public final ModConfigSpec.BooleanValue placeGloriettes;
        public final ModConfigSpec.IntValue structureDistanceFromRoad;
        public final ModConfigSpec.IntValue maxHeightDifference;
        public final ModConfigSpec.IntValue maxTerrainStability;
        
        public ServerConfig(ModConfigSpec.Builder builder) {
            builder.push("structures");
            structureToLocate = builder
                    .comment("Structure tag or ID to locate (e.g., #minecraft:village)")
                    .define("structureToLocate", "#minecraft:village");
            structureSearchRadius = builder
                    .comment("Radius to search for structures")
                    .defineInRange("structureSearchRadius", 100, 50, 200);
            builder.pop();
            
            builder.push("pre-generation");
            initialLocatingCount = builder
                    .comment("Number of initial structures to locate on world load")
                    .defineInRange("initialLocatingCount", 7, 1, 20);
            maxConcurrentRoadGeneration = builder
                    .comment("Maximum number of concurrent road generation tasks")
                    .defineInRange("maxConcurrentRoadGeneration", 3, 1, 10);
            builder.pop();
            
            builder.push("roads");
            averagingRadius = builder
                    .comment("Radius for terrain height averaging")
                    .defineInRange("averagingRadius", 1, 0, 5);
            allowArtificial = builder
                    .comment("Allow artificial (stone) roads")
                    .define("allowArtificial", true);
            allowNatural = builder
                    .comment("Allow natural (dirt/grass) roads")
                    .define("allowNatural", true);
            placeWaypoints = builder
                    .comment("Place waypoint markers along roads")
                    .define("placeWaypoints", false);
            placeRoadFences = builder
                    .comment("Place fence decorations along roads")
                    .define("placeRoadFences", true);
            placeSwings = builder
                    .comment("Place swing decorations along roads")
                    .define("placeSwings", true);
            placeBenches = builder
                    .comment("Place bench decorations along roads")
                    .define("placeBenches", true);
            placeGloriettes = builder
                    .comment("Place gloriette decorations along roads")
                    .define("placeGloriettes", true);
            structureDistanceFromRoad = builder
                    .comment("Distance from road to structure entrance")
                    .defineInRange("structureDistanceFromRoad", 4, 3, 8);
            maxHeightDifference = builder
                    .comment("Maximum height difference for road placement")
                    .defineInRange("maxHeightDifference", 5, 3, 10);
            maxTerrainStability = builder
                    .comment("Maximum terrain stability value")
                    .defineInRange("maxTerrainStability", 4, 2, 10);
            builder.pop();
        }
    }
    
    // 兼容层 - 提供静态访问方法，保持与 Fabric 版本的 API 一致
    public static String structureToLocate() {
        return SERVER.structureToLocate.get();
    }
    
    public static int structureSearchRadius() {
        return SERVER.structureSearchRadius.get();
    }
    
    public static int initialLocatingCount() {
        return SERVER.initialLocatingCount.get();
    }
    
    public static int averagingRadius() {
        return SERVER.averagingRadius.get();
    }
    
    public static boolean allowArtificial() {
        return SERVER.allowArtificial.get();
    }
    
    public static boolean allowNatural() {
        return SERVER.allowNatural.get();
    }
    
    public static boolean placeWaypoints() {
        return SERVER.placeWaypoints.get();
    }
    
    public static boolean placeRoadFences() {
        return SERVER.placeRoadFences.get();
    }
    
    public static boolean placeSwings() {
        return SERVER.placeSwings.get();
    }
    
    public static boolean placeBenches() {
        return SERVER.placeBenches.get();
    }
    
    public static boolean placeGloriettes() {
        return SERVER.placeGloriettes.get();
    }
    
    public static int structureDistanceFromRoad() {
        return SERVER.structureDistanceFromRoad.get();
    }
    
    public static int maxHeightDifference() {
        return SERVER.maxHeightDifference.get();
    }
    
    public static int maxTerrainStability() {
        return SERVER.maxTerrainStability.get();
    }
    
    public static int maxConcurrentRoadGeneration() {
        return SERVER.maxConcurrentRoadGeneration.get();
    }
}
