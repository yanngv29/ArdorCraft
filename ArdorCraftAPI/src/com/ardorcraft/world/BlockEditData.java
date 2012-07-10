/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.world;

import com.ardorcraft.data.Pos;

public class BlockEditData {
    public Pos pos;
    public int type;
    public BlockSide orientation;

    public BlockEditData(final Pos pos, final int type, final BlockSide orientation) {
        this.pos = pos;
        this.type = type;
        this.orientation = orientation;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlockEditData [pos=");
        builder.append(pos);
        builder.append(", type=");
        builder.append(type);
        builder.append(", orientation=");
        builder.append(orientation);
        builder.append("]");
        return builder.toString();
    }
}
