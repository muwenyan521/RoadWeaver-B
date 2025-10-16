package net.shiroha233.roadweaver.client.gui.layers;

import net.minecraft.client.gui.GuiGraphics;
import net.shiroha233.roadweaver.client.gui.core.ColorPalette;
import net.shiroha233.roadweaver.client.gui.core.LODManager;
import net.shiroha233.roadweaver.client.gui.core.MapViewport;
import net.shiroha233.roadweaver.client.gui.core.RenderLayer;
import net.shiroha233.roadweaver.client.gui.util.ModernRenderUtils;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.List;

/**
 * 连接线渲染层（LOD优化版）
 * 
 * @version 2.1
 */
public class ConnectionLayer extends RenderLayer {
    
    private final List<Records.StructureConnection> connections;
    private final LODManager lodManager;
    
    public ConnectionLayer(MapViewport viewport, List<Records.StructureConnection> connections) {
        super(viewport);
        this.connections = connections;
        this.lodManager = new LODManager(viewport);
        setZIndex(8);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!enabled || connections.isEmpty()) return;
        
        lodManager.update();
        
        // 检查是否应该渲染连接线
        if (!lodManager.shouldRenderConnections()) {
            return;
        }
        
        for (Records.StructureConnection conn : connections) {
            if (conn.status() == Records.ConnectionStatus.COMPLETED) continue;
            
            // 使用LOD系统的线段可见性检查
            if (!lodManager.isLineVisible(conn.from().getX(), conn.from().getZ(),
                                         conn.to().getX(), conn.to().getZ())) {
                continue;
            }
            
            MapViewport.ScreenCoord from = viewport.worldToScreen(conn.from().getX(), conn.from().getZ());
            MapViewport.ScreenCoord to = viewport.worldToScreen(conn.to().getX(), conn.to().getZ());
            
            int color = getConnectionColor(conn);
            ModernRenderUtils.drawDashedLine(graphics, from.x(), from.y(), to.x(), to.y(), 
                applyOpacity(color), 6, 3);
        }
    }
    
    @Override
    public void update(float deltaTime) {
        lodManager.update();
    }
    
    private int getConnectionColor(Records.StructureConnection conn) {
        return switch (conn.status()) {
            case PLANNED -> ColorPalette.ROAD_PLANNED;
            case GENERATING -> ColorPalette.ROAD_GENERATING;
            case FAILED -> ColorPalette.ROAD_FAILED;
            default -> ColorPalette.ROAD_COMPLETED;
        };
    }
    
    @Override
    public String getName() { return "Connections"; }
}
