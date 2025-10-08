package net.countered.settlementroads.persistence;

import net.countered.settlementroads.persistence.fabric.FabricWorldDataProvider;

public class WorldDataProviderImpl {
    private static final WorldDataProvider INSTANCE = new FabricWorldDataProvider();

    public static WorldDataProvider getInstance() {
        return INSTANCE;
    }
}
