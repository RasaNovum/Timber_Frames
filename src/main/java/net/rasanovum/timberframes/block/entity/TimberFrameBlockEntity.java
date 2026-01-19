package net.rasanovum.timberframes.block.entity;

import net.minecraft.core.BlockPos;
//? if >=1.20.5
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rasanovum.timberframes.init.ContentInit;
import org.jetbrains.annotations.Nullable;

//? if forge {
/*import net.minecraftforge.client.model.data.ModelData;
import net.rasanovum.timberframes.client.render.ForgeTimberFrameBakedModel;
*///?} else if neoforge {
/*import net.neoforged.neoforge.client.model.data.ModelData;
import net.rasanovum.timberframes.client.render.ForgeTimberFrameBakedModel;
*///?}

public class TimberFrameBlockEntity extends BlockEntity {
    private ResourceLocation timberId = net.rasanovum.timberframes.util.VersionUtils.getLocation("minecraft:stripped_oak_log");

    public TimberFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ContentInit.TIMBER_FRAME_ENTITY.get(), pos, state);
    }

    public void setTimberId(ResourceLocation id) {
        this.timberId = id;
        this.setChanged();
        //? if forge {
        /*requestModelDataUpdate();
        *///?} else if neoforge {
        /*requestModelDataUpdate();
        *///?}
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ResourceLocation getTimberId() {
        return timberId;
    }

    //? if forge {
    /*@Override
    public ModelData getModelData() {
        return level == null ? ModelData.EMPTY
                : ForgeTimberFrameBakedModel.modelData(level, worldPosition, timberId);
    }
    *///?} else if neoforge {
    /*@Override
    public ModelData getModelData() {
        return level == null ? ModelData.EMPTY
                : ForgeTimberFrameBakedModel.modelData(level, worldPosition, timberId);
    }
    *///?}

    private void saveTimberId(CompoundTag tag) {
        tag.putString("timber_id", timberId.toString());
    }

    private void loadTimberId(CompoundTag tag) {
        if (tag.contains("timber_id")) {
            this.timberId = net.rasanovum.timberframes.util.VersionUtils.getLocation(tag.getString("timber_id"));
        }
    }

    //? if <1.20.5 {
    /*@Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveTimberId(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadTimberId(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveTimberId(tag);
        return tag;
    }
    *///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveTimberId(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadTimberId(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveTimberId(tag);
        return tag;
    }
    //?}

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
