package net.shiroha233.roadweaver.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaver.network.ClientNetBridge;
import net.minecraft.core.BlockPos;


public class RoadMapScreen extends Screen {
    private static final ResourceLocation MAP_TEXTURE = new ResourceLocation("roadweaver", "textures/gui/map.png");
    private static final int TEX_W = 1536;
    private static final int TEX_H = 1024;

    private static final int COLOR_TEXT = 0xFF5E3D1E; 
    private static final int COLOR_STRUCT = 0xFF5E3D1E;
    private static final int COLOR_PLANNED = 0xFF4CAF50;
    private static final int COLOR_GENERATING = 0xFF000000;
    private static final int COLOR_COMPLETED = 0xFF000000;
    private static final int COLOR_FAILED = 0xE0E05B50;
    private static final int COLOR_GRID = 0x30999999;
    private static final int GRID_TARGET_PX = 32;
    private static final Component MENU_TELEPORT = Component.translatable("gui.roadweaver.map.menu.teleport");
    private static final Component BTN_CONFIG = Component.translatable("gui.roadweaver.config_button");
    private static final int MENU_BG = 0xF0101010;
    private static final int MENU_BORDER = 0xFFFFFFFF;
    private static final int MENU_HOVER = 0x40FFFFFF;
    private static final int MENU_TEXT = 0xFFFFFFFF;
    private static final int MENU_MIN_W = 0;
    private static final int MENU_ITEM_H = 14;
    private static final int MENU_PAD_X = 6;
    private static final int MENU_PAD_Y = 4;

    private MapSnapshot snapshot = MapSnapshot.empty();

    private int mapX, mapY, mapW, mapH;
    private static final int OUTER_PAD = 36;
    private static final int INNER_PAD = 25;
    private final MapView view = new MapView();

    private boolean dragging;
    private int dragButton;
    private double lastMouseX, lastMouseY;
    private boolean debounceZoomPending;
    private long debounceZoomDeadlineMs;

    private boolean showContextMenu;
    private int menuX, menuY;
    private BlockPos menuTarget;

    public RoadMapScreen() {
        super(Component.translatable("gui.roadweaver.map.title"));
    }

    @Override
    protected void init() {
        super.init();
        computeMapRect();
        int contentW = mapW - INNER_PAD * 2;
        int contentH = mapH - INNER_PAD * 2;
        view.resetFromSnapshot(snapshot);
        Minecraft mc = this.minecraft;
        if (mc != null && mc.player != null) {
            view.calibrateInitialToPlayer(mc, contentW, contentH, GRID_TARGET_PX);
        }
        requestCurrentView();
    }

    private void computeMapRect() {
        int availW = this.width - OUTER_PAD * 2;
        int availH = this.height - OUTER_PAD * 2;
        float ratio = (float) TEX_W / TEX_H;
        int w = availW;
        int h = Math.round(w / ratio);
        if (h > availH) {
            h = availH;
            w = Math.round(h * ratio);
        }
        mapW = w;
        mapH = h;
        mapX = (this.width - w) / 2;
        mapY = (this.height - h) / 2;
    }

    private boolean insideMap(double x, double y) {
        return x >= mapX + INNER_PAD && x <= mapX + mapW - INNER_PAD && y >= mapY + INNER_PAD && y <= mapY + mapH - INNER_PAD;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        g.blit(MAP_TEXTURE, mapX, mapY, mapW, mapH, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        int titleY = mapY - 8;
        g.drawCenteredString(this.font, this.getTitle(), this.width / 2, Math.max(6, titleY), COLOR_TEXT);

        int contentW = mapW - INNER_PAD * 2;
        int contentH = mapH - INNER_PAD * 2;
        view.lockAspect(contentW, contentH);

        int left = mapX + INNER_PAD;
        int top = mapY + INNER_PAD;
        int right = mapX + mapW - INNER_PAD;
        int bottom = mapY + mapH - INNER_PAD;
        g.enableScissor(left, top, right, bottom);
        MapRenderers.renderGrid(g, this.font, mapX, mapY, mapW, mapH, INNER_PAD,
                view.getMinX(), view.getMaxX(), view.getMinZ(), view.getMaxZ(), COLOR_GRID, GRID_TARGET_PX, COLOR_TEXT);

        int thickness = computeThickness();
        java.util.List<net.shiroha233.roadweaver.helpers.Records.StructureConnection> connForLines = new java.util.ArrayList<>(snapshot.connections());
        connForLines.removeIf(c -> c.status() == net.shiroha233.roadweaver.helpers.Records.ConnectionStatus.COMPLETED);
        MapRenderers.renderConnections(
                g,
                connForLines,
                (x1, z1, x2, z2) -> view.segmentInViewWorld(x1, z1, x2, z2),
                v -> view.toScreenX(v, mapX, INNER_PAD, contentW),
                v -> view.toScreenY(v, mapY, INNER_PAD, contentH),
                thickness,
                COLOR_PLANNED, COLOR_GENERATING, COLOR_COMPLETED, COLOR_FAILED,
                left, top, right, bottom
        );

        int lodStep = GridRenderer.computeGridStep(mapX, mapY, mapW, mapH,
                INNER_PAD,
                view.getMinX(), view.getMaxX(), view.getMinZ(), view.getMaxZ(),
                GRID_TARGET_PX);
        MapRenderers.renderRoadPolylines(
                g,
                snapshot.roadPolylines(),
                (x1, z1, x2, z2) -> view.segmentInViewWorld(x1, z1, x2, z2),
                v -> view.toScreenX(v, mapX, INNER_PAD, contentW),
                v -> view.toScreenY(v, mapY, INNER_PAD, contentH),
                thickness,
                COLOR_COMPLETED,
                left, top, right, bottom,
                lodStep
        );

        
        MapRenderers.renderStructures(
                g,
                snapshot.structures(),
                v -> view.toScreenX(v, mapX, INNER_PAD, contentW),
                v -> view.toScreenY(v, mapY, INNER_PAD, contentH),
                (x, z) -> view.isInViewWorld(x, z),
                computePointSize(),
                COLOR_STRUCT,
                left, top, right, bottom
        );
        if (!showContextMenu) {
            MapInteraction.renderHoverHighlight(g, snapshot, view, mapX, mapY, mapW, mapH, INNER_PAD, mouseX, mouseY);
        }
        renderPlayer(g);
        g.disableScissor();
        int legendRight = mapX + mapW - INNER_PAD;
        int legendStartY = mapY + INNER_PAD + 8;
        int gap = 8;
        MapRenderers.renderLegend(
                g, this.font,
                legendRight, legendStartY, gap,
                COLOR_TEXT, COLOR_STRUCT, COLOR_PLANNED, COLOR_GENERATING, COLOR_COMPLETED, COLOR_FAILED,
                snapshot.structuresCount(), snapshot.plannedCount(), snapshot.generatingCount(), snapshot.completedCount(), snapshot.failedCount()
        );
        renderConfigButton(g, mouseX, mouseY);
        if (!showContextMenu) {
            MapInteraction.renderHoverTooltip(g, this.font, snapshot, view, mapX, mapY, mapW, mapH, INNER_PAD, mouseX, mouseY);
        }

        if (debounceZoomPending && System.currentTimeMillis() >= debounceZoomDeadlineMs) {
            debounceZoomPending = false;
            requestCurrentView();
        }

        if (showContextMenu && menuTarget != null) {
            renderContextMenu(g, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }


    private int computeThickness() {
        int contentW = mapW - INNER_PAD * 2;
        int contentH = mapH - INNER_PAD * 2;
        double ppb = Math.min(view.pxPerBlockX(contentW), view.pxPerBlockZ(contentH));
        int t = (int)Math.round(ppb);
        if (t < 1) t = 1;
        if (t > 4) t = 4;
        return t;
    }

    private int computePointSize() { return 2 + computeThickness(); }

    


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void setSnapshot(MapSnapshot snapshot) {
        if (snapshot != null) {
            this.snapshot = snapshot;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!insideMap(mouseX, mouseY)) return super.mouseScrolled(mouseX, mouseY, delta);
        int contentW = mapW - INNER_PAD * 2;
        int contentH = mapH - INNER_PAD * 2;
        double factor = delta > 0 ? 0.9 : 1.1;
        double cx = view.screenToWorldX(mouseX, mapX, INNER_PAD, contentW);
        double cz = view.screenToWorldZ(mouseY, mapY, INNER_PAD, contentH);
        view.applyZoomAround(cx, cz, factor, contentW, contentH, GRID_TARGET_PX);
        debounceZoomPending = true;
        debounceZoomDeadlineMs = System.currentTimeMillis() + 500;
        showContextMenu = false;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && insideConfigButton((int)mouseX, (int)mouseY)) {
            openConfig();
            return true;
        }
        if (showContextMenu) {
            int[] bounds = computeMenuBounds();
            int bx = bounds[0], by = bounds[1], bw = bounds[2], bh = bounds[3];
            boolean inside = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
            if (inside && button == 0) {
                int idx = getMenuHoverIndex((int)mouseX, (int)mouseY);
                if (idx == 0) {
                    onTeleportSelected();
                    showContextMenu = false;
                    return true;
                }
            } else {
                showContextMenu = false;
                // fallthrough to other handling if needed
            }
        }

        if (insideMap(mouseX, mouseY) && button == 0) {
            dragging = true;
            dragButton = button;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            debounceZoomPending = false;
            showContextMenu = false;
            return true;
        }
        if (insideMap(mouseX, mouseY) && button == 1) {
            BlockPos best = findNearestStructure(mouseX, mouseY);
            if (best != null) {
                menuTarget = best;
                menuX = (int) mouseX;
                menuY = (int) mouseY;
                showContextMenu = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == dragButton) {
            int contentW = mapW - INNER_PAD * 2;
            int contentH = mapH - INNER_PAD * 2;
            double dx = mouseX - lastMouseX;
            double dy = mouseY - lastMouseY;
            view.panByScreenDelta(dx, dy, contentW, contentH);
            lastMouseX = mouseX; lastMouseY = mouseY;
            view.lockAspect(contentW, contentH);
            showContextMenu = false;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == dragButton) {
            dragging = false;
            view.clampZoom(mapW - INNER_PAD * 2, mapH - INNER_PAD * 2, GRID_TARGET_PX);
            debounceZoomPending = false;
            requestCurrentView();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    

    private void renderPlayer(GuiGraphics g) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        double wx = this.minecraft.player.getX();
        double wz = this.minecraft.player.getZ();
        int sx = view.toScreenX((int)Math.round(wx), mapX, INNER_PAD, mapW - INNER_PAD * 2);
        int sy = view.toScreenY((int)Math.round(wz), mapY, INNER_PAD, mapH - INNER_PAD * 2);
        if (!insideMap(sx, sy)) return;
        float yaw = this.minecraft.player.getYRot();
        int left = mapX + INNER_PAD, right = mapX + mapW - INNER_PAD;
        int top = mapY + INNER_PAD, bottom = mapY + mapH - INNER_PAD;
        MapRenderers.drawPlayerArrow(
                g,
                sx, sy,
                yaw,
                10, 6, 4,
                0xFF000000,
                left, top, right, bottom,
                view.pxPerBlockX(mapW - INNER_PAD * 2),
                view.pxPerBlockZ(mapH - INNER_PAD * 2)
        );
    }

    private void requestCurrentView() {
        int minX = (int)Math.floor(Math.min(view.getMinX(), view.getMaxX()));
        int maxX = (int)Math.ceil(Math.max(view.getMinX(), view.getMaxX()));
        int minZ = (int)Math.floor(Math.min(view.getMinZ(), view.getMaxZ()));
        int maxZ = (int)Math.ceil(Math.max(view.getMinZ(), view.getMaxZ()));

        // 适度扩展边界，减少边缘拖拽时的频繁请求
        int pad = 32;
        minX -= pad; maxX += pad; minZ -= pad; maxZ += pad;

        Minecraft mc = this.minecraft;
        if (mc == null) return;
        MinecraftServer server = mc.getSingleplayerServer();
        if (server != null) {
            ServerLevel level = server.getLevel(Level.OVERWORLD);
            if (level != null) {
                int cx = 0;
                int cz = 0;
                if (mc.player != null) {
                    cx = (int) Math.round(mc.player.getX());
                    cz = (int) Math.round(mc.player.getZ());
                }
                int radiusChunks;
                try {
                    net.shiroha233.roadweaver.config.ModConfig cfg = net.shiroha233.roadweaver.config.ConfigService.get();
                    radiusChunks = (cfg.dynamicPlanEnabled() ? cfg.dynamicPlanRadiusChunks() : cfg.initialPlanRadiusChunks());
                } catch (Throwable t) {
                    radiusChunks = 256;
                }
                int radiusBlocks = Math.max(1, radiusChunks) * 16;
                setSnapshot(MapDataCollector.build(level, minX, minZ, maxX, maxZ, cx, cz, radiusBlocks));
            }
        } else {
            ClientNetBridge.requestSnapshot(minX, minZ, maxX, maxZ);
        }

    }

    private BlockPos findNearestStructure(double mouseX, double mouseY) {
        int contentW = mapW - INNER_PAD * 2;
        int contentH = mapH - INNER_PAD * 2;
        int bestDist = Integer.MAX_VALUE;
        BlockPos best = null;
        for (BlockPos p : snapshot.structures()) {
            if (!view.isInViewWorld(p.getX(), p.getZ())) continue;
            int x = view.toScreenX(p.getX(), mapX, INNER_PAD, contentW);
            int y = view.toScreenY(p.getZ(), mapY, INNER_PAD, contentH);
            int dx = (int)Math.abs(x - mouseX);
            int dy = (int)Math.abs(y - mouseY);
            int d2 = dx * dx + dy * dy;
            if (d2 < bestDist) { bestDist = d2; best = p; }
        }
        if (best != null && bestDist <= 64) return best;
        return null;
    }

    private int[] computeMenuBounds() {
        int cnt = 1;
        int textW = this.font.width(MENU_TELEPORT);
        int w = Math.max(MENU_MIN_W, textW + MENU_PAD_X * 2);
        int h = MENU_PAD_Y * 2 + MENU_ITEM_H * cnt;
        int x = menuX + 12;
        int y = menuY - 12;
        if (x + w > this.width) x = this.width - w - 4;
        if (y + h > this.height) y = this.height - h - 4;
        if (x < 4) x = 4;
        if (y < 4) y = 4;
        return new int[]{x, y, w, h};
    }

    private int getMenuHoverIndex(int mx, int my) {
        int[] b = computeMenuBounds();
        int x = b[0], y = b[1], w = b[2], h = b[3];
        if (mx < x || mx > x + w || my < y || my > y + h) return -1;
        int innerTop = y + MENU_PAD_Y;
        if (my < innerTop) return -1;
        int rel = my - innerTop;
        int idx = rel / MENU_ITEM_H;
        if (idx < 0) return -1;
        if (idx > 0) return -1;
        return 0;
    }

    private void renderContextMenu(GuiGraphics g, int mouseX, int mouseY) {
        int[] b = computeMenuBounds();
        int x = b[0], y = b[1], w = b[2], h = b[3];
        int shadow = 0x80101010;
        g.fill(x + 3, y + 3, x + w + 3, y + h + 3, shadow);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, MENU_BORDER);
        g.fill(x, y, x + w, y + h, MENU_BG);
        int hover = getMenuHoverIndex(mouseX, mouseY);
        int itemTop = y + MENU_PAD_Y;
        if (hover == 0) g.fill(x + 1, itemTop, x + w - 1, itemTop + MENU_ITEM_H, MENU_HOVER);
        g.fill(x + 1, itemTop + MENU_ITEM_H, x + w - 1, itemTop + MENU_ITEM_H + 1, MENU_BORDER & 0x40FFFFFF);
        int ty = itemTop + (MENU_ITEM_H - this.font.lineHeight) / 2;
        g.drawString(this.font, MENU_TELEPORT, x + MENU_PAD_X, ty, MENU_TEXT, false);
        int baseY = itemTop + MENU_ITEM_H / 2;
        int tipX = x - 6;
        int tipY = baseY;
        int bx1 = x - 1, by1 = baseY - 4;
        int bx2 = x - 1, by2 = baseY + 4;
        RenderUtils.fillTriangle(g, tipX, tipY, bx1, by1, bx2, by2, MENU_BORDER, 0, 0, this.width, this.height);
        RenderUtils.fillTriangle(g, tipX + 1, tipY, bx1, by1 + 1, bx2, by2 - 1, MENU_BG, 0, 0, this.width, this.height);
    }

    private void onTeleportSelected() {
        if (menuTarget == null) return;
        ClientNetBridge.requestTeleport(menuTarget.getX(), menuTarget.getY(), menuTarget.getZ());
    }

    private int[] computeConfigBtnBounds() {
        int x = mapX + INNER_PAD + 4;
        int y = mapY + INNER_PAD + 4;
        int w = this.font.width(BTN_CONFIG) + 6;
        int h = this.font.lineHeight + 4;
        return new int[]{x, y, w, h};
    }

    private boolean insideConfigButton(int mx, int my) {
        int[] b = computeConfigBtnBounds();
        int x = b[0], y = b[1], w = b[2], h = b[3];
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void renderConfigButton(GuiGraphics g, int mouseX, int mouseY) {
        int[] b = computeConfigBtnBounds();
        int x = b[0], y = b[1], h = b[3];
        int ty = y + (h - this.font.lineHeight) / 2;
        g.drawString(this.font, BTN_CONFIG, x + 3, ty, COLOR_TEXT, false);
        if (insideConfigButton(mouseX, mouseY)) {
            int textW = this.font.width(BTN_CONFIG);
            int uy = ty + this.font.lineHeight + 1;
            int underline = (COLOR_TEXT & 0x00FFFFFF) | 0x60000000;
            g.fill(x + 2, uy, x + 2 + textW + 2, uy + 1, underline);
        }
    }

    private void openConfig() {
        if (this.minecraft == null) return;
        Screen next = null;
        try {
            Class<?> c = Class.forName("net.shiroha233.roadweaver.client.fabric.ConfigScreenFactoryImpl");
            next = (Screen) c.getMethod("createConfigScreen", Screen.class).invoke(null, this);
        } catch (Throwable ignored) {}
        if (next == null) {
            try {
                Class<?> c = Class.forName("net.shiroha233.roadweaver.client.forge.ConfigScreenFactoryImpl");
                next = (Screen) c.getMethod("createConfigScreen", Screen.class).invoke(null, this);
            } catch (Throwable ignored) {}
        }
        if (next != null) this.minecraft.setScreen(next);
    }
}
