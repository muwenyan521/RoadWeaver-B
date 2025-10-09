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

/**
 * 通用结构定位实现（Common）。
 * 平台桥接类直接委托到此处，避免重复逻辑。
 */
public final class StructureLocatorImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");

    private StructureLocatorImpl() {}

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
        Set<BlockPos> newlyFound = new HashSet<>();

        Optional<HolderSet<Structure>> targetStructures = resolveStructureTargets(level, config.structureToLocate());
        if (targetStructures.isEmpty()) {
            LOGGER.warn("RoadWeaver: 无法解析结构目标 `{}`，跳过定位。", config.structureToLocate());
            return;
        }

        List<BlockPos> centers = collectSearchCenters(level, locateAtPlayer);
        int radius = Math.max(config.structureSearchRadius(), 1);
        LOGGER.debug("RoadWeaver: locating up to {} structure(s) - centers={}, radius={}, atPlayer={}", locateCount, centers.size(), radius, locateAtPlayer);

        for (BlockPos center : centers) {
            if (locateCount <= 0) {
                break;
            }

            Pair<BlockPos, Holder<Structure>> result = level.getChunkSource()
                    .getGenerator()
                    .findNearestMapStructure(level, targetStructures.get(), center, radius, false);

            if (result != null) {
                BlockPos structurePos = result.getFirst();
                if (!containsBlockPos(knownLocations, structurePos)) {
                    knownLocations.add(structurePos);
                    newlyFound.add(structurePos);
                    locateCount--;
                }
            }
        }

        if (!newlyFound.isEmpty()) {
            dataProvider.setStructureLocations(level, new Records.StructureLocationData(knownLocations));
            LOGGER.debug("RoadWeaver: 定位到 {} 个新结构: {}", newlyFound.size(), newlyFound);
        }
    }

    private static Optional<HolderSet<Structure>> resolveStructureTargets(ServerLevel level, String identifier) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        if (identifier.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.parse(identifier.substring(1));
            TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, tagId);
            return registry.getTag(tag).map(named -> (HolderSet<Structure>) named);
        } else {
            ResourceLocation id = ResourceLocation.parse(identifier);
            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, id);
            return registry.getHolder(key).map(HolderSet::direct);
        }
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
