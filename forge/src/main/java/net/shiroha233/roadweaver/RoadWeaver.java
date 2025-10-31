package net.shiroha233.roadweaver;

import net.shiroha233.roadweaver.config.ConfigService;

import net.shiroha233.roadweaver.datagen.RoadWeaverDataGenerator;
import net.shiroha233.roadweaver.network.forge.MapNetworkForge;
import net.shiroha233.roadweaver.planning.forge.ServerPlanningHooks;
import net.shiroha233.roadweaver.features.forge.RoadFeaturesForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RoadWeaver.MOD_ID)
public class RoadWeaver {

    public static final String MOD_ID = "roadweaver";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public RoadWeaver() {
        LOGGER.info("Initializing RoadWeaver (Forge)...");
        
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // 加载配置（common 实现，写入 config/roadweaver.json）
        ConfigService.load();
        
        // 注册数据生成事件（确保 runData 时 provider 被加入）
        modEventBus.addListener(RoadWeaverDataGenerator::gatherData);

        // 注册 Feature
        RoadFeaturesForge.register(modEventBus);
        
        // 注册网络通道
        MapNetworkForge.register();
        
        // 注册服务器规划钩子：初始与动态增量规划
        ServerPlanningHooks.register();
    }
    
    public static Logger getLogger() {
        return LOGGER;
    }
}
