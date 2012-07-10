/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.pathfinding;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import com.ardorcraft.pathfinding.calculator.DefaultAStarCalculator;
import com.ardorcraft.pathfinding.heuristics.ClosestHeuristic;
import com.ardorcraft.world.BlockProvider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * AStar pathfinding with pluggable cost calculator (since that is game specific).
 * <p>
 * Typical usage:
 * <p>
 * 
 * <pre>
 * ConstrainedAStar pathFinder = new ConstrainedAStar(blockWorld, 40);
 * ...
 * PathResult pathResult = pathFinder.findPath(source.x, source.y, source.z, target.x, target.y, target.z);
 * </pre>
 */
public class ConstrainedAStar {
    private final Set<PathNode> closed = Sets.newHashSet();
    private final PriorityQueue<PathNode> open = new PriorityQueue<PathNode>();
    private final Map<PathNode, PathNode> nodeTable = Maps.newHashMap();

    private final int maxSearchDistance;
    private final AStarHeuristic heuristic;
    private final MoveCalculator moveCalculator;
    private final BlockProvider map;
    private final float heuristicMultiplier = 0.2f;

    public ConstrainedAStar(final BlockProvider map, final int maxSearchDistance) {
        this(map, maxSearchDistance, new ClosestHeuristic(), new DefaultAStarCalculator());
    }

    public ConstrainedAStar(final BlockProvider map, final int maxSearchDistance, final AStarHeuristic heuristic,
            final MoveCalculator moveCalculator) {
        this.heuristic = heuristic;
        this.map = map;
        this.maxSearchDistance = maxSearchDistance;
        this.moveCalculator = moveCalculator;
    }

    public PathResult findPath(final int sx, final int sy, final int sz, final int tx, final int ty, final int tz) {
        closed.clear();
        open.clear();
        nodeTable.clear();
        final MoveData moveData = new MoveData();

        final PathNode startNode = new PathNode(sx, sy, sz);
        final PathNode endNode = new PathNode(tx, ty, tz);

        if (startNode.equals(endNode)) {
            return new PathResult(null, true);
        }

        startNode.heuristic = getHeuristicCost(startNode, tx, ty, tz) * heuristicMultiplier;
        open.add(startNode);
        nodeTable.put(startNode, startNode);
        nodeTable.put(endNode, endNode);

        PathNode current = null;
        int maxDepth = 0;
        while (maxDepth < maxSearchDistance && !open.isEmpty()) {
            current = open.poll();
            if (current == null || current.equals(endNode)) {
                break;
            }

            Thread.yield();

            closed.add(current);

            for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }

                    moveCalculator.calculateMove(map, moveData, current, x, z);
                    if (moveData.canMove) {
                        final float nextStepCost = current.cost + moveData.cost;

                        PathNode neighbour = new PathNode(current.x + x, moveData.newY, current.z + z);
                        if (nodeTable.containsKey(neighbour)) {
                            neighbour = nodeTable.get(neighbour);
                        } else {
                            nodeTable.put(neighbour, neighbour);
                        }

                        if (nextStepCost < neighbour.cost) {
                            if (open.contains(neighbour)) {
                                open.remove(neighbour);
                            }
                            if (closed.contains(neighbour)) {
                                closed.remove(neighbour);
                            }
                        }

                        if (!open.contains(neighbour) && !closed.contains(neighbour)) {
                            neighbour.cost = nextStepCost;
                            neighbour.heuristic = getHeuristicCost(neighbour, tx, ty, tz) * heuristicMultiplier;
                            maxDepth = Math.max(maxDepth, neighbour.setParent(current));
                            open.add(neighbour);
                        }
                    }
                }
            }
        }

        if (current == null) {
            return new PathResult(null, false);
        }
        if (!current.equals(endNode)) {
            PathNode minNode = null;
            float minHeuristic = Float.MAX_VALUE;
            for (final PathNode node : nodeTable.keySet()) {
                if (node != endNode && node.heuristic + node.cost < minHeuristic) {
                    minHeuristic = node.heuristic + node.cost;
                    minNode = node;
                }
            }

            if (minNode == null) {
                return new PathResult(null, false);
            }
            if (minNode.equals(startNode)) {
                final List<PathNode> result = Lists.newArrayList();
                result.add(minNode);
                return new PathResult(result, false);
            }

            final List<PathNode> result = Lists.newArrayList();
            PathNode target = minNode;
            while (target != null && target != startNode) {
                result.add(target);
                target = target.parent;
            }
            Collections.reverse(result);

            closed.clear();
            open.clear();
            nodeTable.clear();

            return new PathResult(result, false);
        }

        final List<PathNode> result = Lists.newArrayList();
        PathNode target = endNode;
        while (target != null && target != startNode) {
            result.add(target);
            target = target.parent;
        }
        Collections.reverse(result);

        closed.clear();
        open.clear();
        nodeTable.clear();

        return new PathResult(result, true);
    }

    private float getHeuristicCost(final PathNode node, final int tx, final int ty, final int tz) {
        return heuristic.getCost(node.getX(), node.getY(), node.getZ(), tx, ty, tz);
    }

}
