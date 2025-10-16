package net.shiroha233.roadweaver.client.gui.layers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.shiroha233.roadweaver.client.gui.core.ColorPalette;
import net.shiroha233.roadweaver.client.gui.core.LODManager;
import net.shiroha233.roadweaver.client.gui.core.MapViewport;
import net.shiroha233.roadweaver.client.gui.core.RenderLayer;

/**
 * 背景层渲染器
 * <p>
 * 绘制现代化的网格背景，包括坐标线、网格标签等。
 * 根据缩放级别自动调整网格密度，确保视觉清晰度。
 * </p>
 * 
 * @author RoadWeaver Team
 * @version 2.0
 */
public class BackgroundLayer extends RenderLayer {
    
    private final LODManager lodManager;
    
    private static final int MIN_GRID_SPACING_PX = 60;  // 最小网格间距（像素）
    private static final int MAX_GRID_SPACING_PX = 120; // 最大网格间距（像素）
    
    public BackgroundLayer(MapViewport viewport) {
        super(viewport);
        this.lodManager = new LODManager(viewport);
        setZIndex(-100); // 最底层
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!enabled) return;
        
        lodManager.update();
        
        // 绘制渐变背景
        renderGradientBackground(graphics);
        
        // 绘制网格（根据LOD级别）
        if (lodManager.shouldRenderGrid()) {
            renderGrid(graphics);
        }
    }
    
    @Override
    public void update(float deltaTime) {
        lodManager.update();
    }
    
    /**
     * 渲染渐变背景
     */
    private void renderGradientBackground(GuiGraphics graphics) {
        int width = viewport.getScreenWidth();
        int height = viewport.getScreenHeight();
        
        // 深色渐变背景
        graphics.fillGradient(
            0, 0, width, height,
            applyOpacity(ColorPalette.GRADIENT_START),
            applyOpacity(ColorPalette.GRADIENT_END)
        );
    }
    
    /**
     * 渲染网格
     */
    private void renderGrid(GuiGraphics graphics) {
        MapViewport.ViewBounds bounds = viewport.getVisibleWorldBounds();
        
        // 计算网格间距
        int gridSpacing = calculateGridSpacing();
        
        // 计算起始位置（对齐到网格）
        int startX = (int)(Math.floor(bounds.minX() / gridSpacing) * gridSpacing);
        int startZ = (int)(Math.floor(bounds.minZ() / gridSpacing) * gridSpacing);
        int endX = (int)(Math.ceil(bounds.maxX() / gridSpacing) * gridSpacing);
        int endZ = (int)(Math.ceil(bounds.maxZ() / gridSpacing) * gridSpacing);
        
        int gridColor = applyOpacity(0x40FFFFFF); // 半透明白色
        int majorGridColor = applyOpacity(0x60FFFFFF); // 主网格线更亮
        
        Font font = Minecraft.getInstance().font;
        
        // 绘制垂直线
        for (int x = startX; x <= endX; x += gridSpacing) {
            MapViewport.ScreenCoord top = viewport.worldToScreen(x, bounds.minZ());
            MapViewport.ScreenCoord bottom = viewport.worldToScreen(x, bounds.maxZ());
            
            if (!viewport.isInViewport(top.x(), top.y()) && 
                !viewport.isInViewport(bottom.x(), bottom.y())) {
                continue;
            }
            
            // 主网格线（每5条）
            boolean isMajor = (x % (gridSpacing * 5)) == 0;
            int color = isMajor ? majorGridColor : gridColor;
            
            graphics.fill(top.x(), top.y(), top.x() + 1, bottom.y(), color);
            
            // 绘制标签（根据LOD级别）
            if (isMajor && lodManager.shouldRenderGridLabels()) {
                String label = String.valueOf(x);
                int labelX = top.x() + 2;
                int labelY = viewport.getPadding() + 2;
                graphics.drawString(font, label, labelX, labelY, 
                    applyOpacity(ColorPalette.TEXT_TERTIARY), false);
            }
        }
        
        // 绘制水平线
        for (int z = startZ; z <= endZ; z += gridSpacing) {
            MapViewport.ScreenCoord left = viewport.worldToScreen(bounds.minX(), z);
            MapViewport.ScreenCoord right = viewport.worldToScreen(bounds.maxX(), z);
            
            if (!viewport.isInViewport(left.x(), left.y()) && 
                !viewport.isInViewport(right.x(), right.y())) {
                continue;
            }
            
            boolean isMajor = (z % (gridSpacing * 5)) == 0;
            int color = isMajor ? majorGridColor : gridColor;
            
            graphics.fill(left.x(), left.y(), right.x(), left.y() + 1, color);
            
            // 绘制标签（根据LOD级别）
            if (isMajor && lodManager.shouldRenderGridLabels()) {
                String label = String.valueOf(z);
                int labelX = viewport.getPadding() + 2;
                int labelY = left.y() + 2;
                graphics.drawString(font, label, labelX, labelY,
                    applyOpacity(ColorPalette.TEXT_TERTIARY), false);
            }
        }
    }
    
    /**
     * 计算合适的网格间距
     */
    private int calculateGridSpacing() {
        double pixelsPerBlock = viewport.getBaseScale() * viewport.getZoom();
        double targetSpacingPx = (MIN_GRID_SPACING_PX + MAX_GRID_SPACING_PX) / 2.0;
        double blocksPerGrid = targetSpacingPx / pixelsPerBlock;
        
        // 使用2的幂次或5的倍数作为间距
        int[] spacings = {1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000};
        
        for (int spacing : spacings) {
            if (spacing >= blocksPerGrid * 0.8) {
                return spacing;
            }
        }
        
        return 10000;
    }
    
    @Override
    public String getName() {
        return "Background";
    }
}
