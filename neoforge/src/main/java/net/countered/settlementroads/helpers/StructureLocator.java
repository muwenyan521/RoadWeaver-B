package net.countered.settlementroads.helpers;

import com.mojang.datafixers.util.Pair;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.persistence.WorldDataHelper;
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

import java.util.Optional;

public class StructureLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    public static void locateConfiguredStructure(ServerLevel serverWorld, int locateCount, boolean locateAtPlayer) {
        LOGGER.debug("Locating " + locateCount + " " + ModConfig.structureToLocate());
        
        for (int x = 0; x < locateCount; x++) {
            if (locateAtPlayer) {
                for (ServerPlayer player : serverWorld.players()) {
                    executeLocateStructure(player.blockPosition(), serverWorld);
                }
            } else {
                executeLocateStructure(serverWorld.getSharedSpawnPos(), serverWorld);
            }
        }
    }

    private static void executeLocateStructure(BlockPos locatePos, ServerLevel serverWorld) {
        String structureId = ModConfig.structureToLocate();
        Registry<Structure> registry = serverWorld.registryAccess().registryOrThrow(Registries.STRUCTURE);
        
        HolderSet<Structure> holderSet = null;
        
        // 检查是否为标签（以 # 开头）
        if (structureId.startsWith("#")) {
            String tagId = structureId.substring(1);
            ResourceLocation tagLocation = ResourceLocation.parse(tagId);
            TagKey<Structure> tagKey = TagKey.create(Registries.STRUCTURE, tagLocation);
            Optional<HolderSet.Named<Structure>> tagOptional = registry.getTag(tagKey);
            
            if (tagOptional.isPresent()) {
                holderSet = tagOptional.get();
                LOGGER.debug("Using structure tag: " + tagId);
            } else {
                LOGGER.warn("Structure tag not found: " + tagId);
                return;
            }
        } else {
            // 作为单个结构 ID 处理
            ResourceLocation structureLocation = ResourceLocation.parse(structureId);
            ResourceKey<Structure> structureKey = ResourceKey.create(Registries.STRUCTURE, structureLocation);
            Optional<Holder.Reference<Structure>> holderOptional = registry.getHolder(structureKey);
            
            if (holderOptional.isPresent()) {
                holderSet = HolderSet.direct(holderOptional.get());
                LOGGER.debug("Using single structure: " + structureId);
            } else {
                LOGGER.warn("Structure not found: " + structureId);
                return;
            }
        }
        
        if (holderSet == null) {
            LOGGER.warn("Failed to resolve structure: " + structureId);
            return;
        }
        
        Pair<BlockPos, Holder<Structure>> pair = serverWorld.getChunkSource()
                .getGenerator()
                .findNearestMapStructure(serverWorld, holderSet, locatePos, ModConfig.structureSearchRadius(), true);
        
        if (pair == null) {
            LOGGER.debug("Structure not found near " + locatePos);
        } else {
            BlockPos structureLocation = pair.getFirst();
            LOGGER.debug("Structure found at " + structureLocation);
            Records.StructureLocationData data = WorldDataHelper.getStructureLocations(serverWorld);
            data.addStructure(structureLocation);
            WorldDataHelper.setStructureLocations(serverWorld, data);
        }
    }
}
