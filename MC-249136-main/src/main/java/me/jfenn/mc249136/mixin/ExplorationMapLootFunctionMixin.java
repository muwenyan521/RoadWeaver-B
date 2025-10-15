package me.jfenn.mc249136.mixin;

import me.jfenn.mc249136.Main;
import me.jfenn.mc249136.TempMapRenderer;
import me.jfenn.mc249136.TreasureMapData;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ExplorationMapLootFunction;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(ExplorationMapLootFunction.class)
abstract class ExplorationMapLootFunctionMixin {

    Logger log = LoggerFactory.getLogger(ExplorationMapLootFunctionMixin.class);

    @Shadow
    TagKey<Structure> destination;
    @Shadow
    MapIcon.Type decoration;
    @Shadow
    byte zoom;
    @Shadow
    int searchRadius;
    @Shadow
    boolean skipExistingChunks;

    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Inject(at = @At(value = "HEAD"), method = "process", cancellable = true)
    public void process(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> ci) {
        if (!stack.isOf(Items.MAP)) {
            ci.setReturnValue(stack);
            ci.cancel();
            return;
        }

        Vec3d vec3d = context.get(LootContextParameters.ORIGIN);
        if (vec3d == null) {
            ci.setReturnValue(stack);
            ci.cancel();
            return;
        }

        ItemStack itemStack = new ItemStack(Items.FILLED_MAP);

        // allocate a map ID for the item
        ServerWorld world = context.getWorld();
        int i = world.getNextMapId();
        var nbt = itemStack.getOrCreateNbt();
        nbt.putInt("map", i);
        nbt.putBoolean("is_buried_treasure", true);

        MapState initialState = MapState.of((byte) 1, false, world.getRegistryKey());
        TempMapRenderer.fillLoadingState(initialState);
        world.putMapState(FilledMapItem.getMapName(i), initialState);

        // this would be BlockPos.ofFloored(), but that isn't compatible with 1.19
        BlockPos origin = new BlockPos((int)Math.floor(vec3d.x), (int)Math.floor(vec3d.y), (int)Math.floor(vec3d.z));

        // run locateStructure() asynchronously
        executor.submit(() -> {
            log.info("Searching for buried treasure for map loot " + i + "...");

            // this needs to be async
            BlockPos blockPos = world.locateStructure(this.destination, origin, this.searchRadius, this.skipExistingChunks);
            if (blockPos != null) {
                log.info("Found buried treasure at " + blockPos + " (" + i + ")");
            } else {
                log.info("Failed to find a buried treasure location (" + i + ")");
            }

            TreasureMapData data = new TreasureMapData(
                    blockPos,
                    zoom,
                    world.getRegistryKey(),
                    this.decoration
            );
            Main.BURIED_TREASURE_LOCATIONS.put(i, data);
        });

        ci.setReturnValue(itemStack);
        ci.cancel();
    }
}
