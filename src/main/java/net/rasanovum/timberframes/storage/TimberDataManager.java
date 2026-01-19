package net.rasanovum.timberframes.storage;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.rasanovum.timberframes.init.ContentInit;
import net.rasanovum.rosetta.network.RosettaNetwork;
import net.rasanovum.timberframes.init.DataInit;
import net.rasanovum.timberframes.network.packets.SyncTimberLinesS2C;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Access and synchronization boundary for chunk attachments and client snapshots. */
public final class TimberDataManager {
    private static final Map<UUID, PlayerVisibility> PLAYER_VISIBILITY = new HashMap<>();
    private static volatile Map<ResourceLocation, Map<Long, List<TimberData.TimberLine>>> clientChunks = Map.of();
    private static volatile TimberData.TimberLine clientPreviewLine;
    private static volatile BiConsumer<ClientRenderSnapshot, ClientRenderSnapshot> clientRefresh = (before, after) -> {};

    private TimberDataManager() {}

    /** Toggles a normalized line and updates every chunk touched by its segments. */
    public static void toggleLine(ServerLevel level, TimberData.TimberLine line) {
        List<TimberData.TimberLine> segments = line.splitAtCorners();
        Map<ChunkPos, LevelChunk> affectedChunks = new LinkedHashMap<>();
        for (TimberData.TimberLine segment : segments) {
            for (ChunkPos chunkPos : segment.chunkPositions()) {
                affectedChunks.computeIfAbsent(
                        chunkPos,
                        ignored -> level.getChunk(chunkPos.x, chunkPos.z)
                );
            }
        }

        boolean remove = segments.stream().allMatch(segment -> containsInAnyChunk(segment, affectedChunks.values()));
        for (Map.Entry<ChunkPos, LevelChunk> entry : affectedChunks.entrySet()) {
            ChunkPos chunkPos = entry.getKey();
            LevelChunk chunk = entry.getValue();
            TimberData data = DataInit.TIMBER_DATA.find(chunk).orElse(null);
            boolean changed = false;

            if (remove) {
                if (data != null) {
                    for (TimberData.TimberLine segment : segments) {
                        changed |= data.removeLine(segment);
                    }
                }
                if (changed) {
                    if (data.lines().isEmpty()) DataInit.TIMBER_DATA.remove(chunk);
                    else DataInit.TIMBER_DATA.markDirty(chunk);
                }
            } else {
                if (data == null) data = new TimberData();
                for (TimberData.TimberLine segment : segments) {
                    changed |= data.addLine(segment);
                }
                if (changed) {
                    DataInit.TIMBER_DATA.set(chunk, data);
                    DataInit.TIMBER_DATA.markDirty(chunk);
                }
            }

            if (changed) syncChunkToPlayers(level, chunkPos, chunk);
        }
    }

    /** Removes lines made unusable by a Timber Frame being replaced. */
    public static void pruneLinesAt(ServerLevel level, BlockPos blockPos) {
        Set<TimberData.TimberLine> candidates = new HashSet<>();
        ChunkPos center = new ChunkPos(blockPos);
        for (int chunkX = center.x - 1; chunkX <= center.x + 1; chunkX++) {
            for (int chunkZ = center.z - 1; chunkZ <= center.z + 1; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                DataInit.TIMBER_DATA.find(chunk).ifPresent(data -> data.lines().stream()
                        .filter(line -> touchesEndpoint(line, blockPos))
                        .forEach(candidates::add));
            }
        }

        for (TimberData.TimberLine line : candidates) {
            if (!lineIsValid(level, line)) {
                removeFromLoadedChunks(level, line);
            }
        }
    }

    private static boolean containsInAnyChunk(TimberData.TimberLine segment, Iterable<LevelChunk> chunks) {
        for (LevelChunk chunk : chunks) {
            if (DataInit.TIMBER_DATA.find(chunk).map(data -> data.contains(segment)).orElse(false)) return true;
        }
        return false;
    }

    /** Sends a snapshot after the loader has sent the corresponding vanilla chunk. */
    public static void chunkSent(ServerPlayer player, ServerLevel level, LevelChunk chunk) {
        pruneChunk(level, chunk);
        PlayerVisibility visibility = PLAYER_VISIBILITY.computeIfAbsent(player.getUUID(), ignored -> new PlayerVisibility());
        ResourceLocation dimension = level.dimension().location();
        if (!dimension.equals(visibility.dimension)) {
            visibility.dimension = dimension;
            visibility.sentChunks.clear();
        }
        visibility.sentChunks.add(ChunkPos.asLong(chunk.getPos().x, chunk.getPos().z));
        sendChunkToPlayer(level, chunk.getPos(), chunk, player);
    }

    public static void chunkUnwatched(ServerPlayer player, ServerLevel level, ChunkPos chunkPos) {
        PlayerVisibility visibility = PLAYER_VISIBILITY.get(player.getUUID());
        if (visibility == null || !level.dimension().location().equals(visibility.dimension)) return;
        visibility.sentChunks.remove(ChunkPos.asLong(chunkPos.x, chunkPos.z));
    }

    public static void removePlayer(ServerPlayer player) {
        PLAYER_VISIBILITY.remove(player.getUUID());
    }

    public static void clearServerState() {
        PLAYER_VISIBILITY.clear();
    }

    private static void syncChunkToPlayers(ServerLevel level, ChunkPos chunkPos, LevelChunk chunk) {
        SyncTimberLinesS2C packet = packetFor(level, chunkPos, chunk);
        for (ServerPlayer player : level.players()) {
            PlayerVisibility visibility = PLAYER_VISIBILITY.get(player.getUUID());
            if (visibility == null || !level.dimension().location().equals(visibility.dimension) || !visibility.sentChunks.contains(ChunkPos.asLong(chunkPos.x, chunkPos.z))) continue;

            RosettaNetwork.sendToPlayer(packet, player);
        }
    }

    private static void sendChunkToPlayer(ServerLevel level, ChunkPos chunkPos, LevelChunk chunk, ServerPlayer player) {
        RosettaNetwork.sendToPlayer(packetFor(level, chunkPos, chunk), player);
    }

    private static SyncTimberLinesS2C packetFor(ServerLevel level, ChunkPos chunkPos, LevelChunk chunk) {
        List<TimberData.TimberLine> lines = DataInit.TIMBER_DATA.find(chunk)
                .map(TimberData::lines)
                .orElseGet(List::of);
        return new SyncTimberLinesS2C(level.dimension().location(), chunkPos, lines);
    }

    private static void pruneChunk(ServerLevel level, LevelChunk chunk) {
        TimberData data = DataInit.TIMBER_DATA.find(chunk).orElse(null);
        if (data == null) return;

        List<TimberData.TimberLine> invalid = data.lines().stream()
                .filter(line -> !lineIsValid(level, line))
                .toList();
        if (invalid.isEmpty()) return;

        for (TimberData.TimberLine line : invalid) data.removeLine(line);
        if (data.lines().isEmpty()) DataInit.TIMBER_DATA.remove(chunk);
        else DataInit.TIMBER_DATA.markDirty(chunk);
        syncChunkToPlayers(level, chunk.getPos(), chunk);
    }

    private static void removeFromLoadedChunks(ServerLevel level, TimberData.TimberLine line) {
        for (ChunkPos chunkPos : line.chunkPositions()) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
            if (chunk == null) continue;

            TimberData data = DataInit.TIMBER_DATA.find(chunk).orElse(null);
            if (data == null || !data.removeLine(line)) continue;

            if (data.lines().isEmpty()) DataInit.TIMBER_DATA.remove(chunk);
            else DataInit.TIMBER_DATA.markDirty(chunk);
            syncChunkToPlayers(level, chunkPos, chunk);
        }
    }

    private static boolean lineIsValid(ServerLevel level, TimberData.TimberLine line) {
        for (ChunkPos chunkPos : line.chunkPositions()) {
            if (level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z) == null) return false;
        }
        return hasTimberFrameAtVertex(level, line.start())
                && hasTimberFrameAtVertex(level, line.end());
    }

    private static boolean hasTimberFrameAtVertex(ServerLevel level, BlockPos vertex) {
        for (int offsetX = -1; offsetX <= 0; offsetX++) {
            for (int offsetY = -1; offsetY <= 0; offsetY++) {
                for (int offsetZ = -1; offsetZ <= 0; offsetZ++) {
                    if (level.getBlockState(vertex.offset(offsetX, offsetY, offsetZ))
                            .is(ContentInit.TIMBER_FRAME.get())) return true;
                }
            }
        }
        return false;
    }

    private static boolean touchesEndpoint(TimberData.TimberLine line, BlockPos blockPos) {
        return sharesVertex(blockPos, line.start()) || sharesVertex(blockPos, line.end());
    }

    private static boolean sharesVertex(BlockPos blockPos, BlockPos vertex) {
        return blockPos.getX() >= vertex.getX() - 1 && blockPos.getX() <= vertex.getX()
                && blockPos.getY() >= vertex.getY() - 1 && blockPos.getY() <= vertex.getY()
                && blockPos.getZ() >= vertex.getZ() - 1 && blockPos.getZ() <= vertex.getZ();
    }

    public static ClientRenderSnapshot clientRenderSnapshot(Level level, ChunkPos chunkPos) {
        if (!level.isClientSide()) return ClientRenderSnapshot.EMPTY;
        ResourceLocation dimension = level.dimension().location();
        Map<Long, List<TimberData.TimberLine>> dimensionChunks = clientChunks.get(dimension);
        List<TimberData.TimberLine> lines = dimensionChunks == null ? List.of() : dimensionChunks.getOrDefault(ChunkPos.asLong(chunkPos.x, chunkPos.z), List.of());
        return new ClientRenderSnapshot(lines, clientPreviewLine);
    }

    /** Applies the same normalized toggle locally before the authoritative packet arrives. */
    public static void toggleClientLine(Level level, TimberData.TimberLine line) {
        ResourceLocation dimension = level.dimension().location();
        List<TimberData.TimberLine> segments = line.splitAtCorners();
        Map<Long, List<TimberData.TimberLine>> dimensionChunks = clientChunks.get(dimension);
        Map<Long, List<TimberData.TimberLine>> knownChunks = dimensionChunks;
        boolean remove = knownChunks != null && segments.stream().allMatch(segment ->
                knownChunks.values().stream().anyMatch(lines -> lines.contains(segment)));

        Set<Long> affected = new HashSet<>();
        for (TimberData.TimberLine segment : segments) {
            for (ChunkPos chunkPos : segment.chunkPositions()) {
                affected.add(ChunkPos.asLong(chunkPos.x, chunkPos.z));
            }
        }

        for (long key : affected) {
            ChunkPos chunkPos = new ChunkPos(key);
            List<TimberData.TimberLine> before = dimensionChunks == null
                    ? List.of()
                    : dimensionChunks.getOrDefault(key, List.of());
            List<TimberData.TimberLine> after = new java.util.ArrayList<>(before);
            for (TimberData.TimberLine segment : segments) {
                if (remove) after.remove(segment);
                else if (!after.contains(segment)) after.add(segment);
            }
            setClientChunkLines(dimension, chunkPos, after);
            dimensionChunks = clientChunks.get(dimension);
        }
        clearClientPreviewLine();
    }

    public static void setClientChunkLines(ResourceLocation dimension, ChunkPos chunkPos,
                                           List<TimberData.TimberLine> lines) {
        Map<Long, List<TimberData.TimberLine>> currentDimension = clientChunks.get(dimension);
        long key = ChunkPos.asLong(chunkPos.x, chunkPos.z);
        List<TimberData.TimberLine> beforeLines = currentDimension == null
                ? List.of()
                : currentDimension.getOrDefault(key, List.of());
        List<TimberData.TimberLine> afterLines = List.copyOf(lines);
        if (beforeLines.equals(afterLines)) return;

        Map<ResourceLocation, Map<Long, List<TimberData.TimberLine>>> next = new HashMap<>(clientChunks);
        Map<Long, List<TimberData.TimberLine>> nextDimension = currentDimension == null
                ? new HashMap<>()
                : new HashMap<>(currentDimension);
        if (afterLines.isEmpty()) nextDimension.remove(key);
        else nextDimension.put(key, afterLines);
        if (nextDimension.isEmpty()) next.remove(dimension);
        else next.put(dimension, Map.copyOf(nextDimension));
        clientChunks = Map.copyOf(next);

        clientRefresh.accept(
                new ClientRenderSnapshot(beforeLines, clientPreviewLine),
                new ClientRenderSnapshot(afterLines, clientPreviewLine)
        );
    }

    public static void clearClientLines() {
        List<TimberData.TimberLine> beforeLines = clientChunks.values().stream()
                .flatMap(chunks -> chunks.values().stream())
                .flatMap(List::stream)
                .distinct()
                .toList();
        clientChunks = Map.of();
        clientRefresh.accept(
                new ClientRenderSnapshot(beforeLines, clientPreviewLine),
                new ClientRenderSnapshot(List.of(), clientPreviewLine)
        );
    }

    public static void setClientPreviewLine(TimberData.TimberLine line) {
        if (java.util.Objects.equals(clientPreviewLine, line)) return;
        ClientRenderSnapshot before = new ClientRenderSnapshot(List.of(), clientPreviewLine);
        clientPreviewLine = line;
        clientRefresh.accept(before, new ClientRenderSnapshot(List.of(), clientPreviewLine));
    }

    public static void clearClientPreviewLine() {
        setClientPreviewLine(null);
    }

    public static void setClientRefresh(BiConsumer<ClientRenderSnapshot, ClientRenderSnapshot> refresh) {
        clientRefresh = refresh == null ? (before, after) -> {} : refresh;
    }

    public record ClientRenderSnapshot(List<TimberData.TimberLine> lines,
                                       TimberData.TimberLine previewLine) {
        public static final ClientRenderSnapshot EMPTY = new ClientRenderSnapshot(List.of(), null);

        public ClientRenderSnapshot {
            lines = List.copyOf(lines == null ? List.of() : lines);
        }
    }

    private static final class PlayerVisibility {
        private ResourceLocation dimension;
        private final Set<Long> sentChunks = new HashSet<>();
    }
}
