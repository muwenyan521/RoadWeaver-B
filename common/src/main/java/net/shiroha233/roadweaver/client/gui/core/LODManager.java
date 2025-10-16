package net.shiroha233.roadweaver.client.gui.core;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * LOD（Level of Detail）管理器
 * <p>
 * 根据缩放级别和可见范围动态调整渲染细节，大幅提升性能。
 * 实现了多层次细节管理、视锥剔除、批量渲染优化等功能。
 * </p>
 * 
 * <h3>LOD级别说明：</h3>
 * <ul>
 *   <li><b>ULTRA_LOW (0)</b>: zoom < 0.4 - 仅显示主要元素，最小细节</li>
 *   <li><b>LOW (1)</b>: 0.4 ≤ zoom < 0.8 - 低细节渲染</li>
 *   <li><b>MEDIUM (2)</b>: 0.8 ≤ zoom < 1.6 - 中等细节渲染</li>
 *   <li><b>HIGH (3)</b>: 1.6 ≤ zoom < 3.0 - 高细节渲染（最佳流畅度）</li>
 *   <li><b>ULTRA_HIGH (4)</b>: zoom ≥ 3.0 - 最高细节渲染</li>
 * </ul>
 * 
 * <h3>性能优化策略：</h3>
 * <ul>
 *   <li>视锥剔除：只渲染可见区域内的元素</li>
 *   <li>距离剔除：根据LOD级别剔除远距离对象</li>
 *   <li>采样步长：根据LOD级别调整道路路径采样间隔</li>
 *   <li>渲染质量：根据LOD级别调整圆形、线条等图形质量</li>
 * </ul>
 * 
 * @author RoadWeaver Team
 * @version 2.0
 * @since 2025-10-17
 */
public class LODManager {
    
    // ==================== LOD级别常量 ====================
    
    /** LOD级别：超低细节 */
    public static final int LOD_ULTRA_LOW = 0;
    
    /** LOD级别：低细节 */
    public static final int LOD_LOW = 1;
    
    /** LOD级别：中等细节 */
    public static final int LOD_MEDIUM = 2;
    
    /** LOD级别：高细节 */
    public static final int LOD_HIGH = 3;
    
    /** LOD级别：超高细节 */
    public static final int LOD_ULTRA_HIGH = 4;
    
    // ==================== 缩放阈值常量 ====================
    
    /** 超低细节缩放阈值 - 极远距离观察 */
    private static final double ZOOM_ULTRA_LOW_THRESHOLD = 0.4;
    
    /** 低细节缩放阈值 - 远距离观察 */
    private static final double ZOOM_LOW_THRESHOLD = 3.0;
    
    /** 中等细节缩放阈值 - 中距离观察 */
    private static final double ZOOM_MEDIUM_THRESHOLD = 6.0;
    
    /** 高细节缩放阈值 - 近距离观察（用户反馈此级别最流畅） */
    private static final double ZOOM_HIGH_THRESHOLD = 7.0;
    
    // ==================== 渲染参数常量 ====================
    
    /** 道路采样步长 - 超低细节 */
    private static final int ROAD_SAMPLE_STEP_ULTRA_LOW = 300;
    
    /** 道路采样步长 - 低细节 */
    private static final int ROAD_SAMPLE_STEP_LOW = 200;
    
    /** 道路采样步长 - 中等细节 */
    private static final int ROAD_SAMPLE_STEP_MEDIUM = 100;
    
    /** 道路采样步长 - 高细节 */
    private static final int ROAD_SAMPLE_STEP_HIGH = 50;
    
    /** 道路采样步长 - 超高细节 */
    private static final int ROAD_SAMPLE_STEP_ULTRA_HIGH = 10;
    
    /** 结构节点基础半径 */
    private static final int NODE_BASE_RADIUS = 5;
    
    /** 圆形渲染质量 - 低细节（使用快速方形近似） */
    private static final int CIRCLE_QUALITY_LOW = 1;
    
    /** 圆形渲染质量 - 中等细节（使用八边形近似） */
    private static final int CIRCLE_QUALITY_MEDIUM = 3;
    
    /** 圆形渲染质量 - 高细节（使用完整圆形） */
    private static final int CIRCLE_QUALITY_HIGH = 5;
    
    // ==================== 实例变量 ====================
    
    private final MapViewport viewport;
    private int currentLOD;
    
    /**
     * 构造LOD管理器
     * 
     * @param viewport 视口控制器
     */
    public LODManager(MapViewport viewport) {
        this.viewport = viewport;
        this.currentLOD = LOD_MEDIUM;
        updateLODLevel();
    }
    
    /**
     * 每帧更新LOD级别
     * <p>
     * 根据当前缩放级别自动调整LOD，确保渲染性能和视觉质量的平衡。
     * </p>
     */
    public void update() {
        updateLODLevel();
    }
    
    /**
     * 更新LOD级别
     */
    private void updateLODLevel() {
        double zoom = viewport.getZoom();
        
        if (zoom < ZOOM_ULTRA_LOW_THRESHOLD) {
            currentLOD = LOD_ULTRA_LOW;
        } else if (zoom < ZOOM_LOW_THRESHOLD) {
            currentLOD = LOD_LOW;
        } else if (zoom < ZOOM_MEDIUM_THRESHOLD) {
            currentLOD = LOD_MEDIUM;
        } else if (zoom < ZOOM_HIGH_THRESHOLD) {
            currentLOD = LOD_HIGH;
        } else {
            currentLOD = LOD_ULTRA_HIGH;
        }
    }
    
    /**
     * 获取当前LOD级别
     * 
     * @return LOD级别 (0-4)
     */
    public int getCurrentLOD() {
        return currentLOD;
    }
    
    /**
     * 获取LOD级别名称（用于调试）
     * 
     * @return LOD级别名称
     */
    public String getLODName() {
        return switch (currentLOD) {
            case LOD_ULTRA_LOW -> "ULTRA_LOW";
            case LOD_LOW -> "LOW";
            case LOD_MEDIUM -> "MEDIUM";
            case LOD_HIGH -> "HIGH";
            case LOD_ULTRA_HIGH -> "ULTRA_HIGH";
            default -> "UNKNOWN";
        };
    }
    
    // ==================== 视锥剔除 ====================
    
    /**
     * 检查点是否在可见范围内（视锥剔除）
     * 
     * @param worldX 世界X坐标
     * @param worldZ 世界Z坐标
     * @return 是否可见
     */
    public boolean isPointVisible(double worldX, double worldZ) {
        MapViewport.ScreenCoord screen = viewport.worldToScreen(worldX, worldZ);
        return viewport.isInViewport(screen.x(), screen.y());
    }
    
    /**
     * 检查点是否在可见范围内（视锥剔除）
     * 
     * @param pos 方块位置
     * @return 是否可见
     */
    public boolean isPointVisible(BlockPos pos) {
        return isPointVisible(pos.getX(), pos.getZ());
    }
    
    /**
     * 检查线段是否与可见范围相交
     * 
     * @param x1 起点X
     * @param z1 起点Z
     * @param x2 终点X
     * @param z2 终点Z
     * @return 是否相交
     */
    public boolean isLineVisible(double x1, double z1, double x2, double z2) {
        MapViewport.ScreenCoord p1 = viewport.worldToScreen(x1, z1);
        MapViewport.ScreenCoord p2 = viewport.worldToScreen(x2, z2);
        return viewport.isLineInViewport(p1.x(), p1.y(), p2.x(), p2.y());
    }
    
    /**
     * 批量剔除不可见的点
     * 
     * @param points 输入点列表
     * @param <T> 点类型（必须有getX()和getZ()方法）
     * @return 可见的点列表
     */
    public <T extends BlockPos> List<T> cullPoints(List<T> points) {
        MapViewport.ViewBounds bounds = viewport.getVisibleWorldBounds();
        List<T> visible = new ArrayList<>();
        
        for (T point : points) {
            if (bounds.contains(point.getX(), point.getZ())) {
                visible.add(point);
            }
        }
        
        return visible;
    }
    
    // ==================== LOD参数获取 ====================
    
    /**
     * 获取道路路径采样步长
     * <p>
     * 根据LOD级别返回合适的采样间隔，减少需要渲染的点数。
     * </p>
     * 
     * @return 采样步长（点数间隔）
     */
    public int getRoadSampleStep() {
        return switch (currentLOD) {
            case LOD_ULTRA_LOW -> ROAD_SAMPLE_STEP_ULTRA_LOW;
            case LOD_LOW -> ROAD_SAMPLE_STEP_LOW;
            case LOD_MEDIUM -> ROAD_SAMPLE_STEP_MEDIUM;
            case LOD_HIGH -> ROAD_SAMPLE_STEP_HIGH;
            case LOD_ULTRA_HIGH -> ROAD_SAMPLE_STEP_ULTRA_HIGH;
            default -> ROAD_SAMPLE_STEP_MEDIUM;
        };
    }
    
    /**
     * 获取结构节点渲染半径
     * <p>
     * 根据LOD级别和缩放级别动态计算节点大小。
     * </p>
     * 
     * @return 节点半径（像素）
     */
    public int getNodeRadius() {
        double zoom = viewport.getZoom();
        double baseFactor = Math.log10(zoom + 1) * 0.3 + 1.0;
        
        int baseRadius = (int)(NODE_BASE_RADIUS * baseFactor);
        
        // LOD级别影响
        return switch (currentLOD) {
            case LOD_ULTRA_LOW -> Math.max(2, baseRadius - 2);
            case LOD_LOW -> Math.max(3, baseRadius - 1);
            case LOD_MEDIUM -> baseRadius;
            case LOD_HIGH -> baseRadius + 1;
            case LOD_ULTRA_HIGH -> baseRadius + 2;
            default -> baseRadius;
        };
    }
    
    /**
     * 获取圆形渲染质量
     * <p>
     * 根据LOD级别返回渲染质量等级：
     * <ul>
     *   <li>0 = 快速方形近似</li>
     *   <li>1 = 八边形近似</li>
     *   <li>2 = 完整圆形</li>
     * </ul>
     * </p>
     * 
     * @return 渲染质量等级
     */
    public int getCircleQuality() {
        return switch (currentLOD) {
            case LOD_ULTRA_LOW, LOD_LOW -> CIRCLE_QUALITY_LOW;
            case LOD_MEDIUM -> CIRCLE_QUALITY_MEDIUM;
            case LOD_HIGH, LOD_ULTRA_HIGH -> CIRCLE_QUALITY_HIGH;
            default -> CIRCLE_QUALITY_MEDIUM;
        };
    }
    
    /**
     * 是否应该渲染网格
     * 
     * @return 是否渲染网格
     */
    public boolean shouldRenderGrid() {
        return currentLOD >= LOD_LOW;
    }
    
    /**
     * 是否应该渲染网格标签
     * 
     * @return 是否渲染标签
     */
    public boolean shouldRenderGridLabels() {
        return currentLOD >= LOD_MEDIUM && viewport.getZoom() > 0.5;
    }
    
    /**
     * 是否应该渲染道路路径
     * 
     * @return 是否渲染道路
     */
    public boolean shouldRenderRoads() {
        return currentLOD >= LOD_LOW;
    }
    
    /**
     * 是否应该渲染连接线
     * 
     * @return 是否渲染连接线
     */
    public boolean shouldRenderConnections() {
        return currentLOD >= LOD_MEDIUM;
    }
    
    /**
     * 是否应该渲染结构高光效果
     * 
     * @return 是否渲染高光
     */
    public boolean shouldRenderStructureHighlight() {
        return currentLOD >= LOD_MEDIUM;
    }
    
    /**
     * 是否应该渲染悬停边框
     * 
     * @return 是否渲染悬停边框
     */
    public boolean shouldRenderHoverBorder() {
        return currentLOD >= LOD_HIGH;
    }
    
    /**
     * 是否应该渲染选中动画
     * 
     * @return 是否渲染选中动画
     */
    public boolean shouldRenderSelectionAnimation() {
        return currentLOD >= LOD_MEDIUM;
    }
    
    /**
     * 获取线条宽度
     * 
     * @return 线条宽度（像素）
     */
    public int getLineWidth() {
        return switch (currentLOD) {
            case LOD_ULTRA_LOW -> 1;
            case LOD_LOW -> 1;
            case LOD_MEDIUM -> 1;
            case LOD_HIGH -> 2;
            case LOD_ULTRA_HIGH -> 2;
            default -> 1;
        };
    }
    
}
