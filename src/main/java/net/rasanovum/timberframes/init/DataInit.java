package net.rasanovum.timberframes.init;

import net.rasanovum.rosetta.attachment.ChunkAttachmentKey;
import net.rasanovum.rosetta.attachment.RosettaAttachments;
import net.rasanovum.timberframes.TimberFrames;
import net.rasanovum.timberframes.storage.TimberData;

/** Rosetta attachment declarations for Timber Frames. */
public final class DataInit {
    private DataInit() {}

    public static final ChunkAttachmentKey<TimberData> TIMBER_DATA =
            RosettaAttachments.chunk(TimberFrames.MODID).persistent(
                    "timber_lines",
                    TimberData::new,
                    TimberData.CODEC
            );

    public static void load() {
        TimberFrames.LOGGER.info("Timber Frames persistent geometry registered");
    }
}
