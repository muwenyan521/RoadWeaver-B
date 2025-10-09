package net.countered.settlementroads.client.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.countered.settlementroads.config.neoforge.NeoForgeModConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClothConfigScreen {
    
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.roadweaver.title"))
                .setSavingRunnable(() -> {
                    // 配置会自动保存到文件
                });
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        // 结构配置分类
        ConfigCategory structures = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.structures"));
        
        structures.addEntry(entryBuilder.startStrField(
                Component.translatable("config.roadweaver.structureToLocate"),
                NeoForgeModConfig.SERVER.structureToLocate.get())
                .setDefaultValue("#minecraft:village")
                .setTooltip(Component.translatable("config.roadweaver.structureToLocate.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.structureToLocate::set)
                .build());
        
        structures.addEntry(entryBuilder.startIntSlider(
                Component.translatable("config.roadweaver.structureSearchRadius"),
                NeoForgeModConfig.SERVER.structureSearchRadius.get(),
                50, 200)
                .setDefaultValue(100)
                .setTooltip(Component.translatable("config.roadweaver.structureSearchRadius.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.structureSearchRadius::set)
                .build());
        
        // 预生成配置分类
        ConfigCategory preGeneration = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.pregeneration"));
        
        preGeneration.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.initialLocatingCount"),
                NeoForgeModConfig.SERVER.initialLocatingCount.get())
                .setDefaultValue(7)
                .setMin(1)
                .setMax(20)
                .setTooltip(Component.translatable("config.roadweaver.initialLocatingCount.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.initialLocatingCount::set)
                .build());
        
        preGeneration.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.maxConcurrentRoadGeneration"),
                NeoForgeModConfig.SERVER.maxConcurrentRoadGeneration.get())
                .setDefaultValue(3)
                .setMin(1)
                .setMax(10)
                .setTooltip(Component.translatable("config.roadweaver.maxConcurrentRoadGeneration.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.maxConcurrentRoadGeneration::set)
                .build());
        
        // 道路配置分类
        ConfigCategory roads = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.roads"));
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.averagingRadius"),
                NeoForgeModConfig.SERVER.averagingRadius.get())
                .setDefaultValue(1)
                .setMin(0)
                .setMax(5)
                .setTooltip(Component.translatable("config.roadweaver.averagingRadius.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.averagingRadius::set)
                .build());
        
        roads.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.allowArtificial"),
                NeoForgeModConfig.SERVER.allowArtificial.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.allowArtificial.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.allowArtificial::set)
                .build());
        
        roads.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.allowNatural"),
                NeoForgeModConfig.SERVER.allowNatural.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.allowNatural.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.allowNatural::set)
                .build());
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.structureDistanceFromRoad"),
                NeoForgeModConfig.SERVER.structureDistanceFromRoad.get())
                .setDefaultValue(4)
                .setMin(3)
                .setMax(8)
                .setTooltip(Component.translatable("config.roadweaver.structureDistanceFromRoad.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.structureDistanceFromRoad::set)
                .build());
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.maxHeightDifference"),
                NeoForgeModConfig.SERVER.maxHeightDifference.get())
                .setDefaultValue(5)
                .setMin(3)
                .setMax(10)
                .setTooltip(Component.translatable("config.roadweaver.maxHeightDifference.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.maxHeightDifference::set)
                .build());
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.maxTerrainStability"),
                NeoForgeModConfig.SERVER.maxTerrainStability.get())
                .setDefaultValue(4)
                .setMin(2)
                .setMax(10)
                .setTooltip(Component.translatable("config.roadweaver.maxTerrainStability.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.maxTerrainStability::set)
                .build());
        
        // 装饰配置分类
        ConfigCategory decorations = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.decorations"));
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeWaypoints"),
                NeoForgeModConfig.SERVER.placeWaypoints.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.roadweaver.placeWaypoints.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.placeWaypoints::set)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeRoadFences"),
                NeoForgeModConfig.SERVER.placeRoadFences.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeRoadFences.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.placeRoadFences::set)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeSwings"),
                NeoForgeModConfig.SERVER.placeSwings.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeSwings.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.placeSwings::set)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeBenches"),
                NeoForgeModConfig.SERVER.placeBenches.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeBenches.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.placeBenches::set)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeGloriettes"),
                NeoForgeModConfig.SERVER.placeGloriettes.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeGloriettes.tooltip"))
                .setSaveConsumer(NeoForgeModConfig.SERVER.placeGloriettes::set)
                .build());
        
        return builder.build();
    }
}
