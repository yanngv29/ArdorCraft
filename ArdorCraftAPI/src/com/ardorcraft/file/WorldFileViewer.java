/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.file;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.util.Map;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.ardor3d.math.ColorRGBA;

public class WorldFileViewer {
    private final Graphics2D g2;
    private final ColorRGBA[] colors;
    private final ColorRGBA[] heightCol;
    private final Color[] heightCol2;

    private final int subMeshSize;
    private final int height;
    private final int subsize = 1;
    private final int size = 16 * subsize;

    private final WorldFile worldFile;

    public WorldFileViewer(final File file, final int subMeshSize, final int height) throws Exception {
        worldFile = new WorldFile(file);
        this.subMeshSize = subMeshSize;
        this.height = height;

        final JPanel panel = new JPanel();
        final JFrame frame = new JFrame("WorldFile Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel);
        frame.setBounds(10, 10, 1000, 1000);
        frame.setVisible(true);
        g2 = (Graphics2D) panel.getGraphics();

        final Random rand = new Random(1337);
        colors = new ColorRGBA[255];
        for (int i = 0; i < 255; i++) {
            colors[i] = new ColorRGBA(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 1);
        }

        heightCol = new ColorRGBA[100];
        heightCol2 = new Color[100];
        for (int i = 0; i < 100; i++) {
            final float v = i / 100.0f * 0.9f + 0.1f;
            heightCol[i] = new ColorRGBA(v, v, v, 1);
            heightCol2[i] = new Color(v, v, v);
        }
    }

    public void view() throws Exception {
        final Map<Long, Long> mapping = worldFile.getMapping();
        for (final Long key : mapping.keySet()) {
            final int x = WorldFile.getCoordinateX(key);
            final int z = WorldFile.getCoordinateZ(key);
            final byte[] dataSource = worldFile.load(x, z);
            paintChunk(x, z, dataSource);
        }
    }

    private void paintChunk(final int x, final int z, final byte[] dataSource) {
        final int xPos = x * size + 400;
        final int yPos = z * size + 400;

        for (int xx = 0; xx < subMeshSize; xx++) {
            for (int zz = 0; zz < subMeshSize; zz++) {
                for (int yy = height - 1; yy >= 0; yy--) {
                    final int b = dataSource[xx + (yy + zz * height) * subMeshSize] & 0xff;

                    if (b != 0) {
                        // g2.setColor(colors[b]);
                        g2.setColor(heightCol2[yy]);

                        // final ColorRGBA c1 = colors[b];
                        // final ColorRGBA c2 = heightCol[yy];
                        // g2.setColor(new Color(c1.getRed() * c2.getRed(), c1.getGreen() * c2.getGreen(), c1.getBlue()
                        // * c2.getBlue()));

                        g2.fillRect(xPos + xx * subsize, yPos + zz * subsize, subsize, subsize);
                        break;
                    }
                }
            }
        }
    }
}
