package net.shiroha233.roadweaver.planning;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.*;

public final class RNGPlanner {
    private RNGPlanner() {}

    public static List<Records.StructureConnection> planRNG(List<BlockPos> points, int maxEdgeLenBlocks) {
        if (points == null || points.size() < 2) return List.of();
        ArrayList<BlockPos> unique = new ArrayList<>();
        HashSet<Long> seen = new HashSet<>();
        for (BlockPos p : points) {
            BlockPos q = new BlockPos(p.getX(), 0, p.getZ());
            long key = PlanningUtils.pos2dKey(q);
            if (seen.add(key)) unique.add(q);
        }
        int n = unique.size();
        if (n < 2) return List.of();
        long maxD2 = maxEdgeLenBlocks > 0 ? (long) maxEdgeLenBlocks * (long) maxEdgeLenBlocks : Long.MAX_VALUE;
        long[] xs = new long[n];
        long[] zs = new long[n];
        for (int i = 0; i < n; i++) { xs[i] = unique.get(i).getX(); zs[i] = unique.get(i).getZ(); }
        HashSet<Long> edgeKeys = new HashSet<>();
        ArrayList<Records.StructureConnection> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                long dx = xs[i] - xs[j];
                long dz = zs[i] - zs[j];
                long d2 = dx * dx + dz * dz;
                if (d2 > maxD2) continue;
                boolean blocked = false;
                for (int k = 0; k < n; k++) {
                    if (k == i || k == j) continue;
                    long dax = xs[i] - xs[k];
                    long daz = zs[i] - zs[k];
                    long dbx = xs[j] - xs[k];
                    long dbz = zs[j] - zs[k];
                    long da2 = dax * dax + daz * daz;
                    long db2 = dbx * dbx + dbz * dbz;
                    if (da2 < d2 && db2 < d2) { blocked = true; break; }
                }
                if (blocked) continue;
                long key = (((long) i) << 32) ^ (long) j;
                if (edgeKeys.add(key)) edges.add(new Records.StructureConnection(unique.get(i), unique.get(j)));
            }
        }
        return edges;
    }
}
