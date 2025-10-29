package net.shiroha233.roadweaver.network;

import net.shiroha233.roadweaver.network.forge.MapNetworkForge;

public final class ClientNetBridgeImpl {
    private ClientNetBridgeImpl() {}

    public static void requestSnapshot(int minX, int minZ, int maxX, int maxZ) {
        MapNetworkForge.requestSnapshot(minX, minZ, maxX, maxZ);
    }
}
