package net.shiroha233.roadweaver.client.gui.core;

import net.minecraft.core.BlockPos;

/**
 * 地图视口控制器
 * <p>
 * 负责管理地图的缩放、平移、坐标转换等核心功能。
 * 采用平滑缓动算法，提供流畅的视觉体验。
 * </p>
 * 
 * @author RoadWeaver Team
 * @version 2.0
 */
public class MapViewport {
    
    // 视口边界
    private final int padding;
    private int screenWidth;
    private int screenHeight;
    
    // 世界坐标范围
    private int worldMinX;
    private int worldMaxX;
    private int worldMinZ;
    private int worldMaxZ;
    
    // 变换参数
    private double zoom = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double baseScale = 1.0;
    
    // 缩放限制
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 10.0;
    private static final double ZOOM_SPEED = 1.1;
    
    // 平滑过渡
    private double targetZoom;
    private double targetOffsetX;
    private double targetOffsetY;
    private static final double SMOOTH_FACTOR = 0.2;
    
    // 初始化标志
    private boolean initialized = false;
    
    /**
     * 构造视口
     * @param padding 屏幕边距
     */
    public MapViewport(int padding) {
        this.padding = padding;
        this.targetZoom = zoom;
        this.targetOffsetX = offsetX;
        this.targetOffsetY = offsetY;
    }
    
    /**
     * 更新屏幕尺寸
     */
    public void updateScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    /**
     * 更新世界边界
     */
    public void updateWorldBounds(int minX, int maxX, int minZ, int maxZ) {
        this.worldMinX = minX;
        this.worldMaxX = maxX;
        this.worldMinZ = minZ;
        this.worldMaxZ = maxZ;
        recalculateBaseScale();
    }
    
    /**
     * 重新计算基础缩放比例
     */
    private void recalculateBaseScale() {
        int viewportWidth = screenWidth - padding * 2;
        int viewportHeight = screenHeight - padding * 2;
        
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            baseScale = 1.0;
            return;
        }
        
        int worldWidth = Math.max(1, worldMaxX - worldMinX);
        int worldHeight = Math.max(1, worldMaxZ - worldMinZ);
        
        double scaleX = (double) viewportWidth / worldWidth;
        double scaleY = (double) viewportHeight / worldHeight;
        
        baseScale = Math.min(scaleX, scaleY) * 0.85; // 留5%边距
    }
    
    /**
     * 初始化视口（居中显示）
     */
    public void initialize(BlockPos centerPos) {
        if (initialized) return;
        
        int viewportWidth = screenWidth - padding * 2;
        int viewportHeight = screenHeight - padding * 2;
        
        if (centerPos != null) {
            // 以指定位置为中心
            double centerScreenX = (centerPos.getX() - worldMinX) * baseScale * zoom;
            double centerScreenY = (centerPos.getZ() - worldMinZ) * baseScale * zoom;
            
            targetOffsetX = viewportWidth / 2.0 - centerScreenX;
            targetOffsetY = viewportHeight / 2.0 - centerScreenY;
        } else {
            // 以世界中心为中心
            int worldWidth = worldMaxX - worldMinX;
            int worldHeight = worldMaxZ - worldMinZ;
            
            targetOffsetX = (viewportWidth - worldWidth * baseScale * zoom) / 2.0;
            targetOffsetY = (viewportHeight - worldHeight * baseScale * zoom) / 2.0;
        }
        
        offsetX = targetOffsetX;
        offsetY = targetOffsetY;
        initialized = true;
    }
    
    /**
     * 每帧更新（平滑过渡）
     */
    public void update() {
        // 平滑缩放
        if (Math.abs(zoom - targetZoom) > 0.001) {
            zoom += (targetZoom - zoom) * SMOOTH_FACTOR;
        }
        
        // 平滑平移
        if (Math.abs(offsetX - targetOffsetX) > 0.1) {
            offsetX += (targetOffsetX - offsetX) * SMOOTH_FACTOR;
        }
        if (Math.abs(offsetY - targetOffsetY) > 0.1) {
            offsetY += (targetOffsetY - offsetY) * SMOOTH_FACTOR;
        }
    }
    
    /**
     * 缩放（以鼠标位置为中心）
     */
    public void zoom(double mouseX, double mouseY, boolean zoomIn) {
        double oldZoom = targetZoom;
        targetZoom = zoomIn ? targetZoom * ZOOM_SPEED : targetZoom / ZOOM_SPEED;
        targetZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, targetZoom));
        
        // 调整偏移使缩放以鼠标为中心
        double mouseWorldX = mouseX - padding;
        double mouseWorldY = mouseY - padding;
        
        targetOffsetX = (targetOffsetX - mouseWorldX) * (targetZoom / oldZoom) + mouseWorldX;
        targetOffsetY = (targetOffsetY - mouseWorldY) * (targetZoom / oldZoom) + mouseWorldY;
    }
    
    /**
     * 平移
     */
    public void pan(double deltaX, double deltaY) {
        targetOffsetX += deltaX;
        targetOffsetY += deltaY;
    }
    
    /**
     * 立即平移（无动画）
     */
    public void panInstant(double deltaX, double deltaY) {
        targetOffsetX += deltaX;
        targetOffsetY += deltaY;
        offsetX = targetOffsetX;
        offsetY = targetOffsetY;
    }
    
    /**
     * 聚焦到指定位置
     */
    public void focusOn(BlockPos pos, boolean instant) {
        int viewportWidth = screenWidth - padding * 2;
        int viewportHeight = screenHeight - padding * 2;
        
        double targetScreenX = (pos.getX() - worldMinX) * baseScale * targetZoom;
        double targetScreenY = (pos.getZ() - worldMinZ) * baseScale * targetZoom;
        
        targetOffsetX = viewportWidth / 2.0 - targetScreenX;
        targetOffsetY = viewportHeight / 2.0 - targetScreenY;
        
        if (instant) {
            offsetX = targetOffsetX;
            offsetY = targetOffsetY;
        }
    }
    
    /**
     * 世界坐标转屏幕坐标
     */
    public ScreenCoord worldToScreen(double worldX, double worldZ) {
        double scale = baseScale * zoom;
        int screenX = padding + (int)((worldX - worldMinX) * scale + offsetX);
        int screenY = padding + (int)((worldZ - worldMinZ) * scale + offsetY);
        return new ScreenCoord(screenX, screenY);
    }
    
    /**
     * 屏幕坐标转世界坐标
     */
    public WorldCoord screenToWorld(int screenX, int screenY) {
        double scale = baseScale * zoom;
        double worldX = worldMinX + (screenX - padding - offsetX) / scale;
        double worldZ = worldMinZ + (screenY - padding - offsetY) / scale;
        return new WorldCoord(worldX, worldZ);
    }
    
    /**
     * 检查屏幕坐标是否在视口内
     */
    public boolean isInViewport(int screenX, int screenY) {
        return screenX >= padding && screenX < screenWidth - padding &&
               screenY >= padding && screenY < screenHeight - padding;
    }
    
    /**
     * 检查线段是否与视口相交
     */
    public boolean isLineInViewport(int x1, int y1, int x2, int y2) {
        int left = padding;
        int right = screenWidth - padding;
        int top = padding;
        int bottom = screenHeight - padding;
        
        // 完全在外部
        if ((x1 < left && x2 < left) || (x1 > right && x2 > right) ||
            (y1 < top && y2 < top) || (y1 > bottom && y2 > bottom)) {
            return false;
        }
        return true;
    }
    
    /**
     * 获取当前可见的世界范围
     */
    public ViewBounds getVisibleWorldBounds() {
        WorldCoord topLeft = screenToWorld(padding, padding);
        WorldCoord bottomRight = screenToWorld(screenWidth - padding, screenHeight - padding);
        return new ViewBounds(topLeft.x, bottomRight.x, topLeft.z, bottomRight.z);
    }
    
    // Getters
    public double getZoom() { return zoom; }
    public double getTargetZoom() { return targetZoom; }
    public double getBaseScale() { return baseScale; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    public int getPadding() { return padding; }
    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }
    
    /**
     * 屏幕坐标
     */
    public record ScreenCoord(int x, int y) {}
    
    /**
     * 世界坐标
     */
    public record WorldCoord(double x, double z) {}
    
    /**
     * 可见范围
     */
    public record ViewBounds(double minX, double maxX, double minZ, double maxZ) {
        public boolean contains(double x, double z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }
}
