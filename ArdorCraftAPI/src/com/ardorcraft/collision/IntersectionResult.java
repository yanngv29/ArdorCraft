/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.collision;

import com.ardorcraft.data.Pos;

/**
 * The result of a picking/tracing operation.
 * <ul>
 * <li>hit = was there a hit during this intersection test
 * <li>oldPos = the last block grid position before a hit (which is the position in front of the hit block face)
 * <li>pos = the block grid position that was hit
 * <li>length = the length of the ray from the source to the hit position
 * </ul>
 */
public class IntersectionResult {
    public boolean hit;
    public Pos oldPos = new Pos();
    public Pos pos = new Pos();
    public double length;

    public void set(final IntersectionResult result) {
        hit = result.hit;
        oldPos = new Pos(result.oldPos);
        pos = new Pos(result.pos);
        length = result.length;
    }

    @Override
    public String toString() {
        return "IntersectionResult [hit=" + hit + ", length=" + length + ", oldPos=" + oldPos + ", pos=" + pos + "]";
    }
}
