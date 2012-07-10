/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.world;

public interface WorldModifier {

    void setBlock(final int x, final int y, final int z, final int data);

    int getBlock(final int x, final int y, final int z);

}