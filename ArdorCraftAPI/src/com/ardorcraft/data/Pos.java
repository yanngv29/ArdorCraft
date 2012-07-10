/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.data;

/**
 * Integer 3D coordinate
 */
public class Pos {
    public int x, y, z;

    public Pos() {

    }

    public Pos(final Pos pos) {
        this(pos.x, pos.y, pos.z);
    }

    public Pos(final int x, final int y, final int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        result = prime * result + z;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Pos)) {
            return false;
        }
        final Pos other = (Pos) obj;
        if (x != other.x) {
            return false;
        }
        if (y != other.y) {
            return false;
        }
        if (z != other.z) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Pos [x=" + x + ", y=" + y + ", z=" + z + "]";
    }

    public void set(final int x, final int y, final int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(final Pos pos) {
        set(pos.x, pos.y, pos.z);
    }

    public void add(final int x, final int y, final int z) {
        this.x += x;
        this.y += y;
        this.z += z;
    }

    public void subtract(final int x, final int y, final int z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
    }
}
