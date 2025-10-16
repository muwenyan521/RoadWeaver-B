package net.shiroha233.roadweaver.features.roadlogic;

import net.shiroha233.roadweaver.config.ConfigProvider;
import net.shiroha233.roadweaver.config.IModConfig;
import net.shiroha233.roadweaver.features.config.RoadFeatureConfig;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class Road {

    private final ServerLevel serverWorld;
    private final Records.StructureConnection structureConnection;
    private final RoadFeatureConfig context;

    public Road(ServerLevel serverWorld,
                Records.StructureConnection structureConnection,
                RoadFeatureConfig config) {
        this.serverWorld = serverWorld;
        this.structureConnection = structureConnection;
        this.context = config;
    }

    public void generateRoad(int maxSteps){
        // 更新连接状态为"生成中"
        updateConnectionStatus(Records.ConnectionStatus.GENERATING);

        RandomSource random = RandomSource.create();
        int width = getRandomWidth(random, context.getWidths());

        IModConfig cfg = ConfigProvider.get();
        int type = allowedRoadTypes(random, cfg);
        if (type == -1) {
            updateConnectionStatus(Records.ConnectionStatus.FAILED);
            return;
        }
        List<BlockState> material = (type == 1)
                ? getRandomMaterials(random, context.getNaturalMaterials())
                : getRandomMaterials(random, context.getArtificialMaterials());

        BlockPos start = structureConnection.from();
        BlockPos end = structureConnection.to();

        int maxHeightDiff = structureConnection.manual() ? cfg.manualMaxHeightDifference() : cfg.maxHeightDifference();
        int maxStability = structureConnection.manual() ? cfg.manualMaxTerrainStability() : cfg.maxTerrainStability();
        boolean ignoreWater = structureConnection.manual() && cfg.manualIgnoreWater();

        List<Records.RoadSegmentPlacement> roadSegmentPlacementList = RoadPathCalculator.calculateAStarRoadPath(
                start, end, width, serverWorld, maxSteps, maxHeightDiff, maxStability, ignoreWater);

        if (roadSegmentPlacementList.isEmpty()) {
            updateConnectionStatus(Records.ConnectionStatus.FAILED);
            return;
        }

        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        List<Records.RoadData> roadDataList = dataProvider.getRoadDataList(serverWorld);
        // 创建可变副本以避免 UnsupportedOperationException
        List<Records.RoadData> mutableList = new ArrayList<>(roadDataList != null ? roadDataList : new ArrayList<>());
        mutableList.add(new Records.RoadData(width, type, material, roadSegmentPlacementList));
        dataProvider.setRoadDataList(serverWorld, mutableList);

        // 完成
        updateConnectionStatus(Records.ConnectionStatus.COMPLETED);
    }

    private void updateConnectionStatus(Records.ConnectionStatus newStatus) {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> connections = dataProvider.getStructureConnections(serverWorld);
        // 创建可变副本以避免 UnsupportedOperationException
        List<Records.StructureConnection> mutableConnections = new ArrayList<>(connections != null ? connections : new ArrayList<>());
        
        for (int i = 0; i < mutableConnections.size(); i++) {
            Records.StructureConnection conn = mutableConnections.get(i);
            if ((conn.from().equals(structureConnection.from()) && conn.to().equals(structureConnection.to())) ||
                (conn.from().equals(structureConnection.to()) && conn.to().equals(structureConnection.from()))) {
                mutableConnections.set(i, new Records.StructureConnection(conn.from(), conn.to(), newStatus, conn.manual()));
                dataProvider.setStructureConnections(serverWorld, mutableConnections);
                break;
            }
        }
    }

    private static int allowedRoadTypes(RandomSource deterministicRandom, IModConfig cfg) {
        if (cfg.allowArtificial() && cfg.allowNatural()) {
            return getRandomRoadType(deterministicRandom);
        } else if (cfg.allowArtificial()) {
            return 0;
        } else if (cfg.allowNatural()) {
            return 1;
        } else {
            return -1;
        }
    }

    private static int getRandomRoadType(RandomSource random) {
        return random.nextInt(2);
    }

    private static List<BlockState> getRandomMaterials(RandomSource random, List<List<BlockState>> materialsList) {
        return materialsList.get(random.nextInt(materialsList.size()));
    }

    private static int getRandomWidth(RandomSource random, List<Integer> widthList) {
        return widthList.get(random.nextInt(widthList.size()));
    }
}
