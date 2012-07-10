/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.pathfinding;

import com.ardorcraft.world.BlockProvider;

public interface MoveCalculator {
    void calculateMove(BlockProvider map, final MoveData moveData, final PathNode current, final int xChange,
            final int zChange);
}
