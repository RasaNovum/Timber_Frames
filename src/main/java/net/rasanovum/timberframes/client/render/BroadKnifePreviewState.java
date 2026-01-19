package net.rasanovum.timberframes.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.rasanovum.timberframes.init.ContentInit;
import net.rasanovum.timberframes.items.BroadKnife;
import net.rasanovum.timberframes.storage.TimberData;
import net.rasanovum.timberframes.storage.TimberDataManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public final class BroadKnifePreviewState {
    private static final long NO_TICK = Long.MIN_VALUE;
    private static TimberData.TimberLine pendingCandidate;
    private static long pendingCandidateTick = NO_TICK;

    private BroadKnifePreviewState() {}

    public static void update(ClientLevel level, Player player) {
        if (player.isSecondaryUseActive()) {
            clearPreview();
            return;
        }

        ItemStack stack = heldBroadKnife(player);
        if (stack.isEmpty()) {
            clearPreview();
            return;
        }

        BlockPos startVertex = BroadKnife.getStartVertex(stack).orElse(null);
        if (startVertex == null) {
            clearPreview();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            observeCandidate(level, null);
            return;
        }

        BlockPos hoveredBlock = hit.getBlockPos();
        if (!level.getBlockState(hoveredBlock).is(ContentInit.TIMBER_FRAME.get())) {
            observeCandidate(level, null);
            return;
        }

        TimberData.TimberLine candidate = nearestValidLine(
                startVertex, hoveredBlock, hit.getDirection(), hit.getLocation()
        );
        observeCandidate(level, candidate);
    }

    private static void observeCandidate(ClientLevel level, @Nullable TimberData.TimberLine candidate) {
        long tick = level.getGameTime();
        if (!Objects.equals(pendingCandidate, candidate)) {
            pendingCandidate = candidate;
            pendingCandidateTick = tick;
            return;
        }

        if (pendingCandidateTick < tick) {
            TimberDataManager.setClientPreviewLine(candidate);
            pendingCandidateTick = tick;
        }
    }

    private static void clearPreview() {
        pendingCandidate = null;
        pendingCandidateTick = NO_TICK;
        TimberDataManager.clearClientPreviewLine();
    }

    private static ItemStack heldBroadKnife(Player player) {
        if (player.getMainHandItem().getItem() instanceof BroadKnife) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof BroadKnife) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    @Nullable
    private static TimberData.TimberLine nearestValidLine(BlockPos start, BlockPos block, Direction face, Vec3 hit) {
        TimberData.TimberLine nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos corner : faceCorners(block, face)) {
            try {
                TimberData.TimberLine line = new TimberData.TimberLine(start, corner);
                double distance = hit.distanceTo(Vec3.atLowerCornerOf(corner));
                if (distance < nearestDistance) {
                    nearest = line;
                    nearestDistance = distance;
                }
            } catch (IllegalArgumentException ignored) {
                // This face corner cannot form a valid broad-knife line from the first corner.
            }
        }

        return nearest;
    }

    private static List<BlockPos> faceCorners(BlockPos block, Direction face) {
        BlockPos base = switch (face) {
            case UP, SOUTH, EAST -> block.relative(face);
            default -> block;
        };
        Direction horizontal = switch (face.getAxis()) {
            case X -> Direction.SOUTH;
            case Y, Z -> Direction.EAST;
        };
        Direction vertical = face.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;

        return List.of(
                base,
                base.relative(horizontal),
                base.relative(vertical),
                base.relative(horizontal).relative(vertical)
        );
    }
}
