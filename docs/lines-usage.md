# Usage

Use a Broad Knife to draw lines across Timber Frames:

1. Right-click a Timber Frame block's corner to choose the first endpoint.
2. Right-click another Timber Frame's corner to choose the second endpoint.
3. The line is added when the second endpoint is valid.

The click positions snap to the nearest world-grid vertex. Right-clicking the
same two endpoints again toggles that line off. Sneak-right-click with the
Broad Knife to cancel a pending first endpoint without changing a line.

## Line rules

- Both endpoint clicks must be on Timber Frame blocks.
- A line cannot have zero length.
- At least one world coordinate must stay constant. Flat horizontal, vertical,
  and diagonal lines on a plane are valid, a fully three-dimensional line where
  X, Y, and Z all change is rejected.
- Lines are visual timber geometry only. They do not add collision, support, or
  a separate block.
- If a Timber Frame supporting either endpoint is removed or replaced, the
  affected line is removed when the world validates that area.

Lines are world data rather than block NBT, so changing a frame's `timber_id`
does not change or remove its lines.
