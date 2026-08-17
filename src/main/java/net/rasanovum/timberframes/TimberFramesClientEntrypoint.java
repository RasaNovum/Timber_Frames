package net.rasanovum.timberframes;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.player.Player;
import net.rasanovum.rosetta.entrypoint.RosettaClientEntrypoint;
import net.rasanovum.timberframes.storage.TimberDataManager;
import net.rasanovum.timberframes.client.render.BroadKnifePreviewState;
import net.rasanovum.timberframes.client.render.TimberFrameNameProfiles;
import net.rasanovum.timberframes.client.render.TimberFrameDebugRenderer;
import net.rasanovum.timberframes.client.render.TimberFrameRenderSync;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTabs;
//? if <1.21 {
/*import net.minecraft.world.item.CreativeModeTab;
*///?}

import java.util.List;
import net.rasanovum.timberframes.items.TimberFrameItem;
import net.rasanovum.timberframes.recipe.RecipeGenerator;
import net.rasanovum.timberframes.mixins.client.MinecraftSearchTreesAccessor;

//? if fabric {
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.rasanovum.rosetta.event.ClientShaderHooks;
import net.rasanovum.timberframes.client.render.TimberFrameBakedModel;
import net.rasanovum.timberframes.init.ShaderInit;
import net.rasanovum.timberframes.util.VersionUtils;

import java.util.function.Function;
//?}

/** Client initialization and lifecycle callbacks routed through Rosetta. */
public final class TimberFramesClientEntrypoint implements RosettaClientEntrypoint {
    private long presentationRevision = -1L;
    private long appliedRevision = -1L;

    @Override
    public void initialize() {
        TimberDataManager.setClientRefresh(TimberFrameRenderSync::refresh);
        TimberFrameAssets.setClientRefresh(() -> Minecraft.getInstance().execute(() -> {
            TimberFrameNameProfiles.refresh();
            refreshPresentation();
        }));
        RecipeGenerator.setClientRefresh(() -> Minecraft.getInstance().execute(this::refreshPresentation));
        TimberFrameAssets.setClientBackgroundRefresh(() -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().reloadResourcePacks()));

        //? if fabric {
        Material plasterMaterial = new Material(InventoryMenu.BLOCK_ATLAS, VersionUtils.getLocation(TimberFrames.MODID, "block/plaster_white"));

        ModelLoadingPlugin.register(pluginContext -> {
            TimberFrameBakedModel.clearBlockSpriteCache();

            pluginContext.modifyModelAfterBake().register((model, context) -> {
                //? if <1.21 {
                /*ResourceLocation id = context.id();
                *///?} else {
                ResourceLocation id = context.resourceId();
                if (id == null && context.topLevelId() != null) {
                    id = context.topLevelId().id();
                }
                //?}

                //? if <1.21 {
                /*ResourceLocation blockId = id != null && id.getPath().startsWith("block/")
                        ? VersionUtils.getLocation(id.getNamespace(),
                        id.getPath().substring("block/".length()))
                        : id;
                *///?} else {
                ResourceLocation blockId = null;
                if (context.topLevelId() != null) {
                    ResourceLocation topLevelId = context.topLevelId().id();
                    blockId = topLevelId.getPath().startsWith("block/")
                            ? VersionUtils.getLocation(topLevelId.getNamespace(),
                            topLevelId.getPath().substring("block/".length()))
                            : topLevelId;
                } else if (id != null && id.getPath().startsWith("block/")) {
                    blockId = VersionUtils.getLocation(
                            id.getNamespace(), id.getPath().substring("block/".length()));
                }
                //?}
                if (blockId != null) {
                    TextureAtlasSprite sprite = model.getParticleIcon();
                    if (sprite != null) {
                        TimberFrameBakedModel.registerBlockSprite(blockId, sprite);
                    }
                }

                if (id != null && id.getNamespace().equals(TimberFrames.MODID) && id.getPath().contains("timber_frame")) {
                    Function<Material, TextureAtlasSprite> textureGetter = context.textureGetter();
                    TextureAtlasSprite plaster = textureGetter.apply(plasterMaterial);
                    return new TimberFrameBakedModel(model, plaster);
                }
                return model;
            });
        });

        ClientShaderHooks.register(registrar -> ShaderInit.registerShader(registrar::register));
        //?}
    }

    @Override
    public void onJoin(Player player) {
        TimberDataManager.clearClientLines();
        TimberDataManager.clearClientPreviewLine();
        refreshPresentation();
    }

    @Override
    public void onDisconnect() {
        TimberDataManager.clearClientLines();
        TimberDataManager.clearClientPreviewLine();
        TimberFrameAssets.clearClientSnapshots();
        TimberFrameNameProfiles.clear();
        presentationRevision = -1L;
        appliedRevision = -1L;
    }

    @Override
    public void renderWorld(PoseStack poseStack, ClientLevel level, Player player, float tickDelta, Frustum frustum) {
        BroadKnifePreviewState.update(level, player);
        TimberFrameDebugRenderer.render(poseStack);
    }

    private void refreshPresentation() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        long revision = RecipeGenerator.getVariantRevision() * 31 + TimberFrameAssets.getRevision();
        if (revision == presentationRevision && revision == appliedRevision) return;
        if (revision != presentationRevision) {
            TimberFrameItem.updateVariantNames(
                    RecipeGenerator.getTimberVariants(), TimberFrameNameProfiles.get()
            );
            presentationRevision = revision;
        }
        if (CreativeModeTabs.tryRebuildTabContents(
                FeatureFlags.DEFAULT_FLAGS, false, minecraft.level.registryAccess())) {
            //? if <1.21 {
            /*CreativeModeTabs.allTabs().forEach(CreativeModeTab::rebuildSearchTree);
            *///?} else {
            List<net.minecraft.world.item.ItemStack> items = List.copyOf(
                    CreativeModeTabs.searchTab().getSearchTabDisplayItems()
            );
            MinecraftSearchTreesAccessor searchTrees =
                    (MinecraftSearchTreesAccessor) minecraft.getConnection();
            searchTrees.timberFrames$searchTrees().updateCreativeTooltips(
                    minecraft.level.registryAccess(), items
            );
            searchTrees.timberFrames$searchTrees().updateCreativeTags(items);
            //?}
            appliedRevision = revision;
        }
    }

}
