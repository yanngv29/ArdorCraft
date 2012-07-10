/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.world;

import com.ardor3d.math.type.ReadOnlyVector3;

public interface IServerConnection {
    void getModifier(ChunkModifier chunkModifier);

    void update(final int x, final int z);

    void requestChunk(final int x, final int z);

    void setBlock(final int x, final int y, final int z, final int blockId, final BlockSide orientation);

    void updatePlayerPosition(final ReadOnlyVector3 location, ReadOnlyVector3 direction);

    void connect(final String address);

    void close();
}