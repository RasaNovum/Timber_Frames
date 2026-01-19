//? if fabric {
package net.rasanovum.timberframes.client.render;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rasanovum.timberframes.block.entity.TimberFrameBlockEntity;
import net.rasanovum.timberframes.items.TimberFrameItem;
import net.rasanovum.timberframes.storage.TimberDataManager;
import net.rasanovum.timberframes.TimberFrames;
import net.rasanovum.timberframes.util.VersionUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Fabric Renderer API adapter for the shared timber-frame rasterizer. */
public final class TimberFrameBakedModel implements BakedModel, FabricBakedModel {
    private static final Map<ResourceLocation, TextureAtlasSprite> BLOCK_SPRITES = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> MISSING_SPRITES_LOGGED = ConcurrentHashMap.newKeySet();
    private final BakedModel originalModel;
    private final TextureAtlasSprite backgroundSprite;

    public TimberFrameBakedModel(BakedModel originalModel, TextureAtlasSprite backgroundSprite) {
        this.originalModel = originalModel;
        this.backgroundSprite = backgroundSprite;
    }

    public static void clearBlockSpriteCache() {
        BLOCK_SPRITES.clear();
        MISSING_SPRITES_LOGGED.clear();
    }

    public static void registerBlockSprite(ResourceLocation blockId, TextureAtlasSprite sprite) {
        BLOCK_SPRITES.put(blockId, sprite);
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos,
                               Supplier<RandomSource> randomSupplier, RenderContext context) {
        TimberFrameBlockEntity blockEntity = blockView.getBlockEntity(pos) instanceof TimberFrameBlockEntity timberFrame
                ? timberFrame : null;
        TextureAtlasSprite timberSprite = getSprite(blockEntity != null
                ? blockEntity.getTimberId() : TimberFrameModelGeometry.DEFAULT_TIMBER_ID);
        ResourceLocation timberId = blockEntity != null
                ? blockEntity.getTimberId() : TimberFrameModelGeometry.DEFAULT_TIMBER_ID;
        TextureAtlasSprite background = TimberFrameBackgrounds.sprite(timberId, backgroundSprite);

        Level level = blockView instanceof Level l ? l : Minecraft.getInstance().level;
        TimberDataManager.ClientRenderSnapshot snapshot =
                level != null ? TimberDataManager.clientRenderSnapshot(level, new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4)) : TimberDataManager.ClientRenderSnapshot.EMPTY;

        QuadEmitter emitter = context.getEmitter();
        for (Direction face : Direction.values()) {
            TimberFrameModelGeometry.renderFace(
                    face,
                    TimberFrameModelGeometry.connections(blockView, pos, face),
                    pos,
                    snapshot.lines(),
                    snapshot.previewLine(),
                    true,
                    (direction, left, bottom, right, top, timber, color) ->
                            draw(emitter, direction, left, bottom, right, top,
                                    timber ? timberSprite : background, color)
            );
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        TextureAtlasSprite timberSprite = getSprite(TimberFrameItem.getTimberId(stack));
        TextureAtlasSprite background = TimberFrameBackgrounds.sprite(TimberFrameItem.getTimberId(stack), backgroundSprite);
        QuadEmitter emitter = context.getEmitter();
        for (Direction face : Direction.values()) {
            TimberFrameModelGeometry.renderItemFace(
                    face,
                    (direction, left, bottom, right, top, timber, color) ->
                            draw(emitter, direction, left, bottom, right, top,
                                    timber ? timberSprite : background, color)
            );
        }
    }

    private void draw(QuadEmitter emitter, Direction direction, float left, float bottom, float right, float top,
                      TextureAtlasSprite sprite, int color) {
        if (isMissing(sprite)) return;

        emitter.square(direction, left, bottom, right, top, 0);
        emitter.spriteBake(0, sprite, MutableQuadView.BAKE_LOCK_UV);
        emitter.spriteColor(0, color, color, color, color);
        emitter.cullFace(direction).nominalFace(direction).emit();
    }

    private static boolean isMissing(TextureAtlasSprite sprite) {
        return sprite == null || sprite.contents().name().getPath().contains("missingno");
    }

    private static TextureAtlasSprite getSprite(ResourceLocation timberId) {
        TextureAtlasSprite sprite = BLOCK_SPRITES.get(timberId);
        if (sprite == null) {
            sprite = Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                    .apply(VersionUtils.getLocation(timberId.getNamespace(), "block/" + timberId.getPath()));
        }
        if (sprite == null && MISSING_SPRITES_LOGGED.add(timberId)) {
            TimberFrames.LOGGER.warn("No baked timber sprite registered for {}", timberId);
        }
        return sprite;
    }

    @Override public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
        return state == null ? List.of() : originalModel.getQuads(state, face, random);
    }

    @Override public boolean isVanillaAdapter() { return false; }

    @Override public boolean useAmbientOcclusion() { return originalModel.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return originalModel.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return backgroundSprite; }
    @Override public ItemTransforms getTransforms() { return originalModel.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return originalModel.getOverrides(); }
}
//?}
