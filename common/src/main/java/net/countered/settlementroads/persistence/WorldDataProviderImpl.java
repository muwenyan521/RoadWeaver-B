package net.countered.settlementroads.persistence;

import dev.architectury.platform.Platform;

/**
 * Implementation loader for WorldDataProvider.
 * The actual implementation will be provided by the platform-specific modules.
 */
public class WorldDataProviderImpl {
    static WorldDataProvider INSTANCE;
    
    static {
        // This will be injected by Architectury at runtime
        // The platform-specific modules will provide the actual implementation
        try {
            if (Platform.isFabric()) {
                INSTANCE = (WorldDataProvider) Class.forName("net.countered.settlementroads.persistence.fabric.FabricWorldDataProvider").getDeclaredConstructor().newInstance();
            } else if (Platform.isNeoForge()) {
                INSTANCE = (WorldDataProvider) Class.forName("net.countered.settlementroads.persistence.neoforge.NeoForgeWorldDataProvider").getDeclaredConstructor().newInstance();
            } else {
                throw new RuntimeException("Unsupported platform for WorldDataProvider");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load WorldDataProvider implementation", e);
        }
    }
}
