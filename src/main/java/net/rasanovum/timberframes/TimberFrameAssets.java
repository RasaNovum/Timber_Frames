package net.rasanovum.timberframes;

import net.minecraft.resources.ResourceLocation;
import net.rasanovum.runeweaver.rosetta.AssetChannel;
import net.rasanovum.timberframes.util.VersionUtils;

import java.util.Arrays;
import java.util.Map;

/** Server-owned Timber Frames presentation assets shared with clients through Runeweaver Rosetta. */
public final class TimberFrameAssets {
    private static final int MAX_NAME_ASSET_BYTES = 64 * 1024;
    private static final int MAX_NAME_TOTAL_BYTES = 512 * 1024;
    private static final int MAX_NAME_ASSETS = 256;
    private static final int MAX_BACKGROUND_ASSET_BYTES = 128 * 1024;
    private static final int MAX_BACKGROUND_TOTAL_BYTES = 2 * 1024 * 1024;
    private static final int MAX_BACKGROUND_ASSETS = 64;

    private static volatile Map<ResourceLocation, byte[]> nameAssets = Map.of();
    private static volatile Map<ResourceLocation, byte[]> backgroundAssets = Map.of();
    private static volatile long revision;
    private static volatile Runnable clientRefresh = () -> {};
    private static volatile Runnable clientBackgroundRefresh = () -> {};
    private static boolean initialized;

    private TimberFrameAssets() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        AssetChannel.register(id("name_profiles"), "timber_frame_names", ".json",
                MAX_NAME_ASSET_BYTES, MAX_NAME_TOTAL_BYTES, MAX_NAME_ASSETS,
                (channel, snapshot) -> acceptNames(snapshot));
        AssetChannel.register(id("backgrounds"), "timber_frame_backgrounds", ".png",
                MAX_BACKGROUND_ASSET_BYTES, MAX_BACKGROUND_TOTAL_BYTES, MAX_BACKGROUND_ASSETS,
                (channel, snapshot) -> acceptBackgrounds(snapshot));
    }

    public static void setClientRefresh(Runnable refresh) {
        clientRefresh = refresh != null ? refresh : () -> {};
    }

    public static void setClientBackgroundRefresh(Runnable refresh) {
        clientBackgroundRefresh = refresh != null ? refresh : () -> {};
    }

    public static Map<ResourceLocation, byte[]> getNameAssets() {
        return nameAssets;
    }

    public static Map<ResourceLocation, byte[]> getBackgroundAssets() {
        return backgroundAssets;
    }

    public static long getRevision() {
        return revision;
    }

    public static void clearClientSnapshots() {
        nameAssets = Map.of();
        backgroundAssets = Map.of();
        revision++;
        clientRefresh.run();
    }

    private static void acceptNames(Map<ResourceLocation, byte[]> snapshot) {
        nameAssets = Map.copyOf(snapshot);
        revision++;
        clientRefresh.run();
    }

    private static void acceptBackgrounds(Map<ResourceLocation, byte[]> snapshot) {
        Map<ResourceLocation, byte[]> previous = backgroundAssets;
        backgroundAssets = Map.copyOf(snapshot);
        revision++;
        clientRefresh.run();
        if (!backgroundSnapshotsEqual(previous, snapshot)) {
            clientBackgroundRefresh.run();
        }
    }

    private static boolean backgroundSnapshotsEqual(Map<ResourceLocation, byte[]> first,
                                                     Map<ResourceLocation, byte[]> second) {
        if (first.size() != second.size() || !first.keySet().equals(second.keySet())) return false;
        return first.entrySet().stream().allMatch(entry ->
                Arrays.equals(entry.getValue(), second.get(entry.getKey()))
        );
    }

    private static ResourceLocation id(String path) {
        return VersionUtils.getLocation(TimberFrames.MODID, path);
    }
}
