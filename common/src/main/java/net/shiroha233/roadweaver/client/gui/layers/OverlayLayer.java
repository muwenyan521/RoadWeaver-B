package net.shiroha233.roadweaver.client.gui.layers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.shiroha233.roadweaver.client.gui.core.ColorPalette;
import net.shiroha233.roadweaver.client.gui.core.MapViewport;
import net.shiroha233.roadweaver.client.gui.core.RenderLayer;
import net.shiroha233.roadweaver.client.gui.util.ModernRenderUtils;

/**
 * 叠加层 - 玩家标记等顶层元素
 */
public class OverlayLayer extends RenderLayer {
    
    public OverlayLayer(MapViewport viewport) {
        super(viewport);
        setZIndex(100); // 最顶层
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!enabled) return;
        
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        // 玩家位置
        MapViewport.ScreenCoord pos = viewport.worldToScreen(player.getX(), player.getZ());
        
        if (!viewport.isInViewport(pos.x(), pos.y())) return;
        
        int radius = Math.max(4, (int)(6 * Math.min(viewport.getZoom() / 2, 1.5)));
        
        // 玩家标记（红色圆点）
        ModernRenderUtils.fillCircle(graphics, pos.x(), pos.y(), radius + 1, 
            applyOpacity(0x80000000));
        ModernRenderUtils.fillCircle(graphics, pos.x(), pos.y(), radius,
            applyOpacity(ColorPalette.PLAYER_MARKER));
        
        // 方向指示箭头
        float yaw = player.getYRot();
        double angle = Math.toRadians(yaw + 90);
        int arrowLength = radius + 5;
        
        int tx = pos.x() + (int)(Math.cos(angle) * arrowLength);
        int ty = pos.y() + (int)(Math.sin(angle) * arrowLength);
        
        ModernRenderUtils.drawSmoothLine(graphics, pos.x(), pos.y(), tx, ty,
            applyOpacity(0xFFFFFFFF));
    }
    
    @Override
    public String getName() { return "Overlay"; }
}
