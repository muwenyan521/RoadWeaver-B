package net.shiroha233.roadweaver.config;

import java.util.ArrayList;
import java.util.List;

public final class ModConfig {
    public enum PlanningAlgorithm {
        KNN,
        DELAUNAY,
        RNG
    }
    private boolean villagePredictionEnabled;
    private int predictRadiusChunks;
    private boolean biomePrefilter;
    private List<String> structureWhitelist;
    private List<String> structureBlacklist;

    // 路网规划配置
    private int initialPlanRadiusChunks;
    private boolean dynamicPlanEnabled;
    private int dynamicPlanRadiusChunks;
    private int dynamicPlanStrideChunks;
    private PlanningAlgorithm planningAlgorithm;

    // 道路生成配置
    private boolean allowArtificial;
    private boolean placeWaypoints;
    private int averagingRadius;
    private int generationThreads;
    private int maxConcurrentGenerations;
    private int aStarStep;

    private int roadWidth;         
    private int lampInterval;      
    private boolean tunnelEnabled;
    private int tunnelClearHeight;
    
    // 增量MST更新与高级寻路优化配置
    private boolean useOptimizedPlanning;
    private boolean useIncrementalMST;
    private boolean useGabrielConstraint;
    private boolean useAngleConstraint;

    private List<String> dimensionSelectors;
    private boolean terrainAnalyzerEnabled;
    private int terrainRoughRadius;
    private int terrainRoughStride;
    private int terrainRangeThreshold;
    private double terrainPenaltyScale;
    private boolean preferLandOverWater;
    private double waterStepPenalty;
    private int coastAvoidBufferBlocks;
    private double coastProximityPenalty;
    private List<String> forbiddenBiomeSelectors;
    private int forbiddenBiomeBufferBlocks;
    private double forbiddenBiomeProximityPenalty;
    private boolean acceptPartialPaths;
    private double partialProgressThreshold;
    private boolean debugVerboseLogs;
    private boolean debugPipelineProfiler;
    
    // 生物群系样式配置
    private List<LampPostConfigEntry> lampPostOverrides;
    private List<RoadStyleConfigEntry> roadStyleOverrides;
    private List<RoadStyleConfigEntry> bopRoadStyleOverrides;
    

    public ModConfig() {
        this.villagePredictionEnabled = true;
        this.predictRadiusChunks = 1024;
        this.biomePrefilter = true;
        this.structureWhitelist = new ArrayList<>();
        this.structureBlacklist = new ArrayList<>();
        this.structureWhitelist.add("#minecraft:village");

        // 默认规划参数：初始64区块；动态规划开启，半径256区块
        this.initialPlanRadiusChunks = 64;
        this.dynamicPlanEnabled = true;
        this.dynamicPlanRadiusChunks = 256;
        this.dynamicPlanStrideChunks = Math.max(8, Math.min(64, this.dynamicPlanRadiusChunks / 2));
        this.planningAlgorithm = PlanningAlgorithm.RNG;

        // 道路生成默认参数
        this.allowArtificial = true;
        this.placeWaypoints = false;
        this.averagingRadius = 8;
        
        this.generationThreads = Math.max(2, Math.min(3, Runtime.getRuntime().availableProcessors()));
        this.maxConcurrentGenerations = Math.max(1, Math.min(3, this.generationThreads));
        this.aStarStep = 16;

        // 新增默认值
        this.roadWidth = 3;    
        this.lampInterval = 32; 
        this.tunnelEnabled = false;
        this.tunnelClearHeight = 5;
        
        // 增量MST更新与寻路优化默认配置
        this.useOptimizedPlanning = true;
        this.useIncrementalMST = true;
        this.useGabrielConstraint = true;
        this.useAngleConstraint = true;

        this.dimensionSelectors = new ArrayList<>();
        this.dimensionSelectors.add("minecraft:overworld");
        
        this.terrainAnalyzerEnabled = true;
        this.terrainRoughRadius = 8;
        this.terrainRoughStride = 4;
        this.terrainRangeThreshold = 12;
        this.terrainPenaltyScale = 1.5;
        
        this.preferLandOverWater = true;
        this.waterStepPenalty = 240.0;
        this.coastAvoidBufferBlocks = 8;
        this.coastProximityPenalty = 80.0;
        
        this.forbiddenBiomeSelectors = new ArrayList<>();
        this.forbiddenBiomeBufferBlocks = 8;
        this.forbiddenBiomeProximityPenalty = 160.0;
        
        this.acceptPartialPaths = true;
        this.partialProgressThreshold = 0.8;
        
        this.debugVerboseLogs = false;
        this.debugPipelineProfiler = false;
        
        // 生物群系样式配置默认值
        this.lampPostOverrides = new ArrayList<>();
        this.roadStyleOverrides = new ArrayList<>();
        this.bopRoadStyleOverrides = new ArrayList<>();
    }

    public boolean villagePredictionEnabled() {
        return villagePredictionEnabled;
    }

    public void setVillagePredictionEnabled(boolean villagePredictionEnabled) {
        this.villagePredictionEnabled = villagePredictionEnabled;
    }

    public int predictRadiusChunks() {
        return predictRadiusChunks;
    }

    public void setPredictRadiusChunks(int predictRadiusChunks) {
        this.predictRadiusChunks = predictRadiusChunks;
    }

    public boolean biomePrefilter() {
        return biomePrefilter;
    }

    public void setBiomePrefilter(boolean biomePrefilter) {
        this.biomePrefilter = biomePrefilter;
    }

    

    public List<String> structureWhitelist() {
        return structureWhitelist;
    }

    public void setStructureWhitelist(List<String> structureWhitelist) {
        this.structureWhitelist = structureWhitelist == null ? new ArrayList<>() : new ArrayList<>(structureWhitelist);
    }

    public List<String> structureBlacklist() {
        return structureBlacklist;
    }

    public void setStructureBlacklist(List<String> structureBlacklist) {
        this.structureBlacklist = structureBlacklist == null ? new ArrayList<>() : new ArrayList<>(structureBlacklist);
    }

    

    // 载入后修复缺省值与兼容项
    public void sanitize() {
        if (structureWhitelist == null) structureWhitelist = new ArrayList<>();
        if (structureBlacklist == null) structureBlacklist = new ArrayList<>();

        

        if (predictRadiusChunks <= 0) predictRadiusChunks = 1024;
        if (initialPlanRadiusChunks <= 0) initialPlanRadiusChunks = 64;
        if (dynamicPlanRadiusChunks <= 0) dynamicPlanRadiusChunks = 256;
        if (dynamicPlanStrideChunks <= 0) dynamicPlanStrideChunks = Math.max(8, Math.min(64, Math.max(1, dynamicPlanRadiusChunks) / 2));
        if (dynamicPlanStrideChunks > dynamicPlanRadiusChunks) dynamicPlanStrideChunks = dynamicPlanRadiusChunks;
        if (dynamicPlanStrideChunks > 256) dynamicPlanStrideChunks = 256;
        if (planningAlgorithm == null) planningAlgorithm = PlanningAlgorithm.RNG;

        // 道路生成安全边界
        if (averagingRadius < 0) averagingRadius = 0;
        if (generationThreads < 1) generationThreads = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
        if (generationThreads > 64) generationThreads = 64;
        if (maxConcurrentGenerations < 1) maxConcurrentGenerations = generationThreads;
        int maxCap = Math.max(1, generationThreads * 2);
        if (maxConcurrentGenerations > maxCap) maxConcurrentGenerations = maxCap;
        if (aStarStep < 4) aStarStep = 16;            // 合理下限
        if (aStarStep > 128) aStarStep = 128;         // 合理上限

        // 新增字段校验
        if (roadWidth < 0) roadWidth = 0;            // 0=自动
        if (roadWidth > 15) roadWidth = 15;          // 宽度上限合理限制
        if (lampInterval < 1) lampInterval = 59;     // 保底
        if (lampInterval > 2048) lampInterval = 2048;
        if (tunnelClearHeight < 2) tunnelClearHeight = 2;
        if (tunnelClearHeight > 16) tunnelClearHeight = 16;
        
        if (dimensionSelectors == null) dimensionSelectors = new ArrayList<>();
        if (dimensionSelectors.isEmpty()) dimensionSelectors.add("minecraft:overworld");
        
        if (terrainRoughRadius < 1) terrainRoughRadius = 8;
        if (terrainRoughRadius > 32) terrainRoughRadius = 32;
        if (terrainRoughStride < 1) terrainRoughStride = 4;
        if (terrainRoughStride > terrainRoughRadius) terrainRoughStride = terrainRoughRadius;
        if (terrainRangeThreshold < 1) terrainRangeThreshold = 12;
        if (terrainRangeThreshold > 64) terrainRangeThreshold = 64;
        if (terrainPenaltyScale < 0.1) terrainPenaltyScale = 0.1;
        if (terrainPenaltyScale > 10.0) terrainPenaltyScale = 10.0;
        
        if (waterStepPenalty < 0.0) waterStepPenalty = 0.0;
        if (waterStepPenalty > 1000.0) waterStepPenalty = 1000.0;
        if (coastAvoidBufferBlocks < 0) coastAvoidBufferBlocks = 0;
        if (coastAvoidBufferBlocks > 64) coastAvoidBufferBlocks = 64;
        if (coastProximityPenalty < 0.0) coastProximityPenalty = 0.0;
        if (coastProximityPenalty > 1000.0) coastProximityPenalty = 1000.0;
        
        if (forbiddenBiomeSelectors == null) forbiddenBiomeSelectors = new ArrayList<>();
        if (forbiddenBiomeBufferBlocks < 0) forbiddenBiomeBufferBlocks = 0;
        if (forbiddenBiomeBufferBlocks > 64) forbiddenBiomeBufferBlocks = 64;
        if (forbiddenBiomeProximityPenalty < 0.0) forbiddenBiomeProximityPenalty = 0.0;
        if (forbiddenBiomeProximityPenalty > 1000.0) forbiddenBiomeProximityPenalty = 1000.0;
        
        if (partialProgressThreshold < 0.0) partialProgressThreshold = 0.0;
        if (partialProgressThreshold > 1.0) partialProgressThreshold = 1.0;
        
        // 生物群系样式配置字段校验
        if (lampPostOverrides == null) lampPostOverrides = new ArrayList<>();
        if (roadStyleOverrides == null) roadStyleOverrides = new ArrayList<>();
        if (bopRoadStyleOverrides == null) bopRoadStyleOverrides = new ArrayList<>();
    }

    // 初始规划半径
    public int initialPlanRadiusChunks() { return initialPlanRadiusChunks; }
    public void setInitialPlanRadiusChunks(int v) { this.initialPlanRadiusChunks = v; }

    // 动态规划开关
    public boolean dynamicPlanEnabled() { return dynamicPlanEnabled; }
    public void setDynamicPlanEnabled(boolean v) { this.dynamicPlanEnabled = v; }

    // 动态规划半径
    public int dynamicPlanRadiusChunks() { return dynamicPlanRadiusChunks; }
    public void setDynamicPlanRadiusChunks(int v) { this.dynamicPlanRadiusChunks = v; }

    // 动态规划触发步进
    public int dynamicPlanStrideChunks() { return dynamicPlanStrideChunks; }
    public void setDynamicPlanStrideChunks(int v) { this.dynamicPlanStrideChunks = v; }

    public boolean allowArtificial() { return allowArtificial; }
    public void setAllowArtificial(boolean v) { this.allowArtificial = v; }


    public boolean placeWaypoints() { return placeWaypoints; }
    public void setPlaceWaypoints(boolean v) { this.placeWaypoints = v; }

    public int averagingRadius() { return averagingRadius; }
    public void setAveragingRadius(int v) { this.averagingRadius = v; }

    

    public int generationThreads() { return generationThreads; }
    public void setGenerationThreads(int v) { this.generationThreads = v; }

    public int maxConcurrentGenerations() { return maxConcurrentGenerations; }
    public void setMaxConcurrentGenerations(int v) { this.maxConcurrentGenerations = v; }

    // A* 采样步长
    public int aStarStep() { return aStarStep; }
    public void setAStarStep(int v) { this.aStarStep = v; }

    // 道路宽度（0=自动）
    public int roadWidth() { return roadWidth; }
    public void setRoadWidth(int v) { this.roadWidth = v; }

    // 路灯间隔（段）
    public int lampInterval() { return lampInterval; }
    public void setLampInterval(int v) { this.lampInterval = v; }

    

    // 路网连边算法
    public PlanningAlgorithm planningAlgorithm() { return planningAlgorithm; }
    public void setPlanningAlgorithm(PlanningAlgorithm v) { this.planningAlgorithm = v; }

    public boolean tunnelEnabled() { return tunnelEnabled; }
    public void setTunnelEnabled(boolean v) { this.tunnelEnabled = v; }

    public int tunnelClearHeight() { return tunnelClearHeight; }
    public void setTunnelClearHeight(int v) { this.tunnelClearHeight = v; }

    // 增量MST更新与寻路优化配置方法
    public boolean useOptimizedPlanning() { return useOptimizedPlanning; }
    public void setUseOptimizedPlanning(boolean v) { this.useOptimizedPlanning = v; }

    public boolean useIncrementalMST() { return useIncrementalMST; }
    public void setUseIncrementalMST(boolean v) { this.useIncrementalMST = v; }

    public boolean useGabrielConstraint() { return useGabrielConstraint; }
    public void setUseGabrielConstraint(boolean v) { this.useGabrielConstraint = v; }

    public boolean useAngleConstraint() { return useAngleConstraint; }
    public void setUseAngleConstraint(boolean v) { this.useAngleConstraint = v; }

    // 新增配置字段方法
    public List<String> dimensionSelectors() { return dimensionSelectors; }
    public void setDimensionSelectors(List<String> v) { 
        this.dimensionSelectors = v == null ? new ArrayList<>() : new ArrayList<>(v); 
    }

    public boolean terrainAnalyzerEnabled() { return terrainAnalyzerEnabled; }
    public void setTerrainAnalyzerEnabled(boolean v) { this.terrainAnalyzerEnabled = v; }

    public int terrainRoughRadius() { return terrainRoughRadius; }
    public void setTerrainRoughRadius(int v) { this.terrainRoughRadius = v; }

    public int terrainRoughStride() { return terrainRoughStride; }
    public void setTerrainRoughStride(int v) { this.terrainRoughStride = v; }

    public int terrainRangeThreshold() { return terrainRangeThreshold; }
    public void setTerrainRangeThreshold(int v) { this.terrainRangeThreshold = v; }

    public double terrainPenaltyScale() { return terrainPenaltyScale; }
    public void setTerrainPenaltyScale(double v) { this.terrainPenaltyScale = v; }

    public boolean preferLandOverWater() { return preferLandOverWater; }
    public void setPreferLandOverWater(boolean v) { this.preferLandOverWater = v; }

    public double waterStepPenalty() { return waterStepPenalty; }
    public void setWaterStepPenalty(double v) { this.waterStepPenalty = v; }

    public int coastAvoidBufferBlocks() { return coastAvoidBufferBlocks; }
    public void setCoastAvoidBufferBlocks(int v) { this.coastAvoidBufferBlocks = v; }

    public double coastProximityPenalty() { return coastProximityPenalty; }
    public void setCoastProximityPenalty(double v) { this.coastProximityPenalty = v; }

    public List<String> forbiddenBiomeSelectors() { return forbiddenBiomeSelectors; }
    public void setForbiddenBiomeSelectors(List<String> v) { 
        this.forbiddenBiomeSelectors = v == null ? new ArrayList<>() : new ArrayList<>(v); 
    }

    public int forbiddenBiomeBufferBlocks() { return forbiddenBiomeBufferBlocks; }
    public void setForbiddenBiomeBufferBlocks(int v) { this.forbiddenBiomeBufferBlocks = v; }

    public double forbiddenBiomeProximityPenalty() { return forbiddenBiomeProximityPenalty; }
    public void setForbiddenBiomeProximityPenalty(double v) { this.forbiddenBiomeProximityPenalty = v; }

    public boolean acceptPartialPaths() { return acceptPartialPaths; }
    public void setAcceptPartialPaths(boolean v) { this.acceptPartialPaths = v; }

    public double partialProgressThreshold() { return partialProgressThreshold; }
    public void setPartialProgressThreshold(double v) { this.partialProgressThreshold = v; }

    public boolean debugVerboseLogs() { return debugVerboseLogs; }
    public void setDebugVerboseLogs(boolean v) { this.debugVerboseLogs = v; }

    public boolean debugPipelineProfiler() { return debugPipelineProfiler; }
    public void setDebugPipelineProfiler(boolean v) { this.debugPipelineProfiler = v; }
    
    // 生物群系样式配置字段方法
    public List<LampPostConfigEntry> lampPostOverrides() { return lampPostOverrides; }
    public void setLampPostOverrides(List<LampPostConfigEntry> v) { 
        this.lampPostOverrides = v == null ? new ArrayList<>() : new ArrayList<>(v); 
    }
    
    public List<RoadStyleConfigEntry> roadStyleOverrides() { return roadStyleOverrides; }
    public void setRoadStyleOverrides(List<RoadStyleConfigEntry> v) { 
        this.roadStyleOverrides = v == null ? new ArrayList<>() : new ArrayList<>(v); 
    }
    
    public List<RoadStyleConfigEntry> bopRoadStyleOverrides() { return bopRoadStyleOverrides; }
    public void setBopRoadStyleOverrides(List<RoadStyleConfigEntry> v) { 
        this.bopRoadStyleOverrides = v == null ? new ArrayList<>() : new ArrayList<>(v); 
    }
}
