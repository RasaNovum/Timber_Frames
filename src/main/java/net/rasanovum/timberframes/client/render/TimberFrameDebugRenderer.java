package net.rasanovum.timberframes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rasanovum.rosetta.util.ClientCompat;
import net.rasanovum.timberframes.block.TimberFrameBlock;
import net.rasanovum.timberframes.storage.TimberDataManager;

/** F3-only visualization of the rectangles emitted by the timber rasterizer. */
public final class TimberFrameDebugRenderer {
    private static final float BOX_PADDING = 0.002f;

    private TimberFrameDebugRenderer() {}

    public static void render(PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!showDebugScreen(minecraft)) return;
        if (!(minecraft.level != null && minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK)) return;

        BlockPos blockPos = hit.getBlockPos();
        if (!(minecraft.level.getBlockState(blockPos).getBlock() instanceof TimberFrameBlock)) return;

        TimberDataManager.ClientRenderSnapshot snapshot =
                TimberDataManager.clientRenderSnapshot(
                        minecraft.level,
                        new ChunkPos(blockPos.getX() >> 4, blockPos.getZ() >> 4)
                );
        Direction face = hit.getDirection();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        Vec3 camera = ClientCompat.cameraPosition(minecraft.gameRenderer.getMainCamera());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        TimberFrameModelGeometry.debugFace(
                face,
                TimberFrameModelGeometry.connections(minecraft.level, blockPos, face),
                blockPos,
                snapshot.lines(),
                snapshot.previewLine(),
                true,
                (x, y, width, height, cell, preview) ->
                        drawGroup(poseStack, lines, blockPos, face, x, y, width, height, cell, preview)
        );
        poseStack.popPose();

        buffers.endBatch(RenderType.lines());
    }

    private static boolean showDebugScreen(Minecraft minecraft) {
        //? if <1.21 {
        /*return minecraft.options.renderDebug;
        *///?} else {
        return minecraft.getDebugOverlay().showDebugScreen();
        //?}
    }

    private static void drawGroup(PoseStack poseStack, VertexConsumer lines, BlockPos blockPos,
                                  Direction face, int x, int y, int width, int height,
                                  int cell, boolean preview) {
        Vec3 first = facePoint(blockPos, face, x / 16f, y / 16f);
        Vec3 second = facePoint(blockPos, face, (x + width) / 16f, (y + height) / 16f);
        AABB box = new AABB(first, second).inflate(BOX_PADDING);
        float[] color = color(cell, preview);
        LevelRenderer.renderLineBox(poseStack, lines, box, color[0], color[1], color[2], 1.0f);
    }

    private static Vec3 facePoint(BlockPos blockPos, Direction face, float u, float v) {
        double x = blockPos.getX();
        double y = blockPos.getY();
        double z = blockPos.getZ();
        return switch (face) {
            case DOWN -> new Vec3(x + u, y, z + v);
            case UP -> new Vec3(x + u, y + 1, z + 1 - v);
            case NORTH -> new Vec3(x + 1 - u, y + v, z);
            case SOUTH -> new Vec3(x + u, y + v, z + 1);
            case WEST -> new Vec3(x, y + v, z + u);
            case EAST -> new Vec3(x + 1, y + v, z + 1 - u);
        };
    }

    private static float[] color(int cell, boolean preview) {
        if (preview) return new float[]{1.0f, 0.1f, 1.0f};
        if (cell < 0) return new float[]{0.55f, 0.55f, 0.8f};
        if (cell == 0) return new float[]{0.2f, 0.85f, 1.0f};
        return new float[]{1.0f, 0.55f, 0.1f};
    }
}
