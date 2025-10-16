package net.shiroha233.roadweaver.client.gui.layers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.client.gui.core.AnimationState;
import net.shiroha233.roadweaver.client.gui.core.ColorPalette;
import net.shiroha233.roadweaver.client.gui.core.LODManager;
import net.shiroha233.roadweaver.client.gui.core.MapViewport;
import net.shiroha233.roadweaver.client.gui.core.RenderLayer;
import net.shiroha233.roadweaver.client.gui.util.ModernRenderUtils;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构节点渲染层（LOD优化版）
 * <p>
 * 负责绘制所有结构节点，支持动画效果、悬停高亮、选中状态等。
 * 集成LOD系统，根据缩放级别动态调整渲染细节，大幅提升性能。
 * </p>
 * 
 * <h3>性能优化特性：</h3>
 * <ul>
 *   <li>视锥剔除：只渲染可见区域内的结构</li>
 *   <li>LOD质量控制：根据缩放级别调整圆形渲染质量</li>
 *   <li>条件渲染：根据LOD级别选择性渲染高光、边框等效果</li>
 *   <li>快速圆形渲染：使用优化的渲染算法，最高可提升4倍性能</li>
 * </ul>
 * 
 * @author RoadWeaver Team
 * @version 2.1
 * @since 2025-10-17
 */
public class StructureLayer extends RenderLayer {
    
    private final List<Records.StructureInfo> structures;
    private final Map<BlockPos, AnimationState.PulseAnimation> selectionAnimations = new HashMap<>();
    private final LODManager lodManager;
    
    private BlockPos hoveredStructure;
    private BlockPos selectedStructure;
    
    // ==================== 渲染常量 ====================
    
    /** 悬停时额外增加的半径 */
    private static final int HOVERED_RADIUS_BONUS = 2;
    
    /** 高光偏移系数（相对于半径） */
    private static final float HIGHLIGHT_OFFSET_RATIO = 0.33f;
    
    /** 高光尺寸系数（相对于半径） */
    private static final float HIGHLIGHT_SIZE_RATIO = 0.33f;
    
    /** 选中光晕最小值 */
    private static final float SELECTION_PULSE_MIN = 0.6f;
    
    /** 选中光晕最大值 */
    private static final float SELECTION_PULSE_MAX = 1.0f;
    
    /** 选中光晕速度 */
    private static final float SELECTION_PULSE_SPEED = 3.0f;
    
    /** 选中光晕扩展系数 */
    private static final int SELECTION_GLOW_EXPAND = 4;
    
    /** 选中光晕透明度 */
    private static final int SELECTION_GLOW_ALPHA = 0x60;
    
    /** 选中光晕颜色 */
    private static final int SELECTION_GLOW_COLOR_BASE = 0xFFD700;
    
    /** 节点外圈亮度系数 */
    private static final float NODE_OUTER_BRIGHTNESS = 0.7f;
    
    /** 悬停边框宽度 */
    private static final int HOVER_BORDER_WIDTH = 1;
    
    /** 悬停边框偏移 */
    private static final int HOVER_BORDER_OFFSET = 2;
    
    /** 高光颜色 */
    private static final int HIGHLIGHT_COLOR = 0x80FFFFFF;
    
    /**
     * 构造结构层
     * 
     * @param viewport 视口控制器
     * @param structures 结构信息列表
     */
    public StructureLayer(MapViewport viewport, List<Records.StructureInfo> structures) {
        super(viewport);
        this.structures = structures;
        this.lodManager = new LODManager(viewport);
        setZIndex(10); // 中层
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!enabled || structures.isEmpty()) return;
        
        // 更新LOD系统
        lodManager.update();
        
        // 更新悬停状态
        updateHoveredStructure(mouseX, mouseY);
        
        // 获取可见范围
        MapViewport.ViewBounds bounds = viewport.getVisibleWorldBounds();
        
        // 获取LOD参数
        int nodeRadius = lodManager.getNodeRadius();
        int circleQuality = lodManager.getCircleQuality();
        
        // 绘制所有结构节点
        for (Records.StructureInfo info : structures) {
            BlockPos pos = info.pos();
            
            // 视锥剔除（LOD优化）
            if (!lodManager.isPointVisible(pos)) {
                continue;
            }
            
            MapViewport.ScreenCoord screen = viewport.worldToScreen(pos.getX(), pos.getZ());
            
            // 渲染节点
            renderStructureNode(graphics, info, screen, nodeRadius, circleQuality);
        }
    }
    
    /**
     * 渲染单个结构节点（LOD优化版）
     * 
     * @param graphics 渲染上下文
     * @param info 结构信息
     * @param screen 屏幕坐标
     * @param baseRadius 基础半径
     * @param quality 渲染质量（0=低，1=中，2=高）
     */
    private void renderStructureNode(GuiGraphics graphics, Records.StructureInfo info, 
                                      MapViewport.ScreenCoord screen, int baseRadius, int quality) {
        BlockPos pos = info.pos();
        boolean isHovered = pos.equals(hoveredStructure);
        boolean isSelected = pos.equals(selectedStructure);
        
        int radius = baseRadius;
        if (isHovered) radius += HOVERED_RADIUS_BONUS;
        
        // 获取结构颜色
        int structureColor = ColorPalette.getStructureColor(info.structureId());
        structureColor = applyOpacity(structureColor);
        
        // 选中效果（金色脉冲光晕） - 仅在中等以上LOD渲染
        if (isSelected && lodManager.shouldRenderSelectionAnimation()) {
            AnimationState.PulseAnimation pulse = selectionAnimations.computeIfAbsent(
                pos, p -> new AnimationState.PulseAnimation(
                    SELECTION_PULSE_MIN, SELECTION_PULSE_MAX, SELECTION_PULSE_SPEED
                )
            );
            
            float pulseValue = pulse.getValue();
            int glowRadius = (int)(radius + SELECTION_GLOW_EXPAND * pulseValue);
            int glowAlpha = (int)(SELECTION_GLOW_ALPHA * pulseValue);
            int glowColor = (glowAlpha << 24) | SELECTION_GLOW_COLOR_BASE;
            
            ModernRenderUtils.fillCircleFast(graphics, screen.x(), screen.y(), glowRadius, 
                applyOpacity(glowColor), quality);
        }
        
        // 主节点（带微妙渐变效果）
        int darkerColor = ColorPalette.Utils.adjustBrightness(structureColor, NODE_OUTER_BRIGHTNESS);
        
        // 外圈（深色）- 使用快速渲染
        ModernRenderUtils.fillCircleFast(graphics, screen.x(), screen.y(), radius + 1, 
            applyOpacity(darkerColor), quality);
        
        // 内圈（亮色）- 使用快速渲染
        ModernRenderUtils.fillCircleFast(graphics, screen.x(), screen.y(), radius,
            applyOpacity(structureColor), quality);
        
        // 高光（小白点）- 仅在中等以上LOD渲染
        if (lodManager.shouldRenderStructureHighlight() && radius >= 4) {
            int highlightSize = Math.max(1, (int)(radius * HIGHLIGHT_SIZE_RATIO));
            int highlightOffsetX = (int)(radius * HIGHLIGHT_OFFSET_RATIO);
            int highlightOffsetY = (int)(radius * HIGHLIGHT_OFFSET_RATIO);
            ModernRenderUtils.fillCircleFast(graphics, 
                screen.x() - highlightOffsetX, 
                screen.y() - highlightOffsetY,
                highlightSize, 
                applyOpacity(HIGHLIGHT_COLOR), 
                quality);
        }
        
        // 悬停边框 - 仅在高LOD渲染
        if (isHovered && lodManager.shouldRenderHoverBorder()) {
            ModernRenderUtils.drawCircleOutline(graphics, screen.x(), screen.y(), 
                radius + HOVER_BORDER_OFFSET, HOVER_BORDER_WIDTH, applyOpacity(0xFFFFFFFF));
        }
    }
    
    /**
     * 更新悬停的结构
     * <p>
     * 检测鼠标是否悬停在某个结构节点上。
     * </p>
     * 
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     */
    private void updateHoveredStructure(int mouseX, int mouseY) {
        hoveredStructure = null;
        
        int nodeRadius = lodManager.getNodeRadius() + HOVERED_RADIUS_BONUS;
        int hoverThreshold = nodeRadius + HOVER_BORDER_OFFSET;
        
        for (Records.StructureInfo info : structures) {
            BlockPos pos = info.pos();
            
            // 快速剔除：只检查可见的结构
            if (!lodManager.isPointVisible(pos)) {
                continue;
            }
            
            MapViewport.ScreenCoord screen = viewport.worldToScreen(pos.getX(), pos.getZ());
            
            int dx = screen.x() - mouseX;
            int dy = screen.y() - mouseY;
            double distSq = dx * dx + dy * dy;
            
            if (distSq <= hoverThreshold * hoverThreshold) {
                hoveredStructure = pos;
                break;
            }
        }
    }
    
    
    @Override
    public void update(float deltaTime) {
        // 更新LOD系统
        lodManager.update();
        
        // 更新选中动画
        selectionAnimations.values().forEach(anim -> anim.update(deltaTime));
    }
    
    // Setters
    public void setSelectedStructure(BlockPos pos) {
        this.selectedStructure = pos;
    }
    
    // ==================== Getters ====================
    
    /**
     * 获取当前悬停的结构位置
     * 
     * @return 悬停的结构位置，如果没有则返回null
     */
    public BlockPos getHoveredStructure() {
        return hoveredStructure;
    }
    
    /**
     * 获取当前悬停的结构ID
     * 
     * @return 结构ID，如果没有则返回null
     */
    public String getHoveredStructureId() {
        if (hoveredStructure == null) return null;
        return structures.stream()
            .filter(info -> info.pos().equals(hoveredStructure))
            .findFirst()
            .map(Records.StructureInfo::structureId)
            .orElse(null);
    }
    
    @Override
    public String getName() {
        return "Structures";
    }
}
