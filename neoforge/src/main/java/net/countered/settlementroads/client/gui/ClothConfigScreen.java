package net.countered.settlementroads.client.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.countered.settlementroads.config.ModConfig;
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
                ModConfig.SERVER.structureToLocate.get())
                .setDefaultValue("#minecraft:village")
                .setTooltip(Component.translatable("config.roadweaver.structureToLocate.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.structureToLocate::set)
                .build());
        
        structures.addEntry(entryBuilder.startIntSlider(
                Component.translatable("config.roadweaver.structureSearchRadius"),
                ModConfig.SERVER.structureSearchRadius.get(),
                50, 200)
                .setDefaultValue(100)
                .setTooltip(Component.translatable("config.roadweaver.structureSearchRadius.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.structureSearchRadius::set)
                .build());
        
        // 预生成配置分类
        ConfigCategory preGeneration = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.pregeneration"));
        
        preGeneration.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.initialLocatingCount"),
                ModConfig.SERVER.initialLocatingCount.get())
                .setDefaultValue(7)
                .setMin(1)
                .setMax(20)
                .setTooltip(Component.translatable("config.roadweaver.initialLocatingCount.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.initialLocatingCount::set)
                .build());
        
        preGeneration.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.maxConcurrentRoadGeneration"),
                ModConfig.SERVER.maxConcurrentRoadGeneration.get())
                .setDefaultValue(3)
                .setMin(1)
                .setMax(10)
                .setTooltip(Component.translatable("config.roadweaver.maxConcurrentRoadGeneration.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.maxConcurrentRoadGeneration::set)
                .build());
        
        // 道路配置分类
        ConfigCategory roads = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.roads"));
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.averagingRadius"),
                ModConfig.SERVER.averagingRadius.get())
                .setDefaultValue(1)
                .setMin(0)
                .setMax(5)
                .setTooltip(Component.translatable("config.roadweaver.averagingRadius.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.averagingRadius::set)
                .build());
        
        roads.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.allowArtificial"),
                ModConfig.SERVER.allowArtificial.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.allowArtificial.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.allowArtificial::set)
                .build());
        
        roads.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.allowNatural"),
                ModConfig.SERVER.allowNatural.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.allowNatural.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.allowNatural::set)
                .build());
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.structureDistanceFromRoad"),
                ModConfig.SERVER.structureDistanceFromRoad.get())
                .setDefaultValue(4)
                .setMin(3)
                .setMax(8)
                .setTooltip(Component.translatable("config.roadweaver.structureDistanceFromRoad.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.structureDistanceFromRoad::set)
                .build());
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.maxHeightDifference"),
                ModConfig.SERVER.maxHeightDifference.get())
                .setDefaultValue(5)
                .setMin(3)
                .setMax(10)
                .setTooltip(Component.translatable("config.roadweaver.maxHeightDifference.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.maxHeightDifference::set)
                .build());
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.maxTerrainStability"),
                ModConfig.SERVER.maxTerrainStability.get())
                .setDefaultValue(4)
                .setMin(2)
                .setMax(10)
                .setTooltip(Component.translatable("config.roadweaver.maxTerrainStability.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.maxTerrainStability::set)
                .build());
        
        // 装饰配置分类
        ConfigCategory decorations = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.decorations"));
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeWaypoints"),
                ModConfig.SERVER.placeWaypoints.get())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.roadweaver.placeWaypoints.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.placeWaypoints::set)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeRoadFences"),
                ModConfig.SERVER.placeRoadFences.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeRoadFences.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.placeRoadFences::set)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeSwings"),
                ModConfig.SERVER.placeSwings.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeSwings.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.placeSwings::set)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeBenches"),
                ModConfig.SERVER.placeBenches.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeBenches.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.placeBenches::set)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeGloriettes"),
                ModConfig.SERVER.placeGloriettes.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeGloriettes.tooltip"))
                .setSaveConsumer(ModConfig.SERVER.placeGloriettes::set)
                .build());
        
        return builder.build();
    }
}
