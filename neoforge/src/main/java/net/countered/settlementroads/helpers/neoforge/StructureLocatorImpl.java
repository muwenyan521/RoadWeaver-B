package net.countered.settlementroads.helpers.neoforge;

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
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StructureLocatorImpl {
    /**
     * Architectury @ExpectPlatform 实现（NeoForge）。
     * 必须提供与 common 中 {@code StructureLocator.locateConfiguredStructure} 相同的方法签名。
     * 这里直接复用 common 的逻辑，避免重复代码。
     */
    public static void locateConfiguredStructure(ServerLevel serverWorld, int locateCount, boolean locateAtPlayer) {
        net.countered.settlementroads.helpers.StructureLocatorImpl.locateConfiguredStructure(serverWorld, locateCount, locateAtPlayer);
    }
    
    public static List<BlockPos> locateStructures(ServerLevel level) {
        IModConfig config = ConfigProvider.get();
        String structureToLocate = config.structureToLocate();
        int searchRadius = config.structureSearchRadius();
        List<BlockPos> locations = new ArrayList<>();
        Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        
        if (structureToLocate.startsWith("#")) {
            // Tag-based search
            String tagName = structureToLocate.substring(1);
            ResourceLocation tagLocation = ResourceLocation.parse(tagName);
            TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, tagLocation);
            
            Optional<HolderSet.Named<Structure>> tagHolders = structureRegistry.getTag(tag);
            if (tagHolders.isPresent()) {
                for (Holder<Structure> holder : tagHolders.get()) {
                    ResourceKey<Structure> key = holder.unwrapKey().orElse(null);
                    if (key != null) {
                        locateStructure(level, key, searchRadius, locations);
                    }
                }
            }
        } else {
            // Direct structure ID search
            ResourceLocation structureId = ResourceLocation.parse(structureToLocate);
            ResourceKey<Structure> structureKey = ResourceKey.create(Registries.STRUCTURE, structureId);
            locateStructure(level, structureKey, searchRadius, locations);
        }
        
        return locations;
    }
    
    private static void locateStructure(ServerLevel level, ResourceKey<Structure> structureKey, 
                                      int searchRadius, List<BlockPos> locations) {
        BlockPos center = new BlockPos(0, 64, 0);
        
        // Use NeoForge's structure location methods
        Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(level.registryAccess()
                        .registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(structureKey)), 
                        center, searchRadius, false);
        
        if (result != null) {
            locations.add(result.getFirst());
        }
    }
}
