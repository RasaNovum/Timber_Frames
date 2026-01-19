package net.rasanovum.timberframes.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.rasanovum.timberframes.block.TimberFrameBlock;
import net.rasanovum.timberframes.storage.TimberData;
import net.rasanovum.timberframes.util.VersionUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;

/** Loader-neutral rasterization */
final class TimberFrameModelGeometry {
    static final ResourceLocation DEFAULT_TIMBER_ID = VersionUtils.getLocation("minecraft:stripped_oak_log");

    private static final int FACE_PIXELS = 16;
    private static final int LAST_PIXEL = FACE_PIXELS - 1;
    private static final float PIXEL_SIZE = 1f / FACE_PIXELS;
    private static final float QUAD_EDGE_EPSILON = 1f / 8192f;
    private static final float[] LINE_BRIGHTNESS = {0.95f, 1.0f, 0.9f, 0.8f};
    private static final int LINE_THICKNESS_PIXELS = LINE_BRIGHTNESS.length;
    private static final float EDGE_THICKNESS_PIXELS = 2.0f;
    private static final float SHADOW_STRENGTH = 0.1f;
    private static final float PREVIEW_DARKENING = 0.5f;
    private static final int SHADOW_CELL = -1;
    private static final int TIMBER_CELL_BASE = 1;
    private static final int CORNER_CELL = TIMBER_CELL_BASE + 1;
    private static final int CELL_OFFSET = 2;

    private TimberFrameModelGeometry() {}

    static int color(float brightness) {
        return color(brightness, 1.0f);
    }

    static int color(float brightness, float multiplier) {
        float deviation = 1.0f - brightness;
        int red = (int) ((1.0f - deviation) * 255 * multiplier);
        int green = (int) ((1.0f - deviation * 1.25f) * 255 * multiplier);
        int blue = (int) ((1.0f - deviation * 1.8f) * 255 * multiplier);
        return (0xFF << 24) | (red << 16) | (green << 8) | blue;
    }

    static void renderItemFace(Direction face, QuadSink sink) {
        float border = EDGE_THICKNESS_PIXELS * PIXEL_SIZE;
        int fullBrightness = color(1.0f);
        sink.emit(face, 0, 0, 1, border, true, fullBrightness);
        sink.emit(face, 0, 1 - border, 1, 1, true, fullBrightness);
        sink.emit(face, 0, border, border, 1 - border, true, fullBrightness);
        sink.emit(face, 1 - border, border, 1, 1 - border, true, fullBrightness);
        sink.emit(face, border, border, 1 - border, 1 - border, false, fullBrightness);
    }

    static void renderFace(Direction face, Connections connections, @Nullable BlockPos blockPos,
                           List<TimberData.TimberLine> authoredLines, @Nullable TimberData.TimberLine previewLine,
                           boolean applyEffects, QuadSink sink) {
        renderFace(face, connections, blockPos, authoredLines, previewLine, applyEffects, sink, null);
    }

    static void debugFace(Direction face, Connections connections, @Nullable BlockPos blockPos,
                          List<TimberData.TimberLine> authoredLines, @Nullable TimberData.TimberLine previewLine,
                          boolean applyEffects, DebugSink sink) {
        renderFace(face, connections, blockPos, authoredLines, previewLine, applyEffects,
                (direction, left, bottom, right, top, timber, color) -> {}, sink);
    }

    private static void renderFace(Direction face, Connections connections, @Nullable BlockPos blockPos,
                                   List<TimberData.TimberLine> authoredLines, @Nullable TimberData.TimberLine previewLine,
                                   boolean applyEffects, QuadSink sink, @Nullable DebugSink debugSink) {
        int[][] cellMap = new int[FACE_PIXELS][FACE_PIXELS];
        boolean[][] authoredLineCells = new boolean[FACE_PIXELS][FACE_PIXELS];
        boolean[][] previewCells = previewLine == null ? null : new boolean[FACE_PIXELS][FACE_PIXELS];

        rasterBorders(cellMap, connections, applyEffects);

        if (blockPos != null) {
            rasterAuthoredLines(cellMap, authoredLineCells, blockPos, face, authoredLines, applyEffects);
            rasterPreviewLine(cellMap, previewCells, blockPos, face, previewLine);
        }

        applyCornerPieces(cellMap, authoredLineCells, previewCells, connections);

        emitCellRectangles(face, cellMap, previewCells, applyEffects, sink, debugSink);
    }

    private static void rasterBorders(int[][] cellMap, Connections connections, boolean applyEffects) {
        if (!connections.up())    raster(cellMap, 0, LAST_PIXEL, FACE_PIXELS, LAST_PIXEL, EDGE_THICKNESS_PIXELS, applyEffects);
        if (!connections.down())  raster(cellMap, 0, 1, FACE_PIXELS, 1, EDGE_THICKNESS_PIXELS, applyEffects);
        if (!connections.left())  raster(cellMap, 1, 0, 1, FACE_PIXELS, EDGE_THICKNESS_PIXELS, applyEffects);
        if (!connections.right()) raster(cellMap, LAST_PIXEL, 0, LAST_PIXEL, FACE_PIXELS, EDGE_THICKNESS_PIXELS, applyEffects);
    }

    private static void rasterAuthoredLines(int[][] cellMap, boolean[][] authoredLineCells,
                                            BlockPos blockPos, Direction face,
                                            List<TimberData.TimberLine> authoredLines, boolean applyEffects) {
        for (TimberData.TimberLine line : authoredLines) {
            if (!line.intersectsFace(blockPos, face)) continue;
            Vector2f start = project(line.start(), blockPos, face), end = project(line.end(), blockPos, face);
            rasterAuthoredLine(cellMap, authoredLineCells,
                    start.x * FACE_PIXELS, start.y * FACE_PIXELS,
                    end.x * FACE_PIXELS, end.y * FACE_PIXELS, LINE_THICKNESS_PIXELS, applyEffects);
        }
    }

    private static void rasterPreviewLine(int[][] cellMap, @Nullable boolean[][] previewCells,
                                          BlockPos blockPos, Direction face,
                                          @Nullable TimberData.TimberLine previewLine) {
        if (previewLine == null || previewCells == null || !previewLine.intersectsFace(blockPos, face)) return;
        Vector2f start = project(previewLine.start(), blockPos, face), end = project(previewLine.end(), blockPos, face);
        rasterPreview(cellMap, previewCells,
                start.x * FACE_PIXELS, start.y * FACE_PIXELS,
                end.x * FACE_PIXELS, end.y * FACE_PIXELS, LINE_THICKNESS_PIXELS, false);
    }

    private static void applyCornerPieces(int[][] cellMap, boolean[][] authoredLineCells,
                                          @Nullable boolean[][] previewCells, Connections connections) {
        if (connections.up() && connections.left() && !connections.upLeft()) {
            applyCornerPiece(cellMap, authoredLineCells, previewCells, 0, LAST_PIXEL, 1, -1);
        }
        if (connections.up() && connections.right() && !connections.upRight()) {
            applyCornerPiece(cellMap, authoredLineCells, previewCells, LAST_PIXEL, LAST_PIXEL, -1, -1);
        }
        if (connections.down() && connections.left() && !connections.downLeft()) {
            applyCornerPiece(cellMap, authoredLineCells, previewCells, 0, 0, 1, 1);
        }
        if (connections.down() && connections.right() && !connections.downRight()) {
            applyCornerPiece(cellMap, authoredLineCells, previewCells, LAST_PIXEL, 0, -1, 1);
        }
    }

    private static void applyCornerPiece(int[][] cellMap, boolean[][] authoredLineCells,
                                         @Nullable boolean[][] previewCells,
                                         int cornerX, int cornerY, int inwardX, int inwardY) {
        setCornerCell(cellMap, authoredLineCells, previewCells, cornerX, cornerY);
        setCornerCell(cellMap, authoredLineCells, previewCells, cornerX + inwardX, cornerY);
        setCornerCell(cellMap, authoredLineCells, previewCells, cornerX, cornerY + inwardY);
    }

    private static void setCornerCell(int[][] cellMap, boolean[][] authoredLineCells,
                                      @Nullable boolean[][] previewCells, int x, int y) {
        if (authoredLineCells[x][y] || previewCells != null && previewCells[x][y]) return;
        cellMap[x][y] = CORNER_CELL;
    }

    private static void emitCellRectangles(Direction face, int[][] cellMap, @Nullable boolean[][] previewCells,
                                           boolean applyEffects, QuadSink sink, @Nullable DebugSink debugSink) {
        int[][] renderCells = new int[FACE_PIXELS][FACE_PIXELS];
        for (int y = 0; y < FACE_PIXELS; y++) {
            for (int x = 0; x < FACE_PIXELS; x++) {
                renderCells[x][y] = encodeCell(cellMap[x][y], previewCells != null && previewCells[x][y]);
            }
        }

        boolean[][] emitted = new boolean[FACE_PIXELS][FACE_PIXELS];
        for (int y = 0; y < FACE_PIXELS; y++) {
            for (int x = 0; x < FACE_PIXELS; x++) {
                if (emitted[x][y]) continue;

                int renderCell = renderCells[x][y];
                int width = rectangleWidth(renderCells, emitted, x, y, renderCell);
                int height = rectangleHeight(renderCells, emitted, x, y, width, renderCell);
                markRectangle(emitted, x, y, width, height);
                int cell = decodeCell(renderCell);
                boolean preview = isPreviewCell(renderCell);
                if (debugSink != null) debugSink.emit(x, y, width, height, cell, preview);

                boolean timber = cell >= TIMBER_CELL_BASE;
                emitQuad(face, x * PIXEL_SIZE, y * PIXEL_SIZE,
                        (x + width) * PIXEL_SIZE, (y + height) * PIXEL_SIZE,
                        timber,
                        color(cellBrightness(cell, applyEffects),
                                preview ? PREVIEW_DARKENING : 1.0f),
                        sink);
            }
        }
    }

    private static void emitQuad(Direction face, float left, float bottom, float right, float top,
                                 boolean timber, int color, QuadSink sink) {
        float paddedLeft = Math.max(0, left - QUAD_EDGE_EPSILON);
        float paddedBottom = Math.max(0, bottom - QUAD_EDGE_EPSILON);
        float paddedRight = Math.min(1, right + QUAD_EDGE_EPSILON);
        float paddedTop = Math.min(1, top + QUAD_EDGE_EPSILON);
        sink.emit(face,
                paddedLeft, paddedBottom, paddedRight, paddedTop,
                timber, color);
    }

    private static int encodeCell(int cell, boolean preview) {
        return ((cell + CELL_OFFSET) << 1) | (preview ? 1 : 0);
    }

    private static int decodeCell(int renderCell) {
        return (renderCell >> 1) - CELL_OFFSET;
    }

    private static boolean isPreviewCell(int renderCell) {
        return (renderCell & 1) != 0;
    }

    private static int rectangleWidth(int[][] cells, boolean[][] emitted, int x, int y, int key) {
        int width = 0;
        while (x + width < FACE_PIXELS
                && !emitted[x + width][y]
                && cells[x + width][y] == key) {
            width++;
        }
        return width;
    }

    private static int rectangleHeight(int[][] cells, boolean[][] emitted,
                                       int x, int y, int width, int key) {
        int height = 1;
        while (y + height < FACE_PIXELS && rowMatches(cells, emitted, x, y + height, width, key)) {
            height++;
        }
        return height;
    }

    private static boolean rowMatches(int[][] cells, boolean[][] emitted,
                                      int x, int y, int width, int key) {
        for (int offset = 0; offset < width; offset++) {
            if (emitted[x + offset][y] || cells[x + offset][y] != key) return false;
        }
        return true;
    }

    private static void markRectangle(boolean[][] emitted, int x, int y, int width, int height) {
        for (int row = y; row < y + height; row++) {
            for (int column = x; column < x + width; column++) {
                emitted[column][row] = true;
            }
        }
    }

    private static float cellBrightness(int cell, boolean applyEffects) {
        if (applyEffects && cell == SHADOW_CELL) return 1.0f - SHADOW_STRENGTH;
        if (applyEffects && cell >= TIMBER_CELL_BASE) {
            return LINE_BRIGHTNESS[Math.min(LINE_BRIGHTNESS.length - 1, cell - TIMBER_CELL_BASE)];
        }
        return 1.0f;
    }

    static Connections connections(BlockAndTintGetter blockView, BlockPos blockPos, Direction face) {
        Direction up = face == Direction.UP ? Direction.NORTH : face == Direction.DOWN ? Direction.SOUTH : Direction.UP;
        Direction left = face.getAxis().isVertical() ? Direction.WEST : face.getClockWise();
        Direction down = up.getOpposite(), right = left.getOpposite();
        return new Connections(
                isTimberFrame(blockView, blockPos.relative(up)),
                isTimberFrame(blockView, blockPos.relative(down)),
                isTimberFrame(blockView, blockPos.relative(left)),
                isTimberFrame(blockView, blockPos.relative(right)),
                isTimberFrame(blockView, blockPos.relative(up).relative(left)),
                isTimberFrame(blockView, blockPos.relative(up).relative(right)),
                isTimberFrame(blockView, blockPos.relative(down).relative(left)),
                isTimberFrame(blockView, blockPos.relative(down).relative(right))
        );
    }

    private static boolean isTimberFrame(BlockAndTintGetter blockView, BlockPos blockPos) {
        return blockView.getBlockState(blockPos).getBlock() instanceof TimberFrameBlock;
    }

    private static void raster(int[][] cellMap, float x1, float y1, float x2, float y2,
                               float lineThickness, boolean applyEffects) {
        raster(cellMap, null, null, x1, y1, x2, y2, lineThickness, applyEffects);
    }

    private static void rasterAuthoredLine(int[][] cellMap, boolean[][] authoredLineCells,
                                           float x1, float y1, float x2, float y2,
                                           float lineThickness, boolean applyEffects) {
        raster(cellMap, null, authoredLineCells, x1, y1, x2, y2, lineThickness, applyEffects);
    }

    private static void rasterPreview(int[][] cellMap, boolean[][] previewCells,
                                      float x1, float y1, float x2, float y2,
                                      float lineThickness, boolean applyEffects) {
        raster(cellMap, previewCells, null, x1, y1, x2, y2, lineThickness, applyEffects);
    }

    private static void raster(int[][] cellMap, @Nullable boolean[][] previewCells,
                               @Nullable boolean[][] authoredLineCells,
                               float x1, float y1, float x2, float y2,
                               float lineThickness, boolean applyEffects) {
        float dx = x2 - x1, dy = y2 - y1, lengthSquared = dx * dx + dy * dy;
        if (lengthSquared < 0.001f) return;
        float lineLength = (float) Math.sqrt(lengthSquared);

        float directionX = dx, directionY = dy;
        if (directionY < 0) {
            directionX = -directionX;
            directionY = -directionY;
        }
        double angleDegrees = Math.toDegrees(Math.atan2(directionY, directionX));
        boolean castShadowRight = angleDegrees >= 45.0 && angleDegrees <= 90.0;

        for (int x = 0; x < FACE_PIXELS; x++) {
            for (int y = 0; y < FACE_PIXELS; y++) {
                if (previewCells == null && cellMap[x][y] >= TIMBER_CELL_BASE) continue;

                float px = x + 0.5f, py = y + 0.5f;
                float t = clamp(((px - x1) * dx + (py - y1) * dy) / lengthSquared, 0, 1);
                float dist = (float) Math.sqrt(Math.pow(px - (x1 + t * dx), 2) + Math.pow(py - (y1 + t * dy), 2));

                if (dist < lineThickness / 2f) {
                    if (previewCells != null) {
                        previewCells[x][y] = true;
                    } else if (applyEffects) {
                        if (authoredLineCells != null) authoredLineCells[x][y] = true;
                        float signedDistance = (dx * (py - y1) - dy * (px - x1)) / lineLength;
                        int brightnessIndex = (int) Math.floor(
                                lineThickness / 2f - (dx != 0 ? signedDistance * Math.signum(dx) : signedDistance));
                        cellMap[x][y] = TIMBER_CELL_BASE + brightnessIndex;
                    } else {
                        if (authoredLineCells != null) authoredLineCells[x][y] = true;
                        cellMap[x][y] = TIMBER_CELL_BASE + 1;
                    }
                    continue;
                }

                if (previewCells == null && applyEffects) {
                    float shadowX = castShadowRight ? px - 1.0f : px;
                    float shadowY = castShadowRight ? py : py + 1.0f;
                    float shadowT = clamp(((shadowX - x1) * dx + (shadowY - y1) * dy) / lengthSquared, 0, 1);
                    float shadowDistance = (float) Math.sqrt(Math.pow(shadowX - (x1 + shadowT * dx), 2)
                            + Math.pow(shadowY - (y1 + shadowT * dy), 2));
                    if (shadowDistance < lineThickness / 2f) cellMap[x][y] = SHADOW_CELL;
                }
            }
        }
    }

    private static Vector2f project(BlockPos vertex, BlockPos blockPos, Direction face) {
        float x = vertex.getX() - blockPos.getX();
        float y = vertex.getY() - blockPos.getY();
        float z = vertex.getZ() - blockPos.getZ();
        return switch (face) {
            case DOWN -> new Vector2f(x, z);
            case UP -> new Vector2f(x, 1 - z);
            case NORTH -> new Vector2f(1 - x, y);
            case SOUTH -> new Vector2f(x, y);
            case WEST -> new Vector2f(z, y);
            default -> new Vector2f(1 - z, y);
        };
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    record Connections(boolean up, boolean down, boolean left, boolean right,
                       boolean upLeft, boolean upRight, boolean downLeft, boolean downRight) {
    }

    @FunctionalInterface
    interface QuadSink {
        void emit(Direction face, float left, float bottom, float right, float top,
                  boolean timber, int color);
    }

    @FunctionalInterface
    interface DebugSink {
        void emit(int x, int y, int width, int height, int cell, boolean preview);
    }
}
