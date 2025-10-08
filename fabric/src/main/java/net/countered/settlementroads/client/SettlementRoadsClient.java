package net.countered.settlementroads.client;

import net.countered.settlementroads.client.gui.RoadDebugScreen;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SettlementRoadsClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    private static KeyMapping debugMapKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing RoadWeaver Client (Fabric)...");

        // 注册按键：默认 H 打开调试地图
        debugMapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.roadweaver.debug_map",
                GLFW.GLFW_KEY_H,
                "category.roadweaver"
        ));

        // 客户端每 tick 轮询按键
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (debugMapKey.consumeClick()) {
                handleDebugMapKey(client);
            }
        });
    }

    private static void handleDebugMapKey(Minecraft client) {
        if (client == null) return;

        // 如果已打开则关闭
        if (client.screen instanceof RoadDebugScreen) {
            client.setScreen(null);
            return;
        }

        // 仅单人世界支持
        if (client.getSingleplayerServer() == null) {
            return;
        }

        ServerLevel world = client.getSingleplayerServer().overworld();
        if (world == null) return;

        try {
            Records.StructureLocationData data = WorldDataProvider.getInstance().getStructureLocations(world);
            List<Records.StructureConnection> connections = WorldDataProvider.getInstance().getStructureConnections(world);
            List<Records.RoadData> roads = WorldDataProvider.getInstance().getRoadDataList(world);

            List<net.minecraft.core.BlockPos> structures = data != null ? new ArrayList<>(data.structureLocations()) : new ArrayList<>();
            client.setScreen(new RoadDebugScreen(structures, connections, roads));
        } catch (Exception e) {
            client.setScreen(new RoadDebugScreen(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        }
    }
}
