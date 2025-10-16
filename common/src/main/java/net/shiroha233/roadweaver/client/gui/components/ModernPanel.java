package net.shiroha233.roadweaver.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.shiroha233.roadweaver.client.gui.core.AnimationState;
import net.shiroha233.roadweaver.client.gui.core.ColorPalette;
import net.shiroha233.roadweaver.client.gui.util.ModernRenderUtils;

/**
 * 现代化UI面板基类
 */
public abstract class ModernPanel {
    
    protected int x, y, width, height;
    protected final AnimationState.FadeAnimation fadeAnimation;
    protected boolean visible = true;
    
    public ModernPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fadeAnimation = new AnimationState.FadeAnimation(0.3f);
        this.fadeAnimation.show();
    }
    
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!fadeAnimation.isVisible()) return;
        
        float opacity = fadeAnimation.getValue();
        renderBackground(graphics, opacity);
        renderContent(graphics, mouseX, mouseY, opacity);
    }
    
    protected void renderBackground(GuiGraphics graphics, float opacity) {
        int bgColor = ColorPalette.Utils.withAlpha(ColorPalette.SURFACE, (int)(240 * opacity));
        int borderColor = ColorPalette.Utils.withAlpha(ColorPalette.BORDER, (int)(255 * opacity));
        ModernRenderUtils.drawModernPanel(graphics, x, y, width, height, bgColor, borderColor, 8);
    }
    
    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float opacity);
    
    public void update(float deltaTime) {
        fadeAnimation.update(deltaTime);
    }
    
    public void show() { fadeAnimation.show(); visible = true; }
    public void hide() { fadeAnimation.hide(); visible = false; }
    public boolean isVisible() { return visible && fadeAnimation.isVisible(); }
    
    protected Font getFont() { return Minecraft.getInstance().font; }
}
