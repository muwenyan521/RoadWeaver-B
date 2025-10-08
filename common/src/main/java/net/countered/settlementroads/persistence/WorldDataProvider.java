package net.countered.settlementroads.persistence;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.countered.settlementroads.helpers.Records;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * 跨平台世界数据访问抽象（Common）。
 * 使用 @ExpectPlatform 提供平台端实现提供者。
 */
public abstract class WorldDataProvider {

    @ExpectPlatform
    public static WorldDataProvider getInstance() {
        throw new AssertionError();
    }

    // 结构位置
    public abstract Records.StructureLocationData getStructureLocations(ServerLevel level);
    public abstract void setStructureLocations(ServerLevel level, Records.StructureLocationData data);

    // 结构连接
    public abstract List<Records.StructureConnection> getStructureConnections(ServerLevel level);
    public abstract void setStructureConnections(ServerLevel level, List<Records.StructureConnection> connections);

    // 道路数据
    public abstract List<Records.RoadData> getRoadDataList(ServerLevel level);
    public abstract void setRoadDataList(ServerLevel level, List<Records.RoadData> roadDataList);
}
