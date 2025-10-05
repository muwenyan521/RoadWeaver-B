package net.countered.settlementroads.persistence.fabric;

import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public class FabricWorldDataProvider implements WorldDataProvider {

    @Override
    public Records.StructureLocationData getStructureLocations(ServerLevel level) {
        return level.getExistingData(WorldDataAttachment.STRUCTURE_LOCATIONS);
    }

    @Override
    public void setStructureLocations(ServerLevel level, Records.StructureLocationData data) {
        level.setData(WorldDataAttachment.STRUCTURE_LOCATIONS, data);
    }

    @Override
    public List<Records.StructureConnection> getStructureConnections(ServerLevel level) {
        return level.getExistingData(WorldDataAttachment.CONNECTED_STRUCTURES, ArrayList::new);
    }

    @Override
    public void setStructureConnections(ServerLevel level, List<Records.StructureConnection> connections) {
        level.setData(WorldDataAttachment.CONNECTED_STRUCTURES, connections);
    }

    @Override
    public List<Records.RoadData> getRoadDataList(ServerLevel level) {
        return level.getExistingData(WorldDataAttachment.ROAD_DATA_LIST, ArrayList::new);
    }

    @Override
    public void setRoadDataList(ServerLevel level, List<Records.RoadData> roadDataList) {
        level.setData(WorldDataAttachment.ROAD_DATA_LIST, roadDataList);
    }
}
