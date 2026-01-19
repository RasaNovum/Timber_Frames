package net.rasanovum.timberframes.items;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.rasanovum.timberframes.init.ContentInit;
import net.rasanovum.timberframes.recipe.RecipeGenerator;
import net.rasanovum.timberframes.util.ItemCustomData;
import net.rasanovum.timberframes.util.VersionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TimberFrameItem extends BlockItem {
    private static final ResourceLocation DEFAULT_TIMBER_ID = VersionUtils.getLocation("minecraft:stripped_oak_log");
    private static volatile Map<ResourceLocation, Component> VARIANT_NAMES = Map.of();

    public TimberFrameItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public static ItemStack createVariantStack(ResourceLocation timberId) {
        ItemStack stack = new ItemStack(ContentInit.TIMBER_FRAME.get());
        setTimberId(stack, timberId);
        return stack;
    }

    public static ResourceLocation getTimberId(ItemStack stack) {
        CompoundTag tag = ItemCustomData.copy(stack);
        if (!tag.contains("timber_id")) return DEFAULT_TIMBER_ID;

        try {
            return VersionUtils.getLocation(tag.getString("timber_id"));
        } catch (IllegalArgumentException ignored) {
            return DEFAULT_TIMBER_ID;
        }
    }

    public static void setTimberId(ItemStack stack, ResourceLocation timberId) {
        CompoundTag tag = new CompoundTag();
        tag.putString("timber_id", timberId.toString());
        ItemCustomData.set(stack, tag);
    }

    public static void updateVariantNames(List<RecipeGenerator.TimberVariant> variants,
                                          Map<ResourceLocation, NameProfile> profiles) {
        Map<ResourceLocation, Component> names = new LinkedHashMap<>();
        for (RecipeGenerator.TimberVariant variant : variants) {
            NameProfile profile = profiles.getOrDefault(variant.nameProfile(), NameProfile.DEFAULT);
            List<String> inputNames = variant.ingredientIds().stream()
                    .filter(BuiltInRegistries.ITEM::containsKey)
                    .map(id -> new ItemStack(BuiltInRegistries.ITEM.get(id)).getHoverName().getString())
                    .distinct()
                    .toList();
            String commonName = commonName(inputNames, profile.noise());
            if (commonName != null) {
                names.put(variant.timberId(), Component.translatable(
                        "block.timber_frames.timber_frame_variant",
                        Component.literal(commonName), profile.suffixComponent()
                ));
            }
        }
        VARIANT_NAMES = Map.copyOf(names);
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation timberId = getTimberId(stack);
        Component variantName = VARIANT_NAMES.get(timberId);
        if (variantName != null) return variantName;

        ResourceLocation materialId = materialId(timberId);
        Block material = BuiltInRegistries.BLOCK.get(materialId);

        return Component.translatable(
                "block.timber_frames.timber_frame_variant",
                Component.translatable(material.getDescriptionId()),
                NameProfile.DEFAULT.suffixComponent()
        );
    }

    private static String commonName(List<String> names, Set<String> noise) {
        if (names.isEmpty()) return null;

        List<List<String>> tokenLists = names.stream()
                .map(TimberFrameItem::tokenize)
                .filter(tokens -> !tokens.isEmpty())
                .toList();
        if (tokenLists.isEmpty()) return null;

        List<String> first = tokenLists.get(0);
        List<String> best = List.of();
        for (int start = 0; start < first.size(); start++) {
            for (int end = start + 1; end <= first.size(); end++) {
                List<String> candidate = first.subList(start, end);
                if (!tokenLists.stream().allMatch(tokens -> contains(tokens, candidate))) continue;

                List<String> meaningful = tokenLists.size() > 1
                        ? candidate.stream()
                        .filter(token -> !noise.contains(token.toLowerCase(Locale.ROOT)))
                        .toList()
                        : candidate;
                if (meaningful.size() <= best.size()) continue;
                if (!meaningful.isEmpty()) {
                    best = List.copyOf(meaningful);
                }
            }
        }
        return best.isEmpty() ? null : String.join(" ", best);
    }

    private static List<String> tokenize(String name) {
        String[] rawTokens = name.split("[^\\p{L}\\p{N}]+");
        List<String> tokens = new ArrayList<>();
        for (String token : rawTokens) {
            if (!token.isEmpty()) tokens.add(token);
        }
        return tokens;
    }

    private static boolean contains(List<String> tokens, List<String> candidate) {
        for (int start = 0; start + candidate.size() <= tokens.size(); start++) {
            boolean match = true;
            for (int i = 0; i < candidate.size(); i++) {
                if (!tokens.get(start + i).toLowerCase(Locale.ROOT)
                        .equals(candidate.get(i).toLowerCase(Locale.ROOT))) {
                    match = false;
                    break;
                }
            }
            if (match) return true;
        }
        return false;
    }

    private static ResourceLocation materialId(ResourceLocation timberId) {
        String path = timberId.getPath();
        if (path.startsWith("stripped_")) {
            return VersionUtils.getLocation(timberId.getNamespace(), path.substring("stripped_".length()));
        }
        return timberId;
    }

    public record NameProfile(String suffix, Set<String> noise) {
        public static final NameProfile DEFAULT = new NameProfile(null, Set.of());

        public NameProfile {
            noise = noise == null ? Set.of() : noise.stream()
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }

        private Component suffixComponent() {
            return suffix == null || suffix.isBlank()
                    ? Component.translatable("block.timber_frames.timber_frame")
                    : Component.literal(suffix);
        }
    }
}
