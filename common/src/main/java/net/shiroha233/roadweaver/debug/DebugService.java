package net.shiroha233.roadweaver.debug;

import net.shiroha233.roadweaver.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public final class DebugService {
    private final ModConfig config;
    private final ConcurrentMap<String, PipelineProfiler> pipelineProfilers;
    private final List<String> debugMessages;
    private final AtomicLong messageCounter;

    public DebugService(ModConfig config) {
        this.config = config;
        this.pipelineProfilers = new ConcurrentHashMap<>();
        this.debugMessages = new ArrayList<>();
        this.messageCounter = new AtomicLong(0);
    }

    public void logDebug(String message) {
        if (config.debugVerboseLogs()) {
            String timestampedMessage = String.format("[%d] %s", messageCounter.incrementAndGet(), message);
            debugMessages.add(timestampedMessage);
            
            // 限制消息数量，防止内存泄漏
            if (debugMessages.size() > 1000) {
                debugMessages.subList(0, 500).clear();
            }
        }
    }

    public void logPipelineStart(String pipelineName) {
        if (config.debugPipelineProfiler()) {
            PipelineProfiler profiler = pipelineProfilers.computeIfAbsent(pipelineName, 
                k -> new PipelineProfiler(pipelineName));
            profiler.start();
            logDebug(String.format("Pipeline '%s' started", pipelineName));
        }
    }

    public void logPipelineEnd(String pipelineName) {
        if (config.debugPipelineProfiler()) {
            PipelineProfiler profiler = pipelineProfilers.get(pipelineName);
            if (profiler != null) {
                profiler.end();
                logDebug(String.format("Pipeline '%s' completed in %d ms", 
                    pipelineName, profiler.getLastDuration()));
            }
        }
    }

    public void logPipelineStep(String pipelineName, String stepName) {
        if (config.debugPipelineProfiler()) {
            PipelineProfiler profiler = pipelineProfilers.get(pipelineName);
            if (profiler != null) {
                profiler.logStep(stepName);
                logDebug(String.format("Pipeline '%s' step: %s", pipelineName, stepName));
            }
        }
    }

    public void logPathfindingStart(String pathId, int startX, int startZ, int endX, int endZ) {
        if (config.debugVerboseLogs()) {
            logDebug(String.format("Pathfinding started: %s from (%d,%d) to (%d,%d)", 
                pathId, startX, startZ, endX, endZ));
        }
    }

    public void logPathfindingProgress(String pathId, int nodesExplored, double progress) {
        if (config.debugVerboseLogs()) {
            logDebug(String.format("Pathfinding progress: %s - %d nodes, %.1f%% complete", 
                pathId, nodesExplored, progress * 100));
        }
    }

    public void logPathfindingComplete(String pathId, boolean success, int totalNodes, long durationMs) {
        if (config.debugVerboseLogs()) {
            String status = success ? "successful" : "failed";
            logDebug(String.format("Pathfinding %s: %s - %d nodes in %d ms", 
                status, pathId, totalNodes, durationMs));
        }
    }

    public void logStructureScan(String structureType, int chunkX, int chunkZ, boolean found) {
        if (config.debugVerboseLogs()) {
            String result = found ? "found" : "not found";
            logDebug(String.format("Structure scan: %s at (%d,%d) - %s", 
                structureType, chunkX, chunkZ, result));
        }
    }

    public void logBiomeStyleApplied(String biomeName, String styleName) {
        if (config.debugVerboseLogs()) {
            logDebug(String.format("Biome style applied: %s -> %s", biomeName, styleName));
        }
    }

    public void logRoadGeneration(String roadId, int length, int width) {
        if (config.debugVerboseLogs()) {
            logDebug(String.format("Road generation: %s - length: %d, width: %d", 
                roadId, length, width));
        }
    }

    public List<String> getDebugMessages() {
        return new ArrayList<>(debugMessages);
    }

    public void clearDebugMessages() {
        debugMessages.clear();
        messageCounter.set(0);
    }

    public List<PipelineProfile> getPipelineProfiles() {
        List<PipelineProfile> profiles = new ArrayList<>();
        for (PipelineProfiler profiler : pipelineProfilers.values()) {
            profiles.add(profiler.getProfile());
        }
        return profiles;
    }

    public void clearPipelineProfiles() {
        pipelineProfilers.clear();
    }

    public static final class PipelineProfiler {
        private final String pipelineName;
        private long startTime;
        private long endTime;
        private final List<String> steps;
        private final List<Long> stepTimes;

        public PipelineProfiler(String pipelineName) {
            this.pipelineName = pipelineName;
            this.steps = new ArrayList<>();
            this.stepTimes = new ArrayList<>();
        }

        public void start() {
            this.startTime = System.currentTimeMillis();
            this.steps.clear();
            this.stepTimes.clear();
        }

        public void end() {
            this.endTime = System.currentTimeMillis();
        }

        public void logStep(String stepName) {
            steps.add(stepName);
            stepTimes.add(System.currentTimeMillis());
        }

        public long getLastDuration() {
            return endTime - startTime;
        }

        public PipelineProfile getProfile() {
            return new PipelineProfile(pipelineName, startTime, endTime, 
                new ArrayList<>(steps), new ArrayList<>(stepTimes));
        }
    }

    public static final class PipelineProfile {
        private final String pipelineName;
        private final long startTime;
        private final long endTime;
        private final List<String> steps;
        private final List<Long> stepTimes;

        public PipelineProfile(String pipelineName, long startTime, long endTime, 
                              List<String> steps, List<Long> stepTimes) {
            this.pipelineName = pipelineName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.steps = steps;
            this.stepTimes = stepTimes;
        }

        public String getPipelineName() {
            return pipelineName;
        }

        public long getDuration() {
            return endTime - startTime;
        }

        public List<String> getSteps() {
            return steps;
        }

        public List<Long> getStepTimes() {
            return stepTimes;
        }

        public String getFormattedProfile() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Pipeline: %s\n", pipelineName));
            sb.append(String.format("Total duration: %d ms\n", getDuration()));
            sb.append("Steps:\n");
            
            for (int i = 0; i < steps.size(); i++) {
                long stepStart = (i == 0) ? startTime : stepTimes.get(i - 1);
                long stepEnd = stepTimes.get(i);
                long stepDuration = stepEnd - stepStart;
                sb.append(String.format("  %s: %d ms\n", steps.get(i), stepDuration));
            }
            
            return sb.toString();
        }
    }
}
