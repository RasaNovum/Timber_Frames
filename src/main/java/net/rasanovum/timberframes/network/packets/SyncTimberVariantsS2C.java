package net.rasanovum.timberframes.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.rosetta.network.RosettaPacket;
import net.rasanovum.timberframes.recipe.RecipeGenerator;

import java.util.List;

/** Server-authoritative timber variants and their group presentation metadata. */
public record SyncTimberVariantsS2C(List<RecipeGenerator.TimberVariant> timberVariants) implements RosettaPacket {
    public SyncTimberVariantsS2C {
        timberVariants = timberVariants == null ? List.of() : List.copyOf(timberVariants);
    }

    public SyncTimberVariantsS2C(FriendlyByteBuf buffer) {
        this(buffer.readList(SyncTimberVariantsS2C::readVariant));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeCollection(timberVariants, SyncTimberVariantsS2C::writeVariant);
    }

    public void handle(Level level, Player player) {
        if (level.isClientSide()) {
            RecipeGenerator.setClientTimberVariants(timberVariants);
        }
    }

    private static RecipeGenerator.TimberVariant readVariant(FriendlyByteBuf buffer) {
        ResourceLocation timberId = buffer.readResourceLocation();
        List<ResourceLocation> ingredientIds = buffer.readList(FriendlyByteBuf::readResourceLocation);
        ResourceLocation nameProfile = buffer.readResourceLocation();
        ResourceLocation backgroundTexture = buffer.readBoolean()
                ? buffer.readResourceLocation() : null;
        return new RecipeGenerator.TimberVariant(timberId, ingredientIds, nameProfile, backgroundTexture);
    }

    private static void writeVariant(FriendlyByteBuf buffer, RecipeGenerator.TimberVariant variant) {
        buffer.writeResourceLocation(variant.timberId());
        buffer.writeCollection(variant.ingredientIds(), FriendlyByteBuf::writeResourceLocation);
        buffer.writeResourceLocation(variant.nameProfile());
        buffer.writeBoolean(variant.backgroundTexture() != null);
        if (variant.backgroundTexture() != null) {
            buffer.writeResourceLocation(variant.backgroundTexture());
        }
    }
}
