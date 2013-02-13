/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.voxel;

import java.nio.FloatBuffer;

import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardorcraft.voxel.MeshVoxelationContext.Section;
import com.ardorcraft.voxel.MeshVoxelationContext.Triangle;

public class VoxelateMesh {
    public static final byte EDGE_DIRECTION_RIGHT = 0x01;
    public static final byte EDGE_DIRECTION_LEFT = 0x02;

    private final static int X = 0;
    private final static int Y = 1;
    private final static int Z = 2;

    public static void surface(final Mesh mesh, final MeshVoxelationContext vcx) {
        voxelate(mesh, vcx, false);
    }

    public static void solid(final Mesh mesh, final MeshVoxelationContext vcx) {
        voxelate(mesh, vcx, true);
    }

    private static void voxelate(final Mesh mesh, final MeshVoxelationContext vcx, final boolean isSolid) {
        if (mesh.getMeshData().getVertexBuffer() == null) {
            return;
        }

        vcx.clearVoxelArea();

        calculateMeshTransform(mesh, vcx);

        final MeshData md = mesh.getMeshData();
        final int sectionCount = md.getSectionCount();
        int primitiveCount;
        if (md.getIndexMode(0) == IndexMode.Triangles) {
            final Vector3[] vertexes = vcx.triangle.vertex;
            for (int j = 0; j < sectionCount; j++) {
                primitiveCount = md.getPrimitiveCount(j);
                for (int i = 0; i < primitiveCount; i++) {
                    md.getPrimitiveVertices(i, j, vertexes);
                    voxelateTriangle(vcx);
                }
            }
        }

        if (isSolid) {
            fill(vcx);
        } else {
            final Section section = vcx.sections[0][0][0];
            final int[] sectionIndex = new int[] {
                    0, 0, 0
            };
            final int[] voxelIndex = new int[3];
            byte[][] slice;
            byte[] line;
            byte mask;
            for (int i = 0; i < vcx._sectionVoxelCount[0]; i++) {
                slice = section.voxel[i];
                for (int j = 0; j < vcx._sectionVoxelCount[1]; j++) {
                    line = slice[j];
                    for (int k = 0; k < vcx._sectionVoxelCount[2]; k++) {
                        mask = line[k];
                        if (mask == 0) {
                            voxelIndex[0] = i;
                            voxelIndex[1] = j;
                            voxelIndex[2] = k;
                            vcx.addSpace(sectionIndex, voxelIndex);
                            continue;
                        }
                        voxelIndex[0] = i;
                        voxelIndex[1] = j;
                        voxelIndex[2] = k;
                        vcx.addVoxel(sectionIndex, voxelIndex);
                    }
                }
            }
        }
    }

    private static void fill(final MeshVoxelationContext vcx) {
        final Section section = vcx.sections[0][0][0];
        final int[] sectionIndex = new int[] {
                0, 0, 0
        };
        final int[] voxelIndex = new int[3];
        byte[][] slice;
        byte[] line;
        byte mask;
        boolean fill = false;
        for (int i = 0; i < vcx._sectionVoxelCount[0]; i++) {
            slice = section.voxel[i];
            for (int j = 0; j < vcx._sectionVoxelCount[1]; j++) {
                line = slice[j];
                for (int k = 0; k < vcx._sectionVoxelCount[2]; k++) {
                    mask = line[k];
                    if (mask == 0) {
                        if (!fill) {
                            voxelIndex[0] = i;
                            voxelIndex[1] = j;
                            voxelIndex[2] = k;
                            vcx.addSpace(sectionIndex, voxelIndex);
                            continue;
                        }
                    } else if ((mask & EDGE_DIRECTION_LEFT) != 0) {
                        fill = true;
                    } else if ((mask & EDGE_DIRECTION_RIGHT) != 0) {
                        fill = false;
                    }
                    voxelIndex[0] = i;
                    voxelIndex[1] = j;
                    voxelIndex[2] = k;
                    vcx.addVoxel(sectionIndex, voxelIndex);
                }
            }
        }
    }

    private static void voxelateTriangle(final MeshVoxelationContext vcx) {
        final Triangle triangle = vcx.triangle;
        Section section;
        boolean isIntersecting;
        int minX, maxX, minY, maxY, minZ, maxZ;
        final Vector3 center = new Vector3();
        final Voxel vox = new Voxel();
        byte mask;

        final Vector3 v1 = triangle.vertex[0];
        final Vector3 v2 = triangle.vertex[1];
        final Vector3 v3 = triangle.vertex[2];

        // If allowed it would be smarter to do the conversion in the mesh
        // vertexes instead of for every primitive
        v1.subtractLocal(vcx.mesh.center);
        v1.multiplyLocal(vcx.transform.getScale());
        v1.addLocal(vcx.transform.getTranslation());

        v2.subtractLocal(vcx.mesh.center);
        v2.multiplyLocal(vcx.transform.getScale());
        v2.addLocal(vcx.transform.getTranslation());

        v3.subtractLocal(vcx.mesh.center);
        v3.multiplyLocal(vcx.transform.getScale());
        v3.addLocal(vcx.transform.getTranslation());

        // Get the bounding box for this triangle
        vcx.positionToVoxel(v1, vox);
        minX = maxX = vox.x;
        minY = maxY = vox.y;
        minZ = maxZ = vox.z;

        vcx.positionToVoxel(v2, vox);
        if (vox.x < minX) {
            minX = vox.x;
        } else if (vox.x > maxX) {
            maxX = vox.x;
        }
        if (vox.y < minY) {
            minY = vox.y;
        } else if (vox.y > maxY) {
            maxY = vox.y;
        }
        if (vox.z < minZ) {
            minZ = vox.z;
        } else if (vox.z > maxZ) {
            maxZ = vox.z;
        }

        vcx.positionToVoxel(v3, vox);
        if (vox.x < minX) {
            minX = vox.x;
        } else if (vox.x > maxX) {
            maxX = vox.x;
        }
        if (vox.y < minY) {
            minY = vox.y;
        } else if (vox.y > maxY) {
            maxY = vox.y;
        }
        if (vox.z < minZ) {
            minZ = vox.z;
        } else if (vox.z > maxZ) {
            maxZ = vox.z;
        }

        // XXX: we should be able to skip checking two voxels but probably minor
        // performance increase, the voxels of the triangle points
        for (int i = minX; i <= maxX; i++) {
            for (int j = minY; j <= maxY; j++) {
                for (int k = minZ; k <= maxZ; k++) {
                    vcx.voxelToPosition(i, j, k, center);
                    isIntersecting = TriangleAABBIntersection.intersect(center, vcx.voxelSize, triangle);
                    if (isIntersecting) {
                        // Check if the projection of the triangle normal is
                        // positive or negative with regards to the slice axis(Z by default)
                        // TODO: Think i need to improve this fill algorithm it isn't 100% robust
                        final double d = triangle.normal.dot(Vector3.UNIT_Z);
                        // XXX: don't know the best limit of this EPSILON. to low and we miss fill stops
                        if (d > vcx.EDGE_EPSILON) {
                            mask = EDGE_DIRECTION_LEFT;
                        } else {
                            mask = EDGE_DIRECTION_RIGHT;
                        }

                        // TODO: Decide which section this voxel belongs to
                        section = vcx.sections[0][0][0];
                        section.addVoxel(mask, i, j, k);
                    }
                }
            }
        }
    }

    private static void calculateMeshTransform(final Mesh mesh, final MeshVoxelationContext vcx) {
        float x, y, z, minX, maxX, minY, maxY, minZ, maxZ;
        final FloatBuffer vBuf = mesh.getMeshData().getVertexBuffer();

        minX = maxX = vBuf.get(X);
        minY = maxY = vBuf.get(Y);
        minZ = maxZ = vBuf.get(Z);

        final int vCount = 3 * mesh.getMeshData().getVertexCount();
        for (int i = 0; i < vCount; i += 3) {
            x = vBuf.get(i);
            y = vBuf.get(i + 1);
            z = vBuf.get(i + 2);

            if (x < minX) {
                minX = x;
            } else if (x > maxX) {
                maxX = x;
            }

            if (y < minY) {
                minY = y;
            } else if (y > maxY) {
                maxY = y;
            }

            if (z < minZ) {
                minZ = z;
            } else if (z > maxZ) {
                maxZ = z;
            }
        }

        vcx.mesh.extent.set(maxX - minX, maxY - minY, maxZ - minZ);
        vcx.mesh.center.set((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);

        // -1 because we want the mesh to be scaled a tiny bit smaller than the entire VoxelationArea
        final double xScale = vcx.voxelSize.getX() * (vcx._sectionCount[X] * vcx._sectionVoxelCount[X] - 1)
                / vcx.mesh.extent.getX();
        final double yScale = vcx.voxelSize.getY() * (vcx._sectionCount[Y] * vcx._sectionVoxelCount[Y] - 1)
                / vcx.mesh.extent.getY();
        final double zScale = vcx.voxelSize.getZ() * (vcx._sectionCount[Z] * vcx._sectionVoxelCount[Z] - 1)
                / vcx.mesh.extent.getZ();

        // Setup transform so that Mesh is strictly in +X,+Y,+Z quadrant and scaled so that it
        // fits inside the VoxelationArea
        double scale;
        if (xScale < yScale) {
            if (xScale < zScale) {
                scale = xScale;
            } else {
                scale = zScale;
            }
        } else {
            if (yScale < zScale) {
                scale = yScale;
            } else {
                scale = zScale;
            }
        }
        vcx.transform.setScale(scale);
        vcx.transform.setTranslation(0.5 * vcx.mesh.extent.getX() * scale, 0.5 * vcx.mesh.extent.getY() * scale, 0.5
                * vcx.mesh.extent.getZ() * scale);
    }
}
