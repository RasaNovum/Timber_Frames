package net.rasanovum.timberframes.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.rasanovum.timberframes.storage.TimberData;
import net.rasanovum.timberframes.storage.TimberDataManager;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Requests a client chunk rebuild after the client render snapshot changes. */
public final class TimberFrameRenderSync {
    private static final Set<TimberData.TimberLine> PENDING_CHANGES = new HashSet<>();
    private static boolean refreshQueued;

    private TimberFrameRenderSync() {}

    public static void refresh(TimberDataManager.ClientRenderSnapshot before, TimberDataManager.ClientRenderSnapshot after) {
        Set<TimberData.TimberLine> changed = changedLines(before, after);
        if (changed.isEmpty()) return;

        boolean queueRefresh;
        synchronized (PENDING_CHANGES) {
            PENDING_CHANGES.addAll(changed);
            queueRefresh = !refreshQueued;
            refreshQueued = true;
        }
        if (!queueRefresh) return;

        Minecraft.getInstance().execute(TimberFrameRenderSync::drainRefresh);
    }

    private static void drainRefresh() {
        Set<TimberData.TimberLine> changed;
        synchronized (PENDING_CHANGES) {
            changed = Set.copyOf(PENDING_CHANGES);
            PENDING_CHANGES.clear();
            refreshQueued = false;
        }

        if (Minecraft.getInstance().level == null) return;
        LevelRenderer renderer = Minecraft.getInstance().levelRenderer;
        changed.forEach(line -> dirty(renderer, line));
    }

    private static Set<TimberData.TimberLine> changedLines(TimberDataManager.ClientRenderSnapshot before, TimberDataManager.ClientRenderSnapshot after) {
        Set<TimberData.TimberLine> changed = new HashSet<>(before.lines());
        for (TimberData.TimberLine line : after.lines()) {
            if (!changed.add(line)) changed.remove(line);
        }

        if (!Objects.equals(before.previewLine(), after.previewLine())) {
            if (before.previewLine() != null) changed.add(before.previewLine());
            if (after.previewLine() != null) changed.add(after.previewLine());
        }
        return changed;
    }

    private static void dirty(LevelRenderer renderer, TimberData.TimberLine line) {
        int minX = Math.min(line.start().getX(), line.end().getX()) - 1;
        int minY = Math.min(line.start().getY(), line.end().getY()) - 1;
        int minZ = Math.min(line.start().getZ(), line.end().getZ()) - 1;
        int maxX = Math.max(line.start().getX(), line.end().getX()) + 1;
        int maxY = Math.max(line.start().getY(), line.end().getY()) + 1;
        int maxZ = Math.max(line.start().getZ(), line.end().getZ()) + 1;
        renderer.setBlocksDirty(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
