/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.util.queue;

import java.util.concurrent.atomic.AtomicLong;

import com.ardor3d.renderer.Renderer;

/**
 * A prioritized Task for the Worker. Sorted based on bucket, distance and time.
 */
public abstract class Task implements Comparable<Task> {
    public abstract void execute(Renderer renderer);

    private static AtomicLong atomicTime = new AtomicLong();
    private Coords playerCoords;

    private final int bucket;
    private final int x;
    private final int z;
    private final long time;

    public Task(final int bucket, final int x, final int z) {
        this.bucket = bucket;
        this.x = x;
        this.z = z;
        time = atomicTime.incrementAndGet();
    }

    public int compareToTest(final Task o2) {
        if (bucket < 2 && o2.bucket >= 2 || bucket >= 2 && o2.bucket < 2) {
            final int bucketDiff = bucket - o2.bucket;
            if (bucketDiff != 0) {
                return bucketDiff;
            }
        }

        final int diffX1 = x - playerCoords.getX();
        final int diffZ1 = z - playerCoords.getZ();
        final int manhattanDistance1 = Math.abs(diffX1) + Math.abs(diffZ1);
        final int diffX2 = o2.x - playerCoords.getX();
        final int diffZ2 = o2.z - playerCoords.getZ();
        final int manhattanDistance2 = Math.abs(diffX2) + Math.abs(diffZ2);
        if (manhattanDistance1 != manhattanDistance2) {
            return manhattanDistance1 - manhattanDistance2;
        }

        final int bucketDiff = bucket - o2.bucket;
        if (bucketDiff != 0) {
            return bucketDiff;
        }

        return (int) (time - o2.time);
    }

    @Override
    public int compareTo(final Task o2) {
        final int bucketDiff = bucket - o2.bucket;
        if (bucketDiff != 0) {
            return bucketDiff;
        }

        final int diffX1 = x - playerCoords.getX();
        final int diffZ1 = z - playerCoords.getZ();
        final int manhattanDistance1 = Math.abs(diffX1) + Math.abs(diffZ1);
        final int diffX2 = o2.x - playerCoords.getX();
        final int diffZ2 = o2.z - playerCoords.getZ();
        final int manhattanDistance2 = Math.abs(diffX2) + Math.abs(diffZ2);
        if (manhattanDistance1 != manhattanDistance2) {
            return manhattanDistance1 - manhattanDistance2;
        }

        return (int) (time - o2.time);
    }

    public void setPlayerCoords(final Coords playerCoords) {
        this.playerCoords = playerCoords;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Task [bucket=");
        builder.append(bucket);
        builder.append(", x=");
        builder.append(x);
        builder.append(", z=");
        builder.append(z);
        builder.append(", time=");
        builder.append(time);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Task other = (Task) obj;
        if (x != other.x) {
            return false;
        }
        if (z != other.z) {
            return false;
        }
        return true;
    }

    public int getBucket() {
        return bucket;
    }
}
