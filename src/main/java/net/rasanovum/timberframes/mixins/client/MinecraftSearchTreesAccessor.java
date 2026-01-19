package net.rasanovum.timberframes.mixins.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if >=1.21 {
import net.minecraft.client.multiplayer.SessionSearchTrees;
//?}

@Mixin(ClientPacketListener.class)
public interface MinecraftSearchTreesAccessor {
    //? if >=1.21 {
    @Accessor("searchTrees")
    SessionSearchTrees timberFrames$searchTrees();
    //?}
}
