package net.shiroha233.roadweaver.achievements;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AchievementService {
    private AchievementService() {}

    private static final ResourceLocation ADV_FIRST_1 = new ResourceLocation("roadweaver", "road_first_step_1");
    private static final ResourceLocation ADV_FIRST_2 = new ResourceLocation("roadweaver", "road_first_step_2");

    // 每个维度缓存道路的 XZ 哈希集合，避免每 Tick 反复遍历列表
    private static final Map<ServerLevel, Set<Long>> ROAD_INDEX = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, Integer> ROAD_COUNT = new ConcurrentHashMap<>();

    public static void tick(ServerLevel level) {
        if (level == null) return;
        ensureIndex(level);
        Set<Long> index = ROAD_INDEX.get(level);
        if (index == null || index.isEmpty()) return;
        for (ServerPlayer sp : level.getServer().getPlayerList().getPlayers()) {
            if (sp == null || sp.serverLevel() != level) continue;
            BlockPos feet = sp.blockPosition();
            // 站立判定：优先脚下方块 XZ
            long key = hashXZ(feet.getX(), feet.getZ());
            long keyBelow = hashXZ(feet.getX(), feet.getZ());
            if (index.contains(key) || index.contains(keyBelow)) {
                award(sp, ADV_FIRST_1, "entered_road");
                award(sp, ADV_FIRST_2, "entered_road");
            }
        }
    }

    private static void ensureIndex(ServerLevel level) {
        WorldDataProvider provider = WorldDataProvider.getInstance();
        java.util.List<Records.RoadData> list = provider.getRoadDataList(level);
        int size = (list == null) ? 0 : list.size();
        Integer prev = ROAD_COUNT.get(level);
        if (prev != null && prev == size && ROAD_INDEX.containsKey(level)) return;
        Set<Long> set = new HashSet<>();
        if (list != null) {
            for (Records.RoadData rd : list) {
                if (rd == null || rd.roadSegmentList() == null) continue;
                for (Records.RoadSegmentPlacement seg : rd.roadSegmentList()) {
                    if (seg == null || seg.positions() == null) continue;
                    for (BlockPos p : seg.positions()) {
                        set.add(hashXZ(p.getX(), p.getZ()));
                    }
                }
            }
        }
        ROAD_INDEX.put(level, set);
        ROAD_COUNT.put(level, size);
    }

    private static void award(ServerPlayer sp, ResourceLocation id, String criterion) {
        var adv = sp.server.getAdvancements().getAdvancement(id);
        if (adv == null) return;
        var progress = sp.getAdvancements().getOrStartProgress(adv);
        if (!progress.isDone()) {
            // 使用与 JSON 中一致的 criterion 名称授予（impossible 触发器）
            sp.getAdvancements().award(adv, criterion);
        }
    }

    private static long hashXZ(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }
}
