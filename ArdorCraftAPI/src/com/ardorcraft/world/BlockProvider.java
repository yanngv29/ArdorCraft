/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.world;

/**
 * Proxy for serving up block data.
 */
public interface BlockProvider {

    /**
     * Get block id for world coordinate x, y, z
     * 
     * @param x
     *            X coordinate
     * @param y
     *            Y coordinate
     * @param z
     *            Z coordinate
     * @return Block id at position
     */
    int getBlock(int x, int y, int z);

    /**
     * Get block extra data for world coordinate x, y, z.<br>
     * bits 0,1,2 = orientation<br>
     * bits 3,4,5,6 = age (or something else)<br>
     * bit 7 = whatever
     * <p>
     * 
     * @param x
     *            X coordinate
     * @param y
     *            Y coordinate
     * @param z
     *            Z coordinate
     * @return Block id at position
     */
    int getBlockExtra(int x, final int y, int z);
}