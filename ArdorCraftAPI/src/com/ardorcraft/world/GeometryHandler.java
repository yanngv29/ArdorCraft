/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.world;

import com.ardorcraft.util.geometryproducers.GeometryProducer;

/**
 * This is the class that {@link GeometryProducer}s use to set geometry into the world.
 */
public final class GeometryHandler {
    private float[] vertices;
    private float[] texcoords;
    private float[] colors;
    private int vertexCount;
    private int startVertexCount;
    private int[] indices;
    private int indexCount;
    private int startIndexCount;

    private final LightProvider lightProvider;

    GeometryHandler(final LightProvider lightProvider) {
        this.lightProvider = lightProvider;
    }

    void setStartIndexCount(final int index) {
        startIndexCount = index;
    }

    void setStartVertexCount(final int index) {
        startVertexCount = index;
    }

    /**
     * Sets the index count.
     * 
     * @param count
     *            the new index count
     */
    public void setIndexCount(final int count) {
        indexCount = count;
    }

    /**
     * Sets the vertex count.
     * 
     * @param count
     *            the new vertex count
     */
    public void setVertexCount(final int count) {
        vertexCount = count;
    }

    /**
     * Gets the index count.
     * 
     * @return the index count
     */
    public int getIndexCount() {
        return indexCount;
    }

    /**
     * Gets the vertex count.
     * 
     * @return the vertex count
     */
    public int getVertexCount() {
        return vertexCount;
    }

    /**
     * Checks for vertices.
     * 
     * @return true, if successful
     */
    public boolean hasVertices() {
        return vertices != null;
    }

    /**
     * Sets the vertex.
     * 
     * @param index
     *            the index
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     */
    public void setVertex(final int index, final float x, final float y, final float z) {
        vertices[startVertexCount * 3 + index * 3 + 0] = x;
        vertices[startVertexCount * 3 + index * 3 + 1] = y;
        vertices[startVertexCount * 3 + index * 3 + 2] = z;
    }

    /**
     * Checks for texture coords.
     * 
     * @return true, if successful
     */
    public boolean hasTextureCoords() {
        return texcoords != null;
    }

    /**
     * Sets the texture coord.
     * 
     * @param index
     *            the index
     * @param u
     *            the u
     * @param v
     *            the v
     */
    public void setTextureCoord(final int index, final float u, final float v) {
        texcoords[startVertexCount * 2 + index * 2 + 0] = u;
        texcoords[startVertexCount * 2 + index * 2 + 1] = v;
    }

    /**
     * Checks for colors.
     * 
     * @return true, if successful
     */
    public boolean hasColors() {
        return colors != null;
    }

    /**
     * Sets the color.
     * 
     * @param index
     *            the index
     * @param globalLight
     *            the global light
     * @param localLight
     *            the local light
     */
    public void setColor(final int index, final float globalLight, final float localLight) {
        colors[startVertexCount * 2 + index * 2 + 0] = globalLight;
        colors[startVertexCount * 2 + index * 2 + 1] = localLight;
    }

    /**
     * Checks for indices.
     * 
     * @return true, if successful
     */
    public boolean hasIndices() {
        return indices != null;
    }

    /**
     * Sets the index.
     * 
     * @param index
     *            the index
     * @param value
     *            the value
     */
    public void setIndex(final int index, final int value) {
        indices[startIndexCount + index] = startVertexCount + value;
    }

    /**
     * Gets the global lighting.
     * 
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     * @return the global lighting
     */
    public float getGlobalLighting(final int x, final int y, final int z) {
        return lightProvider.getGlobalLighting(x, y, z);
    }

    /**
     * Gets the local lighting.
     * 
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     * @return the local lighting
     */
    public float getLocalLighting(final int x, final int y, final int z) {
        return lightProvider.getLocalLighting(x, y, z);
    }

    void setVertices(final float[] vertices) {
        this.vertices = vertices;
    }

    void setTexcoords(final float[] texcoords) {
        this.texcoords = texcoords;
    }

    void setColors(final float[] colors) {
        this.colors = colors;
    }

    void setIndices(final int[] indices) {
        this.indices = indices;
    }
}
