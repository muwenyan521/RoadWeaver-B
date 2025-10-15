package net.countered.settlementroads.mixin;

import net.countered.settlementroads.helpers.VirtualStructureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 监听结构生成的Mixin
 * <p>
 * 在结构生成完成后立即触发虚拟结构创建，
 * 确保虚拟结构和真实结构在世界生成阶段同步创建。
 * </p>
 * 
 * @author RoadWeaver Team
 * @since 1.0.6
 */
@Mixin(StructureStart.class)
public abstract class StructurePlacementMixin {
    
    /**
     * 在结构放置完成后注入
     * <p>
     * 监听{@link StructureStart#placeInChunk}方法，
     * 在结构生成完成后立即通知虚拟结构管理器。
     * </p>
     */
    @Inject(
        method = "placeInChunk",
        at = @At("RETURN")
    )
    private void onStructurePlaced(
        WorldGenLevel level,
        StructureManager structureManager,
        ChunkGenerator chunkGenerator,
        RandomSource random,
        BoundingBox boundingBox,
        ChunkPos chunkPos,
        CallbackInfo ci
    ) {
        // 只在服务器端处理
        if (!(level.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // 获取当前结构的信息
        StructureStart structureStart = (StructureStart) (Object) this;
        
        // 检查结构是否有效
        if (!structureStart.isValid()) {
            return;
        }
        
        // 获取结构类型
        Structure structure = structureStart.getStructure();
        
        // 尝试获取结构ID
        ResourceLocation structureId = serverLevel.registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
            .getKey(structure);
        
        if (structureId == null) {
            return;
        }
        
        // 获取结构中心位置（使用边界框的中心）
        BoundingBox box = structureStart.getBoundingBox();
        BlockPos centerPos = new BlockPos(
            (box.minX() + box.maxX()) / 2,
            box.minY(),
            (box.minZ() + box.maxZ()) / 2
        );
        
        // 通知虚拟结构管理器：结构已生成
        VirtualStructureManager.onStructureGeneratedSync(
            serverLevel,
            structureId,
            centerPos
        );
    }
}
