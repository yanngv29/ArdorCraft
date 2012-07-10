/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.pathfinding;


/**
 * Use to define your own heuristic for use with the {@link ConstrainedAStar}.
 */
public interface AStarHeuristic {
    public float getCost(int x, int y, int z, int tx, int ty, int tz);
}
