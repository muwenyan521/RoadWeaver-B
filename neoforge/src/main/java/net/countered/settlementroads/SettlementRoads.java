package net.countered.settlementroads;

import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.features.config.RoadFeatureRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SettlementRoads.MOD_ID)
public class SettlementRoads {

	public static final String MOD_ID = "roadweaver";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public SettlementRoads(IEventBus modEventBus, ModContainer modContainer) {
		LOGGER.info("Initializing RoadWeaver (NeoForge)...");
		
		// 注册配置
		modContainer.registerConfig(Type.SERVER, ModConfig.SERVER_SPEC);
		
		// 注册通用设置事件
		modEventBus.addListener(this::commonSetup);
		
		// 注册特性
		RoadFeatureRegistry.registerFeatures(modEventBus);
		
		// 注册事件处理器
		ModEventHandler.register(modEventBus);
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		LOGGER.info("RoadWeaver common setup completed");
	}
	
	public static Logger getLogger() {
		return LOGGER;
	}
}
