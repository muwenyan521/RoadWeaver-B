package net.countered.settlementroads.persistence;

import net.countered.settlementroads.persistence.neoforge.NeoForgeWorldDataProvider;

public class WorldDataProviderImpl {
    private static final WorldDataProvider INSTANCE = new NeoForgeWorldDataProvider();

    public static WorldDataProvider getInstance() {
        return INSTANCE;
    }
}
