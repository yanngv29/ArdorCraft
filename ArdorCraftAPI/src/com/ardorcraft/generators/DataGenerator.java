/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.generators;

import com.ardorcraft.world.WorldModifier;

/**
 * A DataGenerator is responsible for generating the terrain in the world. It HAS to be thread safe!
 */
public interface DataGenerator {

    /**
     * Method to implement for custom terrain generation.
     * 
     * @param xStart
     *            Starting point of generation in X
     * @param zStart
     *            Starting point of generation in ZZ
     * @param xEnd
     *            Ending point of generation in X
     * @param zEnd
     *            Ending point of generation in Z
     * @param spacing
     *            Space between sample data points (for faster zoom outs in map viewers etc)
     * @param height
     *            Height of data to generate in Y
     * @param proxy
     *            WorldModifier to build the world against (setBlock etc).
     */
    void generateChunk(final int xStart, final int zStart, final int xEnd, final int zEnd, int spacing,
            final int height, final WorldModifier proxy);

}