package net.countered.settlementroads.config.neoforge;

import net.countered.settlementroads.config.IModConfig;

public class ConfigProviderImpl {
    public static IModConfig get() {
        return new NeoForgeModConfigAdapter();
    }
}
