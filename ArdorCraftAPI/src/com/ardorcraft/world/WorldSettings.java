/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.world;

import com.ardor3d.math.MathUtils;
import com.ardor3d.util.resource.ResourceSource;

/**
 * Settings class for setting up the BlockWorld object.
 * <p>
 * terrainTexture, terrainTextureTileSize, waterTexture and serverConnection has to be setup.
 */
public class WorldSettings {
    private int tileSize = 16;
    private int tileHeight = 128;
    private int gridSize = 16;

    private ResourceSource terrainTexture;
    private int terrainTextureTileSize;
    private ResourceSource waterTexture;

    private boolean useVBO = true;
    private boolean doDefaultTint = true;

    private IServerConnection serverConnection;

    public WorldSettings() {

    }

    /**
     * Copy constructor for WorldSettings.
     * 
     * @param settings
     *            WorldSettings to copy
     */
    public WorldSettings(final WorldSettings settings) {
        tileSize = settings.tileSize;
        tileHeight = settings.tileHeight;
        gridSize = settings.gridSize;

        terrainTexture = settings.terrainTexture;
        terrainTextureTileSize = settings.terrainTextureTileSize;
        waterTexture = settings.waterTexture;

        useVBO = settings.useVBO;
        doDefaultTint = settings.doDefaultTint;

        serverConnection = settings.serverConnection;
    }

    /**
     * Get chunk size in blocks.
     * 
     * @return int
     */
    public int getTileSize() {
        return tileSize;
    }

    /**
     * Set size of chunks in blocks.
     * 
     * @param tileSize
     *            int
     */
    public void setTileSize(final int tileSize) {
        this.tileSize = MathUtils.nearestPowerOfTwo(tileSize);
    }

    /**
     * Get height of chunks in blocks.
     * 
     * @return int
     */
    public int getTileHeight() {
        return tileHeight;
    }

    /**
     * Set height of chunks in blocks.
     * 
     * @param tileHeight
     *            int
     */
    public void setTileHeight(final int tileHeight) {
        this.tileHeight = tileHeight;
    }

    /**
     * Get length of world side, in chunks
     * 
     * @return int
     */
    public int getGridSize() {
        return gridSize;
    }

    /**
     * Set length of world side, in chunks
     * 
     * @param gridSize
     *            int
     */
    public void setGridSize(final int gridSize) {
        this.gridSize = gridSize + gridSize % 2;
    }

    /**
     * Get texture atlas used for terrain.
     * 
     * @return ResourceSource
     */
    public ResourceSource getTerrainTexture() {
        return terrainTexture;
    }

    /**
     * Set texture atlas used for terrain.
     * 
     * @param terrainTexture
     *            ResourceSource
     */
    public void setTerrainTexture(final ResourceSource terrainTexture) {
        this.terrainTexture = terrainTexture;
    }

    /**
     * Get size in pixels of each sub-texture tile in the texture atlas.
     * 
     * @return int
     */
    public int getTerrainTextureTileSize() {
        return terrainTextureTileSize;
    }

    /**
     * Set size in pixels of each sub-texture tile in the texture atlas. (typically 16x16, 32x32 etc)
     * 
     * @param terrainTextureTileSize
     *            int
     */
    public void setTerrainTextureTileSize(final int terrainTextureTileSize) {
        this.terrainTextureTileSize = terrainTextureTileSize;
    }

    /**
     * Get texture used for water.
     * 
     * @return ResourceSource
     */
    public ResourceSource getWaterTexture() {
        return waterTexture;
    }

    /**
     * Set texture used for water. (current not used)
     * 
     * @param waterTexture
     *            ResourceSource
     */
    public void setWaterTexture(final ResourceSource waterTexture) {
        this.waterTexture = waterTexture;
    }

    /**
     * Get server connection.
     * 
     * @return IServerConnection
     */
    public IServerConnection getServerConnection() {
        return serverConnection;
    }

    /**
     * Set server connection.
     * 
     * @param serverConnection
     *            IServerConnection
     */
    public void setServerConnection(final IServerConnection serverConnection) {
        this.serverConnection = serverConnection;
    }

    /**
     * Get if the world should use VBO (vertex buffer objects) for rendering.
     * 
     * @return boolean
     */
    public boolean isUseVBO() {
        return useVBO;
    }

    /**
     * Set if the world should use VBO (vertex buffer objects) for rendering. Default is true.
     * 
     * @param useVBO
     *            boolean
     */
    public void setUseVBO(final boolean useVBO) {
        this.useVBO = useVBO;
    }

    /**
     * Get if default minecraft texture tinting should be done. This will be turned into something completely
     * configurable later on.
     * 
     * @return boolean
     */
    public boolean isDoDefaultTint() {
        return doDefaultTint;
    }

    /**
     * Set if default minecraft texture tinting should be done. This will be turned into something completely
     * configurable later on.
     * 
     * @param doDefaultTint
     */
    public void setDoDefaultTint(final boolean doDefaultTint) {
        this.doDefaultTint = doDefaultTint;
    }
}
