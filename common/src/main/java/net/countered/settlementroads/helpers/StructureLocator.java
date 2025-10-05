package net.countered.settlementroads.helpers;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class StructureLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);
    private static final DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.locate.structure.invalid", id)
    );
    private static final DynamicCommandExceptionType STRUCTURE_NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.locate.structure.not_found", id)
    );

    public static void locateConfiguredStructure(ServerLevel serverLevel, int locateCount, boolean locateAtPlayer) {
        ModConfig config = ModConfig.getInstance();
        LOGGER.debug("Locating " + locateCount + " " + config.structureToLocate());
        try {
            for (int x = 0; x < locateCount; x++) {
                if (locateAtPlayer) {
                    for (ServerPlayer player : serverLevel.players()) {
                        executeLocateStructure(player.blockPosition(), serverLevel, new ResourceOrTagArgument<>(Registries.STRUCTURE).parse(new StringReader(config.structureToLocate())));
                    }
                }
                else {
                    executeLocateStructure(serverLevel.getSharedSpawnPos(), serverLevel, new ResourceOrTagArgument<>(Registries.STRUCTURE).parse(new StringReader(config.structureToLocate())));
                }
            }
        } catch (CommandSyntaxException e) {
            LOGGER.warn("Failed to locate structure: " + config.structureToLocate() + " in dimension " + serverLevel.dimension().location() + " with exception: " + e.getMessage());
        }
    }

    private static void executeLocateStructure(BlockPos locatePos, ServerLevel serverLevel, ResourceOrTagArgument.Result<Structure> predicate) throws CommandSyntaxException {
        Registry<Structure> registry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
        HolderSet<Structure> holderSet = getStructureListForPredicate(predicate, registry)
                .orElseThrow(() -> STRUCTURE_INVALID_EXCEPTION.create(predicate.toString()));
        Pair<BlockPos, Holder<Structure>> pair = serverLevel.getChunkSource()
                .getGenerator()
                .findNearestMapStructure(serverLevel, holderSet, locatePos, ModConfig.getInstance().structureSearchRadius(), true);
        if (pair == null) {
            throw STRUCTURE_NOT_FOUND_EXCEPTION.create(predicate.toString());
        } else {
            BlockPos structureLocation = pair.getFirst();
            LOGGER.debug("Structure found at " + structureLocation);
            WorldDataProvider dataProvider = WorldDataProvider.getInstance();
            var structureLocations = dataProvider.getStructureLocations(serverLevel);
            structureLocations.addStructure(structureLocation);
            dataProvider.setStructureLocations(serverLevel, structureLocations);
        }
    }

    private static Optional<HolderSet<Structure>> getStructureListForPredicate(
            ResourceOrTagArgument.Result<Structure> predicate, Registry<Structure> structureRegistry
    ) {
        return predicate.unwrap().map(
            key -> structureRegistry.getHolder(key).map(HolderSet::direct),
            structureRegistry::getTag
        );
    }
}
