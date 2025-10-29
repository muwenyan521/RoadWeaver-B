package net.shiroha233.roadweaver.network;

public final class ClientNetBridgeImpl {
    private ClientNetBridgeImpl() {}

    public static void requestSnapshot(int minX, int minZ, int maxX, int maxZ) {
        net.shiroha233.roadweaver.network.fabric.MapNetworkFabric.requestSnapshot(minX, minZ, maxX, maxZ);
    }
}
