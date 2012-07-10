/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.voxel;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

public abstract class VoxelationContext {
    // The center position of voxel 0,0,0
    public final Vector3 center = new Vector3();
    // The size of one voxel
    public final Vector3 voxelSize = new Vector3();

    public int[] extent = new int[] {
            32, 32, 32
    };

    public VoxelationListener listener;

    public VoxelationContext(final ReadOnlyVector3 center, final ReadOnlyVector3 voxelSize) {
        this.center.set(center);
        this.voxelSize.set(voxelSize);
    }

    public abstract void init();

    public void positionToVoxel(final ReadOnlyVector3 pos, final Voxel store) {
        store.x = (int) Math.round((pos.getX() - center.getX()) / voxelSize.getX());
        store.y = (int) Math.round((pos.getY() - center.getY()) / voxelSize.getY());
        store.z = (int) Math.round((pos.getZ() - center.getZ()) / voxelSize.getZ());
    }

    public void voxelToPosition(final Voxel voxel, final Vector3 store) {
        store.set(voxel.x * voxelSize.getX() + center.getX(), voxel.y * voxelSize.getY() + center.getY(), voxel.z
                * voxelSize.getZ() + center.getZ());
    }

    public void voxelToPosition(final int[] voxel, final Vector3 store) {
        store.set(voxel[0] * voxelSize.getX() + center.getX(), voxel[1] * voxelSize.getY() + center.getY(), voxel[2]
                * voxelSize.getZ() + center.getZ());
    }

    public void voxelToPosition(final int x, final int y, final int z, final Vector3 store) {
        store.set(x * voxelSize.getX() + center.getX(), y * voxelSize.getY() + center.getY(), z * voxelSize.getZ()
                + center.getZ());
    }

    public void addVoxel(final int[] section, final int[] voxel) {
        if (listener != null) {
            listener.addVoxel(this, section, voxel);
        }
    }

    public void addSpace(final int[] section, final int[] voxel) {
        if (listener != null) {
            listener.addSpace(this, section, voxel);
        }
    }

}
