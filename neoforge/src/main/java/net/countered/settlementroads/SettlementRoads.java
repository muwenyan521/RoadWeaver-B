package net.countered.settlementroads;

import net.countered.settlementroads.client.gui.ClothConfigScreen;
import net.countered.settlementroads.config.neoforge.NeoForgeJsonConfig;
import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.features.config.RoadFeatureRegistry;
import net.countered.settlementroads.datagen.SettlementRoadsDataGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SettlementRoads.MOD_ID)
public class SettlementRoads {

	public static final String MOD_ID = "roadweaver";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public SettlementRoads(IEventBus modEventBus, ModContainer modContainer) {
		LOGGER.info("Initializing roadWeaver (NeoForge)...");
		
		// 加载 JSON 配置（与 Fabric 一致，写入 config/roadweaver.json）
		NeoForgeJsonConfig.load();
		
		// 注册配置屏幕（NeoForge 模组菜单集成）
		modContainer.registerExtensionPoint(
			net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
			(client, parent) -> ClothConfigScreen.createConfigScreen(parent)
		);
		
		// 注册通用设置事件
		modEventBus.addListener(this::commonSetup);
		// 注册数据生成事件（确保 runData 时 provider 被加入）
		modEventBus.addListener(SettlementRoadsDataGenerator::gatherData);
		
		// 注册特性（统一使用 common 的无参注册实现）
		RoadFeatureRegistry.registerFeatures();
		
		// 注册事件处理器（使用 Architectury 事件的 common 实现）
		ModEventHandler.register();
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		LOGGER.info("RoadWeaver common setup completed");
	}
	
	public static Logger getLogger() {
		return LOGGER;
	}
}
