/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.pathfinding.calculator;

import com.ardorcraft.pathfinding.MoveCalculator;
import com.ardorcraft.pathfinding.MoveData;
import com.ardorcraft.pathfinding.PathNode;
import com.ardorcraft.world.BlockProvider;

public class DefaultAStarCalculator implements MoveCalculator {
    @Override
    public void calculateMove(final BlockProvider map, final MoveData moveData, final PathNode current,
            final int xChange, final int zChange) {
        moveData.canMove = false;
        moveData.cost = 0.01f;
        moveData.newY = current.y;

        // Special handling of diagonal movement
        if (xChange != 0 && zChange != 0) {
            final int block1 = map.getBlock(current.x + xChange, current.y, current.z);
            final int block2 = map.getBlock(current.x + xChange, current.y + 1, current.z);

            final int block3 = map.getBlock(current.x, current.y, current.z + zChange);
            final int block4 = map.getBlock(current.x, current.y + 1, current.z + zChange);

            final int block5 = map.getBlock(current.x + xChange, current.y, current.z + zChange);
            final int block6 = map.getBlock(current.x + xChange, current.y + 1, current.z + zChange);

            if (block1 == 0 && block2 == 0 && block3 == 0 && block4 == 0 && block5 == 0 && block6 == 0) {
                moveData.canMove = true;
                moveData.cost = 0.05f * 1.42f;

                for (int y = moveData.newY - 1; y >= 0; y--) {
                    final int block7 = map.getBlock(current.x + xChange, y, current.z + zChange);
                    if (block7 != 0) {
                        moveData.newY = y + 1;
                        final int height = Math.abs(moveData.newY - current.y);
                        moveData.cost = 0.05f * 1.42f + 0.05f * height;
                        if (height > 3) {
                            moveData.cost *= 10.0f;
                        }
                        if (height > 5) {
                            moveData.canMove = false;
                        }
                        break;
                    }
                }
            }
            return;
        }

        final int block1 = map.getBlock(current.x + xChange, current.y, current.z + zChange);
        final int block2 = map.getBlock(current.x + xChange, current.y + 1, current.z + zChange);
        // Walk straight on
        if (block1 == 0 && block2 == 0) {
            moveData.canMove = true;

            for (int y = moveData.newY - 1; y >= 0; y--) {
                final int block3 = map.getBlock(current.x + xChange, y, current.z + zChange);
                if (block3 != 0) {
                    moveData.newY = y + 1;
                    final int height = Math.abs(moveData.newY - current.y);
                    moveData.cost = 0.05f + 0.05f * height;
                    if (height > 3) {
                        moveData.cost *= 10.0f;
                    }
                    if (height > 5) {
                        moveData.canMove = false;
                    }
                    break;
                }
            }
        }
        // Climb
        else if (block1 != 0 && block2 == 0) {
            final int block3 = map.getBlock(current.x, current.y + 2, current.z);
            final int block4 = map.getBlock(current.x + xChange, current.y + 2, current.z + zChange);
            if (block3 == 0 && block4 == 0) {
                moveData.canMove = true;
                moveData.cost = 0.1f;
                moveData.newY += 1;
            }
        }
    }
}
