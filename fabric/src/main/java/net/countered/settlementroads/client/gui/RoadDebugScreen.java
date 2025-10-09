package net.countered.settlementroads.client.gui;

import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureConnector;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

/**
 * 道路网络调试屏幕（Fabric）
 * - 显示结构/连接/道路
 * - 支持拖拽/缩放/点击传送
 * - 手动连接模式：选择两处结构创建 PLANNED 连接，写入世界数据并入队生成
 */
public class RoadDebugScreen extends Screen {

    private static final int PADDING = 20;

    private final List<BlockPos> structures;
    private final List<Records.StructureConnection> connections;
    private final List<Records.RoadData> roads;

    private final Map<String, Integer> statusColors = Map.of(
            "structure", 0xFF27AE60,
            "planned", 0xFFF2C94C,
            "generating", 0xFFE67E22,
            "completed", 0xFF27AE60,
            "failed", 0xFFE74C3C,
            "road", 0xFF3498DB
    );

    private boolean dragging = false;
    private boolean firstLayout = true;
    private boolean layoutDirty = true;
    private double zoom = 3.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private double baseScale = 1.0;
    private int minX, maxX, minZ, maxZ;

    private int lastWidth = 0;
    private int lastHeight = 0;
    private double lastZoom = 1.0;
    private double lastOffsetX = 0;
    private double lastOffsetY = 0;

    private BlockPos hoveredStructure = null;

    // 手动连接模式
    private boolean manualMode = false;
    private BlockPos manualFirst = null;
    private String toastMessage = null;
    private long toastExpireMs = 0;

    // 渲染组件
    private final MapRenderer mapRenderer;
    private final GridRenderer gridRenderer;
    private final UIRenderer uiRenderer;
    private final ScreenBounds bounds;

    // 按钮
    private Button manualButton;

    public RoadDebugScreen(List<BlockPos> structures,
                           List<Records.StructureConnection> connections,
                           List<Records.RoadData> roads) {
        super(Component.translatable("gui.roadweaver.debug_map.title"));
        this.structures = structures != null ? new ArrayList<>(structures) : new ArrayList<>();
        this.connections = connections != null ? new ArrayList<>(connections) : new ArrayList<>();
        this.roads = roads != null ? new ArrayList<>(roads) : new ArrayList<>();

        if (!this.structures.isEmpty()) {
            minX = this.structures.stream().mapToInt(BlockPos::getX).min().orElse(0);
            maxX = this.structures.stream().mapToInt(BlockPos::getX).max().orElse(0);
            minZ = this.structures.stream().mapToInt(BlockPos::getZ).min().orElse(0);
            maxZ = this.structures.stream().mapToInt(BlockPos::getZ).max().orElse(0);
        }

        this.bounds = new ScreenBounds();
        this.mapRenderer = new MapRenderer(statusColors, bounds);
        this.gridRenderer = new GridRenderer();
        this.uiRenderer = new UIRenderer(statusColors);
    }

    @Override
    protected void init() {
        super.init();
        // 左下角：手动连接模式开关
        int buttonW = 110;
        int buttonH = 16;
        int buttonX = 8;
        int buttonY = this.height - buttonH - 8;
        this.manualButton = Button.builder(getManualModeLabel(), b -> toggleManualMode())
                .bounds(buttonX, buttonY, buttonW, buttonH)
                .build();
        this.addRenderableWidget(this.manualButton);
    }

    private Component getManualModeLabel() {
        Component state = Component.translatable(manualMode ? "gui.roadweaver.common.on" : "gui.roadweaver.common.off");
        return Component.translatable("gui.roadweaver.debug_map.manual_mode", state);
    }

    private void toggleManualMode() {
        manualMode = !manualMode;
        manualFirst = null;
        if (manualButton != null) manualButton.setMessage(getManualModeLabel());
        String msg = Component.translatable(manualMode ? "toast.roadweaver.manual_mode_on" : "toast.roadweaver.manual_mode_off").getString();
        toast(msg, 2000);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 不画默认背景
    }

    private void computeLayout() {
        if (structures.isEmpty()) {
            baseScale = 1.0;
            return;
        }
        int w = width - PADDING * 2;
        int h = height - PADDING * 2;
        if (w <= 0 || h <= 0) return;

        int worldW = Math.max(1, maxX - minX);
        int worldH = Math.max(1, maxZ - minZ);
        double scaleX = (double) w / worldW;
        double scaleY = (double) h / worldH;
        baseScale = Math.min(scaleX, scaleY) * 0.8;

        if (firstLayout) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                double playerX = mc.player.getX();
                double playerZ = mc.player.getZ();
                double playerScreenX = (playerX - minX) * baseScale * zoom;
                double playerScreenZ = (playerZ - minZ) * baseScale * zoom;
                offsetX = w / 2.0 - playerScreenX;
                offsetY = h / 2.0 - playerScreenZ;
            } else {
                offsetX = (w - worldW * baseScale * zoom) / 2;
                offsetY = (h - worldH * baseScale * zoom) / 2;
            }
            firstLayout = false;
        }
        layoutDirty = false;
    }

    private void updateUIBounds() {
        bounds.update(PADDING, width - PADDING, PADDING, height - PADDING);
    }

    private ScreenPos worldToScreen(double worldX, double worldZ) {
        int x = PADDING + (int) ((worldX - minX) * baseScale * zoom + offsetX);
        int y = PADDING + (int) ((worldZ - minZ) * baseScale * zoom + offsetY);
        return new ScreenPos(x, y);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        if (layoutDirty || lastWidth != width || lastHeight != height ||
                lastZoom != zoom || lastOffsetX != offsetX || lastOffsetY != offsetY) {
            computeLayout();
            updateUIBounds();
            lastWidth = width;
            lastHeight = height;
            lastZoom = zoom;
            lastOffsetX = offsetX;
            lastOffsetY = offsetY;
            layoutDirty = false;
        }

        // 背景
        ctx.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        MapRenderer.LODLevel lod = mapRenderer.getLODLevel(zoom);
        MapRenderer.WorldToScreenConverter converter = this::worldToScreen;

        if (lod != MapRenderer.LODLevel.MINIMAL) {
            gridRenderer.drawGrid(ctx, lod, width, height, PADDING,
                    baseScale, zoom, offsetX, offsetY, minX, minZ, bounds);
        }

        mapRenderer.drawRoadPaths(ctx, roads, lod, baseScale, zoom, converter);
        mapRenderer.drawConnections(ctx, connections, roads, lod, converter);
        mapRenderer.drawStructures(ctx, structures, hoveredStructure, lod, converter);
        mapRenderer.drawPlayerMarker(ctx, lod, zoom, converter);

        // UI 面板
        uiRenderer.drawTitle(ctx, width);
        uiRenderer.drawStatsPanel(ctx, width, structures, connections, roads, zoom, baseScale);
        uiRenderer.drawLegendPanel(ctx, height);

        // 渲染默认控件（按钮）
        ctx.pose().pushPose();
        ctx.pose().translate(0, 0, 100);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.pose().popPose();

        if (hoveredStructure != null) {
            uiRenderer.drawTooltip(ctx, hoveredStructure, mouseX, mouseY, width);
        }

        updateHoveredStructure(mouseX, mouseY);

        // Toast
        if (toastMessage != null && System.currentTimeMillis() < toastExpireMs) {
            drawToast(ctx, toastMessage);
        }
    }

    private void drawToast(GuiGraphics ctx, String message) {
        var font = Minecraft.getInstance().font;
        int tw = font.width(message);
        int x = 10;
        int y = 10;
        RenderUtils.drawPanel(ctx, x - 4, y - 4, x + tw + 6, y + 12, 0xC0000000, 0xFF666666);
        ctx.drawString(font, message, x, y, 0xFFFFFFFF, false);
    }

    private void toast(String msg, long durationMs) {
        this.toastMessage = msg;
        this.toastExpireMs = System.currentTimeMillis() + durationMs;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        if (dragging && button == 0) {
            offsetX += dragX;
            offsetY += dragY;
            layoutDirty = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double old = zoom;
        zoom = verticalAmount > 0 ? zoom * 1.1 : zoom / 1.1;
        zoom = Math.max(0.1, Math.min(10.0, zoom));
        offsetX = (offsetX - mouseX + PADDING) * (zoom / old) + mouseX - PADDING;
        offsetY = (offsetY - mouseY + PADDING) * (zoom / old) + mouseY - PADDING;
        layoutDirty = true;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        BlockPos clicked = findClickedStructure(mouseX, mouseY);
        if (clicked != null) {
            if (manualMode) {
                handleManualClick(clicked);
            } else {
                teleportTo(clicked);
            }
            return true;
        }
        dragging = true;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (super.mouseReleased(mouseX, mouseY, button)) return true;
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    private void updateHoveredStructure(int mouseX, int mouseY) {
        hoveredStructure = findClickedStructure(mouseX, mouseY);
    }

    private BlockPos findClickedStructure(double mouseX, double mouseY) {
        for (BlockPos structure : structures) {
            ScreenPos pos = worldToScreen(structure.getX(), structure.getZ());
            double dx = pos.x - mouseX;
            double dy = pos.y - mouseY;
            if (Math.sqrt(dx * dx + dy * dy) <= 7) {
                return structure;
            }
        }
        return null;
    }

    private void handleManualClick(BlockPos clicked) {
        if (manualFirst == null) {
            manualFirst = clicked;
            String pick = Component.translatable("toast.roadweaver.manual_pick_start", clicked.getX(), clicked.getZ()).getString();
            toast(pick, 2000);
        } else if (!manualFirst.equals(clicked)) {
            BlockPos first = manualFirst;
            manualFirst = null;
            createManualConnection(first, clicked);
        }
    }

    private void createManualConnection(BlockPos from, BlockPos to) {
        Minecraft client = Minecraft.getInstance();
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            toast(Component.translatable("toast.roadweaver.manual_multiplayer_not_supported").getString(), 2500);
            return;
        }

        Records.StructureConnection newConn = new Records.StructureConnection(from, to, Records.ConnectionStatus.PLANNED, true);

        // 服务器线程执行：写入世界数据并入队
        server.execute(() -> {
            ServerLevel world = server.overworld();
            WorldDataProvider provider = WorldDataProvider.getInstance();
            List<Records.StructureConnection> list = new ArrayList<>(
                    Optional.ofNullable(provider.getStructureConnections(world)).orElseGet(ArrayList::new)
            );
            
            // 移除已存在的失败连接（如果有）
            list.removeIf(conn -> 
                ((conn.from().equals(from) && conn.to().equals(to)) || 
                 (conn.from().equals(to) && conn.to().equals(from))) &&
                conn.status() == Records.ConnectionStatus.FAILED
            );
            
            // 添加新的计划连接
            list.add(newConn);
            provider.setStructureConnections(world, list);
            StructureConnector.cachedStructureConnections.add(newConn);
        });

        // 立即在客户端侧可视化：移除失败连接，添加新连接
        this.connections.removeIf(conn -> 
            ((conn.from().equals(from) && conn.to().equals(to)) || 
             (conn.from().equals(to) && conn.to().equals(from))) &&
            conn.status() == Records.ConnectionStatus.FAILED
        );
        this.connections.add(newConn);
        toast(Component.translatable("toast.roadweaver.manual_created").getString(), 2000);
    }

    private void teleportTo(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        var server = mc.getSingleplayerServer();
        String command = "/tp " + mc.player.getName().getString() + " " + pos.getX() + " ~ " + pos.getZ();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // --- 辅助类型 ---
    public record ScreenPos(int x, int y) {}

    public static class ScreenBounds {
        private int left, right, top, bottom;
        public void update(int left, int right, int top, int bottom) {
            this.left = left; this.right = right; this.top = top; this.bottom = bottom;
        }
        public boolean isInBounds(int x, int y, int margin) {
            return x >= left - margin && x <= right + margin && y >= top - margin && y <= bottom + margin;
        }
        public boolean isLineInBounds(int x1, int y1, int x2, int y2) {
            if ((x1 < left && x2 < left) || (x1 > right && x2 > right) || (y1 < top && y2 < top) || (y1 > bottom && y2 > bottom)) return false;
            return true;
        }
        public int left() { return left; }
        public int right() { return right; }
        public int top() { return top; }
        public int bottom() { return bottom; }
    }
}
