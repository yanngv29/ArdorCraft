/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.world;

public interface LightProvider {
    float getGlobalLighting(final int x, final int y, final int z);

    float getLocalLighting(final int x, final int y, final int z);
}
