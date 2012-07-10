/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.util.geometryproducers;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardorcraft.data.Direction;
import com.ardorcraft.util.BlockUtil;
import com.ardorcraft.world.BlockProvider;
import com.ardorcraft.world.BlockSide;
import com.ardorcraft.world.BlockType;
import com.ardorcraft.world.GeometryHandler;

/**
 * Main blockworld geometry producer. Produces standard 1x1x1 boxes with hidden surface removal.
 */
public final class BoxProducer implements GeometryProducer {
    private final int[] checkDirs = new int[] {//
            -1, 0, 0, //
            1, 0, 0, //
            0, -1, 0, //
            0, 1, 0, //
            0, 0, -1, //
            0, 0, 1, //
    };
    private final boolean[] sides = new boolean[] {//
            false, true,//
            true, false,//
            false, true
    //
    };
    private final Direction[] directions = new Direction[] {//
            Direction.X, Direction.X, //
            Direction.Y, Direction.Y, //
            Direction.Z, Direction.Z, //
    };

    private final float MOD = 0.12f;
    private final float creaseAmount = 0.3f;

    private final BlockSide[][] orientations = new BlockSide[][] {
            { // Front
                    BlockSide.Front, BlockSide.Back, BlockSide.Left, BlockSide.Right, BlockSide.Top, BlockSide.Bottom
            }, { // Back
                    BlockSide.Back, BlockSide.Front, BlockSide.Right, BlockSide.Left, BlockSide.Top, BlockSide.Bottom
            }, { // Left
                    BlockSide.Right, BlockSide.Left, BlockSide.Back, BlockSide.Front, BlockSide.Top, BlockSide.Bottom
            }, { // Right
                    BlockSide.Left, BlockSide.Right, BlockSide.Front, BlockSide.Back, BlockSide.Top, BlockSide.Bottom
            }, { // Top
                    BlockSide.Front, BlockSide.Back, BlockSide.Left, BlockSide.Right, BlockSide.Top, BlockSide.Bottom
            }, { // Bottom
                    BlockSide.Front, BlockSide.Back, BlockSide.Left, BlockSide.Right, BlockSide.Top, BlockSide.Bottom
            }
    };

    public BoxProducer() {}

    @Override
    public void generateBlock(final int blockId, final GeometryHandler geometryHandler, final BlockProvider provider,
            final BlockUtil blockUtil, final int x, final int y, final int z) {
        int startIndex = 0;

        final boolean isTransparent = blockUtil.getIsSemiTransparent(blockId);
        final BlockSide orientation = BlockSide.values()[provider.getBlockExtra(x, y, z)];

        for (int face = 0; face < 6; face++) {
            final int xChange = checkDirs[face * 3 + 0];
            final int yChange = checkDirs[face * 3 + 1];
            final int zChange = checkDirs[face * 3 + 2];
            final boolean side = sides[face];
            final Direction direction = directions[face];

            final int neighbourBlock = provider.getBlock(x + xChange, y + yChange, z + zChange);
            final boolean neighbourSolid = blockUtil.getBlockType(neighbourBlock) == BlockType.Solid;
            final boolean isNeighbourTransparent = blockUtil.getIsSemiTransparent(neighbourBlock);

            if (neighbourBlock == 0 || !neighbourSolid || !isTransparent && isNeighbourTransparent) {
                createBlockSide(geometryHandler, provider, blockUtil, orientation, x + Math.max(0, xChange),
                        y + Math.max(0, yChange), z + Math.max(0, zChange), blockId, side, direction, startIndex);
                startIndex++;
            }
        }
        geometryHandler.setIndexCount(startIndex * 6);
        geometryHandler.setVertexCount(startIndex * 4);
    }

    private void createBlockSide(final GeometryHandler geometryHandler, final BlockProvider provider,
            final BlockUtil blockUtil, final BlockSide orientation, final int x, final int y, final int z,
            final int block, final boolean first, final Direction dir, final int startIndex) {
        if (dir == Direction.X) {
            if (geometryHandler.hasVertices()) {
                geometryHandler.setVertex(startIndex * 4 + 0, x, y, z);
                geometryHandler.setVertex(startIndex * 4 + 1, x, y + 1, z);
                geometryHandler.setVertex(startIndex * 4 + 2, x, y + 1, z + 1);
                geometryHandler.setVertex(startIndex * 4 + 3, x, y, z + 1);
            }

            if (geometryHandler.hasTextureCoords()) {
                if (first) {
                    final ReadOnlyVector2 coord = blockUtil.getBlockTextureCoord(block,
                            getSide(orientation, BlockSide.Left));

                    geometryHandler.setTextureCoord(startIndex * 4 + 0, coord.getXf() + blockUtil.getTileWidth()
                            - blockUtil.getOffsetWidth(), coord.getYf() + blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 1, coord.getXf() + blockUtil.getTileWidth()
                            - blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getTileHeight() - blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 2, coord.getXf() + blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getTileHeight() - blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 3, coord.getXf() + blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getOffsetHeight());
                } else {
                    final ReadOnlyVector2 coord = blockUtil.getBlockTextureCoord(block,
                            getSide(orientation, BlockSide.Right));

                    geometryHandler.setTextureCoord(startIndex * 4 + 0, coord.getXf() + blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 1, coord.getXf() + blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getTileHeight() - blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 2, coord.getXf() + blockUtil.getTileWidth()
                            - blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getTileHeight() - blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 3, coord.getXf() + blockUtil.getTileWidth()
                            - blockUtil.getOffsetWidth(), coord.getYf() + blockUtil.getOffsetHeight());
                }
            }

            if (geometryHandler.hasColors()) {
                float sideModifier = 0.04f;
                if (first) {
                    sideModifier = MOD;
                }

                float globalLighting, localLighting, miniShade;

                miniShade = miniShadeX(provider, blockUtil, x, y, z, 0, first);
                globalLighting = MathUtils.clamp(geometryHandler.getGlobalLighting(x, y, z) - sideModifier, 0f, 1f)
                        * miniShade;
                localLighting = MathUtils.clamp(geometryHandler.getLocalLighting(x, y, z) - sideModifier, 0f, 1f)
                        * miniShade;
                geometryHandler.setColor(startIndex * 4 + 0, globalLighting, localLighting);

                miniShade = miniShadeX(provider, blockUtil, x, y, z, 1, first);
                globalLighting = MathUtils.clamp(geometryHandler.getGlobalLighting(x, y + 1, z) - sideModifier, 0f, 1f)
                        * miniShade;
                localLighting = MathUtils.clamp(geometryHandler.getLocalLighting(x, y + 1, z) - sideModifier, 0f, 1f)
                        * miniShade;
                geometryHandler.setColor(startIndex * 4 + 1, globalLighting, localLighting);

                miniShade = miniShadeX(provider, blockUtil, x, y, z, 2, first);
                globalLighting = MathUtils.clamp(geometryHandler.getGlobalLighting(x, y + 1, z + 1) - sideModifier, 0f,
                        1f) * miniShade;
                localLighting = MathUtils.clamp(geometryHandler.getLocalLighting(x, y + 1, z + 1) - sideModifier, 0f,
                        1f) * miniShade;
                geometryHandler.setColor(startIndex * 4 + 2, globalLighting, localLighting);

                miniShade = miniShadeX(provider, blockUtil, x, y, z, 3, first);
                globalLighting = MathUtils.clamp(geometryHandler.getGlobalLighting(x, y, z + 1) - sideModifier, 0f, 1f)
                        * miniShade;
                localLighting = MathUtils.clamp(geometryHandler.getLocalLighting(x, y, z + 1) - sideModifier, 0f, 1f)
                        * miniShade;
                geometryHandler.setColor(startIndex * 4 + 3, globalLighting, localLighting);
            }

            if (geometryHandler.hasIndices()) {
                if (first) {
                    geometryHandler.setIndex(startIndex * 6 + 0, startIndex * 4 + 0);
                    geometryHandler.setIndex(startIndex * 6 + 1, startIndex * 4 + 1);
                    geometryHandler.setIndex(startIndex * 6 + 2, startIndex * 4 + 3);
                    geometryHandler.setIndex(startIndex * 6 + 3, startIndex * 4 + 1);
                    geometryHandler.setIndex(startIndex * 6 + 4, startIndex * 4 + 2);
                    geometryHandler.setIndex(startIndex * 6 + 5, startIndex * 4 + 3);
                } else {
                    geometryHandler.setIndex(startIndex * 6 + 0, startIndex * 4 + 3);
                    geometryHandler.setIndex(startIndex * 6 + 1, startIndex * 4 + 2);
                    geometryHandler.setIndex(startIndex * 6 + 2, startIndex * 4 + 1);
                    geometryHandler.setIndex(startIndex * 6 + 3, startIndex * 4 + 1);
                    geometryHandler.setIndex(startIndex * 6 + 4, startIndex * 4 + 0);
                    geometryHandler.setIndex(startIndex * 6 + 5, startIndex * 4 + 3);
                }
            }
        } else if (dir == Direction.Y) {
            if (geometryHandler.hasVertices()) {
                geometryHandler.setVertex(startIndex * 4 + 0, x, y, z);
                geometryHandler.setVertex(startIndex * 4 + 1, x + 1, y, z);
                geometryHandler.setVertex(startIndex * 4 + 2, x + 1, y, z + 1);
                geometryHandler.setVertex(startIndex * 4 + 3, x, y, z + 1);
            }

            if (geometryHandler.hasTextureCoords()) {
                if (first) {
                    final ReadOnlyVector2 coord = blockUtil.getBlockTextureCoord(block,
                            getSide(orientation, BlockSide.Bottom));

                    geometryHandler.setTextureCoord(startIndex * 4 + 0, coord.getXf() + blockUtil.getTileWidth()
                            - blockUtil.getOffsetWidth(), coord.getYf() + blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 1, coord.getXf() + blockUtil.getTileWidth()
                            - blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getTileHeight() - blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 2, coord.getXf() + blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getTileHeight() - blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 3, coord.getXf() + blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getOffsetHeight());
                } else {
                    final ReadOnlyVector2 coord = blockUtil.getBlockTextureCoord(block,
                            getSide(orientation, BlockSide.Top));

                    geometryHandler.setTextureCoord(startIndex * 4 + 0, coord.getXf() + blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 1, coord.getXf() + blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getTileHeight() - blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 2, coord.getXf() + blockUtil.getTileWidth()
                            - blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getTileHeight() - blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 3, coord.getXf() + blockUtil.getTileWidth()
                            - blockUtil.getOffsetWidth(), coord.getYf() + blockUtil.getOffsetHeight());
                }
            }

            if (geometryHandler.hasColors()) {
                float sideModifier = 0.0f;
                if (first) {
                    sideModifier = MOD;
                }

                float globalLighting, localLighting, miniShade;

                miniShade = miniShadeY(provider, blockUtil, x, y, z, 0, first);
                globalLighting = MathUtils.clamp(geometryHandler.getGlobalLighting(x, y, z) - sideModifier, 0f, 1f)
                        * miniShade;
                localLighting = MathUtils.clamp(geometryHandler.getLocalLighting(x, y, z) - sideModifier, 0f, 1f)
                        * miniShade;
                geometryHandler.setColor(startIndex * 4 + 0, globalLighting, localLighting);

                miniShade = miniShadeY(provider, blockUtil, x, y, z, 1, first);
                globalLighting = MathUtils.clamp(geometryHandler.getGlobalLighting(x + 1, y, z) - sideModifier, 0f, 1f)
                        * miniShade;
                localLighting = MathUtils.clamp(geometryHandler.getLocalLighting(x + 1, y, z) - sideModifier, 0f, 1f)
                        * miniShade;
                geometryHandler.setColor(startIndex * 4 + 1, globalLighting, localLighting);

                miniShade = miniShadeY(provider, blockUtil, x, y, z, 2, first);
                globalLighting = MathUtils.clamp(geometryHandler.getGlobalLighting(x + 1, y, z + 1) - sideModifier, 0f,
                        1f) * miniShade;
                localLighting = MathUtils.clamp(geometryHandler.getLocalLighting(x + 1, y, z + 1) - sideModifier, 0f,
                        1f) * miniShade;
                geometryHandler.setColor(startIndex * 4 + 2, globalLighting, localLighting);

                miniShade = miniShadeY(provider, blockUtil, x, y, z, 3, first);
                globalLighting = MathUtils.clamp(geometryHandler.getGlobalLighting(x, y, z + 1) - sideModifier, 0f, 1f)
                        * miniShade;
                localLighting = MathUtils.clamp(geometryHandler.getLocalLighting(x, y, z + 1) - sideModifier, 0f, 1f)
                        * miniShade;
                geometryHandler.setColor(startIndex * 4 + 3, globalLighting, localLighting);
            }

            if (geometryHandler.hasIndices()) {
                if (first) {
                    geometryHandler.setIndex(startIndex * 6 + 0, startIndex * 4 + 0);
                    geometryHandler.setIndex(startIndex * 6 + 1, startIndex * 4 + 1);
                    geometryHandler.setIndex(startIndex * 6 + 2, startIndex * 4 + 3);
                    geometryHandler.setIndex(startIndex * 6 + 3, startIndex * 4 + 1);
                    geometryHandler.setIndex(startIndex * 6 + 4, startIndex * 4 + 2);
                    geometryHandler.setIndex(startIndex * 6 + 5, startIndex * 4 + 3);
                } else {
                    geometryHandler.setIndex(startIndex * 6 + 0, startIndex * 4 + 3);
                    geometryHandler.setIndex(startIndex * 6 + 1, startIndex * 4 + 2);
                    geometryHandler.setIndex(startIndex * 6 + 2, startIndex * 4 + 1);
                    geometryHandler.setIndex(startIndex * 6 + 3, startIndex * 4 + 1);
                    geometryHandler.setIndex(startIndex * 6 + 4, startIndex * 4 + 0);
                    geometryHandler.setIndex(startIndex * 6 + 5, startIndex * 4 + 3);
                }
            }
        } else if (dir == Direction.Z) {
            if (geometryHandler.hasVertices()) {
                geometryHandler.setVertex(startIndex * 4 + 0, x, y, z);
                geometryHandler.setVertex(startIndex * 4 + 1, x + 1, y, z);
                geometryHandler.setVertex(startIndex * 4 + 2, x + 1, y + 1, z);
                geometryHandler.setVertex(startIndex * 4 + 3, x, y + 1, z);
            }

            if (geometryHandler.hasTextureCoords()) {
                if (first) {
                    final ReadOnlyVector2 coord = blockUtil.getBlockTextureCoord(block,
                            getSide(orientation, BlockSide.Front));

                    geometryHandler.setTextureCoord(startIndex * 4 + 0, coord.getXf() + blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 1, coord.getXf() + blockUtil.getTileWidth()
                            - blockUtil.getOffsetWidth(), coord.getYf() + blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 2, coord.getXf() + blockUtil.getTileWidth()
                            - blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getTileHeight() - blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 3, coord.getXf() + blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getTileHeight() - blockUtil.getOffsetHeight());
                } else {
                    final ReadOnlyVector2 coord = blockUtil.getBlockTextureCoord(block,
                            getSide(orientation, BlockSide.Back));

                    geometryHandler.setTextureCoord(startIndex * 4 + 0, coord.getXf() + blockUtil.getTileWidth()
                            - blockUtil.getOffsetWidth(), coord.getYf() + blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 1, coord.getXf() + blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 2, coord.getXf() + blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getTileHeight() - blockUtil.getOffsetHeight());
                    geometryHandler.setTextureCoord(startIndex * 4 + 3, coord.getXf() + blockUtil.getTileWidth()
                            - blockUtil.getOffsetWidth(),
                            coord.getYf() + blockUtil.getTileHeight() - blockUtil.getOffsetHeight());
                }
            }

            if (geometryHandler.hasColors()) {
                float sideModifier = 0.04f;
                if (first) {
                    sideModifier = MOD;
                }

                float globalLighting, localLighting, miniShade;

                miniShade = miniShadeZ(provider, blockUtil, x, y, z, 0, first);
                globalLighting = MathUtils.clamp(geometryHandler.getGlobalLighting(x, y, z) - sideModifier, 0f, 1f)
                        * miniShade;
                localLighting = MathUtils.clamp(geometryHandler.getLocalLighting(x, y, z) - sideModifier, 0f, 1f)
                        * miniShade;
                geometryHandler.setColor(startIndex * 4 + 0, globalLighting, localLighting);

                miniShade = miniShadeZ(provider, blockUtil, x, y, z, 1, first);
                globalLighting = MathUtils.clamp(geometryHandler.getGlobalLighting(x + 1, y, z) - sideModifier, 0f, 1f)
                        * miniShade;
                localLighting = MathUtils.clamp(geometryHandler.getLocalLighting(x + 1, y, z) - sideModifier, 0f, 1f)
                        * miniShade;
                geometryHandler.setColor(startIndex * 4 + 1, globalLighting, localLighting);

                miniShade = miniShadeZ(provider, blockUtil, x, y, z, 2, first);
                globalLighting = MathUtils.clamp(geometryHandler.getGlobalLighting(x + 1, y + 1, z) - sideModifier, 0f,
                        1f) * miniShade;
                localLighting = MathUtils.clamp(geometryHandler.getLocalLighting(x + 1, y + 1, z) - sideModifier, 0f,
                        1f) * miniShade;
                geometryHandler.setColor(startIndex * 4 + 2, globalLighting, localLighting);

                miniShade = miniShadeZ(provider, blockUtil, x, y, z, 3, first);
                globalLighting = MathUtils.clamp(geometryHandler.getGlobalLighting(x, y + 1, z) - sideModifier, 0f, 1f)
                        * miniShade;
                localLighting = MathUtils.clamp(geometryHandler.getLocalLighting(x, y + 1, z) - sideModifier, 0f, 1f)
                        * miniShade;
                geometryHandler.setColor(startIndex * 4 + 3, globalLighting, localLighting);
            }

            if (geometryHandler.hasIndices()) {
                if (first) {
                    geometryHandler.setIndex(startIndex * 6 + 0, startIndex * 4 + 0);
                    geometryHandler.setIndex(startIndex * 6 + 1, startIndex * 4 + 1);
                    geometryHandler.setIndex(startIndex * 6 + 2, startIndex * 4 + 3);
                    geometryHandler.setIndex(startIndex * 6 + 3, startIndex * 4 + 1);
                    geometryHandler.setIndex(startIndex * 6 + 4, startIndex * 4 + 2);
                    geometryHandler.setIndex(startIndex * 6 + 5, startIndex * 4 + 3);
                } else {
                    geometryHandler.setIndex(startIndex * 6 + 0, startIndex * 4 + 3);
                    geometryHandler.setIndex(startIndex * 6 + 1, startIndex * 4 + 2);
                    geometryHandler.setIndex(startIndex * 6 + 2, startIndex * 4 + 1);
                    geometryHandler.setIndex(startIndex * 6 + 3, startIndex * 4 + 1);
                    geometryHandler.setIndex(startIndex * 6 + 4, startIndex * 4 + 0);
                    geometryHandler.setIndex(startIndex * 6 + 5, startIndex * 4 + 3);
                }
            }
        }
    }

    private BlockSide getSide(final BlockSide orientation, final BlockSide side) {
        return orientations[orientation.ordinal()][side.ordinal()];
    }

    private float miniShadeX(final BlockProvider provider, final BlockUtil blockUtil, final int x, final int y,
            final int z, final int index, final boolean first) {
        final int xx = first ? x + 0 : x - 1;
        final int yy = y;
        final int zz = z;

        boolean b1, b2, b3;

        if (index == 0) {
            b1 = isSolid(provider, blockUtil, xx, yy - 1, zz);
            b2 = isSolid(provider, blockUtil, xx, yy, zz - 1);
            b3 = isSolid(provider, blockUtil, xx, yy - 1, zz - 1);
            return getShade(b1, b2, b3);
        } else if (index == 1) {
            b1 = isSolid(provider, blockUtil, xx, yy + 1, zz);
            b2 = isSolid(provider, blockUtil, xx, yy, zz - 1);
            b3 = isSolid(provider, blockUtil, xx, yy + 1, zz - 1);
            return getShade(b1, b2, b3);
        } else if (index == 2) {
            b1 = isSolid(provider, blockUtil, xx, yy + 1, zz);
            b2 = isSolid(provider, blockUtil, xx, yy, zz + 1);
            b3 = isSolid(provider, blockUtil, xx, yy + 1, zz + 1);
            return getShade(b1, b2, b3);
        } else if (index == 3) {
            b1 = isSolid(provider, blockUtil, xx, yy - 1, zz);
            b2 = isSolid(provider, blockUtil, xx, yy, zz + 1);
            b3 = isSolid(provider, blockUtil, xx, yy - 1, zz + 1);
            return getShade(b1, b2, b3);
        }

        return 1f;
    }

    private float miniShadeY(final BlockProvider provider, final BlockUtil blockUtil, final int x, final int y,
            final int z, final int index, final boolean first) {
        final int xx = x;
        final int yy = first ? y - 1 : y + 0;
        final int zz = z;

        boolean b1, b2, b3;

        if (index == 0) {
            b1 = isSolid(provider, blockUtil, xx - 1, yy, zz);
            b2 = isSolid(provider, blockUtil, xx, yy, zz - 1);
            b3 = isSolid(provider, blockUtil, xx - 1, yy, zz - 1);
            return getShade(b1, b2, b3);
        } else if (index == 1) {
            b1 = isSolid(provider, blockUtil, xx + 1, yy, zz);
            b2 = isSolid(provider, blockUtil, xx, yy, zz - 1);
            b3 = isSolid(provider, blockUtil, xx + 1, yy, zz - 1);
            return getShade(b1, b2, b3);
        } else if (index == 2) {
            b1 = isSolid(provider, blockUtil, xx + 1, yy, zz);
            b2 = isSolid(provider, blockUtil, xx, yy, zz + 1);
            b3 = isSolid(provider, blockUtil, xx + 1, yy, zz + 1);
            return getShade(b1, b2, b3);
        } else if (index == 3) {
            b1 = isSolid(provider, blockUtil, xx - 1, yy, zz);
            b2 = isSolid(provider, blockUtil, xx, yy, zz + 1);
            b3 = isSolid(provider, blockUtil, xx - 1, yy, zz + 1);
            return getShade(b1, b2, b3);
        }

        return 1f;
    }

    private float miniShadeZ(final BlockProvider provider, final BlockUtil blockUtil, final int x, final int y,
            final int z, final int index, final boolean first) {
        final int xx = x;
        final int yy = y;
        final int zz = first ? z + 0 : z - 1;

        boolean b1, b2, b3;

        if (index == 0) {
            b1 = isSolid(provider, blockUtil, xx, yy - 1, zz);
            b2 = isSolid(provider, blockUtil, xx - 1, yy, zz);
            b3 = isSolid(provider, blockUtil, xx - 1, yy - 1, zz);
            return getShade(b1, b2, b3);
        } else if (index == 1) {
            b1 = isSolid(provider, blockUtil, xx, yy - 1, zz);
            b2 = isSolid(provider, blockUtil, xx + 1, yy, zz);
            b3 = isSolid(provider, blockUtil, xx + 1, yy - 1, zz);
            return getShade(b1, b2, b3);
        } else if (index == 2) {
            b1 = isSolid(provider, blockUtil, xx, yy + 1, zz);
            b2 = isSolid(provider, blockUtil, xx + 1, yy, zz);
            b3 = isSolid(provider, blockUtil, xx + 1, yy + 1, zz);
            return getShade(b1, b2, b3);
        } else if (index == 3) {
            b1 = isSolid(provider, blockUtil, xx, yy + 1, zz);
            b2 = isSolid(provider, blockUtil, xx - 1, yy, zz);
            b3 = isSolid(provider, blockUtil, xx - 1, yy + 1, zz);
            return getShade(b1, b2, b3);
        }

        return 1f;
    }

    // TODO: add solid amount
    private boolean isSolid(final BlockProvider provider, final BlockUtil util, final int x, final int y, final int z) {
        final int blockId = provider.getBlock(x, y, z);
        if (blockId == 0) {
            return false;
        }
        return util.getBlockType(blockId) == BlockType.Solid;
    }

    private float getShade(final boolean b1, final boolean b2, final boolean b3) {
        if (b1 && b2) {
            return 1f - creaseAmount * 2.0f;
        } else if (b1 || b2 || b3) {
            return 1f - creaseAmount;
        }
        return 1f;
    }
}
