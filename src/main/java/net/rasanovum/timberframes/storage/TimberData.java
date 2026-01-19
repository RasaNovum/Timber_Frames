package net.rasanovum.timberframes.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.List;

/**
 * Authoritative authored timber geometry for one server chunk.
 *
 * <p>The endpoints are absolute integer world-grid points. A chunk stores
 * every normalized segment that touches it, so the same segment can appear in
 * more than one chunk when it lies on a boundary.</p>
 */
public record TimberData(List<TimberLine> lines) {
    public static final Codec<TimberData> CODEC = TimberLine.CODEC.listOf().xmap(TimberData::new, TimberData::lines);

    public TimberData() {
        this(List.of());
    }

    public TimberData(List<TimberLine> lines) {
        Objects.requireNonNull(lines, "lines");
        List<TimberLine> normalized = new ArrayList<>();
        for (TimberLine line : lines) {
            for (TimberLine segment : line.splitAtCorners()) {
                if (!normalized.contains(segment)) normalized.add(segment);
            }
        }
        this.lines = normalized;
    }

    @Override
    public List<TimberLine> lines() {
        return Collections.unmodifiableList(lines);
    }

    public boolean contains(TimberLine line) {
        return lines.contains(line);
    }

    public boolean addLine(TimberLine line) {
        boolean changed = false;
        for (TimberLine segment : line.splitAtCorners()) {
            if (!lines.contains(segment)) {
                lines.add(segment);
                changed = true;
            }
        }
        return changed;
    }

    public boolean removeLine(TimberLine line) {
        boolean changed = false;
        for (TimberLine segment : line.splitAtCorners()) {
            changed |= lines.remove(segment);
        }
        return changed;
    }

    /** A line defined only by two canonical world-space grid endpoints. */
    public record TimberLine(BlockPos start, BlockPos end) {
        public static final Codec<TimberLine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("start_x").forGetter(line -> line.start.getX()),
                Codec.INT.fieldOf("start_y").forGetter(line -> line.start.getY()),
                Codec.INT.fieldOf("start_z").forGetter(line -> line.start.getZ()),
                Codec.INT.fieldOf("end_x").forGetter(line -> line.end.getX()),
                Codec.INT.fieldOf("end_y").forGetter(line -> line.end.getY()),
                Codec.INT.fieldOf("end_z").forGetter(line -> line.end.getZ())
        ).apply(instance, (startX, startY, startZ, endX, endY, endZ) -> new TimberLine(
                new BlockPos(startX, startY, startZ),
                new BlockPos(endX, endY, endZ)
        )));

        public TimberLine {
            if (start == null || end == null) {
                throw new IllegalArgumentException("Timber line endpoints cannot be null");
            }
            if (start.equals(end)) {
                throw new IllegalArgumentException("Timber line cannot have zero length");
            }
            if (start.getX() != end.getX()
                    && start.getY() != end.getY()
                    && start.getZ() != end.getZ()) {
                throw new IllegalArgumentException("Timber line must have a constant world coordinate");
            }
            if (start.compareTo(end) > 0) {
                BlockPos temp = start;
                start = end;
                end = temp;
            }
        }

        /** Splits a line at every intermediate integer grid point it passes through. */
        public List<TimberLine> splitAtCorners() {
            long deltaX = (long) end.getX() - start.getX();
            long deltaY = (long) end.getY() - start.getY();
            long deltaZ = (long) end.getZ() - start.getZ();
            long steps = gcd(gcd(Math.abs(deltaX), Math.abs(deltaY)), Math.abs(deltaZ));
            if (steps <= 1) return List.of(this);

            List<TimberLine> segments = new ArrayList<>(Math.toIntExact(steps));
            for (long index = 0; index < steps; index++) {
                segments.add(new TimberLine(
                        pointAt(start, deltaX, deltaY, deltaZ, index, steps),
                        pointAt(start, deltaX, deltaY, deltaZ, index + 1, steps)
                ));
            }
            return List.copyOf(segments);
        }

        private static BlockPos pointAt(BlockPos origin, long deltaX, long deltaY, long deltaZ,
                                        long index, long steps) {
            return new BlockPos(
                    Math.toIntExact(origin.getX() + deltaX * index / steps),
                    Math.toIntExact(origin.getY() + deltaY * index / steps),
                    Math.toIntExact(origin.getZ() + deltaZ * index / steps)
            );
        }

        private static long gcd(long left, long right) {
            while (right != 0) {
                long remainder = left % right;
                left = right;
                right = remainder;
            }
            return left;
        }

        /**
         * Returns every chunk column whose one-block-padded X/Z bounds can
         * contain a block face intersected by this segment.
         */
        public List<ChunkPos> chunkPositions() {
            int minX = Math.min(start.getX(), end.getX()) - 1;
            int maxX = Math.max(start.getX(), end.getX());
            int minZ = Math.min(start.getZ(), end.getZ()) - 1;
            int maxZ = Math.max(start.getZ(), end.getZ());

            int minChunkX = minX >> 4;
            int maxChunkX = maxX >> 4;
            int minChunkZ = minZ >> 4;
            int maxChunkZ = maxZ >> 4;

            List<ChunkPos> result = new ArrayList<>(
                    (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1));
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    result.add(new ChunkPos(chunkX, chunkZ));
                }
            }
            return List.copyOf(result);
        }

        public boolean liesOn(Direction.Axis axis, int coordinate) {
            return coordinate(start, axis) == coordinate && coordinate(end, axis) == coordinate;
        }

        public int coordinate(Direction.Axis axis) {
            return switch (axis) {
                case X -> start.getX();
                case Y -> start.getY();
                case Z -> start.getZ();
            };
        }

        /** Returns whether the closed world-space segment reaches this block face. */
        public boolean intersectsFace(BlockPos block, Direction face) {
            Direction.Axis axis = face.getAxis();
            int plane = switch (face) {
                case DOWN -> block.getY();
                case UP -> block.getY() + 1;
                case NORTH -> block.getZ();
                case SOUTH -> block.getZ() + 1;
                case WEST -> block.getX();
                case EAST -> block.getX() + 1;
            };
            if (!liesOn(axis, plane)) return false;

            Direction.Axis horizontal;
            Direction.Axis vertical;
            if (axis == Direction.Axis.Y) {
                horizontal = Direction.Axis.X;
                vertical = Direction.Axis.Z;
            } else if (axis == Direction.Axis.X) {
                horizontal = Direction.Axis.Z;
                vertical = Direction.Axis.Y;
            } else {
                horizontal = Direction.Axis.X;
                vertical = Direction.Axis.Y;
            }
            return intersectsUnitSquare(
                    coordinate(start, horizontal) - coordinate(block, horizontal),
                    coordinate(start, vertical) - coordinate(block, vertical),
                    coordinate(end, horizontal) - coordinate(block, horizontal),
                    coordinate(end, vertical) - coordinate(block, vertical)
            );
        }

        private static int coordinate(BlockPos pos, Direction.Axis axis) {
            return switch (axis) {
                case X -> pos.getX();
                case Y -> pos.getY();
                case Z -> pos.getZ();
            };
        }

        private static boolean intersectsUnitSquare(double x1, double y1, double x2, double y2) {
            double[] range = {0.0, 1.0};
            double dx = x2 - x1;
            double dy = y2 - y1;

            return clip(-dx, x1, range)
                    && clip(dx, 1.0 - x1, range)
                    && clip(-dy, y1, range)
                    && clip(dy, 1.0 - y1, range);
        }

        private static boolean clip(double p, double q, double[] range) {
            if (Math.abs(p) < 1.0E-7) {
                return q >= -1.0E-7;
            }
            double t = q / p;
            if (p < 0.0) {
                if (t > range[1]) return false;
                range[0] = Math.max(range[0], t);
            } else {
                if (t < range[0]) return false;
                range[1] = Math.min(range[1], t);
            }
            return range[0] <= range[1] + 1.0E-7;
        }

        public void writeNetwork(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(start);
            buffer.writeBlockPos(end);
        }

        public static TimberLine readNetwork(FriendlyByteBuf buffer) {
            return new TimberLine(buffer.readBlockPos(), buffer.readBlockPos());
        }
    }
}
