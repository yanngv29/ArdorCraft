/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.voxel;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardorcraft.voxel.MeshVoxelationContext.Triangle;

/**
 * ported from: http://jgt.akpeters.com/papers/AkenineMoller01/tribox.html
 */
public class TriangleAABBIntersection {
    public static final boolean intersect(final ReadOnlyVector3 center, final ReadOnlyVector3 extent,
            final Triangle triangle) {
        return intersect(center.getX(), center.getY(), center.getZ(), extent, triangle);
    }

    /**
     * Test if Triangle and AABB instersects Will also set triangle.normal
     * 
     * @param cx
     *            - AABB center x value
     * @param cy
     *            - AABB center x value
     * @param cz
     *            - AABB center x value
     * @param extent
     *            - AABB extent of sides
     * @param triangle
     * @return true if intersecting
     */
    public static final boolean intersect(final double cx, final double cy, final double cz,
            final ReadOnlyVector3 extent, final Triangle triangle) {
        double v0x = triangle.vertex[0].getX(), v0y = triangle.vertex[0].getY(), v0z = triangle.vertex[0].getZ(), v1x = triangle.vertex[1]
                .getX(), v1y = triangle.vertex[1].getY(), v1z = triangle.vertex[1].getZ(), v2x = triangle.vertex[2]
                .getX(), v2y = triangle.vertex[2].getY(), v2z = triangle.vertex[2].getZ();
        final double xExt = extent.getX(), yExt = extent.getY(), zExt = extent.getZ();

        // Move everything so that the AABB center is in (0,0,0)
        v0x -= cx;
        v0y -= cy;
        v0z -= cz;
        v1x -= cx;
        v1y -= cy;
        v1z -= cz;
        v2x -= cx;
        v2y -= cy;
        v2z -= cz;

        // Triangle edges
        final double e0x = v1x - v0x, e0y = v1y - v0y, e0z = v1z - v0z, e1x = v2x - v1x, e1y = v2y - v1y, e1z = v2z
                - v1z, e2x = v0x - v2x, e2y = v0y - v2y, e2z = v0z - v2z;

        // Test A:
        // Use separating plane(axis) theorem to test overlap between triangle and box
        // need to test for overlap in these directions:
        // Cross product(edge from tri, {x,y,z}-direction)
        // this gives 3x3=9 more tests
        // test the 9 tests first (this was faster)
        double min, max;
        double p0, p1, p2, rad;
        double fex = abs(e0x), fey = abs(e0y), fez = abs(e0z);

        // AXISTEST_X01(e0[Z], e0[Y], fez, fey);
        p0 = e0z * v0y - e0y * v0z;
        p2 = e0z * v2y - e0y * v2z;
        min = min(p0, p2);
        max = max(p0, p2);
        rad = fez * yExt + fey * zExt;
        if (min > rad || max < -rad) {
            return false;
        }

        // AXISTEST_Y02(e0[Z], e0[X], fez, fex);
        p0 = -e0z * v0x + e0x * v0z;
        p2 = -e0z * v2x + e0x * v2z;
        min = min(p0, p2);
        max = max(p0, p2);
        rad = fez * xExt + fex * zExt;
        if (min > rad || max < -rad) {
            return false;
        }

        // AXISTEST_Z12(e0[Y], e0[X], fey, fex);
        p1 = e0y * v1x - e0x * v1y;
        p2 = e0y * v2x - e0x * v2y;
        min = min(p1, p2);
        max = max(p1, p2);
        rad = fey * xExt + fex * yExt;
        if (min > rad || max < -rad) {
            return false;
        }

        fex = abs(e1x);
        fey = abs(e1y);
        fez = abs(e1z);

        // AXISTEST_X01(e1[Z], e1[Y], fez, fey);
        p0 = e1z * v0y - e1y * v0z;
        p2 = e1z * v2y - e1y * v2z;
        min = min(p0, p2);
        max = max(p0, p2);
        rad = fez * yExt + fey * zExt;
        if (min > rad || max < -rad) {
            return false;
        }

        // AXISTEST_Y02(e1[Z], e1[X], fez, fex);
        p0 = -e1z * v0x + e1x * v0z;
        p2 = -e1z * v2x + e1x * v2z;
        min = min(p0, p2);
        max = max(p0, p2);
        rad = fez * xExt + fex * zExt;
        if (min > rad || max < -rad) {
            return false;
        }

        // AXISTEST_Z0(e1[Y], e1[X], fey, fex);
        p0 = e1y * v0x - e1x * v0y;
        p1 = e1y * v1x - e1x * v1y;
        min = min(p0, p1);
        max = max(p0, p1);
        rad = fey * xExt + fex * yExt;
        if (min > rad || max < -rad) {
            return false;
        }

        fex = abs(e2x);
        fey = abs(e2y);
        fez = abs(e2z);

        // AXISTEST_X2(e2[Z], e2[Y], fez, fey);
        p0 = e2z * v0y - e2y * v0z;
        p1 = e2z * v1y - e2y * v1z;
        min = min(p0, p1);
        max = max(p0, p1);
        rad = fez * yExt + fey * zExt;
        if (min > rad || max < -rad) {
            return false;
        }

        // AXISTEST_Y1(e2[Z], e2[X], fez, fex);
        p0 = -e2z * v0x + e2x * v0z;
        p1 = -e2z * v1x + e2x * v1z;
        min = min(p0, p1);
        max = max(p0, p1);
        rad = fez * xExt + fex * yExt;
        if (min > rad || max < -rad) {
            return false;
        }

        // AXISTEST_Z12(e2[Y], e2[X], fey, fex);
        p1 = e2y * v1x - e2x * v1y;
        p2 = e2y * v2x - e2x * v2y;
        min = min(p1, p2);
        max = max(p1, p2);
        rad = fey * xExt + fex * yExt;
        if (min > rad || max < -rad) {
            return false;
        }

        // === Test 2 ===
        // check if a minimal AABB around the triangle intersect the given AABB

        double minX = v0x, maxX = v0x, minY = v0y, maxY = v0y, minZ = v0z, maxZ = v0z;

        // test in X-direction
        if (v1x < minX) {
            minX = v1x;
        }
        if (v2x < minX) {
            minX = v2x;
        }
        if (v1x > maxX) {
            maxX = v1x;
        }
        if (v2x > maxX) {
            maxX = v2x;
        }
        if (minX > xExt || maxX < -xExt) {
            return false;
        }

        // test in Y-direction
        if (v1y < minY) {
            minY = v1y;
        }
        if (v2y < minY) {
            minY = v2y;
        }
        if (v1y > maxY) {
            maxY = v1y;
        }
        if (v2y > maxY) {
            maxY = v2y;
        }
        if (minY > yExt || maxY < -yExt) {
            return false;
        }

        // test in Z-direction
        if (v1z < minZ) {
            minZ = v1z;
        }
        if (v2z < minZ) {
            minZ = v2z;
        }
        if (v1z > maxZ) {
            maxZ = v1z;
        }
        if (v2z > maxZ) {
            maxZ = v2z;
        }
        if (minZ > zExt || maxZ < -zExt) {
            return false;
        }

        // === Test 3 ===
        // Check if the AABB intersects the plane of the triangle
        // This is done by checking if the farthest and closest vertex
        // of the AABB is on the same side of the plane
        // normal = e0 x e1
        final double nx = e1y * e0z - e1z * e0y, ny = e1z * e0x - e1x * e0z, nz = e1x * e0y - e1y * e0x, d = nx * v0x
                + ny * v0y + nz * v0z;

        triangle.normal.set(nx, ny, nz).normalizeLocal();

        if (nx > 0) {
            minX = -xExt;
            maxX = xExt;
        } else {
            minX = xExt;
            maxX = -xExt;
        }

        if (ny > 0) {
            minY = -yExt;
            maxY = yExt;
        } else {
            minY = yExt;
            maxY = -yExt;
        }

        if (nz > 0) {
            minZ = -zExt;
            maxZ = zExt;
        } else {
            minZ = zExt;
            maxZ = -zExt;
        }

        if (nx * minX + ny * minY + nz * minZ > d) {
            return false;
        }
        if (nx * maxX + ny * maxY + nz * maxZ < d) {
            return false;
        }

        return true;
    }
}
