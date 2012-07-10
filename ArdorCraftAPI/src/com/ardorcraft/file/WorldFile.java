/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.google.common.collect.Maps;

/**
 * ArdorCraft map file format
 * <p>
 * 
 * Layout of each chunk data in the file:
 * <ul>
 * <li>byte 0 = this key (long, 8)
 * <li>byte 8 = next key pos (long, 8)
 * <li>byte 16 = this pos (long, 8 (0 = +4, >0 = to start of next "this pos"))
 * <li>byte 24 = data size (int, 4)
 * <li>byte 28 = compressed data (byte[])
 * 
 */
public final class WorldFile {
    private static final Logger logger = Logger.getLogger(WorldFile.class.getName());

    private final RandomAccessFile worldFile;
    private final Map<Long, Long> mapping = Maps.newHashMap();
    private final Lock lock = new ReentrantLock();
    private final Deflater deflator = new Deflater();
    private final Inflater inflator = new Inflater();
    private final byte[] buf = new byte[1024 * 1024];
    private static final int IDENTIFIER = "ArdorCraft Map".hashCode();
    private static final int VERSION = 1;

    public WorldFile(final File file) throws Exception {
        worldFile = new RandomAccessFile(file, "rw");
        parse();
    }

    public void close() throws IOException {
        lock.lock();
        try {
            deflator.end();
            inflator.end();
            worldFile.close();
        } finally {
            lock.unlock();
        }
    }

    public long size() throws IOException {
        return worldFile.length();
    }

    public boolean contains(final int x, final int z) {
        lock.lock();
        try {
            return mapping.containsKey(getKey(x, z));
        } finally {
            lock.unlock();
        }
    }

    private void parse() throws IOException {
        lock.lock();
        try {
            final long endPos = worldFile.length();

            worldFile.seek(0);
            if (endPos == 0) {
                worldFile.writeInt(IDENTIFIER);
                worldFile.writeInt(VERSION);
                return;
            }
            if (endPos >= 4) {
                final boolean isMapFile = worldFile.readInt() == IDENTIFIER;
                if (!isMapFile) {
                    throw new RuntimeException("This is not an ArdorCraft map file!");
                }
            }
            if (endPos >= 8) {
                final int version = worldFile.readInt();
                if (version < VERSION) {
                    throw new RuntimeException("Map file was created with an older version of the ArdorCraft API ("
                            + version + " < " + VERSION + ")");
                }
                logger.info("Map file version: " + version);
            }

            long pos = 8;
            while (true) {
                if (pos >= endPos) {
                    logger.info("Map file contains " + mapping.keySet().size() + " chunks");
                    return;
                }
                worldFile.seek(pos);
                final long key = worldFile.readLong();
                final long nextKeyPos = worldFile.readLong();

                if (nextKeyPos < 0 || nextKeyPos > endPos) {
                    logger.warning("Corrupt mapping, nextKeyPos: " + nextKeyPos);
                    return;
                }

                if (mapping.containsKey(key)) {
                    pos = nextKeyPos;
                    continue;
                }

                long thisPos = worldFile.readLong();
                while (thisPos > 0) {
                    worldFile.seek(thisPos);
                    thisPos = worldFile.readLong();
                }

                mapping.put(key, worldFile.getFilePointer() - 24);
                pos = nextKeyPos;

                // final int x = WorldFile.getCoordinateX(key);
                // final int z = WorldFile.getCoordinateZ(key);
                // System.out.println("parsed: " + x + "," + z + " = " + (worldFile.getFilePointer() - 24));
            }
        } finally {
            lock.unlock();
        }
    }

    public void remap() throws IOException {
        lock.lock();
        try {
            // File file = File.createTempFile("foo", null);
            final File file = new File("tmpFile.tmp");
            if (file.exists()) {
                file.delete();
            }
            final RandomAccessFile tmpFile = new RandomAccessFile(file, "rw");

            tmpFile.writeInt(IDENTIFIER);
            tmpFile.writeInt(VERSION);
            for (final Entry<Long, Long> entry : mapping.entrySet()) {
                final long key = entry.getKey();
                final long pos = entry.getValue();

                worldFile.seek(pos + 24);
                final int size = worldFile.readInt();

                final long endPos = tmpFile.getFilePointer();

                tmpFile.writeLong(key);
                tmpFile.writeLong(endPos + 28 + size);
                tmpFile.writeLong(0);
                tmpFile.writeInt(size);

                long transfered = 0;
                while (transfered < size) {
                    transfered += worldFile.getChannel().transferTo(pos + 28 + transfered, size - transfered,
                            tmpFile.getChannel());
                }
                tmpFile.seek(endPos + 28 + size);
            }

            worldFile.seek(0);
            final long size = tmpFile.length();
            long transfered = 0;
            while (transfered < size) {
                transfered += tmpFile.getChannel().transferTo(transfered, size - transfered, worldFile.getChannel());
            }
            worldFile.setLength(size);

            tmpFile.close();
            file.delete();
        } finally {
            lock.unlock();
        }
    }

    public void save(final int x, final int z, final byte[] dataSource) throws IOException {
        lock.lock();
        try {
            final long newKey = getKey(x, z);
            final long endPosition = worldFile.length();

            final byte[] data = compress(dataSource);
            final int length = data.length;

            if (!mapping.containsKey(newKey)) {
                worldFile.seek(endPosition);
                worldFile.writeLong(newKey);
                worldFile.writeLong(endPosition + 28 + length);
                worldFile.writeLong(0);
                worldFile.writeInt(length);
                worldFile.write(data);

                mapping.put(newKey, endPosition);
            } else {
                final long pos = mapping.get(newKey);

                // read size
                worldFile.seek(pos + 24);
                final int size = worldFile.readInt();
                if (size < length) {
                    worldFile.seek(pos + 16);
                    worldFile.writeLong(endPosition + 16);

                    worldFile.seek(endPosition);
                    worldFile.writeLong(newKey);
                    worldFile.writeLong(endPosition + 28 + length);
                    worldFile.writeLong(0);
                    worldFile.writeInt(length);
                    worldFile.write(data);

                    mapping.put(newKey, endPosition);
                } else {
                    worldFile.seek(pos + 24);
                    worldFile.writeInt(length);
                    worldFile.write(data);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public byte[] load(final int x, final int z) throws IOException {
        final long newKey = getKey(x, z);
        lock.lock();
        try {
            if (!mapping.containsKey(newKey)) {
                logger.severe("No data found for coords: " + x + "," + z);
                return null;
            }
            long pos = mapping.get(newKey);

            pos += 24; // go to size
            worldFile.seek(pos);
            final int size = worldFile.readInt();
            final byte[] data = new byte[size];
            worldFile.read(data);

            return decompress(data);
        } finally {
            lock.unlock();
        }
    }

    public static long getKey(final int x, final int z) {
        long r = x;
        r <<= 32;
        r |= z & 0XFFFFFFFFL;
        return r;
    }

    public static int getCoordinateX(final long key) {
        return (int) (key >> 32 & 0XFFFFFFFF);
    }

    public static int getCoordinateZ(final long key) {
        return (int) (key & 0XFFFFFFFF);
    }

    private byte[] compress(final byte[] data) {
        deflator.reset();
        deflator.setInput(data);
        deflator.finish();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        while (!deflator.finished()) {
            final int count = deflator.deflate(buf);
            if (count > 0) {
                bos.write(buf, 0, count);
            }
        }
        return bos.toByteArray();
    }

    private byte[] decompress(final byte[] input) {
        inflator.reset();
        inflator.setInput(input);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
        try {
            while (true) {
                final int count = inflator.inflate(buf);
                if (count > 0) {
                    bos.write(buf, 0, count);
                } else if (count == 0 && inflator.finished()) {
                    break;
                } else {
                    throw new RuntimeException("bad zip data, size:" + input.length);
                }
            }
        } catch (final DataFormatException t) {
            throw new RuntimeException(t);
        }
        return bos.toByteArray();
    }

    Map<Long, Long> getMapping() {
        return mapping;
    }
}
