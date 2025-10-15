package me.jfenn.mc249136;

import net.minecraft.item.map.MapIcon;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record TreasureMapData(
        BlockPos blockPos,
        byte zoom,
        RegistryKey<World> dimension,
        MapIcon.Type decoration
) {}
