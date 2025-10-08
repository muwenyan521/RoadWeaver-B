package net.countered.settlementroads.config;

public class ConfigProviderImpl {
    public static IModConfig get() {
        return new NeoForgeModConfigAdapter();
    }
}
