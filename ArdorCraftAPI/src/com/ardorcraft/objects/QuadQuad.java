/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardorcraft.objects;

import java.nio.ByteBuffer;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>QuadQuad</code> defines a four sided, two dimensional shape. The local height of the <code>QuadQuad</code>
 * defines it's size about the y-axis, while the width defines the x-axis. The z-axis will always be 0.
 */
public class QuadQuad extends Mesh {

    protected double _width = 0;
    protected double _height = 0;

    public QuadQuad() {

    }

    /**
     * Constructor creates a new <code>Quade</code> object with the provided width and height.
     * 
     * @param name
     *            the name of the <code>QuadQuad</code>.
     * @param width
     *            the width of the <code>QuadQuad</code>.
     * @param height
     *            the height of the <code>QuadQuad</code>.
     */
    public QuadQuad(final String name, final double width, final double height, final int mode) {
        super(name);
        initialize(width, height, mode);
    }

    /**
     * <code>resize</code> changes the width and height of the given quad by altering its vertices.
     * 
     * @param width
     *            the new width of the <code>QuadQuad</code>.
     * @param height
     *            the new height of the <code>QuadQuad</code>.
     */
    public void resize(final double width, final double height, final int mode) {
        _width = width;
        _height = height;

        _meshData.getVertexBuffer().clear();

        if (mode == 0) {
            _meshData.getVertexBuffer().put(0).put((float) (-width / 2)).put((float) (height / 2));
            _meshData.getVertexBuffer().put(0).put((float) (-width / 2)).put((float) (-height / 2));
            _meshData.getVertexBuffer().put(0).put((float) (width / 2)).put((float) (-height / 2));
            _meshData.getVertexBuffer().put(0).put((float) (width / 2)).put((float) (height / 2));
        } else if (mode == 1) {
            _meshData.getVertexBuffer().put((float) (-width / 2)).put(0).put((float) (height / 2));
            _meshData.getVertexBuffer().put((float) (-width / 2)).put(0).put((float) (-height / 2));
            _meshData.getVertexBuffer().put((float) (width / 2)).put(0).put((float) (-height / 2));
            _meshData.getVertexBuffer().put((float) (width / 2)).put(0).put((float) (height / 2));
        } else if (mode == 2) {
            _meshData.getVertexBuffer().put((float) (-width / 2)).put((float) (height / 2)).put(0);
            _meshData.getVertexBuffer().put((float) (-width / 2)).put((float) (-height / 2)).put(0);
            _meshData.getVertexBuffer().put((float) (width / 2)).put((float) (-height / 2)).put(0);
            _meshData.getVertexBuffer().put((float) (width / 2)).put((float) (height / 2)).put(0);
        }
    }

    /**
     * <code>initialize</code> builds the data for the <code>QuadQuad</code> object.
     * 
     * @param width
     *            the width of the <code>QuadQuad</code>.
     * @param height
     *            the height of the <code>QuadQuad</code>.
     */
    private void initialize(final double width, final double height, final int mode) {
        final int verts = 4;
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(verts));

        final byte[] indices = {
                0, 1, 2, 3
        };
        final ByteBuffer buf = BufferUtils.createByteBuffer(indices.length);
        buf.put(indices);
        buf.rewind();
        _meshData.setIndexBuffer(buf);

        _meshData.setIndexMode(IndexMode.Quads);

        resize(width, height, mode);

        setDefaultColor(ColorRGBA.BLACK);
    }

    public double getWidth() {
        return _width;
    }

    public double getHeight() {
        return _height;
    }
}