package net.rasanovum.timberframes.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rasanovum.timberframes.block.entity.TimberFrameBlockEntity;
import net.rasanovum.timberframes.items.TimberFrameItem;
import net.rasanovum.timberframes.storage.TimberData;
import net.rasanovum.timberframes.storage.TimberDataManager;
import net.rasanovum.timberframes.util.VersionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

//? if forge {
/*import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
*///?} else if neoforge {
/*import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
*///?}

/** Forge/NeoForge baked-model adapter for the shared timber-frame rasterizer. */
public final class ForgeTimberFrameBakedModel implements BakedModel {
    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();
    private static final ChunkRenderTypeSet SOLID = ChunkRenderTypeSet.of(RenderType.solid());
    private static final ResourceLocation PLASTER_TEXTURE =
            VersionUtils.getLocation("timber_frames", "block/plaster_white");

    private final BakedModel originalModel;
    @Nullable
    private final ResourceLocation itemTimberId;

    public ForgeTimberFrameBakedModel(BakedModel originalModel) {
        this(originalModel, null);
    }

    private ForgeTimberFrameBakedModel(BakedModel originalModel, @Nullable ResourceLocation itemTimberId) {
        this.originalModel = originalModel;
        this.itemTimberId = itemTimberId;
    }

    public static ModelData modelData(BlockAndTintGetter blockView, BlockPos pos, ResourceLocation timberId) {
        Level level = blockView instanceof Level l ? l : Minecraft.getInstance().level;
        TimberDataManager.ClientRenderSnapshot snapshot = level != null
                ? TimberDataManager.clientRenderSnapshot(
                        level,
                        new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4)
                )
                : TimberDataManager.ClientRenderSnapshot.EMPTY;

        EnumMap<Direction, TimberFrameModelGeometry.Connections> connections = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
            connections.put(face, TimberFrameModelGeometry.connections(blockView, pos, face));
        }
        return ModelData.builder().with(RENDER_DATA,
                new RenderData(pos.immutable(), Map.copyOf(connections), snapshot.lines(), snapshot.previewLine(), timberId)).build();
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter blockView, BlockPos pos, BlockState state, ModelData modelData) {
        ResourceLocation timberId = blockView.getBlockEntity(pos) instanceof TimberFrameBlockEntity timberFrame
                ? timberFrame.getTimberId() : TimberFrameModelGeometry.DEFAULT_TIMBER_ID;
        return modelData.derive().with(RENDER_DATA, modelData(blockView, pos, timberId).get(RENDER_DATA)).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource random, ModelData modelData, @Nullable RenderType renderType) {
        if (state == null) {
            return renderItem(itemTimberId != null ? itemTimberId : TimberFrameModelGeometry.DEFAULT_TIMBER_ID);
        }
        if (renderType != null && renderType != RenderType.solid()) return List.of();

        RenderData data = modelData == null ? null : modelData.get(RENDER_DATA);
        if (data == null) {
            return originalModel.getQuads(state, side, random);
        }

        if (side == null) return List.of();

        TextureAtlasSprite timberSprite = sprite(data.timberId());
        TextureAtlasSprite plasterSprite = sprite(PLASTER_TEXTURE);
        TextureAtlasSprite backgroundSprite = TimberFrameBackgrounds.sprite(data.timberId(), plasterSprite);
        List<BakedQuad> quads = new ArrayList<>(272);
        TimberFrameModelGeometry.renderFace(
                side,
                data.connections().get(side),
                data.pos(),
                data.authoredLines(),
                data.previewLine(),
                true,
                (face, left, bottom, right, top, timber, color) ->
                        addQuad(quads, face, left, bottom, right, top,
                                timber ? timberSprite : backgroundSprite, color)
        );
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return getQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData modelData) {
        return SOLID;
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        return List.of(new ForgeTimberFrameBakedModel(originalModel, TimberFrameItem.getTimberId(stack)));
    }

    private static List<BakedQuad> renderItem(ResourceLocation timberId) {
        TextureAtlasSprite timberSprite = sprite(timberId);
        TextureAtlasSprite plasterSprite = sprite(PLASTER_TEXTURE);
        TextureAtlasSprite backgroundSprite = TimberFrameBackgrounds.sprite(timberId, plasterSprite);
        List<BakedQuad> quads = new ArrayList<>(Direction.values().length * 5);
        for (Direction face : Direction.values()) {
            TimberFrameModelGeometry.renderItemFace(
                    face,
                    (direction, left, bottom, right, top, timber, color) ->
                            addQuad(quads, direction, left, bottom, right, top,
                                    timber ? timberSprite : backgroundSprite, color)
            );
        }
        return quads;
    }

    private static void addQuad(List<BakedQuad> quads, Direction face,
                                float left, float bottom, float right, float top,
                                TextureAtlasSprite sprite, int argb) {
        if (isMissing(sprite)) return;

        float[][] positions = square(face, left, bottom, right, top);
        int abgr = (argb & 0xFF00FF00) | (argb & 0x00FF0000) >>> 16 | (argb & 0x000000FF) << 16;
        int normal = face.getStepX() * 127 & 0xFF
                | (face.getStepY() * 127 & 0xFF) << 8
                | (face.getStepZ() * 127 & 0xFF) << 16;
        int[] vertices = new int[32];

        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * 8;
            float x = positions[vertex][0], y = positions[vertex][1], z = positions[vertex][2];
            float[] uv = lockedUv(face, x, y, z);
            vertices[offset] = Float.floatToRawIntBits(x);
            vertices[offset + 1] = Float.floatToRawIntBits(y);
            vertices[offset + 2] = Float.floatToRawIntBits(z);
            vertices[offset + 3] = abgr;
            //? if <1.21 {
            vertices[offset + 4] = Float.floatToRawIntBits(sprite.getU(uv[0] * 16));
            vertices[offset + 5] = Float.floatToRawIntBits(sprite.getV(uv[1] * 16));
            //?} else {
            /*vertices[offset + 4] = Float.floatToRawIntBits(sprite.getU(uv[0]));
            vertices[offset + 5] = Float.floatToRawIntBits(sprite.getV(uv[1]));*/
            //?}
            vertices[offset + 6] = 0;
            vertices[offset + 7] = normal;
        }
        quads.add(new BakedQuad(vertices, -1, face, sprite, true));
    }

    /** Matches Fabric MutableQuadView.square(..., depth=0) vertex order exactly. */
    private static float[][] square(Direction face, float left, float bottom, float right, float top) {
        return switch (face) {
            case DOWN -> new float[][]{{left, 0, top}, {left, 0, bottom}, {right, 0, bottom}, {right, 0, top}};
            case UP -> new float[][]{{left, 1, 1 - top}, {left, 1, 1 - bottom}, {right, 1, 1 - bottom}, {right, 1, 1 - top}};
            case NORTH -> new float[][]{{1 - left, top, 0}, {1 - left, bottom, 0}, {1 - right, bottom, 0}, {1 - right, top, 0}};
            case SOUTH -> new float[][]{{left, top, 1}, {left, bottom, 1}, {right, bottom, 1}, {right, top, 1}};
            case WEST -> new float[][]{{0, top, left}, {0, bottom, left}, {0, bottom, right}, {0, top, right}};
            case EAST -> new float[][]{{1, top, 1 - left}, {1, bottom, 1 - left}, {1, bottom, 1 - right}, {1, top, 1 - right}};
        };
    }

    /** Matches Fabric's BAKE_LOCK_UV projection for each nominal face. */
    private static float[] lockedUv(Direction face, float x, float y, float z) {
        return switch (face) {
            case DOWN -> new float[]{x, 1 - z};
            case UP -> new float[]{x, z};
            case NORTH -> new float[]{1 - x, 1 - y};
            case SOUTH -> new float[]{x, 1 - y};
            case WEST -> new float[]{z, 1 - y};
            case EAST -> new float[]{1 - z, 1 - y};
        };
    }

    private static TextureAtlasSprite sprite(ResourceLocation textureId) {
        if (BuiltInRegistries.BLOCK.containsKey(textureId)) {
            Block block = BuiltInRegistries.BLOCK.get(textureId);
            TextureAtlasSprite particle = Minecraft.getInstance().getBlockRenderer()
                    .getBlockModel(block.defaultBlockState())
                    .getParticleIcon();
            if (!isMissing(particle)) return particle;
        }

        ResourceLocation resolved = textureId.getPath().startsWith("block/")
                ? textureId
                : VersionUtils.getLocation(textureId.getNamespace(), "block/" + textureId.getPath());
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(resolved);
    }

    private static boolean isMissing(TextureAtlasSprite sprite) {
        return sprite == null || sprite.contents().name().getPath().contains("missingno");
    }

    @Override public boolean useAmbientOcclusion() { return originalModel.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return originalModel.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return sprite(PLASTER_TEXTURE); }
    @Override public ItemTransforms getTransforms() { return originalModel.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return originalModel.getOverrides(); }

    private record RenderData(BlockPos pos,
                              Map<Direction, TimberFrameModelGeometry.Connections> connections,
                              List<TimberData.TimberLine> authoredLines,
                              @Nullable TimberData.TimberLine previewLine,
                              ResourceLocation timberId) {}
}
