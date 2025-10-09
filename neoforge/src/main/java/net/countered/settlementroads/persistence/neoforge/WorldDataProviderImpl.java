package net.countered.settlementroads.persistence.neoforge;

import net.countered.settlementroads.persistence.WorldDataProvider;

public class WorldDataProviderImpl {
    private static final WorldDataProvider INSTANCE = new NeoForgeWorldDataProvider();

    public static WorldDataProvider getInstance() {
        return INSTANCE;
    }
}
