package net.rasanovum.timberframes.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rasanovum.timberframes.TimberFrames;
import net.rasanovum.timberframes.init.ContentInit;
import net.rasanovum.timberframes.storage.TimberData;
import net.rasanovum.timberframes.storage.TimberDataManager;
import net.rasanovum.timberframes.util.ItemCustomData;
import net.rasanovum.timberframes.util.VersionUtils;

import java.util.Optional;

/**
 * Broad Knife tool for drawing structural diagonals.
 * Snaps clicks to the nearest world-space grid vertex.
 */
public class BroadKnife extends DiggerItem {
    public static final TagKey<Block> MINEABLE_BLOCKS = TagKey.create(
            Registries.BLOCK,
            VersionUtils.getLocation(TimberFrames.MODID, "mineable/broad_knife")
    );

    public BroadKnife(Properties properties) {
        //? if <1.21 {
        /*super(0.0F, -3.0F, Tiers.IRON, MINEABLE_BLOCKS, properties.stacksTo(1));
        *///?} else {
        super(Tiers.IRON, MINEABLE_BLOCKS, properties.stacksTo(1));
        //?}
    }

    public static Optional<BlockPos> getStartVertex(ItemStack stack) {
        CompoundTag tag = ItemCustomData.copy(stack);
        if (!tag.contains("StartVertex")) return Optional.empty();
        CompoundTag pos = tag.getCompound("StartVertex");
        return Optional.of(new BlockPos(pos.getInt("X"), pos.getInt("Y"), pos.getInt("Z")));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (world.isClientSide) {
            return trackClientStart(context, player);
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        ItemStack stack = context.getItemInHand();

        if (player.isSecondaryUseActive()) {
            if (getStartVertex(stack).isPresent()) {
                world.playSound(null, pos, SoundEvents.BAMBOO_BREAK,
                        SoundSource.BLOCKS, 0.8F, 0.8F);
            }
            ItemCustomData.clear(stack);
            return InteractionResult.SUCCESS;
        }

        if (!state.is(ContentInit.TIMBER_FRAME.get())) return InteractionResult.PASS;

        BlockPos clickedVertex = snappedVertex(context.getClickLocation());

        CompoundTag tag = ItemCustomData.copy(stack);

        if (!tag.contains("StartVertex")) {
            tag.put("StartVertex", writeBlockPos(clickedVertex));
            ItemCustomData.set(stack, tag);
            playUseSound(world, pos);
        } else {
            Optional<BlockPos> startVertexOpt = getStartVertex(stack);

            if (startVertexOpt.isPresent()) {
                BlockPos startVertex = startVertexOpt.get();
                try {
                    TimberData.TimberLine line = new TimberData.TimberLine(startVertex, clickedVertex);
                    TimberDataManager.toggleLine((net.minecraft.server.level.ServerLevel) world, line);

                    ItemCustomData.clear(stack);
                    finishLine(world, pos, player, stack, context.getHand());
                } catch (IllegalArgumentException exception) {
                    return InteractionResult.FAIL;
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    private static void playUseSound(Level world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BAMBOO_HIT, SoundSource.BLOCKS, 0.25F, 0.5F);
    }

    private static void finishLine(Level world, BlockPos pos, Player player, ItemStack stack, InteractionHand hand) {
        playUseSound(world, pos);
        //? if <1.21 {
        /*stack.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
        *///?} else {
        stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        //?}
    }

    private InteractionResult trackClientStart(UseOnContext context, Player player) {
        ItemStack stack = context.getItemInHand();
        if (player.isSecondaryUseActive()) {
            ItemCustomData.clear(stack);
            TimberDataManager.clearClientPreviewLine();
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        if (!context.getLevel().getBlockState(pos).is(ContentInit.TIMBER_FRAME.get())) {
            return InteractionResult.PASS;
        }

        BlockPos clickedVertex = snappedVertex(context.getClickLocation());
        Optional<BlockPos> startVertex = getStartVertex(stack);
        if (startVertex.isEmpty()) {
            CompoundTag tag = ItemCustomData.copy(stack);
            tag.put("StartVertex", writeBlockPos(clickedVertex));
            ItemCustomData.set(stack, tag);
        } else {
            try {
                TimberData.TimberLine line = new TimberData.TimberLine(startVertex.get(), clickedVertex);
                TimberDataManager.toggleClientLine(context.getLevel(), line);
                ItemCustomData.clear(stack);
            } catch (IllegalArgumentException ignored) {}
        }

        return InteractionResult.SUCCESS;
    }

    private static BlockPos snappedVertex(Vec3 hit) {
        return new BlockPos(
                (int) Math.round(hit.x),
                (int) Math.round(hit.y),
                (int) Math.round(hit.z)
        );
    }

    private static CompoundTag writeBlockPos(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        return tag;
    }
}
