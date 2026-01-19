package net.rasanovum.timberframes.recipe;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.timberframes.util.VersionUtils;

/** Optional client-facing presentation metadata attached to a generated group. */
record GroupPresentation(ResourceLocation nameProfile, ResourceLocation backgroundTexture) {
    private static final String BACKGROUND_ASSET_FOLDER = "timber_frame_backgrounds/";

    static GroupPresentation read(JsonObject group, ResourceLocation groupId) {
        JsonObject display = group.has("display") ? group.getAsJsonObject("display") : null;
        if (display == null) return new GroupPresentation(groupId, null);

        ResourceLocation nameProfile = display.has("name_profile")
                ? VersionUtils.getLocation(display.get("name_profile").getAsString())
                : groupId;
        if (!display.has("background_texture")) {
            return new GroupPresentation(nameProfile, null);
        }

        ResourceLocation background = VersionUtils.getLocation(
                display.get("background_texture").getAsString()
        );
        String path = background.getPath();
        if (!path.startsWith("block/")) {
            if (!path.startsWith(BACKGROUND_ASSET_FOLDER)) path = BACKGROUND_ASSET_FOLDER + path;
            if (!path.endsWith(".png")) path += ".png";
            background = VersionUtils.getLocation(background.getNamespace(), path);
        }
        return new GroupPresentation(nameProfile, background);
    }

}
