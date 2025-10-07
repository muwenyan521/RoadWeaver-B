package net.countered.settlementroads.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettlementRoadsClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing RoadWeaver Client (Fabric)...");
        
        // TODO: 重新实现调试界面
        // 调试界面暂时禁用，等待完整的 1.21.1 API 更新
    }
}
