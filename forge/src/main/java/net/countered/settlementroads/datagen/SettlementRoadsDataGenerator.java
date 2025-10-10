package net.countered.settlementroads.datagen;

import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.features.config.forge.ModConfiguredFeatures;
import net.countered.settlementroads.features.config.forge.ModPlacedFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = SettlementRoads.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SettlementRoadsDataGenerator {
    
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // 注册世界生成数据（强制启用，避免 includeServer() 为 false 时跳过）
        System.out.println("[RoadWeaver] Adding DatapackBuiltinEntriesProvider for configured/placed features");
        generator.addProvider(true, new DatapackBuiltinEntriesProvider(
                output,
                lookupProvider,
                new RegistrySetBuilder()
                        .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
                        .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap),
                Set.of(SettlementRoads.MOD_ID)
        ));
    }
}
