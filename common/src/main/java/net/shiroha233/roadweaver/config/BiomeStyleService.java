package net.shiroha233.roadweaver.config;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BiomeStyleService {
    private final ModConfig config;
    private final Map<String, RoadStyleConfigEntry> biomeStyleCache;
    private final Map<String, LampPostConfigEntry> lampPostCache;
    private boolean bopModPresent;

    public BiomeStyleService(ModConfig config) {
        this.config = config;
        this.biomeStyleCache = new HashMap<>();
        this.lampPostCache = new HashMap<>();
        this.bopModPresent = false;
        initializeDefaultStyles();
    }

    public void setBopModPresent(boolean present) {
        this.bopModPresent = present;
    }

    private void initializeDefaultStyles() {
        // 默认生物群系样式映射
        Map<String, RoadStyleConfigEntry> defaultStyles = new HashMap<>();
        
        // 平原和草原
        defaultStyles.put("minecraft:plains", new RoadStyleConfigEntry(
            List.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT),
            RoadDecorationType.NONE,
            3
        ));
        defaultStyles.put("minecraft:sunflower_plains", new RoadStyleConfigEntry(
            List.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT),
            RoadDecorationType.NONE,
            3
        ));

        // 森林
        defaultStyles.put("minecraft:forest", new RoadStyleConfigEntry(
            List.of(Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT),
            RoadDecorationType.FENCE,
            3
        ));
        defaultStyles.put("minecraft:flower_forest", new RoadStyleConfigEntry(
            List.of(Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT),
            RoadDecorationType.FENCE,
            3
        ));

        // 针叶林
        defaultStyles.put("minecraft:taiga", new RoadStyleConfigEntry(
            List.of(Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT),
            RoadDecorationType.FENCE,
            3
        ));
        defaultStyles.put("minecraft:snowy_taiga", new RoadStyleConfigEntry(
            List.of(Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW, Blocks.PACKED_ICE),
            RoadDecorationType.FENCE,
            3
        ));

        // 沙漠
        defaultStyles.put("minecraft:desert", new RoadStyleConfigEntry(
            List.of(Blocks.SAND, Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE),
            RoadDecorationType.NONE,
            3
        ));

        // 雪原
        defaultStyles.put("minecraft:snowy_plains", new RoadStyleConfigEntry(
            List.of(Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW, Blocks.PACKED_ICE),
            RoadDecorationType.NONE,
            3
        ));

        // 沼泽
        defaultStyles.put("minecraft:swamp", new RoadStyleConfigEntry(
            List.of(Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS, Blocks.PACKED_MUD),
            RoadDecorationType.FENCE,
            3
        ));

        // 丛林
        defaultStyles.put("minecraft:jungle", new RoadStyleConfigEntry(
            List.of(Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK, Blocks.PODZOL),
            RoadDecorationType.FENCE,
            3
        ));

        // 山地
        defaultStyles.put("minecraft:mountains", new RoadStyleConfigEntry(
            List.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.GRAVEL),
            RoadDecorationType.NONE,
            3
        ));

        // 将默认样式添加到缓存
        biomeStyleCache.putAll(defaultStyles);
    }

    public RoadStyleConfigEntry getRoadStyleForBiome(Holder<Biome> biome) {
        ResourceLocation biomeId = biome.unwrapKey().map(key -> key.location()).orElse(null);
        if (biomeId == null) {
            return getDefaultRoadStyle();
        }

        String biomeName = biomeId.toString();

        // 首先检查用户自定义配置
        RoadStyleConfigEntry customStyle = findCustomStyle(biomeName);
        if (customStyle != null) {
            return customStyle;
        }

        // 检查缓存中的默认样式
        RoadStyleConfigEntry cachedStyle = biomeStyleCache.get(biomeName);
        if (cachedStyle != null) {
            return cachedStyle;
        }

        // 返回默认样式
        return getDefaultRoadStyle();
    }

    public LampPostConfigEntry getLampPostForBiome(Holder<Biome> biome) {
        ResourceLocation biomeId = biome.unwrapKey().map(key -> key.location()).orElse(null);
        if (biomeId == null) {
            return getDefaultLampPost();
        }

        String biomeName = biomeId.toString();

        // 检查用户自定义路灯配置
        LampPostConfigEntry customLampPost = findCustomLampPost(biomeName);
        if (customLampPost != null) {
            return customLampPost;
        }

        // 检查缓存中的默认路灯
        LampPostConfigEntry cachedLampPost = lampPostCache.get(biomeName);
        if (cachedLampPost != null) {
            return cachedLampPost;
        }

        // 返回默认路灯
        return getDefaultLampPost();
    }

    private RoadStyleConfigEntry findCustomStyle(String biomeName) {
        // 检查普通道路样式覆盖
        Optional<RoadStyleConfigEntry> customStyle = config.roadStyleOverrides().stream()
            .filter(style -> style.biomeSelector().equals(biomeName))
            .findFirst();

        if (customStyle.isPresent()) {
            return customStyle.get();
        }

        // 如果Biomes O' Plenty存在，检查BOP特定样式
        if (bopModPresent) {
            Optional<RoadStyleConfigEntry> bopStyle = config.bopRoadStyleOverrides().stream()
                .filter(style -> style.biomeSelector().equals(biomeName))
                .findFirst();

            if (bopStyle.isPresent()) {
                return bopStyle.get();
            }
        }

        return null;
    }

    private LampPostConfigEntry findCustomLampPost(String biomeName) {
        return config.lampPostOverrides().stream()
            .filter(lamp -> lamp.biomeSelector().equals(biomeName))
            .findFirst()
            .orElse(null);
    }

    private RoadStyleConfigEntry getDefaultRoadStyle() {
        return new RoadStyleConfigEntry(
            List.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT),
            RoadDecorationType.NONE,
            3
        );
    }

    private LampPostConfigEntry getDefaultLampPost() {
        return new LampPostConfigEntry(
            "default",
            Blocks.OAK_FENCE,
            Blocks.TORCH,
            3,
            1
        );
    }

    public void clearCache() {
        biomeStyleCache.clear();
        lampPostCache.clear();
        initializeDefaultStyles();
    }

    public void reloadStyles() {
        clearCache();
        // 重新加载用户自定义配置
        config.roadStyleOverrides().forEach(style -> 
            biomeStyleCache.put(style.biomeSelector(), style));
        config.lampPostOverrides().forEach(lamp -> 
            lampPostCache.put(lamp.biomeSelector(), lamp));
        if (bopModPresent) {
            config.bopRoadStyleOverrides().forEach(style -> 
                biomeStyleCache.put(style.biomeSelector(), style));
        }
    }
}
