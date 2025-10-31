package net.shiroha233.roadweaver.network.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.shiroha233.roadweaver.RoadWeaver;
import net.shiroha233.roadweaver.client.map.MapDataCollector;
import net.shiroha233.roadweaver.client.map.MapSnapshot;
import net.shiroha233.roadweaver.client.map.RoadMapScreen;
import net.shiroha233.roadweaver.network.MapSnapshotCodec;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;
import java.util.function.Supplier;

public class MapNetworkForge {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RoadWeaver.MOD_ID, "map"),
            () -> VERSION, VERSION::equals, VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, RequestMapSnapshotC2S.class, RequestMapSnapshotC2S::encode, RequestMapSnapshotC2S::decode, RequestMapSnapshotC2S::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, MapSnapshotS2C.class, MapSnapshotS2C::encode, MapSnapshotS2C::decode, MapSnapshotS2C::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, TeleportC2S.class, TeleportC2S::encode, TeleportC2S::decode, TeleportC2S::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, TeleportAckS2C.class, TeleportAckS2C::encode, TeleportAckS2C::decode, TeleportAckS2C::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static class RequestMapSnapshotC2S {
        public final int minX, minZ, maxX, maxZ;
        public RequestMapSnapshotC2S(int minX, int minZ, int maxX, int maxZ) {
            this.minX = minX; this.minZ = minZ; this.maxX = maxX; this.maxZ = maxZ;
        }
        public static void encode(RequestMapSnapshotC2S msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.minX);
            buf.writeVarInt(msg.minZ);
            buf.writeVarInt(msg.maxX);
            buf.writeVarInt(msg.maxZ);
        }
        public static RequestMapSnapshotC2S decode(FriendlyByteBuf buf) {
            int minX = buf.readVarInt();
            int minZ = buf.readVarInt();
            int maxX = buf.readVarInt();
            int maxZ = buf.readVarInt();
            return new RequestMapSnapshotC2S(minX, minZ, maxX, maxZ);
        }
        public static void handle(RequestMapSnapshotC2S msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context c = ctx.get();
            c.enqueueWork(() -> {
                var player = c.getSender();
                if (player == null) return;
                int cx = (int) Math.round(player.getX());
                int cz = (int) Math.round(player.getZ());
                int radiusChunks;
                try {
                    net.shiroha233.roadweaver.config.ModConfig cfg = net.shiroha233.roadweaver.config.ConfigService.get();
                    radiusChunks = (cfg.dynamicPlanEnabled() ? cfg.dynamicPlanRadiusChunks() : cfg.initialPlanRadiusChunks());
                } catch (Throwable t) {
                    radiusChunks = 256;
                }
                int radiusBlocks = Math.max(1, radiusChunks) * 16;
                MapSnapshot snap = MapDataCollector.build(player.serverLevel(), msg.minX, msg.minZ, msg.maxX, msg.maxZ, cx, cz, radiusBlocks);
                CHANNEL.sendTo(new MapSnapshotS2C(snap), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            });
            c.setPacketHandled(true);
        }
    }

    public static void requestSnapshot(int minX, int minZ, int maxX, int maxZ) {
        CHANNEL.sendToServer(new RequestMapSnapshotC2S(minX, minZ, maxX, maxZ));
    }

    public static class MapSnapshotS2C {
        public final MapSnapshot snapshot;
        public MapSnapshotS2C(MapSnapshot s) { this.snapshot = s; }
        public static void encode(MapSnapshotS2C msg, FriendlyByteBuf buf) {
            MapSnapshotCodec.write(buf, msg.snapshot);
        }
        public static MapSnapshotS2C decode(FriendlyByteBuf buf) {
            return new MapSnapshotS2C(MapSnapshotCodec.read(buf));
        }
        public static void handle(MapSnapshotS2C msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context c = ctx.get();
            c.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof RoadMapScreen screen) screen.setSnapshot(msg.snapshot);
            });
            c.setPacketHandled(true);
        }
    }

    public static class TeleportC2S {
        public final int x, y, z;
        public TeleportC2S(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        public static void encode(TeleportC2S msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.x);
            buf.writeVarInt(msg.y);
            buf.writeVarInt(msg.z);
        }
        public static TeleportC2S decode(FriendlyByteBuf buf) {
            int x = buf.readVarInt();
            int y = buf.readVarInt();
            int z = buf.readVarInt();
            return new TeleportC2S(x, y, z);
        }
        public static void handle(TeleportC2S msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context c = ctx.get();
            c.enqueueWork(() -> {
                var sp = c.getSender();
                if (sp == null) return;
                boolean allowed = sp.isCreative() || sp.hasPermissions(2);
                if (!allowed) {
                    CHANNEL.sendTo(new TeleportAckS2C(false, 0, 0, 0), sp.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                    return;
                }
                var level = sp.serverLevel();
                level.getChunk(msg.x >> 4, msg.z >> 4);
                int ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, msg.x, msg.z);
                if (ty <= level.getMinBuildHeight()) ty = level.getSeaLevel() + 1; else ty += 1;
                sp.teleportTo(level, msg.x + 0.5, ty, msg.z + 0.5, sp.getYRot(), sp.getXRot());
                CHANNEL.sendTo(new TeleportAckS2C(true, msg.x, ty, msg.z), sp.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            });
            c.setPacketHandled(true);
        }
    }

    public static class TeleportAckS2C {
        public final boolean ok;
        public final int x, y, z;
        public TeleportAckS2C(boolean ok, int x, int y, int z) { this.ok = ok; this.x = x; this.y = y; this.z = z; }
        public static void encode(TeleportAckS2C msg, FriendlyByteBuf buf) { buf.writeBoolean(msg.ok); buf.writeVarInt(msg.x); buf.writeVarInt(msg.y); buf.writeVarInt(msg.z); }
        public static TeleportAckS2C decode(FriendlyByteBuf buf) { boolean ok = buf.readBoolean(); int x = buf.readVarInt(); int y = buf.readVarInt(); int z = buf.readVarInt(); return new TeleportAckS2C(ok, x, y, z); }
        public static void handle(TeleportAckS2C msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context c = ctx.get();
            c.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return;
                if (msg.ok) mc.player.displayClientMessage(Component.translatable("gui.roadweaver.map.teleport.success_pos", msg.x, msg.y, msg.z), true);
                else mc.player.displayClientMessage(Component.translatable("gui.roadweaver.map.teleport.denied"), true);
            });
            c.setPacketHandled(true);
        }
    }

    public static void requestTeleport(int x, int y, int z) {
        CHANNEL.sendToServer(new TeleportC2S(x, y, z));
    }
}
