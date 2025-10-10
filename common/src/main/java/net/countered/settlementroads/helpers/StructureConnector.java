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
         LOGGER.debug("🔍 cacheNewConnection called, locateAtPlayer={}", locateAtPlayer);
         
         WorldDataProvider dataProvider = WorldDataProvider.getInstance();
         int beforeCount = dataProvider.getStructureLocations(serverWorld).structureLocations().size();
         
         StructureLocator.locateConfiguredStructure(serverWorld, 1, locateAtPlayer);
         
         Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(serverWorld);
         if (structureLocationData == null) {
             LOGGER.warn("❌ structureLocationData is null");
             return;
         }
         
         List<BlockPos> locations = structureLocationData.structureLocations();
         int afterCount = locations.size();
         LOGGER.debug("Structure count: before={}, after={}", beforeCount, afterCount);
         
         if (locations == null || locations.size() < 2) {
             LOGGER.debug("❌ Not enough structures to create connection (need 2, have {})", locations.size());
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
                 double distance = Math.sqrt(latestVillagePos.distSqr(closestVillage));
                 LOGGER.info("🔗 Created connection between {} and {} (distance: {} blocks, queue size: {})",
                         latestVillagePos, closestVillage, (int) Math.round(distance), cachedStructureConnections.size());
             } else {
                 LOGGER.debug("Connection already exists between {} and {}", latestVillagePos, closestVillage);
             }
         } else {
             LOGGER.warn("❌ Could not find closest structure for {}", latestVillagePos);
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
