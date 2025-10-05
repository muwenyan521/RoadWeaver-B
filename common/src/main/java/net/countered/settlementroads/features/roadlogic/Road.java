package net.countered.settlementroads.features.roadlogic;

import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class Road {

    ServerLevel serverLevel;
    Records.StructureConnection structureConnection;
    RoadFeatureConfig context;
    WorldDataProvider dataProvider;

    public Road(ServerLevel serverLevel, Records.StructureConnection structureConnection, RoadFeatureConfig config, WorldDataProvider dataProvider) {
        this.serverLevel = serverLevel;
        this.structureConnection = structureConnection;
        this.context = config;
        this.dataProvider = dataProvider;
    }

    public void generateRoad(int maxSteps){
        // 更新连接状态为"生成中"
        updateConnectionStatus(Records.ConnectionStatus.GENERATING);
        
        RandomSource random = RandomSource.create();
        int width = getRandomWidth(random, context);
        int type = allowedRoadTypes(random);
        // if all road types are disabled in config
        if (type == -1) {
            updateConnectionStatus(Records.ConnectionStatus.FAILED);
            return;
        }
        List<BlockState> material = (type == 1) ? getRandomNaturalRoadMaterials(random, context) : getRandomArtificialRoadMaterials(random, context);

        BlockPos start = structureConnection.from();
        BlockPos end = structureConnection.to();

        List<Records.RoadSegmentPlacement> roadSegmentPlacementList = RoadPathCalculator.calculateAStarRoadPath(start, end, width, serverLevel, maxSteps);

        // 检查是否生成失败（路径为空）
        if (roadSegmentPlacementList.isEmpty()) {
            updateConnectionStatus(Records.ConnectionStatus.FAILED);
            return;
        }

        List<Records.RoadData> roadDataList = new ArrayList<>(dataProvider.getRoadDataList(serverLevel));
        roadDataList.add(new Records.RoadData(width, type, material, roadSegmentPlacementList));

        dataProvider.setRoadDataList(serverLevel, roadDataList);
        
        // 道路生成完成，更新状态为"已完成"
        updateConnectionStatus(Records.ConnectionStatus.COMPLETED);
    }
    
    private void updateConnectionStatus(Records.ConnectionStatus newStatus) {
        List<Records.StructureConnection> connections = new ArrayList<>(
                dataProvider.getStructureConnections(serverLevel)
        );
        
        // 查找并更新对应的连接
        for (int i = 0; i < connections.size(); i++) {
            Records.StructureConnection conn = connections.get(i);
            if ((conn.from().equals(structureConnection.from()) && conn.to().equals(structureConnection.to())) ||
                (conn.from().equals(structureConnection.to()) && conn.to().equals(structureConnection.from()))) {
                // 更新状态
                connections.set(i, new Records.StructureConnection(conn.from(), conn.to(), newStatus));
                dataProvider.setStructureConnections(serverLevel, connections);
                break;
            }
        }
    }

    private static int allowedRoadTypes(RandomSource deterministicRandom) {
        ModConfig config = ModConfig.getInstance();
        if (config.allowArtificial() && config.allowNatural()){
            return getRandomRoadType(deterministicRandom);
        }
        else if (config.allowArtificial()){
            return 0;
        }
        else if (config.allowNatural()) {
            return 1;
        }
        else {
            return -1;
        }
    }

    private static int getRandomRoadType(RandomSource random) {
        return random.nextInt(2);
    }

    private static List<BlockState> getRandomNaturalRoadMaterials(RandomSource random, RoadFeatureConfig config) {
        List<List<BlockState>> materialsList = config.getNaturalMaterials();
        return materialsList.get(random.nextInt(materialsList.size()));
    }

    private static List<BlockState> getRandomArtificialRoadMaterials(RandomSource random, RoadFeatureConfig config) {
        List<List<BlockState>> materialsList = config.getArtificialMaterials();
        return materialsList.get(random.nextInt(materialsList.size()));
    }

    private static int getRandomWidth(RandomSource random, RoadFeatureConfig config) {
        List<Integer> widthList = config.getWidths();
        return widthList.get(random.nextInt(widthList.size()));
    }
}
