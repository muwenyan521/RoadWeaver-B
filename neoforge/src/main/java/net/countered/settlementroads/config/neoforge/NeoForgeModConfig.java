package net.countered.settlementroads.config.neoforge;

import net.countered.settlementroads.config.IModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class NeoForgeModConfig implements IModConfig {
    
    public static final ServerConfig SERVER;
    public static final ModConfigSpec SERVER_SPEC;
    
    static {
        final Pair<ServerConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static class ServerConfig {
        public final ModConfigSpec.ConfigValue<String> structureToLocate;
        public final ModConfigSpec.IntValue structureSearchRadius;
        public final ModConfigSpec.IntValue initialLocatingCount;
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
        public final ModConfigSpec.IntValue maxConcurrentRoadGeneration;
        // 手动连接模式（更激进阈值）
        public final ModConfigSpec.IntValue manualMaxHeightDifference;
        public final ModConfigSpec.IntValue manualMaxTerrainStability;

        ServerConfig(ModConfigSpec.Builder builder) {
            builder.comment("Structure Configuration").push("structures");
            
            structureToLocate = builder
                    .comment("Structure to locate for road generation")
                    .define("structureToLocate", "#minecraft:village");
                    
            structureSearchRadius = builder
                    .comment("Search radius for structures")
                    .defineInRange("structureSearchRadius", 100, 50, 200);
                    
            builder.pop();
            
            builder.comment("Pre-generation Configuration").push("pre-generation");
            
            initialLocatingCount = builder
                    .comment("Initial structure locating count")
                    .defineInRange("initialLocatingCount", 7, 1, 20);
            maxConcurrentRoadGeneration = builder
                    .comment("Maximum concurrent road generation")
                    .defineInRange("maxConcurrentRoadGeneration", 3, 1, 10);
                    
            builder.pop();

            // Road Configuration
            builder.comment("Road Configuration").push("roads");
            averagingRadius = builder
                    .comment("Averaging radius for road generation")
                    .defineInRange("averagingRadius", 1, 1, 5);
            allowArtificial = builder
                    .comment("Allow artificial road materials")
                    .define("allowArtificial", true);
            allowNatural = builder
                    .comment("Allow natural road materials")
                    .define("allowNatural", true);
            placeWaypoints = builder
                    .comment("Place waypoints on roads")
                    .define("placeWaypoints", false);
            placeRoadFences = builder
                    .comment("Generate road fences: Generate intermittent fence decorations along roads for enhanced visual appeal")
                    .define("placeRoadFences", true);
            placeSwings = builder
                    .comment("Place swings as decorations")
                    .define("placeSwings", true);
            placeBenches = builder
                    .comment("Place benches as decorations")
                    .define("placeBenches", true);
            placeGloriettes = builder
                    .comment("Place gloriettes as decorations")
                    .define("placeGloriettes", true);
            structureDistanceFromRoad = builder
                    .comment("Distance from road for structure placement")
                    .defineInRange("structureDistanceFromRoad", 4, 3, 8);
            maxHeightDifference = builder
                    .comment("Maximum height difference for road generation")
                    .defineInRange("maxHeightDifference", 5, 3, 10);
            maxTerrainStability = builder
                    .comment("Maximum terrain stability for road generation")
                    .defineInRange("maxTerrainStability", 4, 2, 10);
            builder.pop();

            // Manual connect thresholds (more lenient than normal)
            builder.comment("Manual Connection Thresholds").push("manual");
            manualMaxHeightDifference = builder
                    .comment("Maximum height difference when manually connecting structures")
                    .defineInRange("manualMaxHeightDifference", 8, 3, 20);
            manualMaxTerrainStability = builder
                    .comment("Maximum terrain stability when manually connecting structures")
                    .defineInRange("manualMaxTerrainStability", 8, 2, 20);
            builder.pop();
        }
    }

    // Implement ModConfig interface methods
    @Override
    public String structureToLocate() {
        return SERVER.structureToLocate.get();
    }

    @Override
    public int structureSearchRadius() {
        return SERVER.structureSearchRadius.get();
    }

    @Override
    public int initialLocatingCount() {
        return SERVER.initialLocatingCount.get();
    }

    @Override
    public int maxConcurrentRoadGeneration() {
        return SERVER.maxConcurrentRoadGeneration.get();
    }

    @Override
    public int averagingRadius() {
        return SERVER.averagingRadius.get();
    }

    @Override
    public boolean allowArtificial() {
        return SERVER.allowArtificial.get();
    }

    @Override
    public boolean allowNatural() {
        return SERVER.allowNatural.get();
    }

    @Override
    public boolean placeWaypoints() {
        return SERVER.placeWaypoints.get();
    }

    @Override
    public boolean placeRoadFences() {
        return SERVER.placeRoadFences.get();
    }

    @Override
    public boolean placeSwings() {
        return SERVER.placeSwings.get();
    }

    @Override
    public boolean placeBenches() {
        return SERVER.placeBenches.get();
    }

    @Override
    public boolean placeGloriettes() {
        return SERVER.placeGloriettes.get();
    }

    @Override
    public int structureDistanceFromRoad() {
        return SERVER.structureDistanceFromRoad.get();
    }

    @Override
    public int maxHeightDifference() {
        return SERVER.maxHeightDifference.get();
    }

    @Override
    public int maxTerrainStability() {
        return SERVER.maxTerrainStability.get();
    }

    @Override
    public int manualMaxHeightDifference() {
        return SERVER.manualMaxHeightDifference.get();
    }

    @Override
    public int manualMaxTerrainStability() {
        return SERVER.manualMaxTerrainStability.get();
    }
}
