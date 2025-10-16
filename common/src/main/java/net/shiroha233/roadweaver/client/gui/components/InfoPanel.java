package net.shiroha233.roadweaver.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaver.client.gui.core.ColorPalette;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.ArrayList;
import java.util.List;

/**
 * 信息面板 - 显示统计数据
 */
public class InfoPanel extends ModernPanel {
    
    private final List<Records.StructureInfo> structures;
    private final List<Records.StructureConnection> connections;
    private final List<Records.RoadData> roads;
    private final double zoom;
    
    public InfoPanel(int x, int y, List<Records.StructureInfo> structures,
                     List<Records.StructureConnection> connections, 
                     List<Records.RoadData> roads, double zoom) {
        super(x, y, 160, calculateHeight(connections));
        this.structures = structures;
        this.connections = connections;
        this.roads = roads;
        this.zoom = zoom;
    }
    
    private static int calculateHeight(List<Records.StructureConnection> connections) {
        int lines = 6;
        if (connections != null && !connections.isEmpty()) lines += 4;
        return lines * 12 + 16;
    }
    
    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float opacity) {
        Font font = getFont();
        int textColor = ColorPalette.Utils.withAlpha(ColorPalette.TEXT_PRIMARY, (int)(255 * opacity));
        
        int currentY = y + 8;
        int lineHeight = 12;
        
        // 统计数据
        List<String> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.roadweaver.debug_map.structures", structures.size()).getString());
        
        if (connections != null && !connections.isEmpty()) {
            int planned = 0, generating = 0, completed = 0, failed = 0;
            for (var conn : connections) {
                switch (conn.status()) {
                    case PLANNED -> planned++;
                    case GENERATING -> generating++;
                    case COMPLETED -> completed++;
                    case FAILED -> failed++;
                }
            }
            lines.add(Component.translatable("gui.roadweaver.debug_map.planned", planned).getString());
            lines.add(Component.translatable("gui.roadweaver.debug_map.generating", generating).getString());
            lines.add(Component.translatable("gui.roadweaver.debug_map.completed", completed).getString());
            if (failed > 0) {
                lines.add(Component.translatable("gui.roadweaver.debug_map.failed", failed).getString());
            }
        }
        
        lines.add(Component.translatable("gui.roadweaver.debug_map.roads", roads.size()).getString());
        lines.add(Component.translatable("gui.roadweaver.debug_map.zoom", String.format("%.1fx", zoom)).getString());
        
        for (String line : lines) {
            graphics.drawString(font, line, x + 8, currentY, textColor, false);
            currentY += lineHeight;
        }
    }
}
