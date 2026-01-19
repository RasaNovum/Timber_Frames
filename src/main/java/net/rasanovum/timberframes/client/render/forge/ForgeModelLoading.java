package net.rasanovum.timberframes.client.render;

import net.minecraft.client.resources.model.BakedModel;
import net.rasanovum.timberframes.TimberFrames;

//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
*///?} else if neoforge {
/*import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
*///?}

/** Loader-native model loading hooks kept out of the common client entrypoint. */
//? if forge
@Mod.EventBusSubscriber(modid = TimberFrames.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
//? if neoforge
/*@EventBusSubscriber(modid = TimberFrames.MODID, value = Dist.CLIENT)*/
public final class ForgeModelLoading {
    private ForgeModelLoading() {}

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((modelId, model) -> isTimberFrame(modelId)
                ? new ForgeTimberFrameBakedModel(model)
                : model);
    }

    //? if forge {
    private static boolean isTimberFrame(ResourceLocation modelId) {
        return isTimberFrame(modelId.getNamespace(), modelId.getPath());
    }
    //?} else if neoforge {
    /*private static boolean isTimberFrame(ModelResourceLocation modelId) {
        return isTimberFrame(modelId.id().getNamespace(), modelId.id().getPath());
    }
    *///?}

    private static boolean isTimberFrame(String namespace, String path) {
        return namespace.equals(TimberFrames.MODID) && path.contains("timber_frame");
    }
}
