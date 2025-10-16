package net.shiroha233.roadweaver.client.forge;

import net.shiroha233.roadweaver.client.gui.ClothConfigScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Forge 平台的配置屏幕工厂实现
 */
public class ConfigScreenFactoryImpl {
    
    /**
     * 创建配置屏幕 (Forge实现)
     * @param parent 父屏幕
     * @return 配置屏幕实例
     */
    public static Screen createConfigScreen(Screen parent) {
        return ClothConfigScreen.createConfigScreen(parent);
    }
}
