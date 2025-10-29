package net.shiroha233.roadweaver.client.map;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaver.client.map.data.ClientMapNotes;

final class MapInteraction {
    private MapInteraction() {}

    static void renderHoverHighlight(GuiGraphics g,
                                     MapSnapshot snapshot,
                                     MapView view,
                                     int mapX, int mapY, int mapW, int mapH,
                                     int innerPad,
                                     double mouseX, double mouseY) {
        double mx = mouseX;
        double my = mouseY;
        if (!insideMap(mx, my, mapX, mapY, mapW, mapH, innerPad)) return;
        int bestDist = Integer.MAX_VALUE;
        BlockPos best = null;
        int contentW = mapW - innerPad * 2;
        int contentH = mapH - innerPad * 2;
        for (BlockPos p : snapshot.structures()) {
            if (!view.isInViewWorld(p.getX(), p.getZ())) continue;
            int x = view.toScreenX(p.getX(), mapX, innerPad, contentW);
            int y = view.toScreenY(p.getZ(), mapY, innerPad, contentH);
            int dx = (int)Math.abs(x - mx);
            int dy = (int)Math.abs(y - my);
            int d2 = dx*dx + dy*dy;
            if (d2 < bestDist) { bestDist = d2; best = p; }
        }
        if (best != null && bestDist <= 64) {
            int x = view.toScreenX(best.getX(), mapX, innerPad, contentW);
            int y = view.toScreenY(best.getZ(), mapY, innerPad, contentH);
            g.fill(x - 4, y - 4, x + 5, y + 5, 0xCCFFD54F);
        }
    }

    static void renderHoverTooltip(GuiGraphics g,
                                   Font font,
                                   MapSnapshot snapshot,
                                   MapView view,
                                   int mapX, int mapY, int mapW, int mapH,
                                   int innerPad,
                                   double mouseX, double mouseY) {
        double mx = mouseX;
        double my = mouseY;
        int contentW = mapW - innerPad * 2;
        int contentH = mapH - innerPad * 2;
        if (!insideMap(mx, my, mapX, mapY, mapW, mapH, innerPad)) {
            int wx = (int)Math.round(view.screenToWorldX(mx, mapX, innerPad, contentW));
            int wz = (int)Math.round(view.screenToWorldZ(my, mapY, innerPad, contentH));
            g.renderTooltip(font, Component.translatable("gui.roadweaver.map.coord", wx, wz), (int)mx, (int)my);
            return;
        }
        int bestDist = Integer.MAX_VALUE;
        BlockPos best = null;
        for (BlockPos p : snapshot.structures()) {
            if (!view.isInViewWorld(p.getX(), p.getZ())) continue;
            int x = view.toScreenX(p.getX(), mapX, innerPad, contentW);
            int y = view.toScreenY(p.getZ(), mapY, innerPad, contentH);
            int dx = (int)Math.abs(x - mx);
            int dy = (int)Math.abs(y - my);
            int d2 = dx*dx + dy*dy;
            if (d2 < bestDist) { bestDist = d2; best = p; }
        }
        if (best != null && bestDist <= 64) {
            String name = snapshot.structureName(best);
            String alias = ClientMapNotes.getAlias(best);
            Component coords = Component.translatable("gui.roadweaver.map.coord", best.getX(), best.getZ());
            Component label = alias != null ? Component.literal(alias).append(" ").append(coords)
                    : (name != null ? Component.literal(name).append(" ").append(coords) : coords);
            g.renderTooltip(font, label, (int)mx, (int)my);
        } else {
            int wx = (int)Math.round(view.screenToWorldX(mx, mapX, innerPad, contentW));
            int wz = (int)Math.round(view.screenToWorldZ(my, mapY, innerPad, contentH));
            g.renderTooltip(font, Component.translatable("gui.roadweaver.map.coord", wx, wz), (int)mx, (int)my);
        }
    }

    private static boolean insideMap(double x, double y, int mapX, int mapY, int mapW, int mapH, int innerPad) {
        return x >= mapX + innerPad && x <= mapX + mapW - innerPad && y >= mapY + innerPad && y <= mapY + mapH - innerPad;
    }
}
