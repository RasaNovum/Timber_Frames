package net.rasanovum.timberframes.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.rasanovum.timberframes.TimberFrames;
import net.rasanovum.timberframes.recipe.RecipeGenerator;
import net.rasanovum.timberframes.util.VersionUtils;

//? if <1.21 {
/*import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
*///?} else {
import net.minecraft.server.packs.resources.ResourceMetadata;
//?}

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Client-side block-atlas bridge for server-owned background PNGs. */
public final class TimberFrameBackgrounds {
    private static final int MAX_TEXTURE_DIMENSION = 256;

    private TimberFrameBackgrounds() {
    }

    public static TextureAtlasSprite sprite(ResourceLocation timberId, TextureAtlasSprite fallback) {
        ResourceLocation texture = RecipeGenerator.backgroundTexture(timberId);
        if (texture == null) return fallback;

        ResourceLocation spriteId = texture.getPath().startsWith("block/")
                ? texture : dynamicSpriteId(texture);
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(spriteId);
        return sprite == null || sprite.contents().name().getPath().contains("missingno") ? fallback : sprite;
    }

    /** Creates block-atlas sprite contents for the next client resource reload. */
    public static List<SpriteContents> createSprites() {
        List<SpriteContents> contents = new ArrayList<>();
        for (Map.Entry<ResourceLocation, byte[]> entry : net.rasanovum.timberframes.TimberFrameAssets
                .getBackgroundAssets().entrySet()) {
            NativeImage image;
            try {
                image = NativeImage.read(new ByteArrayInputStream(entry.getValue()));
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to decode Timber Frames background texture "
                        + entry.getKey(), exception);
            }
            if (image.getWidth() <= 0 || image.getHeight() <= 0
                    || image.getWidth() > MAX_TEXTURE_DIMENSION
                    || image.getHeight() > MAX_TEXTURE_DIMENSION) {
                throw new IllegalArgumentException("Timber Frames background texture " + entry.getKey()
                        + " exceeds " + MAX_TEXTURE_DIMENSION + "x" + MAX_TEXTURE_DIMENSION);
            }
            contents.add(createContents(dynamicSpriteId(entry.getKey()), image));
        }
        return contents;
    }

    private static SpriteContents createContents(ResourceLocation id, NativeImage image) {
        //? if <1.21 {
        /*return new SpriteContents(id, new FrameSize(image.getWidth(), image.getHeight()), image,
                AnimationMetadataSection.EMPTY);
        *///?} else {
        return new SpriteContents(id, new FrameSize(image.getWidth(), image.getHeight()), image,
                ResourceMetadata.EMPTY);
        //?}
    }

    private static ResourceLocation dynamicSpriteId(ResourceLocation assetId) {
        String path = assetId.getPath().endsWith(".png")
                ? assetId.getPath().substring(0, assetId.getPath().length() - ".png".length())
                : assetId.getPath();
        return VersionUtils.getLocation(
                TimberFrames.MODID, "dynamic_backgrounds/" + assetId.getNamespace() + "/" + path
        );
    }
}
