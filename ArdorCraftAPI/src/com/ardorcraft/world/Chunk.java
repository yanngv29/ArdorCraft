/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.world;

public class Chunk {
    private final byte[] blocks;
    private final byte[] extra;

    public Chunk(final byte[] blocks, final byte[] extra) {
        this.blocks = blocks;
        this.extra = extra;
    }

    public byte[] getBlocks() {
        return blocks;
    }

    public byte[] getExtra() {
        return extra;
    }
}
