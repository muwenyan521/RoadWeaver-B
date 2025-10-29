package net.shiroha233.roadweaver.network.fabric;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaver.client.map.MapDataCollector;
import net.shiroha233.roadweaver.client.map.MapSnapshot;
import net.shiroha233.roadweaver.client.map.RoadMapScreen;
import net.shiroha233.roadweaver.network.MapSnapshotCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.network.chat.Component;


public class MapNetworkFabric {
    public static final ResourceLocation REQ = new ResourceLocation("roadweaver", "map_request");
    public static final ResourceLocation REQ_RECT = new ResourceLocation("roadweaver", "map_request_rect");
    public static final ResourceLocation SNAP = new ResourceLocation("roadweaver", "map_snapshot");
    public static final ResourceLocation TP_REQ = new ResourceLocation("roadweaver", "map_teleport");
    public static final ResourceLocation TP_ACK = new ResourceLocation("roadweaver", "map_teleport_ack");

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(REQ, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ServerPlayer sp = player;
                int cx = (int) Math.round(sp.getX());
                int cz = (int) Math.round(sp.getZ());
                int radiusChunks;
                try {
                    net.shiroha233.roadweaver.config.ModConfig cfg = net.shiroha233.roadweaver.config.ConfigService.get();
                    radiusChunks = (cfg.dynamicPlanEnabled() ? cfg.dynamicPlanRadiusChunks() : cfg.initialPlanRadiusChunks());
                } catch (Throwable t) {
                    radiusChunks = 256;
                }
                int radiusBlocks = Math.max(1, radiusChunks) * 16;
                int minX = cx - radiusBlocks;
                int minZ = cz - radiusBlocks;
                int maxX = cx + radiusBlocks;
                int maxZ = cz + radiusBlocks;
                MapSnapshot snap = MapDataCollector.build(sp.serverLevel(), minX, minZ, maxX, maxZ, cx, cz, radiusBlocks);
                FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
                MapSnapshotCodec.write(out, snap);
                ServerPlayNetworking.send(sp, SNAP, out);
            });
        });

        // 矩形范围请求：minX,minZ,maxX,maxZ
        ServerPlayNetworking.registerGlobalReceiver(REQ_RECT, (server, player, handler, buf, responseSender) -> {
            int minX = buf.readVarInt();
            int minZ = buf.readVarInt();
            int maxX = buf.readVarInt();
            int maxZ = buf.readVarInt();
            server.execute(() -> {
                ServerPlayer sp = player;
                int cx = (int) Math.round(sp.getX());
                int cz = (int) Math.round(sp.getZ());
                int radiusChunks;
                try {
                    net.shiroha233.roadweaver.config.ModConfig cfg = net.shiroha233.roadweaver.config.ConfigService.get();
                    radiusChunks = (cfg.dynamicPlanEnabled() ? cfg.dynamicPlanRadiusChunks() : cfg.initialPlanRadiusChunks());
                } catch (Throwable t) {
                    radiusChunks = 256;
                }
                int radiusBlocks = Math.max(1, radiusChunks) * 16;
                MapSnapshot snap = MapDataCollector.build(sp.serverLevel(), minX, minZ, maxX, maxZ, cx, cz, radiusBlocks);
                FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
                MapSnapshotCodec.write(out, snap);
                ServerPlayNetworking.send(sp, SNAP, out);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TP_REQ, (server, player, handler, buf, responseSender) -> {
            int x = buf.readVarInt();
            buf.readVarInt();
            int z = buf.readVarInt();
            server.execute(() -> {
                ServerPlayer sp = player;
                boolean allowed = sp.isCreative() || sp.hasPermissions(2);
                if (!allowed) {
                    FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
                    out.writeBoolean(false);
                    ServerPlayNetworking.send(sp, TP_ACK, out);
                    return;
                }
                var level = sp.serverLevel();
                level.getChunk(x >> 4, z >> 4);
                int ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (ty <= level.getMinBuildHeight()) ty = level.getSeaLevel() + 1; else ty += 1;
                sp.teleportTo(level, x + 0.5, ty, z + 0.5, sp.getYRot(), sp.getXRot());
                FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
                out.writeBoolean(true);
                out.writeVarInt(x);
                out.writeVarInt(ty);
                out.writeVarInt(z);
                ServerPlayNetworking.send(sp, TP_ACK, out);
            });
        });
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(SNAP, (client, handler, buf, responseSender) -> {
            MapSnapshot s = MapSnapshotCodec.read(buf);
            client.execute(() -> {
                if (client.screen instanceof RoadMapScreen screen) {
                    screen.setSnapshot(s);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(TP_ACK, (client, handler, buf, responseSender) -> {
            boolean ok = buf.readBoolean();
            int rx = 0, ry = 0, rz = 0;
            if (ok) {
                rx = buf.readVarInt();
                ry = buf.readVarInt();
                rz = buf.readVarInt();
            }
            int fx = rx, fy = ry, fz = rz;
            client.execute(() -> {
                if (client.player == null) return;
                if (ok) client.player.displayClientMessage(Component.translatable("gui.roadweaver.map.teleport.success_pos", fx, fy, fz), true);
                else client.player.displayClientMessage(Component.translatable("gui.roadweaver.map.teleport.denied"), true);
            });
        });
    }

    public static void requestSnapshot(int minX, int minZ, int maxX, int maxZ) {
        FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
        out.writeVarInt(minX);
        out.writeVarInt(minZ);
        out.writeVarInt(maxX);
        out.writeVarInt(maxZ);
        ClientPlayNetworking.send(REQ_RECT, out);
    }

    public static void requestTeleport(int x, int y, int z) {
        FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
        out.writeVarInt(x);
        out.writeVarInt(y);
        out.writeVarInt(z);
        ClientPlayNetworking.send(TP_REQ, out);
    }
}
