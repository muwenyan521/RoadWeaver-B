package net.shiroha233.roadweaver.features.roadlogic;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.helpers.Records;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 优化的A*路径查找器，基于上游v1.6.0的性能改进
 * - 使用FastUtil进行高效内存管理
 * - 动态启发式缩放和步数限制
 * - 支持部分路径接受
 * - 地形分析和生物群系优化
 */
public class OptimizedPathFinder {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver/OptimizedPathFinder");
    
    // 配置参数
    private static final int GRID_STEP = 4;
    private static final double HEURISTIC_WEIGHT = 1.5;
    private static final double HEURISTIC_SCALE = 95.0;
    
    private static final int[][] OFFSETS = generateOffsets();
    
    private static final Map<TagKey<Biome>, Double> BIOME_COSTS = Map.of(
        BiomeTags.IS_RIVER, 240.0,
        BiomeTags.IS_OCEAN, 280.0,
        BiomeTags.IS_DEEP_OCEAN, 320.0,
        BiomeTags.IS_MOUNTAIN, 160.0,
        BiomeTags.IS_BEACH, 160.0
    );
    
    private final ServerLevel world;
    private final ChunkGenerator generator;
    private final RandomState noiseConfig;
    private final Climate.Sampler noiseSampler;
    private final BiomeSource biomeSource;
    private volatile List<HolderSet<Biome>> forbiddenBiomeLists;
    
    public OptimizedPathFinder(ServerLevel world) {
        this.world = world;
        this.generator = world.getChunkSource().getGenerator();
        this.noiseConfig = world.getChunkSource().randomState();
        this.noiseSampler = noiseConfig.sampler();
        this.biomeSource = generator.getBiomeSource();
    }
    
    public List<Records.RoadSegmentPlacement> findPath(BlockPos from, BlockPos to, int width) {
        BlockPos startPos = snap(from);
        BlockPos endPos = snap(to);
        
        List<BlockPos> path = aStarPositions(startPos, endPos);
        if (path.isEmpty()) {
            return Collections.emptyList();
        }
        
        return reconstructPath(path, width);
    }
    
    private List<BlockPos> aStarPositions(BlockPos startPos, BlockPos endPos) {
        long startKey = hash(startPos.getX(), startPos.getZ());
        long endKey = hash(endPos.getX(), endPos.getZ());
        
        ModConfig config = ConfigService.get();
        final double localScale = selectHeuristicScale(startPos, endPos);
        final int localStepCap = Math.min(selectMaxSteps(startPos, endPos), 200000);
        
        final int initialL1 = Math.abs(startPos.getX() - endPos.getX()) + Math.abs(startPos.getZ() - endPos.getZ());
        int bestMd = Integer.MAX_VALUE;
        long bestKey = startKey;
        
        PriorityQueue<PathNode> open = new PriorityQueue<>(Comparator.comparingDouble(r -> r.f));
        Long2DoubleMap gScore = new Long2DoubleOpenHashMap();
        gScore.defaultReturnValue(Double.MAX_VALUE);
        Long2LongMap parent = new Long2LongOpenHashMap();
        
        gScore.put(startKey, 0.0);
        open.add(new PathNode(startKey, 0.0, heuristic(startPos, endPos, localScale) * HEURISTIC_WEIGHT));
        
        int iterations = 0;
        while (!open.isEmpty() && iterations++ < localStepCap) {
            PathNode current = open.poll();
            if (current.g > gScore.get(current.key)) {
                continue;
            }
            
            int curX = (int) (current.key >> 32);
            int curZ = (int) current.key;
            int curY = sampleHeight(curX, curZ);
            
            int md = Math.abs(curX - endPos.getX()) + Math.abs(curZ - endPos.getZ());
            if (md < bestMd) {
                bestMd = md;
                bestKey = current.key;
            }
            
            if (current.key == endKey) {
                return reconstructVertices(current.key, startKey, parent);
            }
            
            for (int[] off : OFFSETS) {
                int nx = curX + off[0];
                int nz = curZ + off[1];
                long neighKey = hash(nx, nz);
                
                int ny = sampleHeight(nx, nz);
                if (isSteep(curY, ny)) {
                    continue;
                }
                
                double stab = sampleStability(nx, nz, ny);
                if (stab == Double.MAX_VALUE) {
                    continue;
                }
                
                Holder<Biome> bEntry = sampleBiome(nx, nz, ny);
                if (isForbiddenBiome(bEntry)) {
                    continue;
                }
                double bCost = biomeCost(bEntry);
                
                double preferWaterPenalty = (config.preferLandOverWater() && isWater(bEntry)) ? config.waterStepPenalty() : 0.0;
                double proxPenalty = forbiddenProximityPenalty(nx, nz, ny);
                double coastPenalty = coastProximityPenalty(nx, nz, ny);
                
                double inc = stepCost(off)
                    + elevationCost(curY, ny)
                    + bCost
                    + yLevelCost(ny)
                    + stab
                    + preferWaterPenalty
                    + proxPenalty
                    + coastPenalty;
                
                double tentativeG = gScore.get(current.key) + inc;
                
                if (tentativeG < gScore.get(neighKey)) {
                    parent.put(neighKey, current.key);
                    gScore.put(neighKey, tentativeG);
                    double f = tentativeG + heuristic(nx, nz, endPos, localScale) * HEURISTIC_WEIGHT;
                    open.add(new PathNode(neighKey, tentativeG, f));
                }
            }
        }
        
        // 部分路径接受
        boolean canAcceptPartial = initialL1 > 0 && bestMd != Integer.MAX_VALUE && config.acceptPartialPaths();
        double progress = canAcceptPartial ? (double) (initialL1 - bestMd) / (double) initialL1 : 0.0;
        if (canAcceptPartial && progress >= config.partialProgressThreshold()) {
            List<BlockPos> partial = reconstructVertices(bestKey, startKey, parent);
            if (!partial.isEmpty()) {
                LOGGER.info("Accept partial path (progress={}%, len={}, threshold={}%) {} -> {}",
                    String.format(Locale.ROOT, "%.1f", progress * 100.0),
                    partial.size(),
                    String.format(Locale.ROOT, "%.1f", config.partialProgressThreshold() * 100.0),
                    startPos.toShortString(), endPos.toShortString());
                return partial;
            }
        }
        
        LOGGER.info("Path not found between {} and {} after {} iterations",
            startPos, endPos, Math.min(iterations, localStepCap));
        return Collections.emptyList();
    }
    
    private int sampleHeight(int x, int z) {
        return generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, world, noiseConfig);
    }
    
    private double sampleStability(int x, int z, int y) {
        return RoadPathCalculator.calculateTerrainStability(new BlockPos(x, y, z), y, world);
    }
    
    private Holder<Biome> sampleBiome(int x, int z, int y) {
        return biomeSource.getNoiseBiome(
            QuartPos.fromBlock(x),
            316,
            QuartPos.fromBlock(z),
            noiseSampler
        );
    }
    
    private boolean isForbiddenBiome(Holder<Biome> biome) {
        if (forbiddenBiomeLists == null) {
            Registry<Biome> reg = world.registryAccess().registryOrThrow(Registries.BIOME);
            forbiddenBiomeLists = compileBiomeSelectors(reg, ConfigService.get().forbiddenBiomeSelectors());
        }
        return matchesBiome(biome, forbiddenBiomeLists);
    }
    
    private static boolean isWater(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(BiomeTags.IS_RIVER);
    }
    
    private static double stepCost(int[] off) {
        return (Math.abs(off[0]) == GRID_STEP && Math.abs(off[1]) == GRID_STEP) ? 1.5 : 1.0;
    }
    
    private static double elevationCost(int y1, int y2) {
        return Math.abs(y1 - y2) * 40.0;
    }
    
    private static double biomeCost(Holder<Biome> biome) {
        for (Map.Entry<TagKey<Biome>, Double> entry : BIOME_COSTS.entrySet()) {
            if (biome.is(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 0.0;
    }
    
    private double forbiddenProximityPenalty(int x, int z, int y) {
        ModConfig cfg = ConfigService.get();
        int buf = cfg.forbiddenBiomeBufferBlocks();
        if (buf <= 0) return 0.0;
        int radiusSteps = buf / GRID_STEP;
        if (radiusSteps <= 0) return 0.0;
        
        for (int i = -radiusSteps; i <= radiusSteps; i++) {
            for (int j = -radiusSteps; j <= radiusSteps; j++) {
                if (i == 0 && j == 0) continue;
                int checkX = x + i * GRID_STEP;
                int checkZ = z + j * GRID_STEP;
                if (isForbiddenBiome(sampleBiome(checkX, checkZ, y))) {
                    return cfg.forbiddenBiomeProximityPenalty();
                }
            }
        }
        return 0.0;
    }
    
    private double coastProximityPenalty(int x, int z, int y) {
        ModConfig cfg = ConfigService.get();
        if (!cfg.preferLandOverWater()) return 0.0;
        int buf = cfg.coastAvoidBufferBlocks();
        if (buf <= 0) return 0.0;
        int radiusSteps = buf / GRID_STEP;
        if (radiusSteps <= 0) return 0.0;
        
        for (int i = -radiusSteps; i <= radiusSteps; i++) {
            for (int j = -radiusSteps; j <= radiusSteps; j++) {
                if (i == 0 && j == 0) continue;
                int checkX = x + i * GRID_STEP;
                int checkZ = z + j * GRID_STEP;
                Holder<Biome> b = sampleBiome(checkX, checkZ, y);
                if (isWater(b)) {
                    return cfg.coastProximityPenalty();
                }
            }
        }
        return 0.0;
    }
    
    private static double yLevelCost(int y) {
        return y <= 63 ? 240.0 : 0.0;
    }
    
    private static boolean isSteep(int y1, int y2) {
        return Math.abs(y1 - y2) > 3;
    }
    
    private static double heuristic(int x, int z, BlockPos goal, double scale) {
        int dx = Math.abs(x - goal.getX());
        int dz = Math.abs(z - goal.getZ());
        double a = dx + dz - 0.5 * Math.min(dx, dz);
        return a * scale;
    }
    
    private static double heuristic(BlockPos a, BlockPos b, double scale) {
        return heuristic(a.getX(), a.getZ(), b, scale);
    }
    
    private static double selectHeuristicScale(BlockPos start, BlockPos goal) {
        int l1 = Math.abs(start.getX() - goal.getX()) + Math.abs(start.getZ() - goal.getZ());
        final double L0 = 200.0;
        final double L1 = 1200.0;
        double t = (l1 - L0) / (L1 - L0);
        if (t < 0.0) t = 0.0;
        else if (t > 1.0) t = 1.0;
        double s = t * t * (3.0 - 2.0 * t);
        double scale = 80.0 + s * (120.0 - 80.0);
        return Math.max(80.0, Math.min(120.0, scale));
    }
    
    private static int selectMaxSteps(BlockPos start, BlockPos goal) {
        int l1 = Math.abs(start.getX() - goal.getX()) + Math.abs(start.getZ() - goal.getZ());
        double k = 16.0;
        int min = 512;
        int max = 200000;
        long est = Math.round((k * l1) / GRID_STEP);
        if (est < min) return min;
        if (est > max) return max;
        return (int) est;
    }
    
    private static BlockPos snap(BlockPos p) {
        int x = Math.floorDiv(p.getX(), GRID_STEP) * GRID_STEP;
        int z = Math.floorDiv(p.getZ(), GRID_STEP) * GRID_STEP;
        return new BlockPos(x, p.getY(), z);
    }
    
    private static int[][] generateOffsets() {
        int d = GRID_STEP;
        return new int[][]{{d, 0}, {-d, 0}, {0, d}, {0, -d}, {d, d}, {d, -d}, {-d, d}, {-d, -d}};
    }
    
    private static long hash(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
    
    private List<BlockPos> reconstructVertices(long goal, long start, Long2LongMap parent) {
        List<BlockPos> vertices = new ArrayList<>();
        for (long k = goal; ; k = parent.get(k)) {
            int x = (int) (k >> 32);
            int z = (int) k;
            int y = sampleHeight(x, z);
            vertices.add(new BlockPos(x, y, z));
            if (k == start) {
                break;
            }
        }
        Collections.reverse(vertices);
        return vertices;
    }
    
    private List<Records.RoadSegmentPlacement> reconstructPath(List<BlockPos> path, int width) {
        Map<BlockPos, Set<BlockPos>> segments = new LinkedHashMap<>();
        Set<BlockPos> widthCache = new HashSet<>();
        
        for (int i = 0; i < path.size(); i++) {
            BlockPos p = path.get(i);
            RoadDirection dir = RoadDirection.X_AXIS;
            
            if (i > 0) {
                BlockPos prev = path.get(i - 1);
                int dx = p.getX() - prev.getX();
                int dz = p.getZ() - prev.getZ();
                if ((dx < 0 && dz > 0) || (dx > 0 && dz < 0)) dir = RoadDirection.DIAGONAL_1;
                else if ((dx < 0 && dz < 0) || (dx > 0 && dz > 0)) dir = RoadDirection.DIAGONAL_2;
                else if (dx == 0 && dz != 0) dir = RoadDirection.Z_AXIS;
            }
            
            Set<BlockPos> ws = RoadPathCalculator.generateWidth(p, width / 2, widthCache, dir);
            segments.put(p, ws);
        }
        
        List<Records.RoadSegmentPlacement> out = new ArrayList<>();
        for (Map.Entry<BlockPos, Set<BlockPos>> e : segments.entrySet()) {
            out.add(new Records.RoadSegmentPlacement(e.getKey(), new ArrayList<>(e.getValue())));
        }
        return out;
    }
    
    private static List<HolderSet<Biome>> compileBiomeSelectors(Registry<Biome> registry, List<String> selectors) {
        List<HolderSet<Biome>> result = new ArrayList<>();
        for (String selector : selectors) {
            if (selector.startsWith("#")) {
                TagKey<Biome> tag = TagKey.create(Registries.BIOME, new net.minecraft.resources.ResourceLocation(selector.substring(1)));
                registry.getTag(tag).ifPresent(result::add);
            } else {
                registry.getHolder(new net.minecraft.resources.ResourceLocation(selector)).ifPresent(holder -> 
                    result.add(HolderSet.direct(holder)));
            }
        }
        return result;
    }
    
    private static boolean matchesBiome(Holder<Biome> biome, List<HolderSet<Biome>> selectors) {
        for (HolderSet<Biome> selector : selectors) {
            if (selector.contains(biome)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 路径节点内部类，用于A*算法
     */
    private static class PathNode {
        final long key;
        final double g;
        final double f;
        
        PathNode(long key, double g, double f) {
            this.key = key;
            this.g = g;
            this.f = f;
        }
    }
}
