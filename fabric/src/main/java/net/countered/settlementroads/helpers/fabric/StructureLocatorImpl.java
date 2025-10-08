package net.countered.settlementroads.helpers.fabric;

import net.minecraft.server.level.ServerLevel;

/**
 * Architectury @ExpectPlatform 实现类（Fabric）。
 * 位置必须为：net.countered.settlementroads.helpers.fabric.StructureLocatorImpl
 *
 * 为避免重复实现，这里直接委托到现有的 helpers 包下实现。
 */
public final class StructureLocatorImpl {
    public static void locateConfiguredStructure(ServerLevel serverWorld, int locateCount, boolean locateAtPlayer) {
        net.countered.settlementroads.helpers.StructureLocatorImpl.locateConfiguredStructure(serverWorld, locateCount, locateAtPlayer);
    }
}
