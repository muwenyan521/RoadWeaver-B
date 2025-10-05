package net.countered.settlementroads.persistence;

import net.countered.settlementroads.helpers.Records;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Abstract interface for world data persistence across different mod platforms.
 * This allows the common module to work with both Fabric's Attachment API and NeoForge's SavedData system.
 */
public interface WorldDataProvider {
    
    // Structure location data methods
    Records.StructureLocationData getStructureLocations(ServerLevel level);
    void setStructureLocations(ServerLevel level, Records.StructureLocationData data);
    
    // Structure connections methods
    List<Records.StructureConnection> getStructureConnections(ServerLevel level);
    void setStructureConnections(ServerLevel level, List<Records.StructureConnection> connections);
    
    // Road data methods
    List<Records.RoadData> getRoadDataList(ServerLevel level);
    void setRoadDataList(ServerLevel level, List<Records.RoadData> roadDataList);
    
    // Get platform-specific implementation
    static WorldDataProvider getInstance() {
        return WorldDataProviderImpl.INSTANCE;
    }
}
