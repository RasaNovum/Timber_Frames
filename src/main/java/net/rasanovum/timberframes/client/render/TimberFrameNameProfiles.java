package net.rasanovum.timberframes.client.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.timberframes.TimberFrameAssets;
import net.rasanovum.timberframes.items.TimberFrameItem;
import net.rasanovum.timberframes.util.VersionUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

/** Client-side locale selection and parsing for server-owned name profiles. */
public final class TimberFrameNameProfiles {
    private static final String FOLDER = "timber_frame_names/";
    private static final String JSON_SUFFIX = ".json";
    private static volatile Map<ResourceLocation, TimberFrameItem.NameProfile> profiles = Map.of();

    private TimberFrameNameProfiles() {
    }

    public static void refresh() {
        String locale = net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected();
        Map<ResourceLocation, Candidate> candidates = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, byte[]> entry : TimberFrameAssets.getNameAssets().entrySet()) {
            ProfileId id = parseId(entry.getKey());
            if (!id.locale().equals(locale) && !id.locale().equals("en_us")) continue;

            int priority = id.locale().equals(locale) ? 0 : 1;
            Candidate previous = candidates.get(id.profile());
            if (previous != null && previous.priority() <= priority) continue;
            JsonObject json = JsonParser.parseString(new String(entry.getValue(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            candidates.put(id.profile(), new Candidate(priority, new TimberFrameItem.NameProfile(
                    json.has("suffix") ? json.get("suffix").getAsString() : null,
                    json.has("noise") ? json.getAsJsonArray("noise").asList().stream()
                            .map(value -> value.getAsString()).collect(java.util.stream.Collectors.toSet())
                            : java.util.Set.of()
            )));
        }

        Map<ResourceLocation, TimberFrameItem.NameProfile> loaded = new LinkedHashMap<>();
        candidates.forEach((id, candidate) -> loaded.put(id, candidate.profile()));
        profiles = Map.copyOf(loaded);
    }

    public static void clear() {
        profiles = Map.of();
    }

    public static Map<ResourceLocation, TimberFrameItem.NameProfile> get() {
        return profiles;
    }

    private static ProfileId parseId(ResourceLocation assetId) {
        String path = assetId.getPath();
        if (!path.startsWith(FOLDER) || !path.endsWith(JSON_SUFFIX)) return null;

        String relative = path.substring(FOLDER.length(), path.length() - JSON_SUFFIX.length());
        int separator = relative.indexOf('/');
        if (separator <= 0 || separator == relative.length() - 1) return null;
        return new ProfileId(
                VersionUtils.getLocation(assetId.getNamespace(), relative.substring(separator + 1)),
                relative.substring(0, separator)
        );
    }

    private record ProfileId(ResourceLocation profile, String locale) {
    }

    private record Candidate(int priority, TimberFrameItem.NameProfile profile) {
    }
}
