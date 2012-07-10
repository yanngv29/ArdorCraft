/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.util;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Utility class for tinting areas of an image (for example tint black/white grass patch for biomes)
 */
public class ColorUtil {
    public static BufferedImage tintAreas(final BufferedImage input, final List<Rectangle> areas,
            final int sourceTintX, final int sourceTintY) throws Exception {
        BufferedImage image = input;
        switch (input.getType()) {
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_RGB:
                break;
            case BufferedImage.TYPE_4BYTE_ABGR:
                image = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
                image.getGraphics().drawImage(input, 0, 0, null);
                break;
            default:
                throw new Exception("Input terrain texture, unhandled image data format: " + input.getType());
        }

        boolean alpha = false;
        final int col = image.getRGB(sourceTintX, sourceTintY);
        final int red = col >> 16 & 0xff;
        final int green = col >> 8 & 0xff;
        final int blue = col & 0xff;
        final float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        final float hue = hsb[0];
        final float saturation = hsb[1];// * 0.9f;

        switch (image.getType()) {
            case BufferedImage.TYPE_INT_ARGB:
                alpha = true;
                // Falls through on purpose.
            case BufferedImage.TYPE_INT_RGB:
                for (final Rectangle area : areas) {
                    tintArea(image, area, alpha, hue, saturation);
                }
                break;
            default:
                throw new Exception("Tinting texture, unhandled image data format: " + image.getType());
        }
        return image;
    }

    private static void tintArea(final BufferedImage input, final Rectangle area, final boolean alpha, final float hue,
            final float saturation) {
        for (int x = area.x; x < area.x + area.width; x++) {
            for (int y = area.y; y < area.y + area.height; y++) {
                final int col = input.getRGB(x, y);

                // Specify 3 RGB values
                final int red = col >> 16 & 0xff;
                final int green = col >> 8 & 0xff;
                final int blue = col & 0xff;

                final float[] hsb = Color.RGBtoHSB(red, green, blue, null);
                final float brightness = hsb[2];

                int argb = Color.HSBtoRGB(hue, saturation, brightness) & 0xffffff;

                // add alpha, if applicable
                if (alpha) {
                    argb |= col & 0xff000000;
                }

                // apply to image
                input.setRGB(x, y, argb);
            }
        }
    }
}