package net.countered.settlementroads.persistence.neoforge;

import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.countered.settlementroads.persistence.saveddata.RoadDataStorage;
import net.countered.settlementroads.persistence.saveddata.StructureConnectionsData;
import net.countered.settlementroads.persistence.saveddata.StructureLocationsData;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public class NeoForgeWorldDataProvider implements WorldDataProvider {

    @Override
    public Records.StructureLocationData getStructureLocations(ServerLevel level) {
        StructureLocationsData data = level.getDataStorage().computeIfAbsent(
            StructureLocationsData.factory(),
            StructureLocationsData::new,
            "structure_locations"
        );
        return data.getStructureLocationData();
    }

    @Override
    public void setStructureLocations(ServerLevel level, Records.StructureLocationData data) {
        StructureLocationsData savedData = level.getDataStorage().computeIfAbsent(
            StructureLocationsData.factory(),
            StructureLocationsData::new,
            "structure_locations"
        );
        savedData.setStructureLocationData(data);
        savedData.setDirty();
    }

    @Override
    public List<Records.StructureConnection> getStructureConnections(ServerLevel level) {
        StructureConnectionsData data = level.getDataStorage().computeIfAbsent(
            StructureConnectionsData.factory(),
            StructureConnectionsData::new,
            "structure_connections"
        );
        return data.getConnections();
    }

    @Override
    public void setStructureConnections(ServerLevel level, List<Records.StructureConnection> connections) {
        StructureConnectionsData data = level.getDataStorage().computeIfAbsent(
            StructureConnectionsData.factory(),
            StructureConnectionsData::new,
            "structure_connections"
        );
        data.setConnections(connections);
        data.setDirty();
    }

    @Override
    public List<Records.RoadData> getRoadDataList(ServerLevel level) {
        RoadDataStorage data = level.getDataStorage().computeIfAbsent(
            RoadDataStorage.factory(),
            RoadDataStorage::new,
            "road_data_storage"
        );
        return data.getRoadDataList();
    }

    @Override
    public void setRoadDataList(ServerLevel level, List<Records.RoadData> roadDataList) {
        RoadDataStorage data = level.getDataStorage().computeIfAbsent(
            RoadDataStorage.factory(),
            RoadDataStorage::new,
            "road_data_storage"
        );
        data.setRoadDataList(roadDataList);
        data.setDirty();
    }
}
