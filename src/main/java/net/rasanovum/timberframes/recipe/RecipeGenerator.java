package net.rasanovum.timberframes.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.runeweaver.Runeweaver;
import net.rasanovum.runeweaver.enums.ErrorPolicy;
import net.rasanovum.runeweaver.enums.Lifetime;
import net.rasanovum.runeweaver.util.Index;
import net.rasanovum.runeweaver.util.functions.Event;
import net.rasanovum.timberframes.TimberFrameAssets;
import net.rasanovum.timberframes.TimberFrames;
import net.rasanovum.timberframes.util.VersionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RecipeGenerator {
    //? if <1.21 {
    /*private static final Index GENERATOR_RESOURCE = new Index("timber_frames:recipes/timber_frame_generator");
    *///?} else {
    private static final Index GENERATOR_RESOURCE = new Index("timber_frames:recipe/timber_frame_generator");
    //?}

    //? if <1.21 {
    /*private static final String BLOCK_TAG_PREFIX = "tags/blocks/";
    private static final String ITEM_TAG_PREFIX = "tags/items/";
    *///?} else {
    private static final String BLOCK_TAG_PREFIX = "tags/block/";
    private static final String ITEM_TAG_PREFIX = "tags/item/";
    //?}
    //? if <1.21 {
    /*private static final String GROUP_RESOURCE_PREFIX = "recipes/timber_frame_groups/";
    *///?} else {
    private static final String GROUP_RESOURCE_PREFIX = "recipe/timber_frame_groups/";
    //?}
    private static final Map<ResourceLocation, List<String>> BLOCK_TAGS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<String>> ITEM_TAGS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, GroupDefinition> GROUPS = new ConcurrentHashMap<>();
    private static final Object TAG_STATE_LOCK = new Object();
    private static volatile CompletableFuture<Void> tagsReady = new CompletableFuture<>();
    private static volatile boolean resetOnNextTagPass;
    private static volatile List<TimberVariant> timberVariants = List.of();
    private static volatile Map<ResourceLocation, ResourceLocation> backgroundTextures = Map.of();
    private static volatile long variantRevision;
    private static volatile Runnable clientRefresh = () -> {};
    private static boolean registered;

    private RecipeGenerator() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        register(Runeweaver.DEFAULT_PRIORITY, "timber_frames:collect_log_tags",
                RecipeGenerator::isTagResource, RecipeGenerator::collectTag);
        register(Runeweaver.DEFAULT_PRIORITY, "timber_frames:collect_timber_groups",
                RecipeGenerator::isGroupResource, RecipeGenerator::collectGroup);
        register(Runeweaver.DEFAULT_PRIORITY + 1, "timber_frames:finish_collecting_log_tags",
                RecipeGenerator::isTagResource, context -> tagsReady.complete(null));
        register(Runeweaver.DEFAULT_PRIORITY + 2, "timber_frames:generate_recipes",
                RecipeGenerator::isGeneratorResource, RecipeGenerator::generateRecipes);
    }

    private static void register(int priority, String name, Predicate<Index> filter,
                                 Event<JsonElement> event) {
        Runeweaver.registerEvent(priority, Lifetime.PERSISTENT, ErrorPolicy.THROW, name, filter, event);
    }

    public static List<ResourceLocation> getTimberIds() {
        return timberVariants.stream().map(TimberVariant::timberId).distinct().toList();
    }

    public static long getVariantRevision() {
        return variantRevision;
    }

    public static List<TimberVariant> getTimberVariants() {
        return timberVariants;
    }

    public static ResourceLocation backgroundTexture(ResourceLocation timberId) {
        return backgroundTextures.get(timberId);
    }

    public static void setClientTimberVariants(List<TimberVariant> variants) {
        setTimberVariants(variants);
        clientRefresh.run();
    }

    public static void setClientRefresh(Runnable refresh) {
        clientRefresh = refresh != null ? refresh : () -> {};
    }

    public static void prepareForNextReload() {
        resetOnNextTagPass = true;
    }

    private static boolean isTagResource(Index index) {
        String path = index.id().getPath();
        return path.startsWith(BLOCK_TAG_PREFIX) || path.startsWith(ITEM_TAG_PREFIX);
    }

    private static boolean isGroupResource(Index index) {
        String path = index.id().getPath();
        return path.startsWith(GROUP_RESOURCE_PREFIX);
    }

    private static void resetForNextReloadIfNeeded() {
        if (!resetOnNextTagPass) return;
        synchronized (TAG_STATE_LOCK) {
            if (!resetOnNextTagPass) return;
            BLOCK_TAGS.clear();
            ITEM_TAGS.clear();
            GROUPS.clear();
            setTimberVariants(List.of());
            tagsReady = new CompletableFuture<>();
            resetOnNextTagPass = false;
        }
    }

    private static void collectTag(net.rasanovum.runeweaver.EventContext<JsonElement> context) {
        resetForNextReloadIfNeeded();

        String path = context.getIndex().id().getPath();
        boolean blockTag = path.startsWith(BLOCK_TAG_PREFIX);
        String prefix = blockTag ? BLOCK_TAG_PREFIX : ITEM_TAG_PREFIX;
        ResourceLocation tagId = VersionUtils.getLocation(
                context.getIndex().id().getNamespace(),
                path.substring(prefix.length())
        );

        Map<ResourceLocation, List<String>> tags = blockTag ? BLOCK_TAGS : ITEM_TAGS;
        List<String> values = tags.computeIfAbsent(tagId, ignored -> new CopyOnWriteArrayList<>());
        JsonObject json = context.getFile().getAsJsonObject();
        if (json.has("replace") && json.get("replace").getAsBoolean()) {
            values.clear();
        }

        if (!json.has("values")) return;
        for (JsonElement value : json.getAsJsonArray("values")) {
            String id = value.isJsonObject()
                    ? value.getAsJsonObject().get("id").getAsString()
                    : value.getAsString();
            values.add(id);
        }
    }

    private static void collectGroup(net.rasanovum.runeweaver.EventContext<JsonElement> context) {
        resetForNextReloadIfNeeded();

        String path = context.getIndex().id().getPath();
        String groupPath = path.substring(GROUP_RESOURCE_PREFIX.length());
        if (groupPath.endsWith(".json")) {
            groupPath = groupPath.substring(0, groupPath.length() - ".json".length());
        }
        ResourceLocation groupId = VersionUtils.getLocation(
                context.getIndex().id().getNamespace(), groupPath
        );

        JsonObject json = context.getFile().getAsJsonObject();
        GROUPS.put(groupId, new GroupDefinition(
                readTagIds(json, "target", "block_tag", groupId),
                readTagIds(json, "inputs", "item_tag", groupId),
                readPatterns(json, "target", groupId),
                readPatterns(json, "inputs", groupId),
                GroupPresentation.read(json, groupId)
        ));
        context.markForDeletion(true);
    }

    private static List<ResourceLocation> readTagIds(JsonObject group, String selectorName,
                                                     String fieldName, ResourceLocation groupId) {
        JsonObject selector = required(group, selectorName, groupId, selectorName, JsonObject.class);
        JsonArray alternatives = required(selector, "any", groupId, selectorName + ".any", JsonArray.class);
        if (alternatives.isEmpty()) {
            throw schemaError(groupId, selectorName + ".any", "must not be empty");
        }
        List<ResourceLocation> tagIds = new ArrayList<>();
        for (int i = 0; i < alternatives.size(); i++) {
            JsonElement alternative = alternatives.get(i);
            if (!alternative.isJsonObject()) {
                throw schemaError(groupId, selectorName + ".any[" + i + "]", "must be an object");
            }
            String tagId = requiredString(
                    alternative.getAsJsonObject(), fieldName, groupId,
                    selectorName + ".any[" + i + "]." + fieldName
            );
            try {
                tagIds.add(VersionUtils.getLocation(tagId));
            } catch (IllegalArgumentException exception) {
                throw schemaError(
                        groupId, selectorName + ".any[" + i + "]." + fieldName,
                        "must be a valid resource location: " + tagId
                );
            }
        }
        return List.copyOf(tagIds);
    }

    private static List<GlobPattern> readPatterns(JsonObject group, String selectorName,
                                                  ResourceLocation groupId) {
        JsonObject match = required(group, "match", groupId, "match", JsonObject.class);
        JsonArray patterns = required(match, selectorName, groupId, "match." + selectorName, JsonArray.class);
        if (patterns.isEmpty()) {
            throw schemaError(groupId, "match." + selectorName, "must not be empty");
        }
        List<GlobPattern> result = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) {
            JsonElement pattern = patterns.get(i);
            if (!pattern.isJsonPrimitive() || !pattern.getAsJsonPrimitive().isString()) {
                throw schemaError(groupId, "match." + selectorName + "[" + i + "]", "must be a string");
            }
            String source = pattern.getAsString();
            try {
                result.add(GlobPattern.compile(source, i));
            } catch (IllegalArgumentException exception) {
                throw schemaError(
                        groupId, "match." + selectorName + "[" + i + "]", exception.getMessage()
                );
            }
        }
        return List.copyOf(result);
    }

    private static <T extends JsonElement> T required(JsonObject parent, String field,
                                                       ResourceLocation groupId, String fieldPath,
                                                       Class<T> type) {
        JsonElement value = parent.get(field);
        if (value == null || !type.isInstance(value)) {
            throw schemaError(groupId, fieldPath, "must be a " + type.getSimpleName());
        }
        return type.cast(value);
    }

    private static String requiredString(JsonObject parent, String field,
                                         ResourceLocation groupId, String fieldPath) {
        JsonPrimitive value = required(parent, field, groupId, fieldPath, JsonPrimitive.class);
        if (!value.isString()) {
            throw schemaError(groupId, fieldPath, "must be a string");
        }
        return value.getAsString();
    }

    private static IllegalArgumentException schemaError(ResourceLocation groupId,
                                                        String fieldPath, String detail) {
        return new IllegalArgumentException(
                "Invalid Timber Frames group " + groupId + ": " + fieldPath + " " + detail
        );
    }

    private static boolean isGeneratorResource(Index index) {
        return index.idEquals(GENERATOR_RESOURCE);
    }

    private static void generateRecipes(net.rasanovum.runeweaver.EventContext<JsonElement> context) {
        awaitTags();

        List<Variant> variants = new ArrayList<>();
        GROUPS.entrySet().stream()
                .sorted((a, b) -> a.getKey().toString().compareTo(b.getKey().toString()))
                .forEach(entry -> addGroupVariants(entry.getKey(), entry.getValue(), variants));

        setTimberVariants(variants.stream()
                .map(variant -> new TimberVariant(
                        variant.timberId(), variant.ingredientIds(),
                        variant.presentation().nameProfile(), variant.presentation().backgroundTexture()
                ))
                .toList());
        for (Variant variant : variants) {
            context.createResource(
                    new Index(recipeId(variant.groupId(), variant.timberId()).toString()),
                    createRecipeJson(variant.ingredientIds(), variant.timberId())
            );
        }

        context.markForDeletion(true);
        TimberFrames.LOGGER.info(
                "Generated {} dynamic timber frame recipes from {} datapack groups.",
                variants.size(), GROUPS.size()
        );
        TimberFrames.onTimberIdsChanged();
    }

    private static void awaitTags() {
        try {
            tagsReady.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for block and item tags", e);
        } catch (TimeoutException | ExecutionException e) {
            throw new IllegalStateException("Unable to collect block and item tags", e);
        }
    }

    private static void addGroupVariants(ResourceLocation groupId, GroupDefinition group,
                                         List<Variant> variants) {
        Set<ResourceLocation> targetMembers = resolveTags(
                group.targetTags(), BLOCK_TAGS, BuiltInRegistries.BLOCK::containsKey
        );
        Set<ResourceLocation> inputMembers = resolveTags(
                group.inputTags(), ITEM_TAGS, BuiltInRegistries.ITEM::containsKey
        );

        Map<String, TargetMatch> targetsByFamily = new LinkedHashMap<>();
        for (ResourceLocation targetId : targetMembers) {
            GlobMatch match = match(targetId, group.targetPatterns());
            if (match != null) {
                TargetMatch candidate = new TargetMatch(targetId, match.patternPriority());
                targetsByFamily.merge(match.familyKey(), candidate, RecipeGenerator::preferTarget);
            }
        }

        Map<String, List<ResourceLocation>> inputsByFamily = new LinkedHashMap<>();
        for (ResourceLocation inputId : inputMembers) {
            GlobMatch match = match(inputId, group.inputPatterns());
            if (match != null) {
                inputsByFamily.computeIfAbsent(match.familyKey(), ignored -> new ArrayList<>())
                        .add(inputId);
            }
        }

        int added = 0;
        for (String familyKey : targetsByFamily.keySet().stream().sorted().toList()) {
            List<ResourceLocation> inputs = inputsByFamily.get(familyKey);
            if (inputs == null || inputs.isEmpty()) continue;

            inputs.sort(Comparator.comparing(ResourceLocation::toString));
            variants.add(new Variant(
                    groupId,
                    group.presentation(),
                    List.copyOf(inputs),
                    targetsByFamily.get(familyKey).targetId()
            ));
            added++;
        }
        if (added == 0) {
            TimberFrames.LOGGER.warn("Timber group {} has no matching registered block items.", groupId);
        }
    }

    private static TargetMatch preferTarget(TargetMatch first, TargetMatch second) {
        int priority = Integer.compare(first.patternPriority(), second.patternPriority());
        if (priority != 0) return priority < 0 ? first : second;
        return first.targetId().toString().compareTo(second.targetId().toString()) <= 0 ? first : second;
    }

    private static GlobMatch match(ResourceLocation id, List<GlobPattern> patterns) {
        for (GlobPattern pattern : patterns) {
            GlobMatch match = pattern.match(id);
            if (match != null) return match;
        }
        return null;
    }

    private static void setTimberVariants(List<TimberVariant> variants) {
        timberVariants = List.copyOf(variants);
        backgroundTextures = timberVariants.stream()
                .filter(variant -> variant.backgroundTexture() != null)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        TimberVariant::timberId, TimberVariant::backgroundTexture
                ));
        variantRevision++;
    }

    private static Set<ResourceLocation> resolveTags(List<ResourceLocation> tagIds,
                                                      Map<ResourceLocation, List<String>> tags,
                                                      Predicate<ResourceLocation> isRegistered) {
        Set<ResourceLocation> resolved = new LinkedHashSet<>();
        for (ResourceLocation tagId : tagIds) {
            List<String> values = tags.get(tagId);
            if (values != null) {
                resolved.addAll(resolve(values, tags, new LinkedHashSet<>(), isRegistered));
            }
        }
        return resolved;
    }

    private static Set<ResourceLocation> resolve(List<String> values,
                                                  Map<ResourceLocation, List<String>> tags,
                                                  Set<ResourceLocation> resolving,
                                                  Predicate<ResourceLocation> isRegistered) {
        Set<ResourceLocation> resolved = new LinkedHashSet<>();
        for (String value : values) {
            if (value.startsWith("#")) {
                ResourceLocation tagId = VersionUtils.getLocation(value.substring(1));
                if (!resolving.add(tagId)) continue;
                List<String> nested = tags.get(tagId);
                if (nested != null) {
                    resolved.addAll(resolve(nested, tags, resolving, isRegistered));
                }
                resolving.remove(tagId);
            } else {
                ResourceLocation memberId = VersionUtils.getLocation(value);
                if (isRegistered.test(memberId)) {
                    resolved.add(memberId);
                }
            }
        }
        return resolved;
    }

    private static ResourceLocation recipeId(ResourceLocation groupId, ResourceLocation timberId) {
        String recipeDirectory;
        //? if <1.21 {
        /*recipeDirectory = "recipes";
        *///?} else {
        recipeDirectory = "recipe";
        //?}
        return VersionUtils.getLocation(
                TimberFrames.MODID,
                recipeDirectory + "/timber_frame_"
                        + groupId.getNamespace() + "_" + groupId.getPath()
                        + "_" + timberId.getNamespace() + "_" + timberId.getPath()
        );
    }

    private static JsonObject createRecipeJson(List<ResourceLocation> ingredientIds, ResourceLocation timberId) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shaped");
        json.addProperty("category", "building");

        JsonArray pattern = new JsonArray();
        pattern.add("SCS");
        pattern.add("CLC");
        pattern.add("SCS");
        json.add("pattern", pattern);

        JsonObject key = new JsonObject();
        JsonArray sandOptions = new JsonArray();
        sandOptions.add(itemTag("minecraft:sand"));
        sandOptions.add(itemTag("c:sands"));
        key.add("S", sandOptions);
        key.add("C", item("minecraft:clay"));
        JsonArray inputOptions = new JsonArray();
        ingredientIds.forEach(id -> inputOptions.add(item(id.toString())));
        key.add("L", inputOptions);
        json.add("key", key);

        JsonObject result = new JsonObject();
        result.addProperty("count", 8);
        //? if <1.21 {
        /*result.addProperty("item", TimberFrames.MODID + ":timber_frame");
        *///?} else {
        result.addProperty("id", TimberFrames.MODID + ":timber_frame");
        //?}

        //? if <1.21 {
        /*result.addProperty("nbt", "{timber_id:\"" + timberId + "\"}");
        *///?} else {
        JsonObject components = new JsonObject();
        JsonObject customData = new JsonObject();
        customData.addProperty("timber_id", timberId.toString());
        components.add("minecraft:custom_data", customData);
        result.add("components", components);
        //?}

        json.add("result", result);
        return json;
    }

    private static JsonObject item(String id) {
        JsonObject item = new JsonObject();
        item.addProperty("item", id);
        return item;
    }

    private static JsonObject itemTag(String id) {
        JsonObject tag = new JsonObject();
        tag.addProperty("tag", id);
        return tag;
    }

    private record Variant(ResourceLocation groupId, GroupPresentation presentation,
                           List<ResourceLocation> ingredientIds, ResourceLocation timberId) {
    }

    public record TimberVariant(ResourceLocation timberId, List<ResourceLocation> ingredientIds,
                                ResourceLocation nameProfile, ResourceLocation backgroundTexture) {
        public TimberVariant {
            ingredientIds = List.copyOf(ingredientIds);
        }
    }

    private record GroupDefinition(List<ResourceLocation> targetTags, List<ResourceLocation> inputTags,
                                   List<GlobPattern> targetPatterns, List<GlobPattern> inputPatterns,
                                   GroupPresentation presentation) {
    }

    private record TargetMatch(ResourceLocation targetId, int patternPriority) {
    }

    private record GlobMatch(String familyKey, int patternPriority) {
    }

    private record GlobPattern(Pattern regex, int patternPriority) {
        private static GlobPattern compile(String source, int patternPriority) {
            StringBuilder regex = new StringBuilder("^");
            boolean hasWildcard = false;
            for (int i = 0; i < source.length(); i++) {
                char character = source.charAt(i);
                if (character == '*') {
                    if (hasWildcard) {
                        throw new IllegalArgumentException(
                                "Timber Frames family glob may contain only one '*': " + source
                        );
                    }
                    hasWildcard = true;
                    regex.append("(.*)");
                } else {
                    regex.append(Pattern.quote(String.valueOf(character)));
                }
            }
            if (!hasWildcard) {
                throw new IllegalArgumentException(
                        "Timber Frames family glob must contain '*': " + source
                );
            }
            return new GlobPattern(Pattern.compile(regex.append('$').toString()), patternPriority);
        }

        private GlobMatch match(ResourceLocation id) {
            Matcher matcher = regex.matcher(id.getPath());
            return matcher.matches() ? new GlobMatch(matcher.group(1), patternPriority) : null;
        }
    }

}
