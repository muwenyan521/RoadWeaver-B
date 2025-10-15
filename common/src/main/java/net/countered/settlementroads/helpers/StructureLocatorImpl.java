package net.countered.settlementroads.helpers;

import com.mojang.datafixers.util.Pair;
import net.countered.settlementroads.config.ConfigProvider;
import net.countered.settlementroads.config.IModConfig;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用结构定位实现（Common）。
 * 平台桥接类直接委托到此处，避免重复逻辑。
 */
public final class StructureLocatorImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    
    // 轮询索引：记录当前搜索到哪个结构类型
    private static int currentStructureIndex = 0;
    // 缓存解析后的结构列表，避免重复解析
    private static List<Holder<Structure>> cachedStructureList = null;
    
    // 批量累积缓冲区：按世界存储待加入的结构
    private static final ConcurrentHashMap<String, List<Records.StructureInfo>> pendingStructures = new ConcurrentHashMap<>();
    // 记录每个世界缓冲区的最后更新时间
    private static final ConcurrentHashMap<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    // 自动刷新超时时间（毫秒）：30秒
    private static final long AUTO_FLUSH_TIMEOUT = 30000;

    private StructureLocatorImpl() {}

    /**
     * 异步定位结构（新方法）
     * 提交异步搜索任务，不阻塞主线程
     */
    public static void locateConfiguredStructure(ServerLevel level, int locateCount, boolean locateAtPlayer) {
        if (locateCount <= 0) {
            return;
        }

        IModConfig config = ConfigProvider.get();
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();

        Records.StructureLocationData locationData = dataProvider.getStructureLocations(level);
        if (locationData == null) {
            locationData = new Records.StructureLocationData(new ArrayList<>());
        }

        List<BlockPos> knownLocations = new ArrayList<>(locationData.structureLocations());

        // 获取并缓存结构列表
        if (cachedStructureList == null || cachedStructureList.isEmpty()) {
            Optional<HolderSet<Structure>> targetStructures = resolveStructureTargets(level, config.structuresToLocate());
            if (targetStructures.isEmpty()) {
                LOGGER.warn("RoadWeaver: 无法解析结构目标列表，跳过定位。");
                return;
            }
            cachedStructureList = new ArrayList<>(targetStructures.get().stream().toList());
            currentStructureIndex = 0;
            LOGGER.info("RoadWeaver: 已解析 {} 种结构类型用于轮询搜索", cachedStructureList.size());
        }
        
        if (cachedStructureList.isEmpty()) {
            LOGGER.warn("RoadWeaver: 结构列表为空，跳过定位。");
            return;
        }

        List<BlockPos> centers = collectSearchCenters(level, locateAtPlayer);
        int radius = Math.max(config.structureSearchRadius(), 1);
        
        int structureTypeCount = cachedStructureList.size();
        int totalSubmitted = 0;
        
        LOGGER.info("RoadWeaver: 开始轮询搜索 {} 个任务，共 {} 种结构类型", locateCount, structureTypeCount);

        // 严格轮询：每次只搜索一种结构的一个位置
        for (int i = 0; i < locateCount; i++) {
            // 轮询选择结构类型
            Holder<Structure> currentStructure = cachedStructureList.get(currentStructureIndex);
            String structureName = currentStructure.unwrapKey()
                .map(key -> key.location().toString())
                .orElse("unknown");
            
            // 移动到下一个结构类型
            currentStructureIndex = (currentStructureIndex + 1) % cachedStructureList.size();
            
            // 选择搜索中心（轮流使用）
            BlockPos center = centers.get(i % centers.size());
            
            // 生成唯一任务ID
            String taskId = generateTaskId(level, center) + "_" + i + "_" + structureName.hashCode();
            
            // 创建只包含当前结构的 HolderSet
            HolderSet<Structure> singleStructureSet = HolderSet.direct(currentStructure);
            
            // 提交异步搜索（会被线程池并行执行）
            AsyncStructureLocator.locateStructureAsync(
                level,
                singleStructureSet,
                center,
                radius,
                taskId
            );
            
            totalSubmitted++;
            
            if ((i + 1) % structureTypeCount == 0) {
                LOGGER.debug("RoadWeaver: 已完成一轮轮询（{} 种结构）", structureTypeCount);
            }
        }
        
        LOGGER.info("RoadWeaver: 总共提交 {} 个异步结构搜索任务（并行执行）", totalSubmitted);
    }
    
    /**
     * 处理异步搜索结果
     * 在主线程的 tick 事件中调用，检查并处理完成的搜索任务
     */
    public static void processAsyncResults(ServerLevel level) {
        IModConfig config = ConfigProvider.get();
        int batchSize = config.structureBatchSize();
        
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        Records.StructureLocationData locationData = dataProvider.getStructureLocations(level);
        if (locationData == null) {
            locationData = new Records.StructureLocationData(new ArrayList<>());
        }

        List<BlockPos> knownLocations = new ArrayList<>(locationData.structureLocations());
        List<Records.StructureInfo> structureInfos = new ArrayList<>(locationData.structureInfos());
        
        // 获取当前世界的缓冲区
        String worldKey = level.dimension().location().toString();
        List<Records.StructureInfo> pending = pendingStructures.computeIfAbsent(worldKey, k -> new ArrayList<>());

        // 检查所有完成的任务，加入缓冲区
        for (String taskId : new ArrayList<>(AsyncStructureLocator.LOCATE_RESULTS.keySet())) {
            AsyncStructureLocator.StructureLocateResult result = 
                AsyncStructureLocator.getAndRemoveResult(taskId);
            
            if (result != null && result.completed()) {
                if (result.position() != null) {
                    // 找到了结构，先加入缓冲区
                    if (!containsBlockPos(knownLocations, result.position()) && 
                        !containsStructureInfo(pending, result.position())) {
                        
                        Records.StructureInfo newStructure = new Records.StructureInfo(
                            result.position(), 
                            result.structureId()
                        );
                        pending.add(newStructure);
                        // 更新最后修改时间
                        lastUpdateTime.put(worldKey, System.currentTimeMillis());
                        
                        LOGGER.info("RoadWeaver: 异步搜索发现新结构 {} 于 {} (缓冲区: {}/{})", 
                            result.structureId(), result.position(), pending.size(), batchSize);
                    }
                }
            }
        }

        // 检查是否需要刷新缓冲区
        boolean shouldFlush = false;
        String flushReason = "";
        
        if (pending.size() >= batchSize) {
            // 缓冲区已满
            shouldFlush = true;
            flushReason = "缓冲区已满";
        } else if (!pending.isEmpty()) {
            // 缓冲区有结构但未满，检查是否超时
            long lastUpdate = lastUpdateTime.getOrDefault(worldKey, 0L);
            long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdate;
            if (timeSinceLastUpdate > AUTO_FLUSH_TIMEOUT) {
                shouldFlush = true;
                flushReason = String.format("超时自动刷新 (%d秒)", timeSinceLastUpdate / 1000);
            }
        }
        
        // 当缓冲区达到批量大小或超时时，统一加入道路规划
        if (shouldFlush) {
            LOGGER.info("RoadWeaver: {} ({} 个结构)，开始批量加入道路规划", flushReason, pending.size());
            
            // 将缓冲区中的结构加入已知列表
            for (Records.StructureInfo structure : pending) {
                knownLocations.add(structure.pos());
                structureInfos.add(structure);
            }
            
            // 保存到数据库
            dataProvider.setStructureLocations(level, 
                new Records.StructureLocationData(knownLocations, structureInfos));
            
            LOGGER.info("RoadWeaver: 已批量加入 {} 个结构，总计 {} 个结构位置", 
                pending.size(), knownLocations.size());
            
            // 清空缓冲区和时间戳
            pending.clear();
            lastUpdateTime.remove(worldKey);
            
            // 触发道路规划
            if (knownLocations.size() >= 2) {
                LOGGER.info("RoadWeaver: 触发道路规划检查");
                StructureConnector.createNewStructureConnection(level);
            }
        }
    }
    
    /**
     * 检查结构信息列表中是否包含指定位置
     */
    private static boolean containsStructureInfo(List<Records.StructureInfo> list, BlockPos pos) {
        for (Records.StructureInfo info : list) {
            if (info.pos().equals(pos)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 生成任务ID
     */
    private static String generateTaskId(ServerLevel level, BlockPos center) {
        return level.dimension().location().toString() + "_" + 
               center.getX() + "_" + center.getZ() + "_" + 
               System.currentTimeMillis();
    }

    private static Optional<HolderSet<Structure>> resolveStructureTargets(ServerLevel level, String identifiers) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> holders = new ArrayList<>();

        if (identifiers == null || identifiers.isBlank()) {
            return Optional.empty();
        }

        String[] tokens = identifiers.split("[;,\\s]+");
        for (String raw : tokens) {
            if (raw == null) continue;
            String token = raw.trim()
                    .replace("\r", "")
                    .replace("\n", "");
            // 去除首尾引号/反引号，并去掉末尾标点（逗号/分号/中文标点）
            token = token.replaceAll("^[\\\"'`]+|[\\\"'`]+$", "");
            token = token.replaceAll("[,;，；]+$", "");
            // 规范化：去除 BOM、替换全角符号
            if (!token.isEmpty() && token.charAt(0) == '\uFEFF') token = token.substring(1);
            token = token
                    .replace('＃', '#')
                    .replace('“', ' ')
                    .replace('”', ' ')
                    .replace('「', ' ')
                    .replace('」', ' ')
                    .replace('『', ' ')
                    .replace('』', ' ')
                    .replace('《', ' ')
                    .replace('》', ' ')
                    .trim();
            if (token.isBlank()) continue;

            // 若存在 #，不论位置，将其视为标签起始
            int hashIdx = token.indexOf('#');
            if (hashIdx >= 0) {
                String tagToken = token.substring(hashIdx + 1).trim();
                try {
                    ResourceLocation tagId = new ResourceLocation(tagToken);
                    TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, tagId);
                    registry.getTag(tag).ifPresentOrElse(named -> {
                        for (Holder<Structure> h : named) {
                            holders.add(h);
                        }
                    }, () -> LOGGER.warn("RoadWeaver: structure tag not found: #{}", tagToken));
                } catch (Exception ex) {
                    LOGGER.warn("RoadWeaver: invalid structure tag token skipped: #{} (raw='{}')", tagToken, raw);
                }
            } else {
                try {
                    // 去掉前置非法字符（如意外的引号/符号），保留斜杠
                    String cleaned = token.replaceAll("^[^a-z0-9_.:/\\-]+", "");
                    
                    // 支持通配符匹配（例如：modid:structure_*）
                    if (cleaned.contains("*")) {
                        String pattern = cleaned.replace("*", "");
                        int matchCount = 0;
                        for (var entry : registry.entrySet()) {
                            String structureId = entry.getKey().location().toString();
                            if (structureId.startsWith(pattern)) {
                                registry.getHolder(entry.getKey()).ifPresent(holders::add);
                                matchCount++;
                            }
                        }
                        if (matchCount > 0) {
                            LOGGER.info("RoadWeaver: 通配符 '{}' 匹配到 {} 个结构", cleaned, matchCount);
                        } else {
                            LOGGER.warn("RoadWeaver: 通配符 '{}' 未匹配到任何结构", cleaned);
                        }
                    } else {
                        // 精确匹配
                        ResourceLocation id = new ResourceLocation(cleaned);
                        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, id);
                        registry.getHolder(key).ifPresentOrElse(holders::add,
                                () -> LOGGER.warn("RoadWeaver: structure id not found: {}", cleaned));
                    }
                } catch (Exception ex) {
                    LOGGER.warn("RoadWeaver: invalid structure id token skipped: {} (raw='{}')", token, raw);
                }
            }
        }

        if (holders.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(HolderSet.direct(holders));
    }

    private static Optional<HolderSet<Structure>> resolveStructureTargets(ServerLevel level, java.util.List<String> identifiersList) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> holders = new ArrayList<>();

        if (identifiersList == null || identifiersList.isEmpty()) {
            return Optional.empty();
        }

        for (String line : identifiersList) {
            if (line == null) continue;
            String norm = line.replace('\r', ' ').replace('\n', ' ').trim();
            if (norm.isEmpty()) continue;
            // 允许行内继续使用逗号/分号/空白再分割
            String[] tokens = norm.split("[;,\\s]+");
            for (String raw : tokens) {
                if (raw == null) continue;
                String token = raw.trim();
                if (token.isEmpty()) continue;
                // 重用单字符串解析的清洗逻辑
                token = token
                        .replace("\r", "")
                        .replace("\n", "");
                token = token.replaceAll("^[\\\"'`]+|[\\\"'`]+$", "");
                token = token.replaceAll("[,;，；]+$", "");
                if (!token.isEmpty() && token.charAt(0) == '\uFEFF') token = token.substring(1);
                token = token
                        .replace('＃', '#')
                        .replace('“', ' ')
                        .replace('”', ' ')
                        .replace('「', ' ')
                        .replace('」', ' ')
                        .replace('『', ' ')
                        .replace('』', ' ')
                        .replace('《', ' ')
                        .replace('》', ' ')
                        .trim();
                if (token.isEmpty()) continue;

                int hashIdx = token.indexOf('#');
                if (hashIdx >= 0) {
                    String tagToken = token.substring(hashIdx + 1).trim();
                    try {
                        ResourceLocation tagId = new ResourceLocation(tagToken);
                        TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, tagId);
                        registry.getTag(tag).ifPresentOrElse(named -> {
                            for (Holder<Structure> h : named) holders.add(h);
                        }, () -> LOGGER.warn("RoadWeaver: structure tag not found: #{}", tagToken));
                    } catch (Exception ex) {
                        LOGGER.warn("RoadWeaver: invalid structure tag token skipped: #{} (line='{}')", tagToken, line);
                    }
                } else {
                    try {
                        String cleaned = token.replaceAll("^[^a-z0-9_.:/\\-]+", "");
                        
                        // 支持通配符匹配（例如：modid:structure_*）
                        if (cleaned.contains("*")) {
                            String pattern = cleaned.replace("*", "");
                            int matchCount = 0;
                            for (var entry : registry.entrySet()) {
                                String structureId = entry.getKey().location().toString();
                                if (structureId.startsWith(pattern)) {
                                    registry.getHolder(entry.getKey()).ifPresent(holders::add);
                                    matchCount++;
                                }
                            }
                            if (matchCount > 0) {
                                LOGGER.info("RoadWeaver: 通配符 '{}' 匹配到 {} 个结构", cleaned, matchCount);
                            } else {
                                LOGGER.warn("RoadWeaver: 通配符 '{}' 未匹配到任何结构", cleaned);
                            }
                        } else {
                            // 精确匹配
                            ResourceLocation id = new ResourceLocation(cleaned);
                            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, id);
                            registry.getHolder(key).ifPresentOrElse(holders::add,
                                    () -> LOGGER.warn("RoadWeaver: structure id not found: {}", cleaned));
                        }
                    } catch (Exception ex) {
                        LOGGER.warn("RoadWeaver: invalid structure id token skipped: {} (line='{}')", token, line);
                    }
                }
            }
        }

        if (holders.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(HolderSet.direct(holders));
    }

    private static List<BlockPos> collectSearchCenters(ServerLevel level, boolean locateAtPlayer) {
        List<BlockPos> centers = new ArrayList<>();
        if (locateAtPlayer) {
            for (ServerPlayer player : level.players()) {
                centers.add(player.blockPosition());
            }
        }

        BlockPos spawn = level.getSharedSpawnPos();
        if (centers.isEmpty()) {
            centers.add(spawn);
            // 扩展搜索：以出生点为中心，按配置半径的倍数在八个方向取样
            int r = Math.max(ConfigProvider.get().structureSearchRadius(), 1);
            int[] muls = new int[] {3, 6};
            for (int m : muls) {
                int d = r * m;
                centers.add(spawn.offset( d, 0,  0));
                centers.add(spawn.offset(-d, 0,  0));
                centers.add(spawn.offset( 0, 0,  d));
                centers.add(spawn.offset( 0, 0, -d));
                centers.add(spawn.offset( d, 0,  d));
                centers.add(spawn.offset(-d, 0,  d));
                centers.add(spawn.offset( d, 0, -d));
                centers.add(spawn.offset(-d, 0, -d));
            }
        }
        return centers;
    }

    private static boolean containsBlockPos(List<BlockPos> list, BlockPos pos) {
        for (BlockPos existing : list) {
            if (existing.equals(pos)) {
                return true;
            }
        }
        return false;
    }
}
