package me.jfenn.mc249136;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class Main implements ModInitializer {

    Logger log = LoggerFactory.getLogger(Main.class);

    public static final ConcurrentHashMap<Integer, TreasureMapData> BURIED_TREASURE_LOCATIONS = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.START_SERVER_TICK.register((server -> {
            // if there are no treasure locations, we don't need to do anything
            if (BURIED_TREASURE_LOCATIONS.isEmpty()) return;

            for (var player : server.getPlayerManager().getPlayerList()) {
                // This probably shouldn't run on every tick... but it works well enough for now
                // TODO: could this be moved to a mixin for ItemStack.copy instead? (to track any copied instances of FILLED_MAP stacks)
                var inventory = player.getInventory();
                int inventorySize = inventory.size();
                for (int i = 0; i <= inventorySize; i++) {
                    var stack = inventory.getStack(i);
                    var nbt = stack.getNbt();
                    if (
                            nbt != null
                            && stack.getItem() == Items.FILLED_MAP
                            && nbt.getBoolean("is_buried_treasure")
                            && !nbt.contains("Decorations")
                    ) {
                        var mapId = FilledMapItem.getMapId(stack);
                        if (mapId == null) continue;

                        var location = BURIED_TREASURE_LOCATIONS.remove(mapId);
                        if (location == null) continue;

                        ServerWorld world = server.getWorld(location.dimension());
                        if (world == null) continue;

                        log.info("Replacing treasure map " + mapId + " in player inventory");

                        if (location.blockPos() == null) {
                            // The map has finished, but the locateStructure() failed...
                            // Render an error map
                            MapState mapState = MapState.of((byte) 1, true, location.dimension());
                            TempMapRenderer.fillErrorState(mapState);
                            world.putMapState(FilledMapItem.getMapName(mapId), mapState);
                            continue;
                        }

                        // Fill the exploration map state
                        MapState mapState = MapState.of(location.blockPos().getX(), location.blockPos().getZ(), location.zoom(), true, true, location.dimension());
                        world.putMapState(FilledMapItem.getMapName(mapId), mapState);
                        FilledMapItem.fillExplorationMap(world, stack);

                        // set the decoration NBT
                        MapState.addDecorationsNbt(stack, location.blockPos(), "+", location.decoration());
                    }
                }
            }
        }));

        // clear the locations map between server instances
        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
            BURIED_TREASURE_LOCATIONS.clear();
        });
    }
}
