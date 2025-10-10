package net.countered.settlementroads.api.materials;

/**
 * 道路材料类型枚举
 * 用于区分人工材料和自然材料
 */
public enum MaterialType {
    /**
     * 人工材料（如石砖、混凝土等）
     * 通常用于城镇内部或主要道路
     */
    ARTIFICIAL,
    
    /**
     * 自然材料（如泥土、砂砾等）
     * 通常用于乡村道路或临时路径
     */
    NATURAL
}
