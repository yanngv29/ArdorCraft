/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.pathfinding;

import java.util.List;

public class PathResult {
    private final List<PathNode> path;
    private final boolean isFullPath;

    public PathResult(final List<PathNode> path, final boolean isFullPath) {
        this.path = path;
        this.isFullPath = isFullPath;
    }

    public List<PathNode> getPath() {
        return path;
    }

    public boolean isFullPath() {
        return isFullPath;
    }

    @Override
    public String toString() {
        return "PathResult [path=" + path + ", isFullPath=" + isFullPath + "]";
    }
}
