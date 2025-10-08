package net.countered.settlementroads.config;

import net.countered.settlementroads.config.fabric.FabricModConfigAdapter;

public class ConfigProviderImpl {
    public static IModConfig get() {
        return new FabricModConfigAdapter();
    }
}
