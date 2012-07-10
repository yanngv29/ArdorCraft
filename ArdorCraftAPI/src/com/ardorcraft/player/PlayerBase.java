/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.player;

import com.ardor3d.math.Vector3;

/**
 * Useful baseclass for player handling
 */
public class PlayerBase {
    protected final Vector3 position;
    protected final Vector3 direction;
    protected final Vector3 up;
    protected final Vector3 left;
    protected final Vector3 velocity;
    protected final Vector3 acceleration;

    public PlayerBase() {
        position = new Vector3(0, 0, 0);
        direction = new Vector3(Vector3.UNIT_Z);
        up = new Vector3(Vector3.UNIT_Y);
        left = new Vector3(Vector3.UNIT_X);
        velocity = new Vector3();
        acceleration = new Vector3();
    }

    public Vector3 getPosition() {
        return position;
    }

    public Vector3 getDirection() {
        return direction;
    }

    public Vector3 getUp() {
        return up;
    }

    public Vector3 getVelocity() {
        return velocity;
    }

    public Vector3 getLeft() {
        return left;
    }

    public Vector3 getAcceleration() {
        return acceleration;
    }

    public void normalize() {
        left.normalizeLocal();
        up.normalizeLocal();
        direction.normalizeLocal();
    }

}
