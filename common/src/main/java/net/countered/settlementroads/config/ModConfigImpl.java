package net.countered.settlementroads.config;

import dev.architectury.platform.Platform;

/**
 * Implementation loader for ModConfig.
 * The actual implementation will be provided by the platform-specific modules.
 */
public class ModConfigImpl {
    static ModConfig INSTANCE;
    
    static {
        // This will be injected by Architectury at runtime
        // The platform-specific modules will provide the actual implementation
        try {
            if (Platform.isFabric()) {
                INSTANCE = (ModConfig) Class.forName("net.countered.settlementroads.config.fabric.FabricModConfig").getDeclaredConstructor().newInstance();
            } else if (Platform.isNeoForge()) {
                INSTANCE = (ModConfig) Class.forName("net.countered.settlementroads.config.neoforge.NeoForgeModConfig").getDeclaredConstructor().newInstance();
            } else {
                throw new RuntimeException("Unsupported platform for ModConfig");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ModConfig implementation", e);
        }
    }
}
