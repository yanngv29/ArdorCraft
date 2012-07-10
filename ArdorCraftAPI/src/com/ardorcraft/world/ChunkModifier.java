/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.world;

import java.util.List;

/**
 * Interface for classes that allows block editing.
 */
public interface ChunkModifier extends BlockProvider {
    void postChunk(final int x, final int z, final Chunk chunk);

    void setBlock(final int x, final int y, final int z, final int blockId);

    void setBlock(final int x, final int y, final int z, final int blockId, final BlockSide orientation);

    void setBlocks(final List<BlockEditData> blockList);
}
