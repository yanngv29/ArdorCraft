/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.voxel;

import java.util.Arrays;

import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

public class MeshVoxelationContext extends VoxelationContext {
    public final Mesh mesh = new Mesh();
    public final Triangle triangle = new Triangle();
    public final Transform transform = new Transform();

    public int[] _sectionVoxelCount = new int[3];
    public int[] _sectionCount = new int[3];

    public float EDGE_EPSILON = 0.01f;

    public Section[][][] sections;

    public MeshVoxelationContext(final ReadOnlyVector3 center, final ReadOnlyVector3 voxelSize,
            final int[] sectionCount, final int[] sectionVoxelCount) {
        super(center, voxelSize);

        System.arraycopy(sectionVoxelCount, 0, _sectionVoxelCount, 0, 3);
        System.arraycopy(sectionCount, 0, _sectionCount, 0, 3);

        extent[0] = sectionCount[0] * sectionVoxelCount[0];
        extent[1] = sectionCount[1] * sectionVoxelCount[1];
        extent[2] = sectionCount[2] * sectionVoxelCount[2];

        sections = new Section[sectionCount[0]][sectionCount[1]][sectionCount[2]];
        for (int i = 0; i < sectionCount[0]; i++) {
            for (int j = 0; j < sectionCount[1]; j++) {
                for (int k = 0; k < sectionCount[2]; k++) {
                    sections[i][j][k] = new Section(sectionVoxelCount);
                }
            }
        }
    }

    @Override
    public void init() {}

    public static class Section {
        public final byte[][][] voxel;

        public Section(final int[] sectionVoxelCount) {
            voxel = new byte[sectionVoxelCount[0]][sectionVoxelCount[1]][sectionVoxelCount[2]];
        }

        public void addVoxel(final byte mask, final int i, final int j, final int k) {
            voxel[i][j][k] |= mask;
        }

        public void clear() {
            for (final byte[][] b1 : voxel) {
                for (final byte[] b2 : b1) {
                    Arrays.fill(b2, (byte) 0);
                }
            }
        }

    }

    public class Mesh {
        public final Vector3 extent = new Vector3();
        public final Vector3 center = new Vector3();
    }

    public class Triangle {
        public final Vector3[] vertex = new Vector3[] { new Vector3(), new Vector3(), new Vector3() };
        public final Vector3 normal = new Vector3();
    }

    public void clearVoxelArea() {
        for (final Section[][] s1 : sections) {
            for (final Section[] s2 : s1) {
                for (int i = 0; i < s2.length; i++) {
                    s2[i].clear();
                }
            }
        }
    }

}
