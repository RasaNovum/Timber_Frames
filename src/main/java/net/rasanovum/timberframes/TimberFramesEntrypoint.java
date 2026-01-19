package net.rasanovum.timberframes;

import net.rasanovum.rosetta.registry.RegistrationContext;

//? if fabric {
import net.fabricmc.api.ModInitializer;
//?} else if forge {
/*import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*///?} else if neoforge {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
*///?}

//? if forge || neoforge
/*@Mod(TimberFrames.MODID)*/
public final class TimberFramesEntrypoint /*? if fabric { */ implements ModInitializer /*? } */ {
    //? if fabric {
    @Override
    public void onInitialize() {
        TimberFrames.initialize(RegistrationContext.create());
    }
    //?} else if forge {
    /*public TimberFramesEntrypoint() {
        TimberFrames.initialize(RegistrationContext.create(FMLJavaModLoadingContext.get().getModEventBus()));
    }
    *///?} else if neoforge {
    /*public TimberFramesEntrypoint(IEventBus modEventBus) {
        TimberFrames.initialize(RegistrationContext.create(modEventBus));
    }
    *///?}
}
