package com.sanhiruzu.atelier.space.analyze;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Immutable snapshot of chunk block data for the async candidate analysis pipeline.
 *
 * <p>One byte per cell in a 16x384x16 grid. Flag bits:
 * <ul>
 *   <li>{@link #FLAG_AIR}       (bit 0) — passable air, not fluid</li>
 *   <li>{@link #FLAG_STEP}      (bit 1) — stair/slab/partial collision</li>
 *   <li>{@link #FLAG_NATURAL}   (bit 2) — natural terrain block</li>
 *   <li>{@link #FLAG_ENTRY}     (bit 3) — door, fence gate, or trapdoor</li>
 *   <li>{@link #FLAG_FURNITURE} (bit 4) — crafting table, chest, furnace, bed, etc.</li>
 * </ul>
 *
 * <p>FLAG_ENTRY and FLAG_FURNITURE take priority and suppress all other flags.
 * After those, air check comes next. Then step connector. Then natural.
 *
 * <p>Constructed on the server thread via {@link #of}. All analysis methods are
 * thread-safe because only the immutable {@code cells} array is accessed after
 * construction.
 */
public final class ChunkSnapshotBundle {

    /** Passable air, not fluid. */
    public static final byte FLAG_AIR       = 1;
    /** Stair, slab, or partial-collision block. */
    public static final byte FLAG_STEP      = 2;
    /** Natural terrain block (stone, ore, dirt, etc.). */
    public static final byte FLAG_NATURAL   = 4;
    /** Door, fence gate, or trapdoor. */
    public static final byte FLAG_ENTRY     = 8;
    /** Crafting table, chest, furnace, bed, etc. */
    public static final byte FLAG_FURNITURE = 16;

    public static final int W = 16;
    public static final int H = 384; // y from -64 to 319 inclusive
    /** Total number of cells: 16 * 16 * 384. */
    public static final int SIZE = W * W * H;

    private static final double EPSILON = 1.0E-6;
    private static final double MIN_STEP_HEIGHT = 0.25;
    private static final double MAX_PARTIAL_STEP_HEIGHT = 0.875;

    private final byte[] cells;
    public final int chunkX;
    public final int chunkZ;
    public final long snapshotVersion;

    private ChunkSnapshotBundle(byte[] cells, int chunkX, int chunkZ, long snapshotVersion) {
        this.cells = cells;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.snapshotVersion = snapshotVersion;
    }

    /**
     * Creates a bundle from a live chunk. Must be called on the server thread.
     */
    public static ChunkSnapshotBundle of(ChunkAccess chunk, long snapshotVersion) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        byte[] cells = new byte[SIZE];

        for (int x = 0; x < W; x++) {
            for (int z = 0; z < W; z++) {
                for (int y = -64; y < 320; y++) {
                    BlockPos pos = new BlockPos(minX + x, y, minZ + z);
                    BlockState state = chunk.getBlockState(pos);
                    cells[indexOf(x, y, z)] = classifyBlock(state, chunk, pos);
                }
            }
        }

        return new ChunkSnapshotBundle(cells, chunk.getPos().x, chunk.getPos().z, snapshotVersion);
    }

    /**
     * Creates a bundle from a pre-built cell array. Package-private for tests.
     */
    static ChunkSnapshotBundle forTest(byte[] cells, int chunkX, int chunkZ, long snapshotVersion) {
        byte[] copy = new byte[SIZE];
        System.arraycopy(cells, 0, copy, 0, Math.min(cells.length, SIZE));
        return new ChunkSnapshotBundle(copy, chunkX, chunkZ, snapshotVersion);
    }

    // -------------------------------------------------------------------------
    // Cell queries — all thread-safe (read-only access to immutable cells array)
    // -------------------------------------------------------------------------

    public boolean isAir(int x, int y, int z) {
        return (cells[indexOf(x, y, z)] & FLAG_AIR) != 0;
    }

    public boolean isStepConnector(int x, int y, int z) {
        return (cells[indexOf(x, y, z)] & FLAG_STEP) != 0;
    }

    public boolean isNaturalBlock(int x, int y, int z) {
        return (cells[indexOf(x, y, z)] & FLAG_NATURAL) != 0;
    }

    public boolean isEntryConnector(int x, int y, int z) {
        return (cells[indexOf(x, y, z)] & FLAG_ENTRY) != 0;
    }

    public boolean isFurnitureSignal(int x, int y, int z) {
        return (cells[indexOf(x, y, z)] & FLAG_FURNITURE) != 0;
    }

    /**
     * Returns true if this position is walkable: air here, air above (headroom),
     * and solid (non-air, non-entry) below.
     */
    public boolean isWalkablePosition(int x, int y, int z) {
        if (!isAir(x, y, z)) return false;
        if (y + 1 >= 320 || !isAir(x, y + 1, z)) return false; // no headroom
        if (y - 1 < -64) return false;
        int below = cells[indexOf(x, y - 1, z)] & 0xFF;
        // below must be solid: not air and not an entry connector
        return (below & FLAG_AIR) == 0 && (below & FLAG_ENTRY) == 0;
    }

    /**
     * Returns true if a non-air, non-entry block exists within {@code maxHeight}
     * cells above {@code y}, starting from {@code y+2} (skipping headroom).
     */
    public boolean hasShelterAbove(int x, int y, int z, int maxHeight) {
        int limit = Math.min(y + maxHeight + 2, 320); // y+2 .. y+maxHeight+1 inclusive → exclusive upper = y+maxHeight+2
        for (int ny = y + 2; ny < limit; ny++) {
            int flags = cells[indexOf(x, ny, z)] & 0xFF;
            boolean isAirCell = (flags & FLAG_AIR) != 0;
            boolean isEntryCell = (flags & FLAG_ENTRY) != 0;
            if (!isAirCell && !isEntryCell) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Index encoding — public static, called from worker threads
    // -------------------------------------------------------------------------

    /** Encodes local chunk coordinates as a flat array index. */
    public static int indexOf(int x, int y, int z) {
        return x + W * (z + W * (y + 64));
    }

    public static int decodeX(int idx) { return idx % W; }
    public static int decodeZ(int idx) { return (idx / W) % W; }
    public static int decodeY(int idx) { return (idx / (W * W)) - 64; }

    // -------------------------------------------------------------------------
    // Block classification — server thread only
    // -------------------------------------------------------------------------

    private static byte classifyBlock(BlockState state, ChunkAccess chunk, BlockPos pos) {
        // Entry connectors and furniture take priority — suppress all other flags
        if (state.is(BlockTags.DOORS) || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.TRAPDOORS)) {
            return FLAG_ENTRY;
        }
        if (isFurnitureBlock(state)) {
            return FLAG_FURNITURE;
        }

        // Air (not fluid)
        if (state.isAir() && !chunk.getFluidState(pos).is(Fluids.WATER)) {
            return FLAG_AIR;
        }

        // Step connector
        byte flags = 0;
        if (state.is(BlockTags.STAIRS) || state.is(BlockTags.SLABS)) {
            flags = FLAG_STEP;
        } else {
            VoxelShape shape = state.getCollisionShape(chunk, pos);
            if (isPartialWalkableCollision(shape)) flags = FLAG_STEP;
        }

        // Natural block (can combine with step flag)
        if (isNaturalBlockState(state)) flags |= FLAG_NATURAL;

        return flags;
    }

    private static boolean isFurnitureBlock(BlockState state) {
        return state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.CHEST)
                || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.FURNACE)
                || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER)
                || state.is(Blocks.BARREL)
                || state.is(BlockTags.BEDS)
                || state.is(Blocks.BOOKSHELF)
                || state.is(Blocks.CHISELED_BOOKSHELF)
                || state.is(Blocks.LECTERN)
                || state.is(Blocks.ENCHANTING_TABLE)
                || state.is(Blocks.BREWING_STAND);
    }

    private static boolean isNaturalBlockState(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(Blocks.BEDROCK)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.DRIPSTONE_BLOCK)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.AMETHYST_BLOCK)
                || state.is(Blocks.BUDDING_AMETHYST)
                || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.ANCIENT_DEBRIS)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.END_STONE);
    }

    private static boolean isPartialWalkableCollision(VoxelShape shape) {
        if (shape == null || shape == Shapes.empty() || shape.isEmpty()) return false;
        double minY = shape.min(net.minecraft.core.Direction.Axis.Y);
        double maxY = shape.max(net.minecraft.core.Direction.Axis.Y);
        return minY <= EPSILON && maxY >= MIN_STEP_HEIGHT && maxY <= MAX_PARTIAL_STEP_HEIGHT;
    }
}
