package net.countered.settlementroads.client.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.countered.settlementroads.config.neoforge.NeoForgeJsonConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClothConfigScreen {
    
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.roadweaver.title"))
                .setSavingRunnable(NeoForgeJsonConfig::save);
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        // 结构配置分类
        ConfigCategory structures = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.structures"));
        
        structures.addEntry(entryBuilder.startStrList(
                Component.translatable("config.roadweaver.structureToLocate"),
                new java.util.ArrayList<>(NeoForgeJsonConfig.getStructuresToLocate()))
                .setTooltip(Component.translatable("config.roadweaver.structureToLocate.tooltip"))
                .setExpanded(true)
                .setSaveConsumer(NeoForgeJsonConfig::setStructuresToLocate)
                .build());
        
        structures.addEntry(entryBuilder.startIntSlider(
                Component.translatable("config.roadweaver.structureSearchRadius"),
                NeoForgeJsonConfig.getStructureSearchRadius(),
                50, 200)
                .setDefaultValue(100)
                .setTooltip(Component.translatable("config.roadweaver.structureSearchRadius.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setStructureSearchRadius)
                .build());
        
        // 预生成配置分类
        ConfigCategory preGeneration = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.pregeneration"));
        
        preGeneration.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.initialLocatingCount"),
                NeoForgeJsonConfig.getInitialLocatingCount())
                .setDefaultValue(7)
                .setMin(1)
                .setMax(20)
                .setTooltip(Component.translatable("config.roadweaver.initialLocatingCount.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setInitialLocatingCount)
                .build());
        
        preGeneration.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.maxConcurrentRoadGeneration"),
                NeoForgeJsonConfig.getMaxConcurrentRoadGeneration())
                .setDefaultValue(3)
                .setMin(1)
                .setMax(10)
                .setTooltip(Component.translatable("config.roadweaver.maxConcurrentRoadGeneration.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setMaxConcurrentRoadGeneration)
                .build());
        
        preGeneration.addEntry(entryBuilder.startIntSlider(
                Component.translatable("config.roadweaver.structureSearchTriggerDistance"),
                NeoForgeJsonConfig.getStructureSearchTriggerDistance(),
                150, 1500)
                .setDefaultValue(500)
                .setTooltip(Component.translatable("config.roadweaver.structureSearchTriggerDistance.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setStructureSearchTriggerDistance)
                .build());
        
        // 道路配置分类
        ConfigCategory roads = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.roads"));
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.averagingRadius"),
                NeoForgeJsonConfig.getAveragingRadius())
                .setDefaultValue(1)
                .setMin(0)
                .setMax(5)
                .setTooltip(Component.translatable("config.roadweaver.averagingRadius.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setAveragingRadius)
                .build());
        
        roads.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.allowArtificial"),
                NeoForgeJsonConfig.getAllowArtificial())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.allowArtificial.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setAllowArtificial)
                .build());
        
        roads.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.allowNatural"),
                NeoForgeJsonConfig.getAllowNatural())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.allowNatural.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setAllowNatural)
                .build());
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.structureDistanceFromRoad"),
                NeoForgeJsonConfig.getStructureDistanceFromRoad())
                .setDefaultValue(4)
                .setMin(3)
                .setMax(8)
                .setTooltip(Component.translatable("config.roadweaver.structureDistanceFromRoad.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setStructureDistanceFromRoad)
                .build());
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.maxHeightDifference"),
                NeoForgeJsonConfig.getMaxHeightDifference())
                .setDefaultValue(5)
                .setMin(3)
                .setMax(10)
                .setTooltip(Component.translatable("config.roadweaver.maxHeightDifference.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setMaxHeightDifference)
                .build());
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.maxTerrainStability"),
                NeoForgeJsonConfig.getMaxTerrainStability())
                .setDefaultValue(4)
                .setMin(2)
                .setMax(10)
                .setTooltip(Component.translatable("config.roadweaver.maxTerrainStability.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setMaxTerrainStability)
                .build());
        
        // 装饰配置分类
        ConfigCategory decorations = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.decorations"));
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeWaypoints"),
                NeoForgeJsonConfig.getPlaceWaypoints())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.roadweaver.placeWaypoints.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setPlaceWaypoints)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeRoadFences"),
                NeoForgeJsonConfig.getPlaceRoadFences())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeRoadFences.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setPlaceRoadFences)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeSwings"),
                NeoForgeJsonConfig.getPlaceSwings())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeSwings.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setPlaceSwings)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeBenches"),
                NeoForgeJsonConfig.getPlaceBenches())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeBenches.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setPlaceBenches)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeGloriettes"),
                NeoForgeJsonConfig.getPlaceGloriettes())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeGloriettes.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setPlaceGloriettes)
                .build());
        
        // 手动模式配置分类
        ConfigCategory manual = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.manual"));
        
        manual.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.manualMaxHeightDifference"),
                NeoForgeJsonConfig.getManualMaxHeightDifference())
                .setDefaultValue(8)
                .setMin(3)
                .setMax(20)
                .setTooltip(Component.translatable("config.roadweaver.manualMaxHeightDifference.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setManualMaxHeightDifference)
                .build());
        
        manual.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.manualMaxTerrainStability"),
                NeoForgeJsonConfig.getManualMaxTerrainStability())
                .setDefaultValue(8)
                .setMin(2)
                .setMax(20)
                .setTooltip(Component.translatable("config.roadweaver.manualMaxTerrainStability.tooltip"))
                .setSaveConsumer(NeoForgeJsonConfig::setManualMaxTerrainStability)
                .build());
        
        return builder.build();
    }
}
