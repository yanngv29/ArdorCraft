/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.voxel;

public interface VoxelationListener {
    public void addVoxel(VoxelationContext vcx, int[] section, int[] voxel);

    public void addSpace(VoxelationContext vcx, int[] section, int[] voxel);
}
