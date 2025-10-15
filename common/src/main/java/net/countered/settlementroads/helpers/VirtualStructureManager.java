package net.countered.settlementroads.helpers;

import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 虚拟结构管理器
 * <p>
 * 用于在真实结构生成时，自动在同一位置或偏移位置注册"虚拟结构"，
 * 让道路系统可以连接到这些虚拟位置。
 * </p>
 * 
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>村庄生成时，在村庄中心创建虚拟"集市"位置</li>
 *   <li>前哨站生成时，创建虚拟"哨塔观察点"</li>
 *   <li>自定义结构生成时，创建对应的功能性虚拟节点</li>
 * </ul>
 * 
 * <h3>工作原理：</h3>
 * 预先配置"触发结构→虚拟位置"的映射关系，当检测到触发结构生成时，
 * 自动计算虚拟位置并注册到道路系统的结构列表中。
 * 
 * @author RoadWeaver Team
 * @since 1.0.6
 */
public class VirtualStructureManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    
    /**
     * 虚拟结构配置：定义哪些真实结构应该生成对应的虚拟结构
     * <p>
     * 键：触发结构的ResourceLocation（例如 "minecraft:village_plains"）
     * 值：虚拟结构配置列表（可以为一个真实结构创建多个虚拟位置）
     * </p>
     */
    private static final Map<ResourceLocation, List<VirtualStructureConfig>> VIRTUAL_STRUCTURE_MAPPINGS = new HashMap<>();
    
    /**
     * 已生成的虚拟结构记录，避免重复生成
     * <p>
     * 格式：世界维度 → (真实结构位置 → 已生成的虚拟位置列表)
     * </p>
     */
    private static final Map<String, Map<BlockPos, List<BlockPos>>> generatedVirtualStructures = new HashMap<>();
    
    static {
        // 示例配置：村庄生成时，在村庄中心创建虚拟"村庄集市"位置
        // 偏移量 (0, 0, 0) 表示与村庄完全同位置
        registerVirtualStructure(
            new ResourceLocation("minecraft", "village_plains"),
            new VirtualStructureConfig("村庄集市", new BlockPos(0, 0, 0))
        );
        
        registerVirtualStructure(
            new ResourceLocation("minecraft", "village_desert"),
            new VirtualStructureConfig("沙漠村庄集市", new BlockPos(0, 0, 0))
        );
        
        registerVirtualStructure(
            new ResourceLocation("minecraft", "village_savanna"),
            new VirtualStructureConfig("热带草原村庄集市", new BlockPos(0, 0, 0))
        );
        
        registerVirtualStructure(
            new ResourceLocation("minecraft", "village_snowy"),
            new VirtualStructureConfig("雪地村庄集市", new BlockPos(0, 0, 0))
        );
        
        registerVirtualStructure(
            new ResourceLocation("minecraft", "village_taiga"),
            new VirtualStructureConfig("针叶林村庄集市", new BlockPos(0, 0, 0))
        );
        
        // 示例：前哨站生成时，在东侧50格创建虚拟"巡逻哨点"
        registerVirtualStructure(
            new ResourceLocation("minecraft", "pillager_outpost"),
            new VirtualStructureConfig("掠夺者巡逻哨点", new BlockPos(50, 0, 0))
        );
        
        // 你可以在这里添加更多配置...
    }
    
    /**
     * 注册虚拟结构映射
     * 
     * @param triggerStructure 触发结构的ResourceLocation
     * @param config 虚拟结构配置
     */
    public static void registerVirtualStructure(ResourceLocation triggerStructure, VirtualStructureConfig config) {
        VIRTUAL_STRUCTURE_MAPPINGS
            .computeIfAbsent(triggerStructure, k -> new ArrayList<>())
            .add(config);
        
        LOGGER.debug("已注册虚拟结构映射: {} -> {} (偏移: {})", 
            triggerStructure, config.displayName, config.offset);
    }
    
    /**
     * 当检测到结构生成时调用此方法（世界生成阶段同步调用）
     * <p>
     * 此方法在结构生成时立即调用（通过Mixin），确保虚拟结构与真实结构同步创建。
     * 检查该结构是否需要生成虚拟位置，如果需要则自动创建并注册。
     * </p>
     * 
     * @param level 服务器世界
     * @param structureId 生成的结构ID
     * @param structurePos 结构的中心位置
     */
    public static void onStructureGeneratedSync(ServerLevel level, ResourceLocation structureId, BlockPos structurePos) {
        handleStructureGeneration(level, structureId, structurePos, true);
    }
    
    /**
     * 当通过异步搜索发现结构时调用此方法（后期发现）
     * <p>
     * 此方法用于处理已存在但之前未被发现的结构。
     * </p>
     * 
     * @param level 服务器世界
     * @param structureId 生成的结构ID
     * @param structurePos 结构的中心位置
     */
    public static void onStructureGenerated(ServerLevel level, ResourceLocation structureId, BlockPos structurePos) {
        handleStructureGeneration(level, structureId, structurePos, false);
    }
    
    /**
     * 处理结构生成的核心逻辑
     * 
     * @param level 服务器世界
     * @param structureId 结构ID
     * @param structurePos 结构位置
     * @param isSync 是否为同步生成（世界生成阶段）
     */
    private static void handleStructureGeneration(ServerLevel level, ResourceLocation structureId, BlockPos structurePos, boolean isSync) {
        // 检查是否有对应的虚拟结构配置
        List<VirtualStructureConfig> configs = VIRTUAL_STRUCTURE_MAPPINGS.get(structureId);
        if (configs == null || configs.isEmpty()) {
            return; // 该结构不需要生成虚拟位置
        }
        
        String worldKey = level.dimension().location().toString();
        Map<BlockPos, List<BlockPos>> worldVirtualStructures = 
            generatedVirtualStructures.computeIfAbsent(worldKey, k -> new HashMap<>());
        
        // 检查是否已经为这个位置生成过虚拟结构
        if (worldVirtualStructures.containsKey(structurePos)) {
            LOGGER.debug("虚拟结构已存在，跳过: {} at {}", structureId, structurePos);
            return;
        }
        
        List<BlockPos> generatedPositions = new ArrayList<>();
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        
        // 为每个配置创建虚拟位置
        for (VirtualStructureConfig config : configs) {
            // 计算虚拟位置（真实位置 + 偏移量）
            BlockPos virtualPos = structurePos.offset(config.offset);
            
            // 注册到道路系统的结构列表
            dataProvider.addStructureLocation(level, virtualPos);
            generatedPositions.add(virtualPos);
            
            String generationType = isSync ? "同步生成" : "异步发现";
            LOGGER.info("✨ 已创建虚拟结构[{}]: {} '{}' at {} (来自真实结构 {} at {})", 
                generationType, structureId, config.displayName, virtualPos, structureId, structurePos);
        }
        
        // 记录已生成的虚拟结构
        worldVirtualStructures.put(structurePos, generatedPositions);
    }
    
    /**
     * 清理指定世界的虚拟结构记录
     * 
     * @param worldKey 世界维度键
     */
    public static void clearVirtualStructures(String worldKey) {
        generatedVirtualStructures.remove(worldKey);
        LOGGER.debug("已清理世界 {} 的虚拟结构记录", worldKey);
    }
    
    /**
     * 虚拟结构配置类
     */
    public static class VirtualStructureConfig {
        /** 虚拟结构的显示名称（用于日志） */
        public final String displayName;
        
        /** 相对于真实结构的偏移量 */
        public final BlockPos offset;
        
        /**
         * 创建虚拟结构配置
         * 
         * @param displayName 显示名称
         * @param offset 偏移量（相对于真实结构位置）
         */
        public VirtualStructureConfig(String displayName, BlockPos offset) {
            this.displayName = displayName;
            this.offset = offset;
        }
    }
}
