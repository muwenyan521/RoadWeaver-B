package net.shiroha233.roadweaver.client.forge;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.ModConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Forge 平台的配置屏幕工厂实现
 */
public class ConfigScreenFactoryImpl {
    
    /**
     * 创建配置屏幕 (Forge实现)
     * @param parent 父屏幕
     * @return 配置屏幕实例
     */
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.roadweaver.title"));

        ModConfig conf = ConfigService.get();
        builder.setSavingRunnable(ConfigService::save);

        ConfigCategory filters = builder.getOrCreateCategory(Component.translatable("config.roadweaver.category.structure_filters"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        filters.addEntry(
                eb.startBooleanToggle(Component.translatable("config.roadweaver.enable_prediction"), conf.villagePredictionEnabled())
                        .setTooltip(Component.translatable("config.roadweaver.enable_prediction.tooltip"))
                        .setSaveConsumer(conf::setVillagePredictionEnabled)
                        .build()
        );

        filters.addEntry(
                eb.startIntField(Component.translatable("config.roadweaver.radius_chunks"), conf.predictRadiusChunks())
                        .setTooltip(Component.translatable("config.roadweaver.radius_chunks.tooltip"))
                        .setMin(1)
                        .setMax(4096)
                        .setSaveConsumer(conf::setPredictRadiusChunks)
                        .build()
        );

        filters.addEntry(
                eb.startBooleanToggle(Component.translatable("config.roadweaver.biome_prefilter"), conf.biomePrefilter())
                        .setTooltip(Component.translatable("config.roadweaver.biome_prefilter.tooltip"))
                        .setSaveConsumer(conf::setBiomePrefilter)
                        .build()
        );

        List<String> whitelist = new ArrayList<>(conf.structureWhitelist() == null ? List.of() : conf.structureWhitelist());
        List<String> blacklist = new ArrayList<>(conf.structureBlacklist() == null ? List.of() : conf.structureBlacklist());

        filters.addEntry(
                eb.startStrList(Component.translatable("config.roadweaver.whitelist"), whitelist)
                        .setTooltip(Component.translatable("config.roadweaver.whitelist.tooltip"))
                        .setSaveConsumer(list -> conf.setStructureWhitelist(normalize(list)))
                        .build()
        );

        filters.addEntry(
                eb.startStrList(Component.translatable("config.roadweaver.blacklist"), blacklist)
                        .setTooltip(Component.translatable("config.roadweaver.blacklist.tooltip"))
                        .setSaveConsumer(list -> conf.setStructureBlacklist(normalize(list)))
                        .build()
        );

        

        // 路网规划分类
        ConfigCategory planning = builder.getOrCreateCategory(Component.translatable("config.roadweaver.category.road_planning"));

        planning.addEntry(
                eb.startIntField(Component.translatable("config.roadweaver.initial_plan_radius_chunks"), conf.initialPlanRadiusChunks())
                        .setTooltip(Component.translatable("config.roadweaver.initial_plan_radius_chunks.tooltip"))
                        .setMin(1)
                        .setMax(4096)
                        .setSaveConsumer(conf::setInitialPlanRadiusChunks)
                        .build()
        );

        planning.addEntry(
                eb.startBooleanToggle(Component.translatable("config.roadweaver.dynamic_plan_enabled"), conf.dynamicPlanEnabled())
                        .setTooltip(Component.translatable("config.roadweaver.dynamic_plan_enabled.tooltip"))
                        .setSaveConsumer(conf::setDynamicPlanEnabled)
                        .build()
        );

        planning.addEntry(
                eb.startIntField(Component.translatable("config.roadweaver.dynamic_plan_radius_chunks"), conf.dynamicPlanRadiusChunks())
                        .setTooltip(Component.translatable("config.roadweaver.dynamic_plan_radius_chunks.tooltip"))
                        .setMin(1)
                        .setMax(4096)
                        .setSaveConsumer(conf::setDynamicPlanRadiusChunks)
                        .build()
        );

        planning.addEntry(
                eb.startIntField(Component.translatable("config.roadweaver.dynamic_plan_stride_chunks"), conf.dynamicPlanStrideChunks())
                        .setTooltip(Component.translatable("config.roadweaver.dynamic_plan_stride_chunks.tooltip"))
                        .setMin(1)
                        .setMax(256)
                        .setSaveConsumer(conf::setDynamicPlanStrideChunks)
                        .build()
        );

        planning.addEntry(
                eb.startEnumSelector(
                                Component.translatable("config.roadweaver.planning_algorithm"),
                                ModConfig.PlanningAlgorithm.class,
                                conf.planningAlgorithm())
                        .setTooltip(Component.translatable("config.roadweaver.planning_algorithm.tooltip"))
                        .setEnumNameProvider(v -> Component.translatable("config.roadweaver.planning_algorithm.option." + v.name().toLowerCase(Locale.ROOT)))
                        .setSaveConsumer(conf::setPlanningAlgorithm)
                        .build()
        );

        ConfigCategory genSurface = builder.getOrCreateCategory(Component.translatable("config.roadweaver.category.gen_surface"));

        genSurface.addEntry(
                eb.startBooleanToggle(Component.translatable("config.roadweaver.allow_artificial"), conf.allowArtificial())
                        .setTooltip(Component.translatable("config.roadweaver.allow_artificial.tooltip"))
                        .setSaveConsumer(conf::setAllowArtificial)
                        .build()
        );

        genSurface.addEntry(
                eb.startIntField(Component.translatable("config.roadweaver.averaging_radius"), conf.averagingRadius())
                        .setTooltip(Component.translatable("config.roadweaver.averaging_radius.tooltip"))
                        .setMin(0).setMax(64)
                        .setSaveConsumer(conf::setAveragingRadius)
                        .build()
        );

        // 新增：道路宽度（0=自动）
        genSurface.addEntry(
                eb.startIntField(Component.translatable("config.roadweaver.road_width"), conf.roadWidth())
                        .setTooltip(Component.translatable("config.roadweaver.road_width.tooltip"))
                        .setMin(0).setMax(15)
                        .setSaveConsumer(conf::setRoadWidth)
                        .build()
        );

        // 新增：路灯间隔（段）
        genSurface.addEntry(
                eb.startIntField(Component.translatable("config.roadweaver.lamp_interval"), conf.lampInterval())
                        .setTooltip(Component.translatable("config.roadweaver.lamp_interval.tooltip"))
                        .setMin(1).setMax(2048)
                        .setSaveConsumer(conf::setLampInterval)
                        .build()
        );

        

        ConfigCategory genPerformance = builder.getOrCreateCategory(Component.translatable("config.roadweaver.category.gen_performance"));

        genPerformance.addEntry(
                eb.startIntField(Component.translatable("config.roadweaver.generation_threads"), conf.generationThreads())
                        .setMin(1).setMax(64)
                        .setSaveConsumer(conf::setGenerationThreads)
                        .build()
        );

        genPerformance.addEntry(
                eb.startIntField(Component.translatable("config.roadweaver.a_star_step"), conf.aStarStep())
                        .setTooltip(Component.translatable("config.roadweaver.a_star_step.tooltip"))
                        .setMin(4).setMax(128)
                        .setSaveConsumer(conf::setAStarStep)
                        .build()
        );

        genPerformance.addEntry(
                eb.startIntField(Component.translatable("config.roadweaver.max_concurrent_generations"), conf.maxConcurrentGenerations())
                        .setMin(1).setMax(128)
                        .setSaveConsumer(conf::setMaxConcurrentGenerations)
                        .build()
        );

        return builder.build();
    }

    private static List<String> normalize(List<String> src) {
        if (src == null) return List.of();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String s : src) {
            if (s == null) continue;
            String v = s.trim().toLowerCase(Locale.ROOT);
            if (v.isEmpty()) continue;
            set.add(v);
        }
        return new ArrayList<>(set);
    }
}

