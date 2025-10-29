package net.shiroha233.roadweaver.features.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.shiroha233.roadweaver.features.decoration.util.BiomeWoodAware;
import net.shiroha233.roadweaver.helpers.Records;


public class DistanceSignDecoration extends OrientedDecoration implements BiomeWoodAware {
    private final boolean isStart;
    private final String signText;
    private Records.WoodAssets wood;

    public DistanceSignDecoration(BlockPos pos, Vec3i direction, WorldGenLevel world, boolean isStart, String distanceText) {
        super(pos, direction, world);
        this.isStart = isStart;
        this.signText = distanceText;
    }

    @Override
    public void place() {
        if (!placeAllowed()) return;
        int rotation = getCardinalRotationFromVector(getOrthogonalVector(), isStart);
        DirectionProperties props = getDirectionProperties(rotation);

        BlockPos basePos = this.getPos();
        WorldGenLevel world = this.getWorld();

        BlockPos signPos = basePos.above(2).relative(props.offsetDirection.getOpposite());
        world.setBlock(signPos,
                wood.hangingSign().defaultBlockState()
                        .setValue(BlockStateProperties.ROTATION_16, rotation)
                        .setValue(BlockStateProperties.ATTACHED, true),
                3);
        updateSigns(world, signPos, signText);

        placeFenceStructure(basePos, props);
    }

    private void placeFenceStructure(BlockPos pos, DirectionProperties props) {
        WorldGenLevel world = this.getWorld();
        world.setBlock(pos.above(3).relative(props.offsetDirection.getOpposite()), wood.fence().defaultBlockState().setValue(props.directionProperty, true), 3);
        world.setBlock(pos.above(0), wood.fence().defaultBlockState(), 3);
        world.setBlock(pos.above(1), wood.fence().defaultBlockState(), 3);
        world.setBlock(pos.above(2), wood.fence().defaultBlockState(), 3);
        world.setBlock(pos.above(3), wood.fence().defaultBlockState().setValue(props.reverseDirectionProperty, true), 3);
    }

    private void updateSigns(WorldGenLevel level, BlockPos pos, String text) {
        net.minecraft.world.level.Level l = level.getLevel();
        if (!(l instanceof net.minecraft.server.level.ServerLevel)) return;
        net.minecraft.server.level.ServerLevel sLevel = (net.minecraft.server.level.ServerLevel) l;
        sLevel.getServer().execute(() -> {
            BlockEntity be = sLevel.getBlockEntity(pos);
            if (be instanceof HangingSignBlockEntity) {
                HangingSignBlockEntity sign = (HangingSignBlockEntity) be;
                SignText front = sign.getText(true);
                front = front.setMessage(0, Component.translatable("gui.roadweaver.sign.next_location"));
                front = front.setMessage(1, Component.literal(text + " m"));
                front = front.setMessage(2, Component.literal(""));
                front = front.setMessage(3, Component.literal(""));
                sign.setText(front, true);

                SignText back = sign.getText(false);
                back = back.setMessage(0, Component.literal("----------"));
                back = back.setMessage(1, Component.translatable("gui.roadweaver.sign.welcome"));
                back = back.setMessage(2, Component.translatable("gui.roadweaver.sign.traveller"));
                back = back.setMessage(3, Component.literal("----------"));
                sign.setText(back, false);

                sign.setChanged();
            }
        });
    }

    @Override
    public void setWoodType(Records.WoodAssets assets) {
        this.wood = assets;
    }
}
