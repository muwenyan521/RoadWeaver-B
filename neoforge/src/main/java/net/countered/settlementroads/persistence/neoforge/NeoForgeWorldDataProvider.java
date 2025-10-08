package net.countered.settlementroads.persistence.neoforge;

import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.countered.settlementroads.persistence.WorldDataHelper;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public class NeoForgeWorldDataProvider extends WorldDataProvider {

    @Override
    public Records.StructureLocationData getStructureLocations(ServerLevel level) {
        return WorldDataHelper.getStructureLocations(level);
    }

    @Override
    public void setStructureLocations(ServerLevel level, Records.StructureLocationData data) {
        WorldDataHelper.setStructureLocations(level, data);
    }

    @Override
    public List<Records.StructureConnection> getStructureConnections(ServerLevel level) {
        return WorldDataHelper.getConnectedStructures(level);
    }

    @Override
    public void setStructureConnections(ServerLevel level, List<Records.StructureConnection> connections) {
        WorldDataHelper.setConnectedStructures(level, connections);
    }

    @Override
    public List<Records.RoadData> getRoadDataList(ServerLevel level) {
        return WorldDataHelper.getRoadDataList(level);
    }

    @Override
    public void setRoadDataList(ServerLevel level, List<Records.RoadData> roadDataList) {
        WorldDataHelper.setRoadDataList(level, roadDataList);
    }
}
