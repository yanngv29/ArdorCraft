/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.world.utils;

import java.util.Comparator;

import com.ardorcraft.data.Pos;

public class ChunkDistanceComparator implements Comparator<Pos> {
    private final int X;
    private final int Z;

    public ChunkDistanceComparator(final int X, final int Z) {
        this.X = X;
        this.Z = Z;
    }

    @Override
    public int compare(final Pos o1, final Pos o2) {
        int xDist = X - o1.x;
        int zDist = Z - o1.z;
        final int l1 = xDist * xDist + zDist * zDist;
        xDist = X - o2.x;
        zDist = Z - o2.z;
        final int l2 = xDist * xDist + zDist * zDist;
        return l1 - l2;
    }
}
