package net.countered.settlementroads.helpers;

import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class StructureConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);
    public static Queue<Records.StructureConnection> cachedStructureConnections = new ArrayDeque<>();
    
    public static void cacheNewConnection(ServerLevel serverLevel, boolean locateAtPlayer) {
        StructureLocator.locateConfiguredStructure(serverLevel, 1, locateAtPlayer);
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        List<BlockPos> villagePosList = dataProvider.getStructureLocations(serverLevel).structureLocations();
        if (villagePosList == null || villagePosList.size() < 2) {
            return;
        }
        createNewStructureConnection(serverLevel);
    }

    private static void createNewStructureConnection(ServerLevel serverLevel) {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        List<BlockPos> villagePosList = dataProvider.getStructureLocations(serverLevel).structureLocations();
        BlockPos latestVillagePos = villagePosList.get(villagePosList.size() - 1);
        Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(serverLevel);
        List<BlockPos> worldStructureLocations = structureLocationData.structureLocations();

        BlockPos closestVillage = findClosestStructure(latestVillagePos, worldStructureLocations);

        if (closestVillage != null) {
            List<Records.StructureConnection> connections = new ArrayList<>(
                    dataProvider.getStructureConnections(serverLevel)
            );
            if (!connectionExists(connections, latestVillagePos, closestVillage)) {
                Records.StructureConnection structureConnection = new Records.StructureConnection(latestVillagePos, closestVillage);
                connections.add(structureConnection);
                dataProvider.setStructureConnections(serverLevel, connections);
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
                    (connection.to().equals(b) && connection.from().equals(a))) {
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
