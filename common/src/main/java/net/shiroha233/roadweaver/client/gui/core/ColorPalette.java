package net.shiroha233.roadweaver.client.gui.core;

import java.util.*;

/**
 * 现代化颜色调色板
 * <p>
 * 采用Material Design和Tailwind CSS的配色理念。
 * 提供高对比度、易区分的颜色方案。
 * </p>
 * 
 * @author RoadWeaver Team
 * @version 2.0
 */
public class ColorPalette {
    
    // === 主题色 ===
    public static final int PRIMARY = 0xFF6366F1;        // 靛蓝
    public static final int PRIMARY_LIGHT = 0xFFA5B4FC;  // 浅靛蓝
    public static final int PRIMARY_DARK = 0xFF4338CA;   // 深靛蓝
    
    public static final int SECONDARY = 0xFF8B5CF6;      // 紫色
    public static final int ACCENT = 0xFF10B981;         // 翠绿
    
    // === 状态色 ===
    public static final int SUCCESS = 0xFF10B981;  // 成功-绿色
    public static final int WARNING = 0xFFF59E0B;  // 警告-琥珀
    public static final int ERROR = 0xFFEF4444;    // 错误-红色
    public static final int INFO = 0xFF3B82F6;     // 信息-蓝色
    
    // === 道路系统颜色 ===
    public static final int ROAD_PLANNED = 0xFFF59E0B;      // 计划中-琥珀
    public static final int ROAD_GENERATING = 0xFFEA580C;   // 生成中-橙色
    public static final int ROAD_COMPLETED = 0xFF10B981;    // 完成-绿色
    public static final int ROAD_FAILED = 0xFFEF4444;       // 失败-红色
    public static final int ROAD_PATH = 0xFF6366F1;         // 道路路径-靛蓝
    
    // === 结构颜色调色板（30种高对比度颜色）===
    private static final int[] STRUCTURE_COLORS = {
        0xFF10B981, // 翠绿
        0xFF3B82F6, // 蓝色
        0xFFEF4444, // 红色
        0xFFF59E0B, // 琥珀
        0xFF8B5CF6, // 紫色
        0xFF14B8A6, // 青色
        0xFFEC4899, // 粉红
        0xFFEAB308, // 黄色
        0xFF06B6D4, // 天蓝
        0xFFF97316, // 橙色
        0xFF84CC16, // 柠檬绿
        0xFFFF6B2B, // 珊瑚红
        0xFF7C3AED, // 靛紫
        0xFF059669, // 翡翠绿
        0xFFD946EF, // 洋红
        0xFF65A30D, // 草绿
        0xFF0891B2, // 深青
        0xFFDC2626, // 深红
        0xFF9333EA, // 紫罗兰
        0xFF0D9488, // 水鸭绿
        0xFFEAB308, // 金色
        0xFFC026D3, // 玫瑰紫
        0xFF16A34A, // 森林绿
        0xFF2563EB, // 皇家蓝
        0xFFF43F5E, // 玫瑰红
        0xFFCA8A04, // 古铜色
        0xFFA855F7, // 薰衣草
        0xFF0E7490, // 海蓝
        0xFFF97316, // 火焰橙
        0xFF6D28D9  // 深紫罗兰
    };
    
    // === UI颜色 ===
    public static final int BACKGROUND = 0xE0111827;       // 深色背景（带透明度）
    public static final int BACKGROUND_SOLID = 0xFF111827; // 深色背景（不透明）
    public static final int SURFACE = 0xF01F2937;          // 表面色
    public static final int SURFACE_HOVER = 0xFF374151;    // 悬停色
    
    public static final int BORDER = 0xFF374151;           // 边框
    public static final int BORDER_LIGHT = 0xFF4B5563;     // 浅边框
    public static final int DIVIDER = 0x40FFFFFF;          // 分隔线
    
    public static final int TEXT_PRIMARY = 0xFFF9FAFB;     // 主文本
    public static final int TEXT_SECONDARY = 0xFFD1D5DB;   // 次要文本
    public static final int TEXT_TERTIARY = 0xFF9CA3AF;    // 三级文本
    
    public static final int PLAYER_MARKER = 0xFFEF4444;    // 玩家标记-红色
    public static final int SELECTED = 0xFFFFD700;         // 选中-金色
    
    // === 渐变色 ===
    public static final int GRADIENT_START = 0xC00F172A;
    public static final int GRADIENT_END = 0xD01E293B;
    
    // === 结构颜色管理器 ===
    private static final Map<String, Integer> structureColorMap = new HashMap<>();
    private static int nextColorIndex = 0;
    
    /**
     * 获取结构类型的颜色
     */
    public static int getStructureColor(String structureId) {
        if (structureId == null || structureId.equals("unknown")) {
            return 0xFF6B7280; // 灰色
        }
        
        return structureColorMap.computeIfAbsent(structureId, id -> {
            int color = STRUCTURE_COLORS[nextColorIndex % STRUCTURE_COLORS.length];
            nextColorIndex++;
            return color;
        });
    }
    
    /**
     * 获取结构显示名称
     */
    public static String getStructureDisplayName(String structureId) {
        if (structureId == null || structureId.equals("unknown")) {
            return "未知结构";
        }
        
        // 移除命名空间
        String name = structureId;
        int colonIndex = name.indexOf(':');
        if (colonIndex >= 0) {
            name = name.substring(colonIndex + 1);
        }
        
        // 格式化名称
        name = name.replace('_', ' ');
        
        // 限制长度
        if (name.length() > 20) {
            name = name.substring(0, 17) + "...";
        }
        
        return name;
    }
    
    /**
     * 清除颜色缓存
     */
    public static void clearStructureColors() {
        structureColorMap.clear();
        nextColorIndex = 0;
    }
    
    /**
     * 颜色工具方法
     */
    public static class Utils {
        
        /**
         * 混合两个颜色
         */
        public static int blend(int color1, int color2, float ratio) {
            ratio = Math.max(0, Math.min(1, ratio));
            
            int a1 = (color1 >> 24) & 0xFF;
            int r1 = (color1 >> 16) & 0xFF;
            int g1 = (color1 >> 8) & 0xFF;
            int b1 = color1 & 0xFF;
            
            int a2 = (color2 >> 24) & 0xFF;
            int r2 = (color2 >> 16) & 0xFF;
            int g2 = (color2 >> 8) & 0xFF;
            int b2 = color2 & 0xFF;
            
            int a = (int)(a1 + (a2 - a1) * ratio);
            int r = (int)(r1 + (r2 - r1) * ratio);
            int g = (int)(g1 + (g2 - g1) * ratio);
            int b = (int)(b1 + (b2 - b1) * ratio);
            
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
        
        /**
         * 增加/减少亮度
         */
        public static int adjustBrightness(int color, float factor) {
            int a = (color >> 24) & 0xFF;
            int r = Math.min(255, (int)(((color >> 16) & 0xFF) * factor));
            int g = Math.min(255, (int)(((color >> 8) & 0xFF) * factor));
            int b = Math.min(255, (int)((color & 0xFF) * factor));
            
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
        
        /**
         * 设置透明度
         */
        public static int withAlpha(int color, int alpha) {
            alpha = Math.max(0, Math.min(255, alpha));
            return (color & 0x00FFFFFF) | (alpha << 24);
        }
        
        /**
         * 获取透明度
         */
        public static int getAlpha(int color) {
            return (color >> 24) & 0xFF;
        }
    }
}
