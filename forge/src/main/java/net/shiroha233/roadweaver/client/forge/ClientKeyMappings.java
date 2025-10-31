package net.shiroha233.roadweaver.client.forge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shiroha233.roadweaver.RoadWeaver;
import net.shiroha233.roadweaver.client.map.RoadMapScreen;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = RoadWeaver.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientKeyMappings {
    public static KeyMapping OPEN_MAP;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_MAP = new KeyMapping("key.roadweaver.open_map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.roadweaver");
        event.register(OPEN_MAP);
    }

    @Mod.EventBusSubscriber(modid = RoadWeaver.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusHandlers {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (OPEN_MAP == null) return;
            while (OPEN_MAP.consumeClick()) {
                mc.setScreen(new RoadMapScreen());
            }
        }
    }
}
