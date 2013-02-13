/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardorcraft.util;

import com.ardor3d.math.MathUtils;

/**
 * <code>ImprovedNoise</code> Fast perlin noise.
 * 
 * @author Ken Perlin
 */
public final class ImprovedNoise {
    public static double noise(double x, double y, double z) {
        final int X = (int) MathUtils.floor(x) & 255, // FIND UNIT CUBE THAT
        Y = (int) MathUtils.floor(y) & 255, // CONTAINS POINT.
        Z = (int) MathUtils.floor(z) & 255;
        x -= MathUtils.floor(x); // FIND RELATIVE X,Y,Z
        y -= MathUtils.floor(y); // OF POINT IN CUBE.
        z -= MathUtils.floor(z);
        final double u = fade(x), // COMPUTE FADE CURVES
        v = fade(y), // FOR EACH OF X,Y,Z.
        w = fade(z);
        final int A = p[X] + Y, AA = p[A] + Z, AB = p[A + 1] + Z, // HASH COORDINATES OF
        B = p[X + 1] + Y, BA = p[B] + Z, BB = p[B + 1] + Z; // THE 8 CUBE CORNERS,

        return lerp(w, lerp(v, lerp(u, grad(p[AA], x, y, z), // AND ADD
                grad(p[BA], x - 1, y, z)), // BLENDED
                lerp(u, grad(p[AB], x, y - 1, z), // RESULTS
                        grad(p[BB], x - 1, y - 1, z))),// FROM 8
                lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1), // CORNERS
                        grad(p[BA + 1], x - 1, y, z - 1)), // OF CUBE
                        lerp(u, grad(p[AB + 1], x, y - 1, z - 1), grad(p[BB + 1], x - 1, y - 1, z - 1))));
    }

    private static double fade(final double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(final double t, final double a, final double b) {
        return a + t * (b - a);
    }

    private static double grad(final int hash, final double x, final double y, final double z) {
        final int h = hash & 15; // CONVERT LO 4 BITS OF HASH CODE
        final double u = h < 8 ? x : y, // INTO 12 GRADIENT DIRECTIONS.
        v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private static final int p[] = new int[512], permutation[] = {
            151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99,
            37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32,
            57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27,
            166, 77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102,
            143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116,
            188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126,
            255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248, 152,
            2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224,
            232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81,
            51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45,
            127, 4, 150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156,
            180
    };

    static {
        for (int i = 0; i < 256; i++) {
            p[256 + i] = p[i] = permutation[i];
        }
    }

    public static double noise2d(final double x, final double y, final int nbOctave) {
        int result = 0;
        final int frequence256 = 256;
        int sx = (int) (x * frequence256);
        int sy = (int) (y * frequence256);
        int octave = nbOctave;
        while (octave != 0) {
            final int bX = sx & 0xFF;
            final int bY = sy & 0xFF;

            final int sxp = sx >> 8;
            final int syp = sy >> 8;

            // Compute noise for each corner of current cell
            final int Y1376312589_00 = syp * 1376312589;
            final int Y1376312589_01 = Y1376312589_00 + 1376312589;

            final int XY1376312589_00 = sxp + Y1376312589_00;
            final int XY1376312589_10 = XY1376312589_00 + 1;
            final int XY1376312589_01 = sxp + Y1376312589_01;
            final int XY1376312589_11 = XY1376312589_01 + 1;

            final int XYBASE_00 = XY1376312589_00 << 13 ^ XY1376312589_00;
            final int XYBASE_10 = XY1376312589_10 << 13 ^ XY1376312589_10;
            final int XYBASE_01 = XY1376312589_01 << 13 ^ XY1376312589_01;
            final int XYBASE_11 = XY1376312589_11 << 13 ^ XY1376312589_11;

            int alt1 = XYBASE_00 * (XYBASE_00 * XYBASE_00 * 15731 + 789221) + 1376312589;
            int alt2 = XYBASE_10 * (XYBASE_10 * XYBASE_10 * 15731 + 789221) + 1376312589;
            int alt3 = XYBASE_01 * (XYBASE_01 * XYBASE_01 * 15731 + 789221) + 1376312589;
            int alt4 = XYBASE_11 * (XYBASE_11 * XYBASE_11 * 15731 + 789221) + 1376312589;

            /*
             * NOTE : on for true grandiant noise uncomment following block for true gradiant we need to perform scalar
             * product here, gradiant vector are created/deducted using the above pseudo random values (alt1...alt4) :
             * by cutting thoses values in twice values to get for each a fixed x,y vector gradX1= alt1&0xFF gradY1=
             * (alt1&0xFF00)>>8
             * 
             * the last part of the PRN (alt1&0xFF0000)>>8 is used as an offset to correct one of the gradiant problem
             * wich is zero on cell edge
             * 
             * source vector (sXN;sYN) for scalar product are computed using (bX,bY)
             * 
             * each four values must be replaced by the result of the following altN=(gradXN;gradYN) scalar (sXN;sYN)
             * 
             * all the rest of the code (interpolation+accumulation) is identical for value & gradiant noise
             */

            /* START BLOCK FOR TRUE GRADIANT NOISE */

            final int grad1X = (alt1 & 0xFF) - 128;
            final int grad1Y = (alt1 >> 8 & 0xFF) - 128;
            final int grad2X = (alt2 & 0xFF) - 128;
            final int grad2Y = (alt2 >> 8 & 0xFF) - 128;
            final int grad3X = (alt3 & 0xFF) - 128;
            final int grad3Y = (alt3 >> 8 & 0xFF) - 128;
            final int grad4X = (alt4 & 0xFF) - 128;
            final int grad4Y = (alt4 >> 8 & 0xFF) - 128;

            final int sX1 = bX >> 1;
            final int sY1 = bY >> 1;
            final int sX2 = 128 - sX1;
            final int sY2 = sY1;
            final int sX3 = sX1;
            final int sY3 = 128 - sY1;
            final int sX4 = 128 - sX1;
            final int sY4 = 128 - sY1;
            alt1 = grad1X * sX1 + grad1Y * sY1 + 16384 + ((alt1 & 0xFF0000) >> 9); // to avoid seams to be 0 we use an
                                                                                   // offset
            alt2 = grad2X * sX2 + grad2Y * sY2 + 16384 + ((alt2 & 0xFF0000) >> 9);
            alt3 = grad3X * sX3 + grad3Y * sY3 + 16384 + ((alt3 & 0xFF0000) >> 9);
            alt4 = grad4X * sX4 + grad4Y * sY4 + 16384 + ((alt4 & 0xFF0000) >> 9);

            /* END BLOCK FOR TRUE GRADIANT NOISE */

            /* START BLOCK FOR VALUE NOISE */
            /*
             * alt1&=0xFFFF; alt2&=0xFFFF; alt3&=0xFFFF; alt4&=0xFFFF;
             */
            /* END BLOCK FOR VALUE NOISE */

            /* START BLOCK FOR LINEAR INTERPOLATION */
            // BiLinear interpolation
            /*
             * int f24=(bX*bY)>>8; int f23=bX-f24; int f14=bY-f24; int f13=256-f14-f23-f24;
             * 
             * int val=(alt1*f13+alt2*f23+alt3*f14+alt4*f24);
             */
            /* END BLOCK FOR LINEAR INTERPOLATION */

            // BiCubic interpolation ( in the form alt(bX) = alt[n] - (3*bX^2 - 2*bX^3) * (alt[n] - alt[n+1]) )
            /* START BLOCK FOR BICUBIC INTERPOLATION */
            final int bX2 = bX * bX >> 8;
            final int bX3 = bX2 * bX >> 8;
            final int _3bX2 = 3 * bX2;
            final int _2bX3 = 2 * bX3;
            final int alt12 = alt1 - ((_3bX2 - _2bX3) * (alt1 - alt2) >> 8);
            final int alt34 = alt3 - ((_3bX2 - _2bX3) * (alt3 - alt4) >> 8);

            final int bY2 = bY * bY >> 8;
            final int bY3 = bY2 * bY >> 8;
            final int _3bY2 = 3 * bY2;
            final int _2bY3 = 2 * bY3;
            int val = alt12 - ((_3bY2 - _2bY3) * (alt12 - alt34) >> 8);

            val *= 256;
            /* END BLOCK FOR BICUBIC INTERPOLATION */

            // Accumulate in result
            result += val << octave;

            octave--;
            sx <<= 1;
            sy <<= 1;

        }
        return (result >>> 16 + nbOctave + 1) / 255.0 - 0.5;
    }
}