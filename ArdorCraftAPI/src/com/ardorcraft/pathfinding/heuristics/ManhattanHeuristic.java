/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.pathfinding.heuristics;

import com.ardorcraft.pathfinding.AStarHeuristic;

public class ManhattanHeuristic implements AStarHeuristic {
    private final int minimumCost;

    public ManhattanHeuristic(final int minimumCost) {
        this.minimumCost = minimumCost;
    }

    public float getCost(final int x, final int y, final int z, final int tx, final int ty, final int tz) {
        return minimumCost * (Math.abs(x - tx) + Math.abs(y - ty) + Math.abs(z - tz));
    }

}
