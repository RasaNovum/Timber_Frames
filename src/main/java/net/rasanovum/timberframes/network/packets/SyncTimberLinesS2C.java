package net.rasanovum.timberframes.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.rasanovum.rosetta.network.RosettaPacket;
import net.rasanovum.timberframes.storage.TimberData;
import net.rasanovum.timberframes.storage.TimberDataManager;

import java.util.List;

/** Full authoritative snapshot of one dimension's authored timber chunk. */
public record SyncTimberLinesS2C(
        ResourceLocation dimension,
        ChunkPos chunk,
        List<TimberData.TimberLine> lines
) implements RosettaPacket {
    public SyncTimberLinesS2C {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public SyncTimberLinesS2C(FriendlyByteBuf buffer) {
        this(buffer.readResourceLocation(), buffer.readChunkPos(), buffer.readList(TimberData.TimberLine::readNetwork));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension);
        buffer.writeChunkPos(chunk);
        buffer.writeCollection(lines, (target, line) -> line.writeNetwork(target));
    }

    public void handle(Level level, Player player) {
        if (level.isClientSide()) {
            TimberDataManager.setClientChunkLines(dimension, chunk, lines);
        }
    }
}
