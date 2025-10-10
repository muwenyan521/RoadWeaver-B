package net.countered.settlementroads.client;

import net.countered.settlementroads.client.gui.ClothConfigScreen;
import net.countered.settlementroads.client.gui.RoadDebugScreen;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.neoforge.WorldDataHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "roadweaver", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SettlementRoadsClient {

    private static KeyMapping debugMapKey;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // 注册按键绑定 (默认 H 键)
        debugMapKey = new KeyMapping(
                "key.roadweaver.debug_map",
                GLFW.GLFW_KEY_H,
                "category.roadweaver"
        );
        event.register(debugMapKey);
    }
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 配置屏幕已在主类中通过 ModContainer.registerExtensionPoint 注册
        // 这里不需要额外操作
    }

    @EventBusSubscriber(modid = "roadweaver", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft client = Minecraft.getInstance();
            
            while (debugMapKey.consumeClick()) {
                handleDebugMapKey(client);
            }
        }
    }

    private static void handleDebugMapKey(Minecraft client) {
        // 如果已经打开调试屏幕，关闭它
        if (client.screen instanceof RoadDebugScreen) {
            client.execute(() -> client.setScreen(null));
            return;
        }

        // 获取服务器世界（仅单人游戏）
        if (client.getSingleplayerServer() == null) {
            return;
        }
        
        ServerLevel world = client.getSingleplayerServer().overworld();
        if (world == null) {
            return;
        }

        // 获取数据并在渲染线程打开界面
        try {
            Records.StructureLocationData structureData = WorldDataHelper.getStructureLocations(world);
            List<Records.StructureConnection> connections = WorldDataHelper.getConnectedStructures(world);
            List<Records.RoadData> roads = WorldDataHelper.getRoadDataList(world);

            List<Records.StructureInfo> structureInfos = structureData != null ?
                new ArrayList<>(structureData.structureInfos()) : new ArrayList<>();

            client.execute(() -> client.setScreen(new RoadDebugScreen(structureInfos, connections, roads)));
        } catch (Exception e) {
            // 如果获取数据失败，打开空屏幕（仍然在渲染线程调度）
            client.execute(() -> client.setScreen(new RoadDebugScreen(new ArrayList<>(), new ArrayList<>(), new ArrayList<>())));
        }
    }
}
