/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.util;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.geom.MeshCombiner;
import com.ardorcraft.util.geometryproducers.BoxProducer;
import com.ardorcraft.util.geometryproducers.GeometryProducer;
import com.ardorcraft.util.geometryproducers.MeshProducer;
import com.ardorcraft.world.BlockSide;
import com.ardorcraft.world.BlockType;
import com.ardorcraft.world.BlockWorld;
import com.google.common.collect.Maps;

/**
 * Main class to use for setting up properties for all block types.
 */
public final class BlockUtil {
    private static final Logger logger = Logger.getLogger(BlockUtil.class.getName());

    private final int totalTextureSizeWidth;
    private final int totalTextureSizeHeight;
    private final int textureSize;
    private final float textureReciprocalX;
    private final float textureReciprocalY;
    private final float offsetWidth;
    private final float offsetHeight;
    private final int xCount;
    private final int yCount;

    private final Map<Integer, EnumMap<BlockSide, Vector2>> blockTextureCoords = Maps.newHashMap();
    private final BlockType[] types = new BlockType[256];
    private final boolean[] isLocalLightMap = new boolean[256];
    private final GeometryProducer[] geometryProducers = new GeometryProducer[256];
    private final boolean[] isCollidable = new boolean[256];
    private final boolean[] isPickable = new boolean[256];
    private final boolean[] isSemiTransparent = new boolean[256];

    public BlockUtil(final int textureWidth, final int textureHeight, final int subTextureSize) {
        totalTextureSizeWidth = textureWidth;
        totalTextureSizeHeight = textureHeight;
        textureSize = subTextureSize;

        textureReciprocalX = (float) textureSize / (float) totalTextureSizeWidth;
        textureReciprocalY = (float) textureSize / (float) totalTextureSizeHeight;

        xCount = totalTextureSizeWidth / textureSize;
        yCount = totalTextureSizeHeight / textureSize;

        offsetWidth = 0.1f / totalTextureSizeWidth;
        offsetHeight = 0.1f / totalTextureSizeHeight;

        setupDefaultBlocks();
    }

    private void setupDefaultBlocks() {
        final GeometryProducer producer = new BoxProducer();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                final int index = y * 16 + x + 1;
                if (index > 255) {
                    break;
                }
                setBlockMapping(index, x, y);
                setBlockType(index, BlockType.Solid);
                setGeometryProducer(index, producer);
                setIsCollidable(index, true);
                setIsPickable(index, true);
                setIsLocalLightSource(index, false);
            }
        }

        final Mesh vegetationMesh = createVegetationMesh();
        final MeshProducer vegetationMeshProducer = new MeshProducer(vegetationMesh);
        vegetationMeshProducer.setTransformTextureCoords(true);

        // setBlockType(BlockWorld.WATER, BlockType.Transparent);
        setIsSemiTransparent(BlockWorld.WATER, true);
        setIsPickable(BlockWorld.WATER, false);
        setIsCollidable(BlockWorld.WATER, false);

        setBlockMapping(1, 1, 0); // stone
        setBlockMapping(2, 0, 0, 3, 0, 2, 0); // grass
        setBlockMapping(3, 2, 0); // dirt
        setBlockMapping(4, 0, 1); // cobblestone
        setBlockMapping(5, 4, 0); // wooden plank

        setBlockMapping(6, 15, 0); // sapling
        setBlockType(6, BlockType.Transparent);
        setGeometryProducer(6, vegetationMeshProducer);
        setIsCollidable(6, false);

        setBlockMapping(7, 1, 1); // bedrock
        setBlockMapping(8, 13, 12); // water
        setBlockMapping(9, 13, 12); // stationary water
        setBlockMapping(10, 13, 14); // lava
        setBlockMapping(11, 13, 14); // stationary lava
        setBlockMapping(12, 2, 1); // sand
        setBlockMapping(13, 3, 1); // gravel
        setBlockMapping(14, 0, 2); // gold ore
        setBlockMapping(15, 1, 2); // iron ore
        setBlockMapping(16, 2, 2); // coal ore
        setBlockMapping(17, 4, 1); // wood

        setBlockMapping(18, 4, 3); // leaves
        setBlockType(18, BlockType.Transparent);

        setBlockMapping(19, 0, 3); // sponge

        setBlockMapping(20, 1, 3); // glass
        setBlockType(20, BlockType.Transparent);

        setBlockMapping(21, 0, 10); // lapis ore
        setBlockMapping(22, 0, 9); // lapis block
        setBlockMapping(23, 14, 3, 13, 2, 14, 3); // dispenser
        setBlockMapping(24, 0, 12); // sandstone

        setBlockMapping(37, 13, 0); // dandelion
        setBlockType(37, BlockType.Transparent);
        setGeometryProducer(37, vegetationMeshProducer);
        setIsCollidable(37, false);
        setBlockMapping(38, 12, 0); // rose
        setBlockType(38, BlockType.Transparent);
        setGeometryProducer(38, vegetationMeshProducer);
        setIsCollidable(38, false);
        setBlockMapping(39, 12, 1); // brown mushroom
        setBlockType(39, BlockType.Transparent);
        setGeometryProducer(39, vegetationMeshProducer);
        setIsCollidable(39, false);
        setBlockMapping(40, 13, 1); // red mushroom
        setBlockType(40, BlockType.Transparent);
        setGeometryProducer(40, vegetationMeshProducer);
        setIsCollidable(40, false);

        setBlockMapping(41, 7, 1); // gold block
        setBlockMapping(42, 6, 1); // iron block
        setBlockMapping(43, 5, 0); // double slabs
        setBlockMapping(44, 5, 0); // slabs
        setBlockMapping(45, 7, 0); // brick block
        setBlockMapping(46, 8, 0); // tnt
        setBlockMapping(47, 4, 0, 3, 2, 4, 0, 4, 0, 4, 0, 4, 0); // bookshelf
        setBlockMapping(48, 4, 2); // moss stone
        setBlockMapping(49, 5, 2); // obsidian

        setBlockMapping(50, 0, 5); // torch
        setBlockType(50, BlockType.Transparent);
        setGeometryProducer(50, vegetationMeshProducer);
        setIsCollidable(50, false);
        setIsLocalLightSource(50, true);

        setBlockMapping(51, 0, 5); // fire
        setBlockType(51, BlockType.Transparent);
        setIsCollidable(51, false);
        setIsLocalLightSource(51, true);

        setBlockMapping(52, 1, 4); // monster spawner
        setBlockType(52, BlockType.Transparent);

        setBlockMapping(53, 4, 0); // stairs
        setBlockMapping(54, 9, 1, 10, 1, 11, 1); // chest

        setBlockMapping(55, 4, 10); // redstone wire
        setBlockType(55, BlockType.Transparent);

        setBlockMapping(56, 2, 3); // diamond ore
        setBlockMapping(57, 8, 1); // diamond block
        setBlockMapping(58, 11, 2, 11, 3, 11, 2); // crafting table

        setBlockMapping(59, 15, 5); // seeds
        setBlockType(59, BlockType.Transparent);

        setBlockMapping(60, 7, 5); // farmland
        setBlockMapping(61, 12, 2); // furnace
        setBlockMapping(62, 13, 3); // burning furnace
        setBlockMapping(63, 4, 0); // sign post

        setBlockMapping(64, 1, 5); // door
        setBlockType(64, BlockType.Transparent);
        setBlockMapping(65, 3, 5); // ladders
        setBlockType(65, BlockType.Transparent);
        setBlockMapping(66, 0, 8); // rails
        setBlockType(66, BlockType.Transparent);

        setBlockMapping(67, 0, 1); // cobblestone stairs
        setBlockMapping(68, 4, 0); // wallsign
        setBlockMapping(69, 4, 8); // lever
        setBlockMapping(70, 6, 0); // stone pressure plate
        setBlockMapping(71, 2, 5); // iron door
        setBlockMapping(72, 4, 0); // wooden pressure plate
        setBlockMapping(73, 3, 3); // redstone ore
        setBlockMapping(74, 3, 3); // glowing redstone ore

        setBlockMapping(75, 0, 6); // redstone torch off
        setBlockType(75, BlockType.Transparent);

        setBlockMapping(76, 0, 5); // restone torch on
        setBlockType(76, BlockType.Transparent);
        setIsLocalLightSource(76, true);

        setBlockMapping(77, 0, 1); // stone button
        setBlockMapping(78, 2, 4); // snow
        setBlockMapping(79, 2, 4); // ice
        setBlockMapping(80, 2, 4); // snow block

        setBlockMapping(81, 5, 4, 6, 4, 5, 4); // cactus
        setBlockType(81, BlockType.Transparent);

        setBlockMapping(82, 3, 1); // clay block

        setBlockMapping(83, 9, 4); // sugar cane
        setBlockType(83, BlockType.Transparent);

        setBlockMapping(84, 11, 4, 10, 4, 10, 4); // jukebox

        setBlockMapping(85, 5, 9); // fence
        setBlockType(85, BlockType.Transparent);

        setBlockMapping(86, 6, 6, 7, 7, 6, 7); // pumpkin
        setBlockMapping(87, 7, 6); // netherrack
        setBlockMapping(88, 8, 6); // soul sand
        setBlockMapping(89, 9, 6); // glowstone block
        setBlockMapping(90, 1, 12); // portal
        setBlockMapping(91, 1, 12); // jack-o-lantern

        setBlockMapping(92, 9, 7, 10, 7, 12, 7); // cake block
        setBlockType(92, BlockType.Transparent);

        setBlockMapping(93, 7, 6); // redstone repeater off
        setBlockMapping(94, 7, 6); // redstone repeater on
        setBlockMapping(95, 9, 1, 11, 1, 10, 1, 10, 1, 10, 1, 9, 1); // locked chest

        // Some grass stuff
        for (int i = 0; i < 8; i++) {
            setBlockType(100 + i, BlockType.Transparent);
            setBlockMapping(100 + i, 8 + i, 5);
            setIsCollidable(100 + i, false);
            setIsPickable(100 + i, false);
            setGeometryProducer(100 + i, vegetationMeshProducer);
        }
    }

    /**
     * Set if this block should emit light
     * 
     * @param blockId
     * @param value
     */
    public void setIsLocalLightSource(final int blockId, final boolean value) {
        isLocalLightMap[blockId] = value;
    }

    /**
     * Setup texture tiles to use for this block
     * 
     * @param blockId
     * @param x
     * @param y
     */
    public void setBlockMapping(final int blockId, final int x, final int y) {
        setBlockMapping(blockId, x, y, x, y, x, y);
    }

    /**
     * Setup texture tiles to use for this block
     * 
     * @param blockId
     * @param xTop
     * @param yTop
     * @param xSide
     * @param ySide
     * @param xBottom
     * @param yBottom
     */
    public void setBlockMapping(final int blockId, final int xTop, final int yTop, final int xSide, final int ySide,
            final int xBottom, final int yBottom) {
        setBlockMapping(blockId, xTop, yTop, xSide, ySide, xSide, ySide, xSide, ySide, xSide, ySide, xBottom, yBottom);
    }

    /**
     * Setup texture tiles to use for this block
     * 
     * @param blockId
     * @param xTop
     * @param yTop
     * @param xFront
     * @param yFront
     * @param xBack
     * @param yBack
     * @param xLeft
     * @param yLeft
     * @param xRight
     * @param yRight
     * @param xBottom
     * @param yBottom
     */
    public void setBlockMapping(final int blockId, final int xTop, final int yTop, final int xFront, final int yFront,
            final int xBack, final int yBack, final int xLeft, final int yLeft, final int xRight, final int yRight,
            final int xBottom, final int yBottom) {
        setBlockMapping(blockId, BlockSide.Top, xTop, yTop);
        setBlockMapping(blockId, BlockSide.Front, xFront, yFront);
        setBlockMapping(blockId, BlockSide.Back, xBack, yBack);
        setBlockMapping(blockId, BlockSide.Left, xLeft, yLeft);
        setBlockMapping(blockId, BlockSide.Right, xRight, yRight);
        setBlockMapping(blockId, BlockSide.Bottom, xBottom, yBottom);
    }

    /**
     * Setup texture tiles to use for this block
     * 
     * @param blockId
     * @param side
     * @param x
     * @param y
     */
    public void setBlockMapping(final int blockId, final BlockSide side, final int x, final int y) {
        EnumMap<BlockSide, Vector2> coord = blockTextureCoords.get(blockId);
        if (coord == null) {
            coord = Maps.newEnumMap(BlockSide.class);
            blockTextureCoords.put(blockId, coord);
        }
        fillCoord(coord, side, (yCount - y - 1) * xCount + x);
    }

    /**
     * Set if this block is see-through or not (if not, then hidden surface removal can be done)
     * 
     * @param blockId
     * @param type
     */
    public void setBlockType(final int blockId, final BlockType type) {
        types[blockId] = type;
    }

    public BlockType getBlockType(final int blockId) {
        return types[blockId];
    }

    public boolean isLocalLight(final int block) {
        return isLocalLightMap[block];
    }

    private void fillCoord(final EnumMap<BlockSide, Vector2> coord, final BlockSide blockSide, final int pos) {
        final double x = pos % xCount * textureReciprocalX;
        final double y = pos / yCount * textureReciprocalY;

        coord.put(blockSide, new Vector2(x, y));
    }

    /**
     * Get start coordinates for a specific side of a block.
     * 
     * @param blockId
     * @param blockSide
     * @return
     */
    public ReadOnlyVector2 getBlockTextureCoord(final int blockId, final BlockSide blockSide) {
        if (!blockTextureCoords.containsKey(blockId)) {
            logger.info("No mapping found for blockId: " + blockId);
            return Vector2.ZERO;
        }
        return blockTextureCoords.get(blockId).get(blockSide);
    }

    /**
     * Get offset to fix the artifact issue of texture bleeding.
     * 
     * @return
     */
    public float getOffsetWidth() {
        return offsetWidth;
    }

    /**
     * Get offset to fix the artifact issue of texture bleeding.
     * 
     * @return
     */
    public float getOffsetHeight() {
        return offsetHeight;
    }

    /**
     * Get the width of each tile in the texture atlas.
     * 
     * @return
     */
    public float getTileWidth() {
        return textureReciprocalX;
    }

    /**
     * Get the height of each tile in the texture atlas.
     * 
     * @return
     */
    public float getTileHeight() {
        return textureReciprocalY;
    }

    /**
     * Set the geometry producer to use for this block id.
     * 
     * @param blockId
     * @param handler
     */
    public void setGeometryProducer(final int blockId, final GeometryProducer handler) {
        geometryProducers[blockId] = handler;
    }

    public GeometryProducer getGeometryProducer(final int blockId) {
        return geometryProducers[blockId];
    }

    /**
     * Set if the block should be collidable.
     * 
     * @param blockId
     * @param value
     */
    public void setIsCollidable(final int blockId, final boolean value) {
        isCollidable[blockId] = value;
    }

    public boolean getIsCollidable(final int blockId) {
        return isCollidable[blockId];
    }

    /**
     * Set if the block should be pickable.
     * 
     * @param blockId
     * @param value
     */
    public void setIsPickable(final int blockId, final boolean value) {
        isPickable[blockId] = value;
    }

    public boolean getIsPickable(final int blockId) {
        return isPickable[blockId];
    }

    /**
     * Set if block is semi-transparent like water and should not generate sides when next to itself.
     * 
     * @param blockId
     * @param value
     */
    public void setIsSemiTransparent(final int blockId, final boolean value) {
        isSemiTransparent[blockId] = value;
    }

    public boolean getIsSemiTransparent(final int blockId) {
        return isSemiTransparent[blockId];
    }

    private Mesh createVegetationMesh() {
        final Node combineNode = new Node();

        final double sizeX = 1;
        final double sizeY = 1;

        Quad q = new Quad("quad", sizeX, sizeY);
        q.setRotation(new Quaternion().fromAngleAxis(MathUtils.HALF_PI * 0.5, Vector3.UNIT_Y));
        combineNode.attachChild(q);

        q = new Quad("quad", sizeX, sizeY);
        q.setRotation(new Quaternion().fromAngleAxis(-MathUtils.HALF_PI * 0.5, Vector3.UNIT_Y));
        combineNode.attachChild(q);

        q = new Quad("quad", sizeX, sizeY);
        q.setRotation(new Quaternion().fromAngleAxis(MathUtils.HALF_PI * 1.5, Vector3.UNIT_Y));
        combineNode.attachChild(q);

        q = new Quad("quad", sizeX, sizeY);
        q.setRotation(new Quaternion().fromAngleAxis(-MathUtils.HALF_PI * 1.5, Vector3.UNIT_Y));
        combineNode.attachChild(q);

        combineNode.updateWorldTransform(true);

        return MeshCombiner.combine(combineNode);
    }
}
