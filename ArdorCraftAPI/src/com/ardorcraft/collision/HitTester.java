/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.collision;

/**
 * A HitTester allows for user configurable picking.
 */
public interface HitTester {
    boolean isHit(int blockId);
}
