package net.rasanovum.timberframes;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.rasanovum.rosetta.entrypoint.RosettaClientEntrypoint;
import net.rasanovum.rosetta.entrypoint.RosettaCommonEntrypoint;
import net.rasanovum.rosetta.entrypoint.RosettaEntrypoints;
import net.rasanovum.rosetta.event.ServerHooks;
import net.rasanovum.rosetta.network.RosettaNetwork;
import net.rasanovum.rosetta.registry.RegistrationContext;
import net.rasanovum.timberframes.init.ContentInit;
import net.rasanovum.timberframes.init.DataInit;
import net.rasanovum.timberframes.network.PacketRegistration;
import net.rasanovum.timberframes.network.packets.SyncTimberVariantsS2C;
import net.rasanovum.timberframes.recipe.RecipeGenerator;

import net.rasanovum.timberframes.storage.TimberDataManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimberFrames {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "timber_frames";

    private static volatile MinecraftServer currentServer = null;
    private static boolean initialized;

    public static void initialize(RegistrationContext registrationContext) {
        if (initialized) return;

        initialized = true;
        LOGGER.info("Initializing Timber Frames");

        ContentInit.register(registrationContext);
        DataInit.load();
        RecipeGenerator.register();
        TimberFrameAssets.initialize();
        PacketRegistration.initCommon();
        RosettaEntrypoints.register(new Hooks());
    }

    public static void onJoin(ServerPlayer player) {
        syncTimberVariantsToPlayer(player);
    }

    public static void onLeave(ServerPlayer player) {
        TimberDataManager.removePlayer(player);
    }

    public static void onChunkSent(ServerPlayer player, ServerLevel level, LevelChunk chunk) {
        TimberDataManager.chunkSent(player, level, chunk);
    }

    public static void onChunkUnwatched(ServerPlayer player, ServerLevel level, ChunkPos chunkPos) {
        TimberDataManager.chunkUnwatched(player, level, chunkPos);
    }

    public static void onServerStart(MinecraftServer server) {
        currentServer = server;
    }

    public static void onServerStop() {
        TimberDataManager.clearServerState();
        currentServer = null;
    }

    public static void onDataPackReload(MinecraftServer server) {
        RecipeGenerator.prepareForNextReload();
    }

    public static void onTimberIdsChanged() {
        MinecraftServer server = currentServer;
        if (server == null) return;

        server.execute(() -> {
            SyncTimberVariantsS2C packet = new SyncTimberVariantsS2C(RecipeGenerator.getTimberVariants());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                RosettaNetwork.sendToPlayer(packet, player);
            }
        });
    }

    private static void syncTimberVariantsToPlayer(ServerPlayer player) {
        RosettaNetwork.sendToPlayer(
                new SyncTimberVariantsS2C(RecipeGenerator.getTimberVariants()), player
        );
    }

    public static MinecraftServer getServer() {
        return currentServer;
    }

    private static final class Hooks implements RosettaCommonEntrypoint {
        @Override
        public ServerHooks.Callbacks serverHooks() {
            return new ServerHooks.Callbacks() {
                @Override public void onPlayerJoin(ServerPlayer player) { TimberFrames.onJoin(player); }
                @Override public void onPlayerLeave(ServerPlayer player) { TimberFrames.onLeave(player); }
                @Override public void onChunkSent(ServerPlayer player, ServerLevel level, LevelChunk chunk) { TimberFrames.onChunkSent(player, level, chunk); }
                @Override public void onChunkUnwatched(ServerPlayer player, ServerLevel level, ChunkPos chunkPos) { TimberFrames.onChunkUnwatched(player, level, chunkPos); }
                @Override public void onServerStarting(MinecraftServer server) { TimberFrames.onServerStart(server); }
                @Override public void onServerStopping(MinecraftServer server) { TimberFrames.onServerStop(); }
                @Override public void onDataPackReload(MinecraftServer server) { TimberFrames.onDataPackReload(server); }
            };
        }

        @Override
        public RosettaClientEntrypoint clientEntrypoint() {
            return new TimberFramesClientEntrypoint();
        }
    }
}
