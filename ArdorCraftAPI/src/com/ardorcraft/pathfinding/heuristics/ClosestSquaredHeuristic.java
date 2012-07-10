/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.pathfinding.heuristics;

import com.ardorcraft.pathfinding.AStarHeuristic;

public class ClosestSquaredHeuristic implements AStarHeuristic {

    public float getCost(final int x, final int y, final int z, final int tx, final int ty, final int tz) {
        final float dx = tx - x;
        final float dy = ty - y;
        final float dz = tz - z;

        return dx * dx + dy * dy + dz * dz;
    }

}
