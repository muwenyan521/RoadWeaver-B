package net.shiroha233.roadweaver.client.gui.layers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.client.gui.core.ColorPalette;
import net.shiroha233.roadweaver.client.gui.core.LODManager;
import net.shiroha233.roadweaver.client.gui.core.MapViewport;
import net.shiroha233.roadweaver.client.gui.core.RenderLayer;
import net.shiroha233.roadweaver.client.gui.util.ModernRenderUtils;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.List;

/**
 * 道路路径渲染层（LOD优化版）
 * <p>
 * 负责渲染道路路径。集成LOD系统，根据缩放级别动态调整采样间隔。
 * </p>
 * 
 * <h3>性能优化特性：</h3>
 * <ul>
 *   <li>自适应采样：根据LOD级别调整路径点采样步长</li>
 *   <li>视锥剔除：只渲染可见的道路段</li>
 *   <li>条件渲染：低LOD级别自动跳过道路渲染</li>
 * </ul>
 * 
 * @author RoadWeaver Team
 * @version 2.1
 * @since 2025-10-17
 */
public class RoadPathLayer extends RenderLayer {
    
    private final List<Records.RoadData> roads;
    private final LODManager lodManager;
    
    /** 默认道路颜色透明度 */
    private static final int ROAD_DEFAULT_ALPHA = 180;
    
    /**
     * 构造道路路径层
     * 
     * @param viewport 视口控制器
     * @param roads 道路数据列表
     */
    public RoadPathLayer(MapViewport viewport, List<Records.RoadData> roads) {
        super(viewport);
        this.roads = roads;
        this.lodManager = new LODManager(viewport);
        setZIndex(5);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!enabled || roads.isEmpty()) return;
        
        // 更新LOD系统
        lodManager.update();
        
        // 检查是否应该渲染道路（低LOD跳过）
        if (!lodManager.shouldRenderRoads()) {
            return;
        }
        
        // 获取采样步长
        int sampleStep = lodManager.getRoadSampleStep();
        
        int roadColor = applyOpacity(ColorPalette.Utils.withAlpha(ColorPalette.ROAD_PATH, ROAD_DEFAULT_ALPHA));
        
        for (Records.RoadData road : roads) {
            List<Records.RoadSegmentPlacement> segments = road.roadSegmentList();
            if (segments == null || segments.size() < 2) continue;
            
            renderRoadPath(graphics, segments, roadColor, sampleStep);
        }
    }
    
    /**
     * 渲染单个道路路径（LOD优化版）
     * 
     * @param graphics 渲染上下文
     * @param segments 道路段列表
     * @param color 颜色
     * @param sampleStep LOD采样步长
     */
    private void renderRoadPath(GuiGraphics graphics, List<Records.RoadSegmentPlacement> segments, 
                                int color, int sampleStep) {
        if (segments.size() < 2) return;
        
        MapViewport.ScreenCoord prev = null;
        
        // 使用LOD系统提供的采样步长
        for (int i = 0; i < segments.size(); i += sampleStep) {
            BlockPos pos = segments.get(i).middlePos();
            
            // 视锥剔除：检查点是否可见
            if (!lodManager.isPointVisible(pos)) {
                // 如果当前点不可见，重置prev以避免绘制断开的线条
                prev = null;
                continue;
            }
            
            MapViewport.ScreenCoord curr = viewport.worldToScreen(pos.getX(), pos.getZ());
            
            if (prev != null && lodManager.isLineVisible(
                    segments.get(i - sampleStep).middlePos().getX(),
                    segments.get(i - sampleStep).middlePos().getZ(),
                    pos.getX(), pos.getZ())) {
                ModernRenderUtils.drawSmoothLine(graphics, prev.x(), prev.y(), 
                    curr.x(), curr.y(), color);
            }
            prev = curr;
        }
        
        // 确保渲染最后一段
        if (!segments.isEmpty()) {
            BlockPos lastPos = segments.get(segments.size() - 1).middlePos();
            if (lodManager.isPointVisible(lastPos) && prev != null) {
                MapViewport.ScreenCoord last = viewport.worldToScreen(lastPos.getX(), lastPos.getZ());
                ModernRenderUtils.drawSmoothLine(graphics, prev.x(), prev.y(), 
                    last.x(), last.y(), color);
            }
        }
    }
    
    @Override
    public void update(float deltaTime) {
        lodManager.update();
    }
    
    @Override
    public String getName() { 
        return "RoadPaths"; 
    }
}
