package net.rasanovum.timberframes.network;

import net.rasanovum.rosetta.network.RosettaNetwork;
import net.rasanovum.timberframes.TimberFrames;
import net.rasanovum.timberframes.network.packets.SyncTimberLinesS2C;
import net.rasanovum.timberframes.network.packets.SyncTimberVariantsS2C;

/** Declares Timber Frames packets through Rosetta's loader-neutral channel. */
public final class PacketRegistration {
    private static final RosettaNetwork.Channel CHANNEL = RosettaNetwork.channel(TimberFrames.MODID);

    private PacketRegistration() {}

    public static void initCommon() {
        TimberFrames.LOGGER.info("Registering Timber Frames network packets");
        CHANNEL.clientbound(
                "sync_timber_lines_s2c",
                SyncTimberLinesS2C.class,
                SyncTimberLinesS2C::write,
                SyncTimberLinesS2C::new,
                SyncTimberLinesS2C::handle
        );
        CHANNEL.clientbound(
                "sync_timber_variants_s2c",
                SyncTimberVariantsS2C.class,
                SyncTimberVariantsS2C::write,
                SyncTimberVariantsS2C::new,
                SyncTimberVariantsS2C::handle
        );
    }
}
