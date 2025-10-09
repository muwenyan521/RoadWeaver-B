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
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * 道路网络调试屏幕 - NeoForge版本（重构版）
 * 功能: 显示结构节点、道路连接、支持平移/缩放、点击传送
 * 包含LOD系统、高级渲染、性能优化
 * 
 * 修复内容：
 * 1. 计划道路现在正确连接结构位置（使用虚线）
 * 2. 优化LOD系统，改善缩小时的精度
 * 3. 模块化代码结构，便于维护
 * 4. 手动连接模式：选择两处结构创建 PLANNED 连接，写入世界数据并入队生成
 */
public class RoadDebugScreen extends Screen {

    private static final int PADDING = 20;

    private final List<BlockPos> structures;
    private final List<Records.StructureConnection> connections;
    private final List<Records.RoadData> roads;

    private final Map<String, Integer> statusColors = Map.of(
            "structure", 0xFF27AE60,   // 绿色 - 结构
            "planned", 0xFFF2C94C,     // 黄色 - 计划中
            "generating", 0xFFE67E22,  // 橙色 - 生成中
            "completed", 0xFF27AE60,   // 绿色 - 已完成（不显示）
            "failed", 0xFFE74C3C,      // 红色 - 生成失败
            "road", 0xFF3498DB         // 蓝色 - 道路
    );

    private boolean dragging = false;
    private boolean firstLayout = true;
    private boolean layoutDirty = true;
    private double zoom = 3.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private double baseScale = 1.0;
    private int minX, maxX, minZ, maxZ;
    
    // 性能优化缓存
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
    
    // 拖动检测
    private double mouseDownX = 0;
    private double mouseDownY = 0;
    private boolean hasDragged = false;
    
    // 传送确认
    private BlockPos pendingTeleport = null;
    private long teleportConfirmExpireMs = 0;
    
    // 渲染器
    private final MapRenderer mapRenderer;
    private final GridRenderer gridRenderer;
    private final UIRenderer uiRenderer;
    private final ScreenBounds bounds;
    
    // 按钮
    private Button configButton;
    private Button manualButton;

    public RoadDebugScreen(List<BlockPos> structures,
                           List<Records.StructureConnection> connections,
                           List<Records.RoadData> roads) {
        super(Component.translatable("gui.roadweaver.debug_map.title"));
        // 创建不可变副本，避免并发修改异常
        this.structures = structures != null ? new ArrayList<>(structures) : new ArrayList<>();
        this.connections = connections != null ? new ArrayList<>(connections) : new ArrayList<>();
        this.roads = roads != null ? new ArrayList<>(roads) : new ArrayList<>();

        if (!this.structures.isEmpty()) {
            minX = this.structures.stream().mapToInt(BlockPos::getX).min().orElse(0);
            maxX = this.structures.stream().mapToInt(BlockPos::getX).max().orElse(0);
            minZ = this.structures.stream().mapToInt(BlockPos::getZ).min().orElse(0);
            maxZ = this.structures.stream().mapToInt(BlockPos::getZ).max().orElse(0);
        }
        
        // 初始化渲染器
        this.bounds = new ScreenBounds();
        this.mapRenderer = new MapRenderer(statusColors, bounds);
        this.gridRenderer = new GridRenderer();
        this.uiRenderer = new UIRenderer(statusColors);
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 在右上角添加配置按钮（透明背景，不遮挡地图）
        int buttonWidth = 50;
        int buttonHeight = 16;
        int buttonX = this.width - buttonWidth - 8;
        int buttonY = 8;
        
        this.configButton = new Button.Builder(
                Component.translatable("gui.roadweaver.config"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(ClothConfigScreen.createConfigScreen(this));
                    }
                })
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build();
        
        this.addRenderableWidget(this.configButton);
        
        // 左下角：手动连接模式开关
        int manualButtonW = 110;
        int manualButtonH = 16;
        int manualButtonX = 8;
        int manualButtonY = this.height - manualButtonH - 8;
        this.manualButton = Button.builder(getManualModeLabel(), b -> toggleManualMode())
                .bounds(manualButtonX, manualButtonY, manualButtonW, manualButtonH)
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
    public void renderBackground(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 不渲染默认背景，保持地图清晰
    }
    
    private void computeLayout() {
        if (structures.isEmpty()) {
            baseScale = 1.0;
            return;
        }
        
        int w = width - PADDING * 2;
        int h = height - PADDING * 2;
        
        if (w <= 0 || h <= 0) return;
        
        int worldW = maxX - minX;
        int worldH = maxZ - minZ;
        
        if (worldW <= 0 || worldH <= 0) {
            baseScale = 1.0;
            return;
        }
        
        double scaleX = (double) w / worldW;
        double scaleY = (double) h / worldH;
        baseScale = Math.min(scaleX, scaleY) * 0.8;
        
        if (firstLayout) {
            // 修复：初始中心位置为玩家当前位置
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                double playerX = mc.player.getX();
                double playerZ = mc.player.getZ();
                
                // 计算玩家在世界坐标系中的位置，然后转换为屏幕偏移
                double playerScreenX = (playerX - minX) * baseScale * zoom;
                double playerScreenZ = (playerZ - minZ) * baseScale * zoom;
                
                // 将玩家位置居中
                offsetX = w / 2.0 - playerScreenX;
                offsetY = h / 2.0 - playerScreenZ;
            } else {
                // 回退到原来的居中逻辑
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
        // 只有在必要时才重新计算布局和UI边界
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
        
        // 深色半透明背景
        ctx.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        
        // 获取当前LOD级别
        MapRenderer.LODLevel lod = mapRenderer.getLODLevel(zoom);
        
        // 创建坐标转换器
        MapRenderer.WorldToScreenConverter converter = this::worldToScreen;
        
        // 根据LOD级别绘制各种元素
        if (lod != MapRenderer.LODLevel.MINIMAL) {
            gridRenderer.drawGrid(ctx, lod, width, height, PADDING, 
                baseScale, zoom, offsetX, offsetY, minX, minZ, bounds);
        }
        
        // 绘制道路路径（实际路径）
        mapRenderer.drawRoadPaths(ctx, roads, lod, baseScale, zoom, converter);
        
        // 绘制连接线（计划中的道路显示为虚线）
        mapRenderer.drawConnections(ctx, connections, roads, lod, converter);
        
        // 绘制结构节点
        mapRenderer.drawStructures(ctx, structures, hoveredStructure, manualFirst, lod, converter);
        
        // 绘制玩家标记（始终显示）
        mapRenderer.drawPlayerMarker(ctx, lod, zoom, converter);
        
        // UI元素
        uiRenderer.drawTitle(ctx, width);
        uiRenderer.drawStatsPanel(ctx, width, structures, connections, roads, zoom, baseScale);
        uiRenderer.drawLegendPanel(ctx, height);
        
        // 渲染按钮 - 使用 pose stack 确保在最上层且清晰
        ctx.pose().pushPose();
        ctx.pose().translate(0, 0, 100); // 提升到最上层
        super.render(ctx, mouseX, mouseY, delta);
        ctx.pose().popPose();
        
        // 工具提示最后渲染（确保在最上层）
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 检查是否按下了调试地图按键（默认 H 键）
        if (this.minecraft != null && this.minecraft.options.keyMappings != null) {
            for (var keyMapping : this.minecraft.options.keyMappings) {
                if (keyMapping.getName().equals("key.roadweaver.debug_map") && 
                    keyMapping.matches(keyCode, scanCode)) {
                    this.onClose();
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 优先处理按钮拖拽（虽然按钮不需要拖拽，但要防止穿透）
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        
        if (dragging && button == 0) {
            // 检测是否真的在拖动（移动超过3像素）
            double totalDrag = Math.abs(mouseX - mouseDownX) + Math.abs(mouseY - mouseDownY);
            if (totalDrag > 3) {
                hasDragged = true;
            }
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
        // 优先处理按钮点击，防止穿透
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        if (button != 0) return false;

        // 记录鼠标按下位置
        mouseDownX = mouseX;
        mouseDownY = mouseY;
        hasDragged = false;
        dragging = true;
        return true;
    }
    
    private void handleManualClick(BlockPos clicked) {
        if (manualFirst == null) {
            manualFirst = clicked;
            String pick = Component.translatable("toast.roadweaver.manual_pick_start", clicked.getX(), clicked.getZ()).getString();
            toast(pick, 2000);
        } else if (manualFirst.equals(clicked)) {
            // 再次点击同一个结构：取消选中
            manualFirst = null;
            String cancel = Component.translatable("toast.roadweaver.manual_cancelled").getString();
            toast(cancel, 2000);
        } else {
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
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // 优先处理按钮释放事件
        if (super.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        
        if (button == 0 && dragging) {
            dragging = false;
            
            // 只有在没有拖动时才处理点击
            if (!hasDragged) {
                BlockPos clicked = findClickedStructure(mouseX, mouseY);
                if (clicked != null) {
                    if (manualMode) {
                        handleManualClick(clicked);
                    } else {
                        // 传送确认逻辑
                        if (pendingTeleport != null && pendingTeleport.equals(clicked) && 
                            System.currentTimeMillis() < teleportConfirmExpireMs) {
                            // 确认传送
                            teleportTo(clicked);
                            pendingTeleport = null;
                        } else {
                            // 第一次点击：请求确认
                            pendingTeleport = clicked;
                            teleportConfirmExpireMs = System.currentTimeMillis() + 3000;
                            String confirm = Component.translatable("toast.roadweaver.teleport_confirm", 
                                clicked.getX(), clicked.getZ()).getString();
                            toast(confirm, 3000);
                        }
                    }
                    return true;
                }
            }
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
            if (Math.sqrt(dx * dx + dy * dy) <= 5 + 2) {
                return structure;
            }
        }
        return null;
    }
    
    private void teleportTo(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.hasSingleplayerServer()) return;
        
        Player player = mc.player;
        String command = "/tp " + player.getName().getString() + " " + pos.getX() + " ~ " + pos.getZ();
        
        if (mc.getSingleplayerServer() != null) {
            mc.getSingleplayerServer().getCommands().performPrefixedCommand(
                mc.getSingleplayerServer().createCommandSourceStack(), command);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    /**
     * 屏幕坐标记录
     */
    public record ScreenPos(int x, int y) {}
    
    /**
     * 屏幕边界类
     */
    public static class ScreenBounds {
        private int left, right, top, bottom;
        
        public void update(int left, int right, int top, int bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
        
        public boolean isInBounds(int x, int y, int margin) {
            return x >= left - margin && x <= right + margin && 
                   y >= top - margin && y <= bottom + margin;
        }
        
        public boolean isLineInBounds(int x1, int y1, int x2, int y2) {
            if ((x1 < left && x2 < left) || (x1 > right && x2 > right) ||
                (y1 < top && y2 < top) || (y1 > bottom && y2 > bottom)) {
                return false;
            }
            return true;
        }
        
        public int left() { return left; }
        public int right() { return right; }
        public int top() { return top; }
        public int bottom() { return bottom; }
    }
}
