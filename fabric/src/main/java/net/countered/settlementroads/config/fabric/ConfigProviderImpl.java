package net.countered.settlementroads.config.fabric;

import net.countered.settlementroads.config.IModConfig;
import net.countered.settlementroads.config.SyncedConfigWrapper;

/**
 * Architectury @ExpectPlatform 实现类（Fabric）。
 * 位置必须为：net.countered.settlementroads.config.fabric.ConfigProviderImpl
 */
public final class ConfigProviderImpl {
    private static final IModConfig INSTANCE = new SyncedConfigWrapper(new FabricModConfigAdapter());
    
    public static IModConfig get() {
        // 返回包装后的配置，支持多人游戏时使用服务端配置
        return INSTANCE;
    }
}
