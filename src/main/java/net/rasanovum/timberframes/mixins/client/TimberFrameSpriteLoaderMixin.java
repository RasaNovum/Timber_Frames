package net.rasanovum.timberframes.mixins.client;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.rasanovum.timberframes.client.render.TimberFrameBackgrounds;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.ArrayList;

@Mixin(SpriteLoader.class)
public abstract class TimberFrameSpriteLoaderMixin {
    @Shadow @Final private ResourceLocation location;

    @ModifyVariable(method = "stitch", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), argsOnly = true)
    private List<SpriteContents> timberFrames$addBackgroundSprites(List<SpriteContents> sprites) {
        if (!InventoryMenu.BLOCK_ATLAS.equals(location)) return sprites;
        List<SpriteContents> mutable = new ArrayList<>(sprites);
        mutable.addAll(TimberFrameBackgrounds.createSprites());
        return mutable;
    }
}
