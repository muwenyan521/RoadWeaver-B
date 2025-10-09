package net.countered.settlementroads.persistence.neoforge;

import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.helpers.Records;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge 数据持久化系统
 * 替代 Fabric 的 Attachment API
 */
public class WorldDataHelper {
    
    private static final String STRUCTURE_LOCATIONS_NAME = SettlementRoads.MOD_ID + "_structure_locations";
    private static final String CONNECTED_STRUCTURES_NAME = SettlementRoads.MOD_ID + "_connected_structures";
    private static final String ROAD_DATA_NAME = SettlementRoads.MOD_ID + "_road_data";
    
    // ==================== 结构位置数据 ====================
    
    public static Records.StructureLocationData getStructureLocations(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        StructureLocationsData data = storage.computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new StructureLocationsData(new ArrayList<>()),
                        (tag, provider) -> StructureLocationsData.load(tag)
                ),
                STRUCTURE_LOCATIONS_NAME
        );
        return data.data;
    }
    
    public static void setStructureLocations(ServerLevel level, Records.StructureLocationData data) {
        DimensionDataStorage storage = level.getDataStorage();
        StructureLocationsData savedData = storage.computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new StructureLocationsData(data),
                        (tag, provider) -> StructureLocationsData.load(tag)
                ),
                STRUCTURE_LOCATIONS_NAME
        );
        savedData.data = data;
        savedData.setDirty();
    }
    
    // ==================== 结构连接数据 ====================
    
    public static List<Records.StructureConnection> getConnectedStructures(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        ConnectedStructuresData data = storage.computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new ConnectedStructuresData(new ArrayList<>()),
                        (tag, provider) -> ConnectedStructuresData.load(tag)
                ),
                CONNECTED_STRUCTURES_NAME
        );
        return data.connections;
    }
    
    public static void setConnectedStructures(ServerLevel level, List<Records.StructureConnection> connections) {
        DimensionDataStorage storage = level.getDataStorage();
        ConnectedStructuresData data = storage.computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new ConnectedStructuresData(connections),
                        (tag, provider) -> ConnectedStructuresData.load(tag)
                ),
                CONNECTED_STRUCTURES_NAME
        );
        data.connections = connections;
        data.setDirty();
    }
    
    // ==================== 道路数据 ====================
    
    public static List<Records.RoadData> getRoadDataList(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        RoadDataStorage data = storage.computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new RoadDataStorage(new ArrayList<>()),
                        (tag, provider) -> RoadDataStorage.load(tag)
                ),
                ROAD_DATA_NAME
        );
        return data.roadDataList;
    }
    
    public static void setRoadDataList(ServerLevel level, List<Records.RoadData> roadDataList) {
        DimensionDataStorage storage = level.getDataStorage();
        RoadDataStorage data = storage.computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new RoadDataStorage(roadDataList),
                        (tag, provider) -> RoadDataStorage.load(tag)
                ),
                ROAD_DATA_NAME
        );
        data.roadDataList = roadDataList;
        data.setDirty();
    }
    
    // ==================== SavedData 实现类 ====================
    
    private static class StructureLocationsData extends SavedData {
        private Records.StructureLocationData data;
        
        public StructureLocationsData(Records.StructureLocationData data) {
            this.data = data;
        }
        
        public StructureLocationsData(List<BlockPos> locations) {
            this.data = new Records.StructureLocationData(locations);
        }
        
        public static StructureLocationsData load(CompoundTag tag) {
            List<BlockPos> locations = Records.StructureLocationData.CODEC
                    .parse(NbtOps.INSTANCE, tag.get("data"))
                    .resultOrPartial(error -> SettlementRoads.getLogger().error("Failed to load structure locations: {}", error))
                    .map(Records.StructureLocationData::structureLocations)
                    .orElse(new ArrayList<>());
            return new StructureLocationsData(locations);
        }
        
        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            Records.StructureLocationData.CODEC
                    .encodeStart(NbtOps.INSTANCE, data)
                    .resultOrPartial(error -> SettlementRoads.getLogger().error("Failed to save structure locations: {}", error))
                    .ifPresent(nbt -> tag.put("data", nbt));
            return tag;
        }
    }
    
    private static class ConnectedStructuresData extends SavedData {
        private List<Records.StructureConnection> connections;
        
        public ConnectedStructuresData(List<Records.StructureConnection> connections) {
            this.connections = connections;
        }
        
        public static ConnectedStructuresData load(CompoundTag tag) {
            Codec<List<Records.StructureConnection>> codec = Codec.list(Records.StructureConnection.CODEC);
            List<Records.StructureConnection> connections = codec
                    .parse(NbtOps.INSTANCE, tag.get("data"))
                    .resultOrPartial(error -> SettlementRoads.getLogger().error("Failed to load connected structures: {}", error))
                    .orElse(new ArrayList<>());
            return new ConnectedStructuresData(connections);
        }
        
        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            Codec<List<Records.StructureConnection>> codec = Codec.list(Records.StructureConnection.CODEC);
            codec.encodeStart(NbtOps.INSTANCE, connections)
                    .resultOrPartial(error -> SettlementRoads.getLogger().error("Failed to save connected structures: {}", error))
                    .ifPresent(nbt -> tag.put("data", nbt));
            return tag;
        }
    }
    
    private static class RoadDataStorage extends SavedData {
        private List<Records.RoadData> roadDataList;
        
        public RoadDataStorage(List<Records.RoadData> roadDataList) {
            this.roadDataList = roadDataList;
        }
        
        public static RoadDataStorage load(CompoundTag tag) {
            Codec<List<Records.RoadData>> codec = Codec.list(Records.RoadData.CODEC);
            List<Records.RoadData> roadDataList = codec
                    .parse(NbtOps.INSTANCE, tag.get("data"))
                    .resultOrPartial(error -> SettlementRoads.getLogger().error("Failed to load road data: {}", error))
                    .orElse(new ArrayList<>());
            return new RoadDataStorage(roadDataList);
        }
        
        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            Codec<List<Records.RoadData>> codec = Codec.list(Records.RoadData.CODEC);
            codec.encodeStart(NbtOps.INSTANCE, roadDataList)
                    .resultOrPartial(error -> SettlementRoads.getLogger().error("Failed to save road data: {}", error))
                    .ifPresent(nbt -> tag.put("data", nbt));
            return tag;
        }
    }
}
