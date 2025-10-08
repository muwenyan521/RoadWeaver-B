package net.countered.settlementroads;

import eu.midnightdust.lib.config.MidnightConfig;
import net.countered.settlementroads.config.fabric.FabricModConfig;
import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.features.config.RoadFeatureRegistry;
import net.countered.settlementroads.features.config.FabricBiomeInjection;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettlementRoads implements ModInitializer {

	public static final String MOD_ID = "roadweaver";

	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing roadWeaver (Fabric)...");
		
		// 注册 Fabric Attachment API
		WorldDataAttachment.registerWorldDataAttachment();
		
		// 初始化 MidnightConfig
		MidnightConfig.init(MOD_ID, FabricModConfig.class);
		
		// 注册特性
		RoadFeatureRegistry.registerFeatures();
		// Fabric 端通过 BiomeModifications 注入放置特性
		FabricBiomeInjection.inject();
		
		// 注册事件处理器
		ModEventHandler.register();
	}
}