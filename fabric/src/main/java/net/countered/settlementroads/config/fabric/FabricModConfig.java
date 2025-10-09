package net.countered.settlementroads.config.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FabricModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("roadweaver.json");
    
    private static ConfigData data = new ConfigData();
    
    // 结构配置
    public static String getStructureToLocate() { return data.structureToLocate; }
    public static void setStructureToLocate(String value) { data.structureToLocate = value; }
    
    public static int getStructureSearchRadius() { return data.structureSearchRadius; }
    public static void setStructureSearchRadius(int value) { data.structureSearchRadius = value; }
    
    // 预生成配置
    public static int getInitialLocatingCount() { return data.initialLocatingCount; }
    public static void setInitialLocatingCount(int value) { data.initialLocatingCount = value; }
    
    public static int getMaxConcurrentRoadGeneration() { return data.maxConcurrentRoadGeneration; }
    public static void setMaxConcurrentRoadGeneration(int value) { data.maxConcurrentRoadGeneration = value; }
    
    public static int getStructureSearchTriggerDistance() { return data.structureSearchTriggerDistance; }
    public static void setStructureSearchTriggerDistance(int value) { data.structureSearchTriggerDistance = value; }
    
    // 道路配置
    public static int getAveragingRadius() { return data.averagingRadius; }
    public static void setAveragingRadius(int value) { data.averagingRadius = value; }
    
    public static boolean getAllowArtificial() { return data.allowArtificial; }
    public static void setAllowArtificial(boolean value) { data.allowArtificial = value; }
    
    public static boolean getAllowNatural() { return data.allowNatural; }
    public static void setAllowNatural(boolean value) { data.allowNatural = value; }
    
    public static int getStructureDistanceFromRoad() { return data.structureDistanceFromRoad; }
    public static void setStructureDistanceFromRoad(int value) { data.structureDistanceFromRoad = value; }
    
    public static int getMaxHeightDifference() { return data.maxHeightDifference; }
    public static void setMaxHeightDifference(int value) { data.maxHeightDifference = value; }
    
    public static int getMaxTerrainStability() { return data.maxTerrainStability; }
    public static void setMaxTerrainStability(int value) { data.maxTerrainStability = value; }
    
    // 装饰配置
    public static boolean getPlaceWaypoints() { return data.placeWaypoints; }
    public static void setPlaceWaypoints(boolean value) { data.placeWaypoints = value; }
    
    public static boolean getPlaceRoadFences() { return data.placeRoadFences; }
    public static void setPlaceRoadFences(boolean value) { data.placeRoadFences = value; }
    
    public static boolean getPlaceSwings() { return data.placeSwings; }
    public static void setPlaceSwings(boolean value) { data.placeSwings = value; }
    
    public static boolean getPlaceBenches() { return data.placeBenches; }
    public static void setPlaceBenches(boolean value) { data.placeBenches = value; }
    
    public static boolean getPlaceGloriettes() { return data.placeGloriettes; }
    public static void setPlaceGloriettes(boolean value) { data.placeGloriettes = value; }
    
    // 手动模式配置
    public static int getManualMaxHeightDifference() { return data.manualMaxHeightDifference; }
    public static void setManualMaxHeightDifference(int value) { data.manualMaxHeightDifference = value; }
    
    public static int getManualMaxTerrainStability() { return data.manualMaxTerrainStability; }
    public static void setManualMaxTerrainStability(int value) { data.manualMaxTerrainStability = value; }
    
    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                data = GSON.fromJson(json, ConfigData.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }
    
    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static class ConfigData {
        // 结构配置
        String structureToLocate = "#minecraft:village";
        int structureSearchRadius = 100;
        
        // 预生成配置
        int initialLocatingCount = 7;
        int maxConcurrentRoadGeneration = 3;
        int structureSearchTriggerDistance = 500;
        
        // 道路配置
        int averagingRadius = 1;
        boolean allowArtificial = true;
        boolean allowNatural = true;
        int structureDistanceFromRoad = 4;
        int maxHeightDifference = 5;
        int maxTerrainStability = 4;
        
        // 装饰配置
        boolean placeWaypoints = false;
        boolean placeRoadFences = true;
        boolean placeSwings = true;
        boolean placeBenches = true;
        boolean placeGloriettes = true;
        
        // 手动模式配置
        int manualMaxHeightDifference = 8;
        int manualMaxTerrainStability = 8;
    }
}
