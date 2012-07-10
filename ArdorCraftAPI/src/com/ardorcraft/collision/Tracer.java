/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.collision;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardorcraft.world.BlockProvider;

/**
 * Ray tracing for a block world.
 */
public class Tracer {
    private double mult = 1;
    private final Vector3 tmax = new Vector3();
    private final Vector3 tdelta = new Vector3();
    private final Vector3 newPos = new Vector3();

    private static final class DefaultHitTester implements HitTester {
        @Override
        public boolean isHit(final int blockId) {
            return blockId != 0;
        }
    }

    private final int maxHeight;
    private final HitTester hitTester;
    private final BlockProvider provider;

    public Tracer(final BlockProvider provider) {
        this.provider = provider;
        hitTester = new DefaultHitTester();
        maxHeight = -1;
    }

    public Tracer(final BlockProvider provider, final HitTester hitTester) {
        this.provider = provider;
        this.hitTester = hitTester;
        maxHeight = -1;
    }

    public Tracer(final BlockProvider provider, final HitTester hitTester, final int maxHeight) {
        this.provider = provider;
        this.hitTester = hitTester;
        this.maxHeight = maxHeight;
    }

    public void traceCollision(ReadOnlyVector3 curpos, ReadOnlyVector3 raydir, final int iterations,
            final IntersectionResult result) {
        tmax.set(0, 0, 0);
        tdelta.set(0, 0, 0);

        mult = 1;
        boolean back = false;

        result.hit = false;
        result.length = 0;

        if (curpos.getY() < 0) {
            return;
        }
        if (maxHeight > 0 && curpos.getY() >= maxHeight - 1 && raydir.getY() >= 0) {
            return;
        }

        if (maxHeight > 0 && curpos.getY() >= maxHeight - 1) {
            final double diff = maxHeight - 1 - curpos.getY();
            final double t = diff / raydir.getY();
            newPos.set(raydir).multiplyLocal(t).addLocal(curpos);
            curpos = newPos;
        }

        int X = (int) MathUtils.floor(curpos.getX());
        int Y = (int) MathUtils.floor(curpos.getY());
        int Z = (int) MathUtils.floor(curpos.getZ());

        final int block1 = provider.getBlock(X, Y, Z);
        if (block1 != 0 && hitTester.isHit(block1)) {
            raydir = new Vector3(raydir).negateLocal();
            mult = -1;
            back = true;
        }

        int stepX, stepY, stepZ;
        double cbx, cby, cbz;

        if (raydir.getX() > 0.0) {
            stepX = 1;
            cbx = X + 1;
        } else {
            stepX = -1;
            cbx = X;
        }
        if (raydir.getY() > 0.0) {
            stepY = 1;
            cby = Y + 1;
        } else {
            stepY = -1;
            cby = Y;
        }
        if (raydir.getZ() > 0.0) {
            stepZ = 1;
            cbz = Z + 1;
        } else {
            stepZ = -1;
            cbz = Z;
        }

        if (raydir.getX() != 0) {
            final double rxr = 1.0 / raydir.getX();
            tmax.setX((cbx - curpos.getX()) * rxr);
            tdelta.setX(stepX * rxr);
        } else {
            tmax.setX(1000000);
        }
        if (raydir.getY() != 0) {
            final double ryr = 1.0 / raydir.getY();
            tmax.setY((cby - curpos.getY()) * ryr);
            tdelta.setY(stepY * ryr);
        } else {
            tmax.setY(1000000);
        }
        if (raydir.getZ() != 0) {
            final double rzr = 1.0 / raydir.getZ();
            tmax.setZ((cbz - curpos.getZ()) * rzr);
            tdelta.setZ(stepZ * rzr);
        } else {
            tmax.setZ(1000000);
        }

        int oldX = X, oldY = Y, oldZ = Z;

        for (int i = 0; i < iterations; i++) {
            if (tmax.getX() < tmax.getY()) {
                if (tmax.getX() < tmax.getZ()) {
                    X = X + stepX;
                    final int block = provider.getBlock(X, Y, Z);
                    final boolean isHit = block != 0 && hitTester.isHit(block);
                    if (back && !isHit || !back && isHit) {
                        gatherMin(result, tmax, X, Y, Z, oldX, oldY, oldZ);
                        result.hit = true;
                        return;
                    }
                    tmax.setX(tmax.getX() + tdelta.getX());
                } else {
                    Z = Z + stepZ;
                    final int block = provider.getBlock(X, Y, Z);
                    final boolean isHit = block != 0 && hitTester.isHit(block);
                    if (back && !isHit || !back && isHit) {
                        gatherMin(result, tmax, X, Y, Z, oldX, oldY, oldZ);
                        result.hit = true;
                        return;
                    }
                    tmax.setZ(tmax.getZ() + tdelta.getZ());
                }
            } else {
                if (tmax.getY() < tmax.getZ()) {
                    Y = Y + stepY;
                    if (maxHeight > 0 && Y >= maxHeight - 1) {
                        return;
                    }
                    final int block = provider.getBlock(X, Y, Z);
                    final boolean isHit = block != 0 && hitTester.isHit(block);
                    if (back && !isHit || !back && isHit) {
                        gatherMin(result, tmax, X, Y, Z, oldX, oldY, oldZ);
                        result.hit = true;
                        return;
                    }
                    tmax.setY(tmax.getY() + tdelta.getY());
                } else {
                    Z = Z + stepZ;
                    final int block = provider.getBlock(X, Y, Z);
                    final boolean isHit = block != 0 && hitTester.isHit(block);
                    if (back && !isHit || !back && isHit) {
                        gatherMin(result, tmax, X, Y, Z, oldX, oldY, oldZ);
                        result.hit = true;
                        return;
                    }
                    tmax.setZ(tmax.getZ() + tdelta.getZ());
                }
            }

            oldX = X;
            oldY = Y;
            oldZ = Z;
        }
    }

    private void gatherMin(final IntersectionResult result, final Vector3 tmax, final int X, final int Y, final int Z,
            final int oldX, final int oldY, final int oldZ) {
        result.oldPos.set(oldX, oldY, oldZ);
        result.pos.set(X, Y, Z);

        double min = tmax.getX();
        if (tmax.getY() < min) {
            min = tmax.getY();
        }
        if (tmax.getZ() < min) {
            min = tmax.getZ();
        }

        final double epsilon = 0.0001;

        double length = min * mult;
        if (length > 0) {
            length = Math.max(length - epsilon, 0.0);
        } else if (length < 0) {
            length = Math.min(length - epsilon, 0.0);
        }

        result.length = length;
    }
}
