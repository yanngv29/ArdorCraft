/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.pathfinding;

public class PathNode implements Comparable<PathNode> {
    public final int x;
    public final int y;
    public final int z;

    PathNode parent;

    float cost;
    float heuristic;
    int depth;

    public PathNode(final int x, final int y, final int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int setParent(final PathNode parent) {
        depth = parent.depth + 1;
        this.parent = parent;

        return depth;
    }

    @Override
    public int compareTo(final PathNode other) {
        final PathNode o = other;

        final float f = heuristic + cost;
        final float of = o.heuristic + o.cost;

        if (f < of) {
            return -1;
        } else if (f > of) {
            return 1;
        } else {
            return 0;
        }
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
        if (!(obj instanceof PathNode)) {
            return false;
        }
        final PathNode other = (PathNode) obj;
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

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public float getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "PathNode [x=" + x + ", y=" + y + ", z=" + z + ", cost=" + cost + ", heuristic=" + heuristic + "]";
    }
}
