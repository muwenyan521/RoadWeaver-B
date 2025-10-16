package net.shiroha233.roadweaver.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.client.gui.components.InfoPanel;
import net.shiroha233.roadweaver.client.gui.core.MapViewport;
import net.shiroha233.roadweaver.client.gui.layers.*;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * RoadWeaver地图主屏幕 v2.1（LOD优化版）
 * <p>
 * 全新设计的现代化地图界面，采用分层渲染架构。
 * 支持流畅的缩放、平移、动画效果等。
 * </p>
 * 
 * <h3>性能优化特性：</h3>
 * <ul>
 *   <li>LOD系统：根据缩放级别动态调整渲染细节</li>
 *   <li>视锥剔除：只渲染可见区域内的元素</li>
 *   <li>快速圆形渲染：使用优化算法提升4倍性能</li>
 * </ul>
 * 
 * @author RoadWeaver Team
 * @version 2.1
 * @since 2025-10-17
 */
public class RoadWeaverMapScreen extends Screen {
    
    // 数据
    private final List<Records.StructureInfo> structures;
    private final List<Records.StructureConnection> connections;
    private final List<Records.RoadData> roads;
    
    // 核心组件
    private MapViewport viewport;
    private List<net.shiroha233.roadweaver.client.gui.core.RenderLayer> layers;
    
    // UI组件
    private InfoPanel infoPanel;
    private Button closeButton;
    private Button refreshButton;
    
    // 交互状态
    private boolean dragging = false;
    private double dragStartX, dragStartY;
    private long lastFrameTime;
    
    // ==================== 常量定义 ====================
    
    /** 关闭按钮宽度 */
    private static final int CLOSE_BUTTON_WIDTH = 20;
    
    /** 关闭按钮高度 */
    private static final int CLOSE_BUTTON_HEIGHT = 20;
    
    /** 刷新按钮宽度 */
    private static final int REFRESH_BUTTON_WIDTH = 60;
    
    /** 刷新按钮高度 */
    private static final int REFRESH_BUTTON_HEIGHT = 20;
    
    /** 按钮顶部边距 */
    private static final int BUTTON_TOP_MARGIN = 8;
    
    /** 关闭按钮右边距 */
    private static final int CLOSE_BUTTON_RIGHT_MARGIN = 30;
    
    /** 刷新按钮右边距 */
    private static final int REFRESH_BUTTON_RIGHT_MARGIN = 140;
    
    /** 信息面板宽度 */
    private static final int INFO_PANEL_WIDTH = 180;
    
    /** 信息面板右边距 */
    private static final int INFO_PANEL_RIGHT_MARGIN = 180;
    
    /** 信息面板顶部边距 */
    private static final int INFO_PANEL_TOP_MARGIN = 40;
    
    /** 最大帧时间（秒） */
    private static final float MAX_FRAME_TIME = 0.1f;
    
    /** 视口边距 */
    private static final int VIEWPORT_PADDING = 20;
    
    public RoadWeaverMapScreen() {
        super(Component.translatable("gui.roadweaver.debug_map.title"));
        
        // 加载数据
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            ServerLevel world = mc.getSingleplayerServer().overworld();
            WorldDataProvider provider = WorldDataProvider.getInstance();
            
            Records.StructureLocationData data = provider.getStructureLocations(world);
            this.structures = data != null && data.structureInfos() != null ? 
                new ArrayList<>(data.structureInfos()) : new ArrayList<>();
            
            this.connections = provider.getStructureConnections(world) != null ?
                new ArrayList<>(provider.getStructureConnections(world)) : new ArrayList<>();
                
            this.roads = provider.getRoadDataList(world) != null ?
                new ArrayList<>(provider.getRoadDataList(world)) : new ArrayList<>();
        } else {
            this.structures = new ArrayList<>();
            this.connections = new ArrayList<>();
            this.roads = new ArrayList<>();
        }
        
        this.lastFrameTime = System.currentTimeMillis();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 初始化视口
        viewport = new MapViewport(VIEWPORT_PADDING);
        viewport.updateScreenSize(width, height);
        
        if (!structures.isEmpty()) {
            int minX = structures.stream().mapToInt(i -> i.pos().getX()).min().orElse(0);
            int maxX = structures.stream().mapToInt(i -> i.pos().getX()).max().orElse(0);
            int minZ = structures.stream().mapToInt(i -> i.pos().getZ()).min().orElse(0);
            int maxZ = structures.stream().mapToInt(i -> i.pos().getZ()).max().orElse(0);
            viewport.updateWorldBounds(minX, maxX, minZ, maxZ);
            
            // 居中到玩家位置
            if (minecraft != null && minecraft.player != null) {
                viewport.initialize(minecraft.player.blockPosition());
            } else {
                viewport.initialize(null);
            }
        }
        
        // 初始化渲染层
        layers = new ArrayList<>();
        layers.add(new BackgroundLayer(viewport));
        layers.add(new RoadPathLayer(viewport, roads));
        layers.add(new ConnectionLayer(viewport, connections));
        layers.add(new StructureLayer(viewport, structures));
        layers.add(new OverlayLayer(viewport));
        
        // 排序层（按zIndex）
        layers.sort((a, b) -> Integer.compare(a.getZIndex(), b.getZIndex()));
        
        // 创建UI组件
        infoPanel = new InfoPanel(
            width - INFO_PANEL_RIGHT_MARGIN, 
            INFO_PANEL_TOP_MARGIN, 
            structures, 
            connections, 
            roads, 
            viewport.getZoom()
        );
        
        // 关闭按钮
        closeButton = Button.builder(Component.literal("✕"), btn -> onClose())
            .bounds(
                width - CLOSE_BUTTON_RIGHT_MARGIN, 
                BUTTON_TOP_MARGIN, 
                CLOSE_BUTTON_WIDTH, 
                CLOSE_BUTTON_HEIGHT
            )
            .build();
        addRenderableWidget(closeButton);
        
        // 刷新按钮
        refreshButton = Button.builder(Component.translatable("gui.roadweaver.debug_map.refresh"), btn -> refreshData())
            .bounds(
                width - REFRESH_BUTTON_RIGHT_MARGIN, 
                BUTTON_TOP_MARGIN, 
                REFRESH_BUTTON_WIDTH, 
                REFRESH_BUTTON_HEIGHT
            )
            .build();
        addRenderableWidget(refreshButton);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 计算deltaTime
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastFrameTime) / 1000.0f, MAX_FRAME_TIME);
        lastFrameTime = currentTime;
        
        // 更新视口
        viewport.update();
        
        // 渲染所有层
        for (var layer : layers) {
            if (layer.isEnabled()) {
                layer.update(deltaTime);
                layer.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        
        // 渲染UI组件
        infoPanel.update(deltaTime);
        infoPanel.render(graphics, mouseX, mouseY, partialTick);
        
        // 渲染标题
        renderTitle(graphics);
        
        // 渲染按钮
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // 渲染提示
        renderTooltip(graphics, mouseX, mouseY);
    }
    
    private void renderTitle(GuiGraphics graphics) {
        var font = Minecraft.getInstance().font;
        String title = Component.translatable("gui.roadweaver.debug_map.title").getString();
        int titleWidth = font.width(title);
        int titleX = (width - titleWidth) / 2;
        graphics.drawString(font, title, titleX, 12, 0xFFFFFFFF, true);
    }
    
    private void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        // 从StructureLayer获取悬停信息
        for (var layer : layers) {
            if (layer instanceof StructureLayer structureLayer) {
                BlockPos hovered = structureLayer.getHoveredStructure();
                String structureId = structureLayer.getHoveredStructureId();
                
                if (hovered != null && structureId != null) {
                    String displayName = net.shiroha233.roadweaver.client.gui.core.ColorPalette.getStructureDisplayName(structureId);
                    String posText = String.format("X: %d, Z: %d", hovered.getX(), hovered.getZ());
                    
                    // 转换为FormattedCharSequence
                    List<net.minecraft.util.FormattedCharSequence> tooltipLines = new ArrayList<>();
                    tooltipLines.add(net.minecraft.network.chat.Component.literal(displayName).getVisualOrderText());
                    tooltipLines.add(net.minecraft.network.chat.Component.literal(posText).getVisualOrderText());
                    
                    graphics.renderTooltip(font, tooltipLines, mouseX, mouseY);
                }
                break;
            }
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        viewport.zoom(mouseX, mouseY, delta > 0);
        updateInfoPanel();
        return true;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        
        if (button == 0) {
            dragging = true;
            dragStartX = mouseX;
            dragStartY = mouseY;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (super.mouseReleased(mouseX, mouseY, button)) return true;
        
        if (button == 0) {
            dragging = false;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        
        if (dragging && button == 0) {
            viewport.pan(dragX, dragY);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // H键关闭
        if (minecraft != null && minecraft.options != null) {
            for (var keyMapping : minecraft.options.keyMappings) {
                if (keyMapping.getName().equals("key.roadweaver.debug_map") && 
                    keyMapping.matches(keyCode, scanCode)) {
                    onClose();
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void refreshData() {
        if (minecraft != null && minecraft.getSingleplayerServer() != null) {
            ServerLevel world = minecraft.getSingleplayerServer().overworld();
            WorldDataProvider provider = WorldDataProvider.getInstance();
            
            structures.clear();
            connections.clear();
            roads.clear();
            
            Records.StructureLocationData data = provider.getStructureLocations(world);
            if (data != null && data.structureInfos() != null) {
                structures.addAll(data.structureInfos());
            }
            
            List<Records.StructureConnection> conns = provider.getStructureConnections(world);
            if (conns != null) connections.addAll(conns);
            
            List<Records.RoadData> roadData = provider.getRoadDataList(world);
            if (roadData != null) roads.addAll(roadData);
            
            // 重新初始化
            init();
        }
    }
    
    private void updateInfoPanel() {
        infoPanel = new InfoPanel(
            width - INFO_PANEL_RIGHT_MARGIN, 
            INFO_PANEL_TOP_MARGIN, 
            structures, 
            connections, 
            roads, 
            viewport.getZoom()
        );
    }
    
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        // 不渲染默认背景
    }
}
