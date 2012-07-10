/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.world;

/**
 * Identifies if blocks are see through or covers the entire 1x1x1 box (which then allows for hidden surface removal).
 */
public enum BlockType {
    Solid,
    Transparent
}
