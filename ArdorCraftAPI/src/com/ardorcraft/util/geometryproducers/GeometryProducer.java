/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.util.geometryproducers;

import com.ardorcraft.util.BlockUtil;
import com.ardorcraft.world.BlockProvider;
import com.ardorcraft.world.GeometryHandler;

/**
 * Pluggable interface to create new geometry in block generation
 */
public interface GeometryProducer {
    /**
     * @param blockId
     *            ID of block to generate geometry for
     * @param geometryHandler
     *            geometry handler to use for setting geometry data
     * @param blockProvider
     *            blockprovider can give access to neighbouring blocks etc
     * @param blockUtil
     *            block util gives information about a block, tile texture coords etc
     * @param x
     *            world space x position
     * @param y
     *            world space y position
     * @param z
     *            world space z position
     */
    void generateBlock(final int blockId, final GeometryHandler geometryHandler, final BlockProvider blockProvider,
            final BlockUtil blockUtil, final int x, final int y, final int z);
}
