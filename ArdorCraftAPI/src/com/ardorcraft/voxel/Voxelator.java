/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.voxel;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardorcraft.data.Pos;
import com.ardorcraft.world.ChunkModifier;

public class Voxelator {
    private MeshVoxelationContext vcx;
    private final ChunkModifier blockScene;
    private final Pos position = new Pos();
    private int blockType;

    private final int xSize;
    private final int ySize;
    private final int zSize;

    public Voxelator(final ChunkModifier blockScene, final int xSize, final int ySize, final int zSize) {
        this.blockScene = blockScene;
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
    }

    public void voxelate(final Vector3 position, final Mesh mesh, final float edgeEpsilon, final int blockType) {
        final int X = MathUtils.floor(position.getXf());
        final int Y = MathUtils.floor(position.getYf());
        final int Z = MathUtils.floor(position.getZf());

        voxelate(new Pos(X, Y, Z), mesh, edgeEpsilon, blockType);
    }

    public void voxelate(final Pos pos, final Mesh mesh, final float edgeEpsilon, final int blockType) {
        if (vcx == null) {
            init();
        }

        this.blockType = blockType;
        position.set(pos.x - xSize / 2, pos.y, pos.z - zSize / 2);

        vcx.EDGE_EPSILON = edgeEpsilon;

        VoxelateMesh.solid(mesh, vcx);
    }

    protected void init() {
        vcx = new MeshVoxelationContext(Vector3.ZERO, new Vector3(1, 1, 1), new int[] {
                1, 1, 1
        }, new int[] {
                xSize, ySize, zSize
        });

        vcx.listener = new VoxelationListener() {
            Vector3 pos = new Vector3();

            @Override
            public void addVoxel(final VoxelationContext context, final int[] section, final int[] voxel) {
                vcx.voxelToPosition(voxel, pos);

                final int X = MathUtils.floor(pos.getXf());
                final int Y = MathUtils.floor(pos.getYf());
                final int Z = MathUtils.floor(pos.getZf());

                blockScene.setBlock(X + position.x, Y + position.y, Z + position.z, blockType);
            }

            @Override
            public void addSpace(final VoxelationContext vcx, final int[] section, final int[] voxel) {
                // TODO Auto-generated method stub

            }
        };
    }
}
