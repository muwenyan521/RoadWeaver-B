package net.shiroha233.roadweaver.client.gui.core;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 渲染层基类
 * <p>
 * 所有渲染层的抽象基类，提供分层渲染的基础框架。
 * 每个层负责渲染特定类型的内容，互不干扰。
 * </p>
 * 
 * @author RoadWeaver Team
 * @version 2.0
 */
public abstract class RenderLayer {
    
    protected final MapViewport viewport;
    protected boolean enabled = true;
    protected float opacity = 1.0f;
    protected int zIndex = 0;
    
    /**
     * 构造渲染层
     * @param viewport 视口控制器
     */
    public RenderLayer(MapViewport viewport) {
        this.viewport = viewport;
    }
    
    /**
     * 渲染层内容
     * @param graphics 渲染上下文
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param partialTick 帧间插值
     */
    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    
    /**
     * 每帧更新
     * @param deltaTime 帧时间（秒）
     */
    public void update(float deltaTime) {
        // 子类可选实现
    }
    
    /**
     * 获取渲染层名称（用于调试）
     */
    public abstract String getName();
    
    /**
     * 启用/禁用层
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 设置不透明度
     */
    public void setOpacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }
    
    /**
     * 设置Z轴顺序（越大越靠前）
     */
    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public float getOpacity() { return opacity; }
    public int getZIndex() { return zIndex; }
    
    /**
     * 应用不透明度到颜色
     */
    protected int applyOpacity(int color) {
        if (opacity >= 1.0f) return color;
        
        int alpha = (color >> 24) & 0xFF;
        int newAlpha = (int)(alpha * opacity);
        return (color & 0x00FFFFFF) | (newAlpha << 24);
    }
}
