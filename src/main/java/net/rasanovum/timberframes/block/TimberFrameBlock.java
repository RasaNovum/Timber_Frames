package net.rasanovum.timberframes.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//? if <1.21 {
/*import net.minecraft.world.level.BlockGetter;
*///?} else {
import net.minecraft.world.level.LevelReader;
//?}
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rasanovum.timberframes.block.entity.TimberFrameBlockEntity;
import net.rasanovum.timberframes.items.TimberFrameItem;
import net.rasanovum.timberframes.storage.TimberDataManager;
import org.jetbrains.annotations.Nullable;

public class TimberFrameBlock extends Block implements EntityBlock {
    public TimberFrameBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TimberFrameBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TimberFrameBlockEntity timberBE) {
            timberBE.setTimberId(TimberFrameItem.getTimberId(stack));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel && !newState.is(this)) {
            TimberDataManager.pruneLinesAt(serverLevel, pos);
        }
    }

    //? if <1.21 {
    /*@Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
    *///?} else {
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
    //?}
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TimberFrameBlockEntity timberFrame) {
            return TimberFrameItem.createVariantStack(timberFrame.getTimberId());
        }
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (blockEntity instanceof TimberFrameBlockEntity timberFrame) {
            if (!level.isClientSide) {
                popResource(level, pos, TimberFrameItem.createVariantStack(timberFrame.getTimberId()));
            }
        } else if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(asItem()));
        }
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005f);
    }
}
