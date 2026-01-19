package net.rasanovum.timberframes.init;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.function.Consumer;

public class ShaderInit {
    public static ShaderInstance baseShader;

    @FunctionalInterface
    public interface ShaderRegistrar {
        void register(ResourceLocation id, VertexFormat format, Consumer<ShaderInstance> onLoad) throws IOException;
    }

    public static void registerShader(ShaderRegistrar registrar) throws IOException {
//        registrar.register(
//                ResourceLocation.fromNamespaceAndPath("timber_frames", "base"),
//                DefaultVertexFormat.POSITION_TEX,
//                shader -> baseShader = shader
//        );
    }
}