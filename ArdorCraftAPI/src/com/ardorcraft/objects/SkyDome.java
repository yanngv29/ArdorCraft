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

import java.io.IOException;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.CullState.Face;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * A half sphere.
 */
public class SkyDome extends Mesh {

    private int _planes;

    private int _radialSamples;

    /** The radius of the dome */
    private double _radius;

    private final ColorRGBA midColor = new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f);
    private final ColorRGBA topColor = new ColorRGBA(0.5f, 0.6f, 1.0f, 1.0f);
    private final ColorRGBA tmpColor = new ColorRGBA();

    public SkyDome() {}

    /**
     * Constructs a dome. By default the dome has not geometry data or center.
     * 
     * @param name
     *            The name of the dome.
     */
    public SkyDome(final String name) {
        super(name);
    }

    /**
     * Constructs a dome with center at the origin. For details, see the other constructor.
     * 
     * @param name
     *            Name of dome.
     * @param planes
     *            The number of planes along the Z-axis.
     * @param radialSamples
     *            The samples along the radial.
     * @param radius
     *            Radius of the dome.
     */
    public SkyDome(final String name, final int planes, final int radialSamples, final double radius) {
        this(name, new Vector3(0, 0, 0), planes, radialSamples, radius);
    }

    /**
     * Constructs a dome. All geometry data buffers are updated automatically. Both planes and radialSamples increase
     * the quality of the generated dome.
     * 
     * @param name
     *            Name of the dome.
     * @param center
     *            Center of the dome.
     * @param planes
     *            The number of planes along the Z-axis.
     * @param radialSamples
     *            The number of samples along the radial.
     * @param radius
     *            The radius of the dome.
     */
    public SkyDome(final String name, final Vector3 center, final int planes, final int radialSamples,
            final double radius) {

        super(name);
        setData(center, planes, radialSamples, radius, true, false);
    }

    /**
     * Constructs a dome. All geometry data buffers are updated automatically. Both planes and radialSamples increase
     * the quality of the generated dome.
     * 
     * @param name
     *            Name of the dome.
     * @param center
     *            Center of the dome.
     * @param planes
     *            The number of planes along the Z-axis.
     * @param radialSamples
     *            The number of samples along the radial.
     * @param radius
     *            The radius of the dome.
     * @param outsideView
     *            If true, the triangles will be connected for a view outside of the dome.
     */
    public SkyDome(final String name, final Vector3 center, final int planes, final int radialSamples,
            final double radius, final boolean outsideView) {
        super(name);
        setData(center, planes, radialSamples, radius, true, outsideView);
    }

    /**
     * Changes the information of the dome into the given values. The boolean at the end signals if buffer data should
     * be updated as well. If the dome is to be rendered, then that value should be true.
     * 
     * @param center
     *            The new center of the dome.
     * @param planes
     *            The number of planes along the Z-axis.
     * @param radialSamples
     *            The new number of radial samples of the dome.
     * @param radius
     *            The new radius of the dome.
     * @param updateBuffers
     *            If true, buffer information is updated as well.
     * @param outsideView
     *            If true, the triangles will be connected for a view outside of the dome.
     */
    public void setData(final Vector3 center, final int planes, final int radialSamples, final double radius,
            final boolean updateBuffers, final boolean outsideView) {
        _planes = planes;
        _radialSamples = radialSamples;
        _radius = radius;

        getSceneHints().setLightCombineMode(LightCombineMode.Off);
        getSceneHints().setCullHint(CullHint.Never);
        getSceneHints().setRenderBucketType(RenderBucketType.Skip);

        final ZBufferState zbuff = new ZBufferState();
        zbuff.setEnabled(false);
        setRenderState(zbuff);

        final FogState fs = new FogState();
        fs.setEnabled(false);
        setRenderState(fs);

        final CullState cs = new CullState();
        cs.setCullFace(Face.Front);
        setRenderState(cs);

        if (updateBuffers) {
            setGeometryData(outsideView, center);
            setIndexData();
        }
    }

    /**
     * Generates the vertices of the dome
     * 
     * @param outsideView
     *            If the dome should be viewed from the outside (if not zbuffer is used)
     * @param center
     */
    private void setGeometryData(final boolean outsideView, final Vector3 center) {
        final Vector3 tempVa = Vector3.fetchTempInstance();
        final Vector3 tempVb = Vector3.fetchTempInstance();
        final Vector3 tempVc = Vector3.fetchTempInstance();

        // allocate vertices, we need one extra in each radial to get the
        // correct texture coordinates
        final int verts = (_planes - 1) * (_radialSamples + 1) + 1;
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(verts));

        // allocate normals
        _meshData.setColorBuffer(BufferUtils.createColorBuffer(verts));

        // generate geometry
        final double fInvRS = 1.0 / _radialSamples;
        final double fYFactor = 1.0 / (_planes - 1);

        // Generate points on the unit circle to be used in computing the mesh
        // points on a dome slice.
        final double[] afSin = new double[_radialSamples];
        final double[] afCos = new double[_radialSamples];
        for (int iR = 0; iR < _radialSamples; iR++) {
            final double fAngle = MathUtils.TWO_PI * fInvRS * iR;
            afCos[iR] = MathUtils.cos(fAngle);
            afSin[iR] = MathUtils.sin(fAngle);
        }

        // generate the dome itself
        int i = 0;
        for (int iY = 0; iY < _planes - 1; iY++) {
            final double fYFraction = fYFactor * iY; // in (0,1)
            final double fY = _radius * fYFraction;
            // compute center of slice
            final Vector3 kSliceCenter = tempVb.set(center);
            kSliceCenter.addLocal(0, fY, 0);

            // compute radius of slice
            final double fSliceRadius = Math.sqrt(Math.abs(_radius * _radius - fY * fY));

            // compute slice vertices
            final int iSave = i;
            for (int iR = 0; iR < _radialSamples; iR++) {
                final Vector3 kRadial = tempVc.set(afCos[iR], 0, afSin[iR]);
                kRadial.multiply(fSliceRadius, tempVa);
                _meshData.getVertexBuffer().put((float) (kSliceCenter.getX() + tempVa.getX()))
                        .put((float) (kSliceCenter.getY() + tempVa.getY()))
                        .put((float) (kSliceCenter.getZ() + tempVa.getZ()));

                BufferUtils.populateFromBuffer(tempVa, _meshData.getVertexBuffer(), i);

                ColorRGBA.lerp(midColor, topColor, (float) Math.sqrt(fYFraction), tmpColor);
                _meshData.getColorBuffer().put(tmpColor.getRed()).put(tmpColor.getGreen()).put(tmpColor.getBlue())
                        .put(1);

                i++;
            }

            BufferUtils.copyInternalVector3(_meshData.getVertexBuffer(), iSave, i);
            BufferUtils.copyInternalColor(_meshData.getColorBuffer(), iSave, i);

            i++;
        }

        // pole
        _meshData.getVertexBuffer().put((float) center.getX()).put((float) (center.getY() + _radius))
                .put((float) center.getZ());

        _meshData.getColorBuffer().put(topColor.getRed()).put(topColor.getGreen()).put(topColor.getBlue()).put(1);

        Vector3.releaseTempInstance(tempVa);
        Vector3.releaseTempInstance(tempVb);
        Vector3.releaseTempInstance(tempVc);
    }

    public void updateColors() {
        final Vector3 tempVa = Vector3.fetchTempInstance();
        final Vector3 tempVb = Vector3.fetchTempInstance();
        final Vector3 tempVc = Vector3.fetchTempInstance();

        final double fYFactor = 1.0 / (_planes - 1);

        _meshData.getColorBuffer().clear();

        // generate the dome itself
        int i = 0;
        for (int iY = 0; iY < _planes - 1; iY++) {
            final double fYFraction = fYFactor * iY; // in (0,1)

            // compute slice vertices
            final int iSave = i;
            for (int iR = 0; iR < _radialSamples; iR++) {
                ColorRGBA.lerp(midColor, topColor, (float) Math.sqrt(fYFraction), tmpColor);
                _meshData.getColorBuffer().put(tmpColor.getRed()).put(tmpColor.getGreen()).put(tmpColor.getBlue())
                        .put(1);
                i++;
            }

            BufferUtils.copyInternalVector3(_meshData.getVertexBuffer(), iSave, i);
            BufferUtils.copyInternalColor(_meshData.getColorBuffer(), iSave, i);

            i++;
        }

        _meshData.getColorBuffer().put(topColor.getRed()).put(topColor.getGreen()).put(topColor.getBlue()).put(1);

        Vector3.releaseTempInstance(tempVa);
        Vector3.releaseTempInstance(tempVb);
        Vector3.releaseTempInstance(tempVc);
    }

    /**
     * Generates the connections
     */
    private void setIndexData() {
        // allocate connectivity
        final int verts = (_planes - 1) * (_radialSamples + 1) + 1;
        final int tris = (_planes - 2) * _radialSamples * 2 + _radialSamples;
        _meshData.setIndices(BufferUtils.createIndexBufferData(3 * tris, verts - 1));

        // Generate only for middle planes
        for (int plane = 1; plane < _planes - 1; plane++) {
            final int bottomPlaneStart = (plane - 1) * (_radialSamples + 1);
            final int topPlaneStart = plane * (_radialSamples + 1);
            for (int sample = 0; sample < _radialSamples; sample++) {
                _meshData.getIndices().put(bottomPlaneStart + sample);
                _meshData.getIndices().put(topPlaneStart + sample);
                _meshData.getIndices().put(bottomPlaneStart + sample + 1);
                _meshData.getIndices().put(bottomPlaneStart + sample + 1);
                _meshData.getIndices().put(topPlaneStart + sample);
                _meshData.getIndices().put(topPlaneStart + sample + 1);
            }
        }

        // pole triangles
        final int bottomPlaneStart = (_planes - 2) * (_radialSamples + 1);
        for (int samples = 0; samples < _radialSamples; samples++) {
            _meshData.getIndices().put(bottomPlaneStart + samples);
            _meshData.getIndices().put(_meshData.getVertexCount() - 1);
            _meshData.getIndices().put(bottomPlaneStart + samples + 1);
        }
    }

    public int getPlanes() {
        return _planes;
    }

    public int getRadialSamples() {
        return _radialSamples;
    }

    public double getRadius() {
        return _radius;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_planes, "planes", 0);
        capsule.write(_radialSamples, "radialSamples", 0);
        capsule.write(_radius, "radius", 0);

    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _planes = capsule.readInt("planes", 0);
        _radialSamples = capsule.readInt("radialSamples", 0);
        _radius = capsule.readDouble("radius", 0);

    }

    public ColorRGBA getMidColor() {
        return midColor;
    }

    public ColorRGBA getTopColor() {
        return topColor;
    }
}