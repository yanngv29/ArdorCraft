/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.util;

import java.io.IOException;

import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.ui.text.BMFont;
import com.ardor3d.ui.text.BMText.Align;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.URLResourceSource;

public class FontHandler {
    public static BMFont MINI_FONT;
    public static int MINI_FONT_SIZE = 8;
    public static BMFont BIG_FONT;
    public static int BIG_FONT_SIZE = 15;
    public static BMFont SMALL_FONT;
    public static int SMALL_FONT_SIZE = 12;
    public static BMFont MEDIUM_FONT;
    public static int MEDIUM_FONT_SIZE = 16;

    static {
        try {
            MINI_FONT = new BMFont(new URLResourceSource(ResourceLocatorTool.getClassPathResource(FontHandler.class,
                    "com/ardorcraft/resources/mini.fnt")), true);
            MINI_FONT.getPageTexture().setMinificationFilter(MinificationFilter.NearestNeighborNoMipMaps);
            MINI_FONT.getPageTexture().setMagnificationFilter(MagnificationFilter.NearestNeighbor);
            BIG_FONT = new BMFont(new URLResourceSource(ResourceLocatorTool.getClassPathResource(FontHandler.class,
                    "com/ardorcraft/resources/big.fnt")), true);
            BIG_FONT.getPageTexture().setMinificationFilter(MinificationFilter.NearestNeighborNoMipMaps);
            BIG_FONT.getPageTexture().setMagnificationFilter(MagnificationFilter.NearestNeighbor);
            SMALL_FONT = new BMFont(new URLResourceSource(ResourceLocatorTool.getClassPathResource(FontHandler.class,
                    "com/ardor3d/extension/ui/font/arial-12-regular.fnt")), true);
            MEDIUM_FONT = new BMFont(new URLResourceSource(ResourceLocatorTool.getClassPathResource(FontHandler.class,
                    "com/ardor3d/extension/ui/font/arial-16-bold-regular.fnt")), true);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private static BlendState bs = new BlendState();
    static {
        bs.setBlendEnabled(true);
    }

    public static BasicText createText(final String text) {
        return createText(text, FontHandler.SMALL_FONT, FontHandler.SMALL_FONT_SIZE);
    }

    public static BasicText createText(final String text, final BMFont font, final int size) {
        final BasicText itemLabel = new BasicText("Test", text, font, size);
        itemLabel.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        itemLabel.setAlign(Align.East);
        itemLabel.setRenderState(bs);
        itemLabel.updateGeometricState(0, true);
        return itemLabel;
    }
}
