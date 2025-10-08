package net.countered.settlementroads.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

import net.countered.settlementroads.persistence.WorldDataProvider;

public class StructureConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");

    public static Queue<Records.StructureConnection> cachedStructureConnections = new ArrayDeque<>();

    public static void cacheNewConnection(ServerLevel serverWorld, boolean locateAtPlayer) {
        StructureLocator.locateConfiguredStructure(serverWorld, 1, locateAtPlayer);
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(serverWorld);
        if (structureLocationData == null) return;
        List<BlockPos> locations = structureLocationData.structureLocations();
        if (locations == null || locations.size() < 2) {
            return;
        }
        createNewStructureConnection(serverWorld);
    }

    private static void createNewStructureConnection(ServerLevel serverWorld) {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(serverWorld);
        if (structureLocationData == null) return;
        List<BlockPos> worldStructureLocations = structureLocationData.structureLocations();
        if (worldStructureLocations == null || worldStructureLocations.size() < 2) return;

        BlockPos latestVillagePos = worldStructureLocations.get(worldStructureLocations.size() - 1);
        BlockPos closestVillage = findClosestStructure(latestVillagePos, worldStructureLocations);

        if (closestVillage != null) {
            List<Records.StructureConnection> connections = new ArrayList<>(
                    Optional.ofNullable(dataProvider.getStructureConnections(serverWorld)).orElseGet(ArrayList::new)
            );
            if (!connectionExists(connections, latestVillagePos, closestVillage)) {
                Records.StructureConnection structureConnection = new Records.StructureConnection(latestVillagePos, closestVillage);
                connections.add(structureConnection);
                dataProvider.setStructureConnections(serverWorld, connections);
                cachedStructureConnections.add(structureConnection);
                LOGGER.debug("Created connection between {} and {} (distance: {} blocks)",
                        latestVillagePos, closestVillage,
                        Math.sqrt(latestVillagePos.distSqr(closestVillage)));
            }
        }
    }

    private static boolean connectionExists(List<Records.StructureConnection> existingConnections, BlockPos a, BlockPos b) {
        for (Records.StructureConnection connection : existingConnections) {
            if ((connection.from().equals(a) && connection.to().equals(b)) ||
                (connection.from().equals(b) && connection.to().equals(a))) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos findClosestStructure(BlockPos currentVillage, List<BlockPos> allVillages) {
        BlockPos closestVillage = null;
        double minDistance = Double.MAX_VALUE;
        for (BlockPos village : allVillages) {
            if (!village.equals(currentVillage)) {
                double distance = currentVillage.distSqr(village);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestVillage = village;
                }
            }
        }
        return closestVillage;
    }
}
