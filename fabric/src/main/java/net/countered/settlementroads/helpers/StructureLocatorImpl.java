package net.countered.settlementroads.helpers;

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
import com.mojang.datafixers.util.Pair;

import java.util.Optional;

public class StructureLocatorImpl {
    public static void locateConfiguredStructure(ServerLevel serverWorld, int locateCount, boolean locateAtPlayer) {
        IModConfig cfg = ConfigProvider.get();
        for (int i = 0; i < locateCount; i++) {
            if (locateAtPlayer) {
                for (ServerPlayer player : serverWorld.players()) {
                    executeLocateStructure(player.blockPosition(), serverWorld, cfg);
                }
            } else {
                executeLocateStructure(serverWorld.getSharedSpawnPos(), serverWorld, cfg);
            }
        }
    }

    private static void executeLocateStructure(BlockPos locatePos, ServerLevel serverWorld, IModConfig cfg) {
        String structureId = cfg.structureToLocate();
        Registry<Structure> registry = serverWorld.registryAccess().registryOrThrow(Registries.STRUCTURE);
        HolderSet<Structure> holderSet = null;

        if (structureId.startsWith("#")) {
            String tagId = structureId.substring(1);
            ResourceLocation tagLocation = ResourceLocation.parse(tagId);
            TagKey<Structure> tagKey = TagKey.create(Registries.STRUCTURE, tagLocation);
            Optional<HolderSet.Named<Structure>> tagOptional = registry.getTag(tagKey);
            if (tagOptional.isPresent()) {
                holderSet = tagOptional.get();
            } else {
                return;
            }
        } else {
            ResourceLocation structureLocation = ResourceLocation.parse(structureId);
            ResourceKey<Structure> structureKey = ResourceKey.create(Registries.STRUCTURE, structureLocation);
            Optional<Holder.Reference<Structure>> holderOptional = registry.getHolder(structureKey);
            if (holderOptional.isPresent()) {
                holderSet = HolderSet.direct(holderOptional.get());
            } else {
                return;
            }
        }

        if (holderSet == null) return;

        Pair<BlockPos, Holder<Structure>> pair = serverWorld.getChunkSource()
                .getGenerator()
                .findNearestMapStructure(serverWorld, holderSet, locatePos, cfg.structureSearchRadius(), true);

        if (pair != null) {
            BlockPos structureLocation = pair.getFirst();
            WorldDataProvider dataProvider = WorldDataProvider.getInstance();
            Records.StructureLocationData data = dataProvider.getStructureLocations(serverWorld);
            if (data == null) data = new Records.StructureLocationData(new java.util.ArrayList<>());
            data.addStructure(structureLocation);
            dataProvider.setStructureLocations(serverWorld, data);
        }
    }
}
