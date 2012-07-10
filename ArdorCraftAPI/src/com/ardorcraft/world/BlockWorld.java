/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.world;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.ApplyMode;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.AbstractBufferData.VBOAccessMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.DataMode;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;
import com.ardorcraft.collision.HitTester;
import com.ardorcraft.collision.IntersectionResult;
import com.ardorcraft.collision.Tracer;
import com.ardorcraft.data.Pos;
import com.ardorcraft.util.BlockUtil;
import com.ardorcraft.util.ColorUtil;
import com.ardorcraft.util.DoubleBufferedList;
import com.ardorcraft.util.queue.Coords;
import com.ardorcraft.util.queue.Task;
import com.ardorcraft.util.queue.WorkerManager;
import com.ardorcraft.world.utils.ChunkDistanceComparator;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * BlockWorld is the main class when it comes to building and rendering the block world. It handles asynchronous
 * updating and rebuilding of the chunks after receiving chunk or block updates. It also handles light calculations.
 * <p>
 * In the simplest setup, the blockworld is just initialized with a {@link WorldSettings} object. Then the main node for
 * rendering the world is extracted and added to your scenegraph. Finally when you have everything else setup, all the
 * threads are kicked off to start processing.
 * <p>
 * <code>
 * blockWorld = new BlockWorld(settings);<br>
 * root.attachChild(blockWorld.getWorldNode());<br>
 * ...<br>
 * blockWorld.startThreads();
 * </code>
 */
public final class BlockWorld implements BlockProvider, LightProvider, ChunkModifier {
    private static final Logger logger = Logger.getLogger(BlockWorld.class.getName());

    public static int WATER = 223;

    private static final int CHUNK = 0;
    private static final int BLOCKS = 1;
    private static final int REBUILD = 2;
    private static final int LIGHT = 3;

    private final int width;
    private final int height;
    private final int subMeshSize;

    private static final byte MAX_LIGHT = 15;
    private static final byte LIGHT_STEP = 2;
    private float globalLight = 1f;
    private boolean lightingChanged = false;
    private boolean updateLighting = true;
    private boolean firstRender = true;

    private final ColorRGBA caveLight = new ColorRGBA(0.08f, 0.07f, 0.07f, 1.0f);
    private final ColorRGBA nightLight = new ColorRGBA(0.1f, 0.16f, 0.2f, 1.0f);
    private final ColorRGBA dayLight = new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f);
    private final ColorRGBA torchLight = new ColorRGBA(1.0f, 0.8f, 0.5f, 1.0f);

    private ByteBuffer lightingBuffer;

    private final byte blocks[];
    // Extra data
    // 0,1,2 = orientation
    // 3,4,5,6 = age (or something else)
    // 7 = whatever
    private final byte blockExtra[];

    private BlockUtil blockUtil;

    private final short lightHeightmap[];
    private final byte lightingWrite[];
    private final byte localLightDataWrite[];

    private final BitSet isLightSolid;

    private final Set<Pos> currentPos = new LinkedHashSet<Pos>();
    private int oldX = Integer.MAX_VALUE;
    private int oldZ = Integer.MAX_VALUE;
    private int playerPositionX = Integer.MIN_VALUE;
    private int playerPositionZ = Integer.MIN_VALUE;

    private Texture lightTexture;
    private TextureState terrainTextureState;
    private BlendState terrainBlendState;
    // private TextureState waterTextureState;
    // private Texture waterTexture;

    private BlendState transparentState;
    private CullState cullState;
    private CullState windowCullState;

    private Node worldNode;
    private Node solidNode;
    private Node transparentNode;

    private final IServerConnection serverConnection;

    private final Map<Pos, Mesh> meshCache = Maps.newHashMap();
    private final Map<Pos, Mesh> meshCacheTransparent = Maps.newHashMap();
    private final Set<Pos> isLoaded = Collections.synchronizedSet(new HashSet<Pos>());

    private final DoubleBufferedList<Pos> lightUpdateBox = new DoubleBufferedList<Pos>();
    private final DoubleBufferedList<UpdateMessage> chunkMailBox = new DoubleBufferedList<UpdateMessage>();

    private final int gridSize;
    private int currentTileX;
    private int currentTileZ;
    private final int gridUnitSize;

    private final CountDownLatch exitLatch = new CountDownLatch(2);
    private boolean threadsStarted = false;
    private boolean exit = false;

    private final WorldSettings settings;
    private boolean hasVBOSupport = false;
    private boolean doDefaultTint = true;

    private final GeometryHandler geometryHandler;
    private final float[] tmpVertices;
    private final float[] tmpTexcoords;
    private final float[] tmpColors;
    private final int[] tmpIndices;

    private final GeometryHandler geometryHandlerTransparent;
    private final float[] tmpVerticesTransparent;
    private final float[] tmpTexcoordsTransparent;
    private final float[] tmpColorsTransparent;
    private final int[] tmpIndicesTransparent;

    /**
     * Create a new BlockWorld instance based on the provided settings.
     * 
     * @param settings
     */
    public BlockWorld(final WorldSettings settings) {
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(settings.getServerConnection());

        this.settings = new WorldSettings(settings);
        width = settings.getGridSize() * settings.getTileSize();
        height = settings.getTileHeight();
        subMeshSize = settings.getTileSize();

        doDefaultTint = settings.isDoDefaultTint();

        hasVBOSupport = settings.isUseVBO() && ContextManager.getCurrentContext().getCapabilities().isVBOSupported();
        if (hasVBOSupport) {
            logger.info("Using VBO");
        }

        WorkerManager.getWorker().setExecutionTime(8);

        gridUnitSize = settings.getGridSize();
        gridSize = width / 2;

        geometryHandler = new GeometryHandler(BlockWorld.this);
        tmpVertices = new float[subMeshSize * height * subMeshSize * 24 * 3 / 2];
        tmpTexcoords = new float[subMeshSize * height * subMeshSize * 24 * 2 / 2];
        tmpColors = new float[subMeshSize * height * subMeshSize * 24 * 2 / 2];
        tmpIndices = new int[subMeshSize * height * subMeshSize * 36 * 1 / 2];

        geometryHandlerTransparent = new GeometryHandler(BlockWorld.this);
        tmpVerticesTransparent = new float[subMeshSize * height * subMeshSize * 24 * 3 / 2];
        tmpTexcoordsTransparent = new float[subMeshSize * height * subMeshSize * 24 * 2 / 2];
        tmpColorsTransparent = new float[subMeshSize * height * subMeshSize * 24 * 2 / 2];
        tmpIndicesTransparent = new int[subMeshSize * height * subMeshSize * 36 * 1 / 2];

        blocks = new byte[width * height * width];
        blockExtra = new byte[width * height * width];

        lightingWrite = new byte[width * height * width];
        lightHeightmap = new short[width * width];
        localLightDataWrite = new byte[width * height * width];

        Arrays.fill(lightingWrite, (byte) 255);

        isLightSolid = new BitSet();

        serverConnection = settings.getServerConnection();
        serverConnection.getModifier(this);

        createWorld(settings);
    }

    private void createWorld(final WorldSettings settings) {
        terrainTextureState = new TextureState();

        final int blockSize = settings.getTerrainTextureTileSize();
        try {
            BufferedImage img = ImageIO.read(settings.getTerrainTexture().openStream());

            if (doDefaultTint) {
                final List<Rectangle> areas = Lists.newArrayList();
                areas.add(new Rectangle(0, 0, blockSize, blockSize));
                areas.add(new Rectangle(4 * blockSize, 3 * blockSize, blockSize, blockSize));
                try {
                    img = ColorUtil.tintAreas(img, areas, blockSize * 4 - 1, 0);
                } catch (final Exception e) {
                    logger.log(Level.WARNING, "Tinting failed!", e);
                }
            }
            final Image image = AWTImageLoader.makeArdor3dImage(img, true);

            final Texture texture1 = TextureManager.loadFromImage(image,
                    Texture.MinificationFilter.NearestNeighborLinearMipMap, TextureStoreFormat.GuessNoCompressedFormat);
            texture1.setMagnificationFilter(MagnificationFilter.NearestNeighbor);
            terrainTextureState.setTexture(texture1, 0);

            final int tw = texture1.getImage().getWidth();
            final int th = texture1.getImage().getHeight();
            logger.info("Texture size: " + tw + "x" + th);

            final int numLevels = (int) Math.floor(Math.log(blockSize) / Math.log(2));
            texture1.setTextureMaxLevel(numLevels);

            blockUtil = new BlockUtil(tw, th, blockSize);
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }

        final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        final ColorRGBA outLight = ColorRGBA.lerp(nightLight, dayLight, globalLight, new ColorRGBA());
        final ColorRGBA tmp1 = new ColorRGBA();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                final float global = x / 15f;
                final float local = y / 15f;

                ColorRGBA.lerp(caveLight, outLight, global, tmp1);
                tmp1.lerpLocal(torchLight, local);

                image.setRGB(x, y, tmp1.asIntARGB());
            }
        }
        final Image lightImage = AWTImageLoader.makeArdor3dImage(image, false);
        lightTexture = TextureManager.loadFromImage(lightImage, Texture.MinificationFilter.BilinearNoMipMaps);
        lightTexture.setApply(ApplyMode.Modulate);
        lightTexture.setWrap(WrapMode.EdgeClamp);
        terrainTextureState.setTexture(lightTexture, 1);
        lightingBuffer = BufferUtils.createByteBuffer(16 * 16 * 4);

        // waterTextureState = new TextureState();
        // waterTexture = TextureManager.load(settings.getWaterTexture(),
        // Texture.MinificationFilter.NearestNeighborLinearMipMap, TextureStoreFormat.GuessNoCompressedFormat,
        // true);
        // waterTexture.setMagnificationFilter(MagnificationFilter.NearestNeighbor);
        // waterTextureState.setTexture(waterTexture, 0);

        terrainBlendState = new BlendState();
        terrainBlendState.setTestEnabled(true);
        terrainBlendState.setBlendEnabled(false);
        terrainBlendState.setReference(0.5f);

        transparentState = new BlendState();
        transparentState.setBlendEnabled(true);

        cullState = new CullState();
        cullState.setCullFace(CullState.Face.Back);
        cullState.setEnabled(true);

        windowCullState = new CullState();
        windowCullState.setCullFace(CullState.Face.None);
        windowCullState.setEnabled(true);

        worldNode = new WorldNode("WorldModifier");
        worldNode.getSceneHints().setCullHint(CullHint.Never);

        solidNode = new Node("Solid");
        solidNode.getSceneHints().setCullHint(CullHint.Never);

        transparentNode = new Node("Water");
        transparentNode.getSceneHints().setCullHint(CullHint.Never);

        worldNode.attachChild(solidNode);
        worldNode.attachChild(transparentNode);
    }

    private class WorldNode extends Node {
        public WorldNode(final String name) {
            super(name);
        }

        @Override
        public void draw(final Renderer renderer) {
            if (!firstRender && updateLighting) {
                updateLightingTexture(renderer);
                updateLighting = false;
            }
            if (firstRender) {
                firstRender = false;
            }

            solidNode.draw(renderer);
            transparentNode.draw(renderer);
        }

        private final ColorRGBA resultColorFirst = new ColorRGBA();
        private final ColorRGBA resultColorFinal = new ColorRGBA();

        private void updateLightingTexture(final Renderer renderer) {
            ColorRGBA.lerp(nightLight, dayLight, globalLight, resultColorFirst);

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    final float global = y / 15f;
                    final float local = x / 15f;

                    ColorRGBA.lerp(caveLight, resultColorFirst, global, resultColorFinal);
                    resultColorFinal.lerpLocal(torchLight, local);

                    lightingBuffer.put((byte) (resultColorFinal.getRed() * 255));
                    lightingBuffer.put((byte) (resultColorFinal.getGreen() * 255));
                    lightingBuffer.put((byte) (resultColorFinal.getBlue() * 255));
                    lightingBuffer.put((byte) 255);
                }
            }

            lightingBuffer.flip();
            renderer.updateTexture2DSubImage((Texture2D) lightTexture, 0, 0, 16, 16, lightingBuffer, 0, 0, 16);
        }

        @Override
        public void onDraw(final Renderer renderer) {
            draw(renderer);
        }
    }

    /**
     * Start all processing threads. Currently a chunk updater and a light updater.
     */
    public void startThreads() {
        if (!threadsStarted) {
            exit = false;

            final Thread chunkUpdater = new Thread(new ChunkUpdater(), "ChunkUpdater");
            chunkUpdater.setDaemon(true);
            chunkUpdater.start();

            final Thread lightUpdater = new Thread(new LightUpdater(), "LightUpdater");
            lightUpdater.setDaemon(true);
            lightUpdater.start();

            threadsStarted = true;
        }
    }

    /**
     * Get the Node used for rendering the block world.
     * 
     * @return Node with block world
     */
    public Node getWorldNode() {
        return worldNode;
    }

    // private final Matrix4 textureMatrix = new Matrix4();

    /**
     * Issue world updates based on currently set player position/direction.
     * 
     * @param timer
     */
    public void update(final ReadOnlyTimer timer) {
        // textureMatrix.setValue(3, 0, timer.getTimeInSeconds() * 0.2);
        // waterTexture.setTextureMatrix(textureMatrix);

        if (playerPositionX == oldX && playerPositionZ == oldZ) {
            if (lightingChanged) {
                for (final Pos pos : currentPos) {
                    chunkMailBox.add(new LightMessage(pos.x, pos.z));
                }
                lightingChanged = false;
            }
            return;
        }
        // final int diffX = playerPositionX - oldX;
        // final int diffZ = playerPositionZ - oldZ;
        oldX = playerPositionX;
        oldZ = playerPositionZ;

        // TODO
        serverConnection.update(playerPositionX, playerPositionZ);

        final Set<Pos> newPos = new LinkedHashSet<Pos>();
        for (int x = 0; x < gridUnitSize; x++) {
            for (int z = 0; z < gridUnitSize; z++) {
                final int xx = playerPositionX + x - gridUnitSize / 2;
                final int zz = playerPositionZ + z - gridUnitSize / 2;
                newPos.add(new Pos(xx, 0, zz));
            }
        }

        final Iterator<Pos> tileIterator = currentPos.iterator();
        while (tileIterator.hasNext()) {
            final Pos pos = tileIterator.next();

            if (!newPos.contains(pos)) {
                isLoaded.remove(pos);
                tileIterator.remove();
            } else {
                newPos.remove(pos);
            }
        }

        if (!newPos.isEmpty()) {
            final List<Pos> sortedPos = Lists.newArrayList();
            sortedPos.addAll(newPos);
            Collections.sort(sortedPos, new ChunkDistanceComparator(playerPositionX, playerPositionZ));

            // TODO
            for (final Pos coll : sortedPos) {
                serverConnection.requestChunk(coll.x, coll.z);
            }
        }

        if (lightingChanged) {
            for (final Pos pos : currentPos) {
                chunkMailBox.add(new LightMessage(pos.x, pos.z));
            }
            lightingChanged = false;
        }

        currentPos.addAll(newPos);
    }

    @Override
    public void postChunk(final int x, final int z, final Chunk chunk) {
        chunkMailBox.add(new ChunkMessage(x, z, chunk.getBlocks(), chunk.getExtra()));
    }

    /**
     * Let the block world know the current player position and direction.
     * 
     * @param location
     * @param direction
     */
    public void updatePlayer(final ReadOnlyVector3 location, final ReadOnlyVector3 direction) {
        serverConnection.updatePlayerPosition(location, direction);

        playerPositionX = MathUtils.floor(location.getXf() / subMeshSize);
        playerPositionZ = MathUtils.floor(location.getZf() / subMeshSize);

        WorkerManager.getWorker().setPlayerCoords(playerPositionX, playerPositionZ);

        currentTileX = playerPositionX * subMeshSize;
        currentTileZ = playerPositionZ * subMeshSize;
    }

    /**
     * Reload all chunks.
     */
    public void reloadAll() {
        currentPos.clear();
        oldX = Integer.MAX_VALUE;
        oldZ = Integer.MAX_VALUE;
    }

    private final class LightUpdater implements Runnable {
        private final Set<Pos> updates = new LinkedHashSet<Pos>();

        @Override
        public void run() {
            while (!exit) {
                final List<Pos> lightList = lightUpdateBox.switchAndGet();

                if (!lightList.isEmpty()) {
                    updates.clear();

                    for (final Pos pos : lightList) {
                        if (pos == null) {
                            continue;
                        }

                        final Pos gridpos = new Pos(MathUtils.floor((float) pos.x / subMeshSize), 0,
                                MathUtils.floor((float) pos.z / subMeshSize));

                        if (isChunkValid(gridpos.x, gridpos.z)) {
                            updates.add(gridpos);
                        }

                        final int xUpdate = (float) MathUtils.moduloPositive(pos.x, subMeshSize) / (float) subMeshSize > 0.5f ? gridpos.x + 1
                                : gridpos.x - 1;
                        final int zUpdate = (float) MathUtils.moduloPositive(pos.z, subMeshSize) / (float) subMeshSize > 0.5f ? gridpos.z + 1
                                : gridpos.z - 1;
                        if (isChunkValid(xUpdate, gridpos.z)) {
                            updates.add(new Pos(xUpdate, 0, gridpos.z));
                        }
                        if (isChunkValid(gridpos.x, zUpdate)) {
                            updates.add(new Pos(gridpos.x, 0, zUpdate));
                        }
                        if (isChunkValid(xUpdate, zUpdate)) {
                            updates.add(new Pos(xUpdate, 0, zUpdate));
                        }
                    }

                    final Set<Pos> localLightOpenList = Sets.newLinkedHashSet();
                    for (final Pos pos : updates) {
                        // initiateLocalLightList(localLightOpenList, pos.x * subMeshSize, pos.z * subMeshSize, pos.x
                        // * subMeshSize + subMeshSize, pos.z * subMeshSize + subMeshSize, height);
                        initiateLighting(localLightOpenList, pos.x * subMeshSize, pos.z * subMeshSize, pos.x
                                * subMeshSize + subMeshSize, pos.z * subMeshSize + subMeshSize, height);
                        Thread.yield();
                    }

                    for (final Pos pos : updates) {
                        fillQueueLocal(localLightDataWrite, localLightOpenList, pos.x * subMeshSize + subMeshSize,
                                pos.z * subMeshSize, pos.x * subMeshSize + subMeshSize + 1, pos.z * subMeshSize
                                        + subMeshSize, height);
                        fillQueueLocal(localLightDataWrite, localLightOpenList, pos.x * subMeshSize - 1, pos.z
                                * subMeshSize, pos.x * subMeshSize, pos.z * subMeshSize + subMeshSize, height);

                        fillQueueLocal(localLightDataWrite, localLightOpenList, pos.x * subMeshSize, pos.z
                                * subMeshSize + subMeshSize, pos.x * subMeshSize + subMeshSize, pos.z * subMeshSize
                                + subMeshSize + 1, height);
                        fillQueueLocal(localLightDataWrite, localLightOpenList, pos.x * subMeshSize, pos.z
                                * subMeshSize - 1, pos.x * subMeshSize + subMeshSize, pos.z * subMeshSize, height);
                        Thread.yield();
                    }

                    djikstraLight(localLightDataWrite, localLightOpenList);
                    Thread.yield();

                    final Set<Pos> openList = Sets.newLinkedHashSet();
                    for (final Pos pos : updates) {
                        openList.clear();
                        floodLighting(openList, pos.x * subMeshSize, pos.z * subMeshSize, pos.x * subMeshSize
                                + subMeshSize, pos.z * subMeshSize + subMeshSize, height);

                        fillQueue(lightingWrite, openList, pos.x * subMeshSize + subMeshSize, pos.z * subMeshSize,
                                pos.x * subMeshSize + subMeshSize + 1, pos.z * subMeshSize + subMeshSize, height);
                        fillQueue(lightingWrite, openList, pos.x * subMeshSize - 1, pos.z * subMeshSize, pos.x
                                * subMeshSize, pos.z * subMeshSize + subMeshSize, height);

                        fillQueue(lightingWrite, openList, pos.x * subMeshSize, pos.z * subMeshSize + subMeshSize,
                                pos.x * subMeshSize + subMeshSize, pos.z * subMeshSize + subMeshSize + 1, height);
                        fillQueue(lightingWrite, openList, pos.x * subMeshSize, pos.z * subMeshSize - 1, pos.x
                                * subMeshSize + subMeshSize, pos.z * subMeshSize, height);
                        Thread.yield();

                        djikstraLight(lightingWrite, openList);

                        Thread.yield();
                    }

                    for (final Pos pos : updates) {
                        buildLightReadData(lightingWrite, localLightDataWrite, pos.x * subMeshSize,
                                pos.z * subMeshSize, pos.x * subMeshSize + subMeshSize, pos.z * subMeshSize
                                        + subMeshSize, height);
                        Thread.yield();
                    }

                    for (final Pos pos : updates) {
                        chunkMailBox.add(new LightMessage(pos.x, pos.z));
                    }

                    try {
                        Thread.sleep(5);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Thread.sleep(5);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            exitLatch.countDown();
        }
    }

    private final class ChunkUpdater implements Runnable {
        @Override
        public void run() {
            final List<UpdateMessage> dataUpdates = Lists.newArrayList();
            final List<BlocksMessage> blockDataUpdates = Lists.newArrayList();
            final List<UpdateMessage> rebuildUpdates = Lists.newArrayList();
            final List<UpdateMessage> lightUpdates = Lists.newArrayList();

            while (!exit) {
                final List<UpdateMessage> list = chunkMailBox.switchAndGet();

                if (!list.isEmpty()) {
                    dataUpdates.clear();
                    blockDataUpdates.clear();
                    rebuildUpdates.clear();
                    lightUpdates.clear();

                    for (final UpdateMessage updateMessage : list) {
                        if (updateMessage == null) {
                            continue;
                        }

                        if (isChunkOutside(updateMessage.chunkX, updateMessage.chunkZ)) {
                            continue;
                        }

                        if (updateMessage instanceof ChunkMessage) {
                            dataUpdates.add(updateMessage);

                            final RebuildMessage rebuildMessageX = new RebuildMessage(updateMessage.chunkX,
                                    updateMessage.chunkZ);
                            if (!rebuildUpdates.contains(rebuildMessageX)) {
                                rebuildUpdates.add(rebuildMessageX);
                            }

                            if (!isChunkOutside(updateMessage.chunkX - 1, updateMessage.chunkZ)) {
                                final RebuildMessage rebuildMessage = new RebuildMessage(updateMessage.chunkX - 1,
                                        updateMessage.chunkZ);
                                if (!rebuildUpdates.contains(rebuildMessage)) {
                                    rebuildUpdates.add(rebuildMessage);
                                }
                            }
                            if (!isChunkOutside(updateMessage.chunkX + 1, updateMessage.chunkZ)) {
                                final RebuildMessage rebuildMessage = new RebuildMessage(updateMessage.chunkX + 1,
                                        updateMessage.chunkZ);
                                if (!rebuildUpdates.contains(rebuildMessage)) {
                                    rebuildUpdates.add(rebuildMessage);
                                }
                            }
                            if (!isChunkOutside(updateMessage.chunkX, updateMessage.chunkZ - 1)) {
                                final RebuildMessage rebuildMessage = new RebuildMessage(updateMessage.chunkX,
                                        updateMessage.chunkZ - 1);
                                if (!rebuildUpdates.contains(rebuildMessage)) {
                                    rebuildUpdates.add(rebuildMessage);
                                }
                            }
                            if (!isChunkOutside(updateMessage.chunkX, updateMessage.chunkZ + 1)) {
                                final RebuildMessage rebuildMessage = new RebuildMessage(updateMessage.chunkX,
                                        updateMessage.chunkZ + 1);
                                if (!rebuildUpdates.contains(rebuildMessage)) {
                                    rebuildUpdates.add(rebuildMessage);
                                }
                            }

                        } else if (updateMessage instanceof BlocksMessage) {
                            final BlocksMessage message = (BlocksMessage) updateMessage;

                            if (blockDataUpdates.contains(message)) {
                                final BlocksMessage messageOld = blockDataUpdates
                                        .get(blockDataUpdates.indexOf(message));
                                messageOld.blockUpdates.addAll(message.blockUpdates);
                            } else {
                                blockDataUpdates.add((BlocksMessage) updateMessage);

                                final RebuildMessage rebuildMessageX = new RebuildMessage(updateMessage.chunkX,
                                        updateMessage.chunkZ);
                                if (!rebuildUpdates.contains(rebuildMessageX)) {
                                    rebuildUpdates.add(rebuildMessageX);
                                }
                            }

                            for (final BlockEditData data : message.blockUpdates) {
                                addBlockEditDatas(rebuildUpdates, updateMessage, data.pos);
                            }
                        } else if (updateMessage instanceof RebuildMessage) {
                            if (!rebuildUpdates.contains(updateMessage)) {
                                rebuildUpdates.add(updateMessage);
                            }
                        }
                    }

                    for (final UpdateMessage updateMessage : list) {
                        if (updateMessage instanceof LightMessage) {
                            if (!rebuildUpdates.contains(updateMessage) && !lightUpdates.contains(updateMessage)) {
                                lightUpdates.add(updateMessage);
                            }
                        }
                    }

                    for (final UpdateMessage updateMessage : dataUpdates) {
                        if (updateMessage instanceof ChunkMessage) {
                            final ChunkMessage message = (ChunkMessage) updateMessage;

                            WorkerManager.getWorker().enqueue(
                                    new Task(CHUNK, updateMessage.chunkX, updateMessage.chunkZ) {
                                        @Override
                                        public void execute(final Renderer renderer) {
                                            final byte[] data = message.data;
                                            final byte[] extra = message.extra;

                                            final int x = message.chunkX;
                                            final int z = message.chunkZ;

                                            if (isChunkOutside(x, z)) {
                                                return;
                                            }

                                            for (int xx = 0; xx < subMeshSize; xx++) {
                                                for (int yy = 1; yy < height; yy++) {
                                                    for (int zz = 0; zz < subMeshSize; zz++) {
                                                        final int index = xx + (yy + zz * height) * subMeshSize;
                                                        final int blockId = data[index] & 0xff;
                                                        final BlockSide orientation = extra == null ? BlockSide.Front
                                                                : BlockSide.values()[extra[index] & 0xff];

                                                        setBlockInternal(xx + x * subMeshSize, yy,
                                                                zz + z * subMeshSize, blockId, orientation);

                                                        // TODO: optimize
                                                        setLightMax(lightingWrite, xx + x * subMeshSize, yy, zz + z
                                                                * subMeshSize);
                                                    }
                                                }
                                            }

                                            // initiateLightingOnly(x * subMeshSize, z * subMeshSize, x * subMeshSize
                                            // + subMeshSize, z * subMeshSize + subMeshSize, height);

                                            isLoaded.add(new Pos(x, 0, z));

                                            lightUpdateBox.add(new Pos(x * subMeshSize, 0, z * subMeshSize));
                                            lightUpdateBox.add(new Pos(x * subMeshSize + subMeshSize - 1, 0, z
                                                    * subMeshSize));
                                            lightUpdateBox.add(new Pos(x * subMeshSize, 0, z * subMeshSize
                                                    + subMeshSize - 1));
                                            lightUpdateBox.add(new Pos(x * subMeshSize + subMeshSize - 1, 0, z
                                                    * subMeshSize + subMeshSize - 1));
                                        }

                                        private void setLightMax(final byte[] buffer, int x, int y, int z) {
                                            x = MathUtils.moduloPositive(x, width);
                                            y = MathUtils.clamp(y, 0, height - 1);
                                            z = MathUtils.moduloPositive(z, width);

                                            buffer[x + (y + z * height) * width] = (byte) 0xff;
                                        }
                                    });
                        }
                    }

                    for (final UpdateMessage updateMessage : blockDataUpdates) {
                        if (updateMessage instanceof BlocksMessage) {
                            final BlocksMessage message = (BlocksMessage) updateMessage;

                            WorkerManager.getWorker().enqueue(
                                    new Task(BLOCKS, updateMessage.chunkX, updateMessage.chunkZ) {
                                        @Override
                                        public void execute(final Renderer renderer) {
                                            final int x = message.chunkX;
                                            final int z = message.chunkZ;

                                            if (!isChunkValid(x, z)) {
                                                return;
                                            }

                                            final List<BlockEditData> blockUpdates = message.blockUpdates;
                                            for (final BlockEditData blockEditData : blockUpdates) {
                                                final Pos pos = blockEditData.pos;
                                                setBlockInternal(pos.x, pos.y, pos.z, blockEditData.type,
                                                        blockEditData.orientation);
                                                lightUpdateBox.add(pos);
                                                serverConnection.setBlock(pos.x, pos.y, pos.z, blockEditData.type,
                                                        blockEditData.orientation);
                                            }

                                            // initiateLightingOnly(x * subMeshSize, z * subMeshSize, x * subMeshSize
                                            // + subMeshSize, z * subMeshSize + subMeshSize, height);
                                        }
                                    });
                        }
                    }

                    for (final UpdateMessage updateMessage : rebuildUpdates) {
                        if (updateMessage instanceof RebuildMessage) {
                            WorkerManager.getWorker().enqueue(
                                    new Task(REBUILD, updateMessage.chunkX, updateMessage.chunkZ) {
                                        @Override
                                        public void execute(final Renderer renderer) {
                                            if (!isChunkValid(updateMessage.chunkX, updateMessage.chunkZ)) {
                                                return;
                                            }

                                            createSubMesh(renderer, updateMessage.chunkX * subMeshSize, 0,
                                                    updateMessage.chunkZ * subMeshSize, subMeshSize, height,
                                                    subMeshSize, ALL);
                                        }
                                    });
                        }
                    }

                    for (final UpdateMessage updateMessage : lightUpdates) {
                        if (updateMessage instanceof LightMessage) {
                            WorkerManager.getWorker().enqueue(
                                    new Task(LIGHT, updateMessage.chunkX, updateMessage.chunkZ) {
                                        @Override
                                        public void execute(final Renderer renderer) {
                                            final int x = updateMessage.chunkX;
                                            final int z = updateMessage.chunkZ;

                                            if (!isChunkValid(x, z)) {
                                                return;
                                            }

                                            createSubMesh(renderer, x * subMeshSize, 0, z * subMeshSize, subMeshSize,
                                                    height, subMeshSize, COLORS);
                                        }
                                    });
                        }
                    }
                }

                try {
                    Thread.sleep(5);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
            exitLatch.countDown();
        }

        private void addBlockEditDatas(final List<UpdateMessage> rebuildUpdates, final UpdateMessage updateMessage,
                final Pos pos) {
            final int xx = MathUtils.moduloPositive(pos.x, subMeshSize);
            final int zz = MathUtils.moduloPositive(pos.z, subMeshSize);
            if (xx == 0 && isChunkValid(updateMessage.chunkX - 1, updateMessage.chunkZ)) {
                final RebuildMessage rebuildMessage = new RebuildMessage(updateMessage.chunkX - 1, updateMessage.chunkZ);
                if (!rebuildUpdates.contains(rebuildMessage)) {
                    rebuildUpdates.add(rebuildMessage);
                }
            } else if (xx == subMeshSize - 1 && isChunkValid(updateMessage.chunkX + 1, updateMessage.chunkZ)) {
                final RebuildMessage rebuildMessage = new RebuildMessage(updateMessage.chunkX + 1, updateMessage.chunkZ);
                if (!rebuildUpdates.contains(rebuildMessage)) {
                    rebuildUpdates.add(rebuildMessage);
                }
            }
            if (zz == 0 && isChunkValid(updateMessage.chunkX, updateMessage.chunkZ - 1)) {
                final RebuildMessage rebuildMessage = new RebuildMessage(updateMessage.chunkX, updateMessage.chunkZ - 1);
                if (!rebuildUpdates.contains(rebuildMessage)) {
                    rebuildUpdates.add(rebuildMessage);
                }
            } else if (zz == subMeshSize - 1 && isChunkValid(updateMessage.chunkX, updateMessage.chunkZ + 1)) {
                final RebuildMessage rebuildMessage = new RebuildMessage(updateMessage.chunkX, updateMessage.chunkZ + 1);
                if (!rebuildUpdates.contains(rebuildMessage)) {
                    rebuildUpdates.add(rebuildMessage);
                }
            }

            if (xx == 0 && zz == 0 && isChunkValid(updateMessage.chunkX - 1, updateMessage.chunkZ - 1)) {
                final RebuildMessage rebuildMessage = new RebuildMessage(updateMessage.chunkX - 1,
                        updateMessage.chunkZ - 1);
                if (!rebuildUpdates.contains(rebuildMessage)) {
                    rebuildUpdates.add(rebuildMessage);
                }
            } else if (xx == subMeshSize - 1 && zz == 0
                    && isChunkValid(updateMessage.chunkX + 1, updateMessage.chunkZ - 1)) {
                final RebuildMessage rebuildMessage = new RebuildMessage(updateMessage.chunkX + 1,
                        updateMessage.chunkZ - 1);
                if (!rebuildUpdates.contains(rebuildMessage)) {
                    rebuildUpdates.add(rebuildMessage);
                }
            } else if (xx == subMeshSize - 1 && zz == subMeshSize - 1
                    && isChunkValid(updateMessage.chunkX + 1, updateMessage.chunkZ + 1)) {
                final RebuildMessage rebuildMessage = new RebuildMessage(updateMessage.chunkX + 1,
                        updateMessage.chunkZ + 1);
                if (!rebuildUpdates.contains(rebuildMessage)) {
                    rebuildUpdates.add(rebuildMessage);
                }
            } else if (xx == 0 && zz == subMeshSize - 1
                    && isChunkValid(updateMessage.chunkX - 1, updateMessage.chunkZ + 1)) {
                final RebuildMessage rebuildMessage = new RebuildMessage(updateMessage.chunkX - 1,
                        updateMessage.chunkZ + 1);
                if (!rebuildUpdates.contains(rebuildMessage)) {
                    rebuildUpdates.add(rebuildMessage);
                }
            }
        }

    }

    private boolean isChunkValid(final int chunkX, final int chunkZ) {
        if (!isLoaded.contains(new Pos(chunkX, 0, chunkZ))) {
            return false;
        }
        return !isChunkOutside(chunkX, chunkZ);
    }

    private boolean isChunkOutside(final int chunkX, final int chunkZ) {
        final int diffX1 = playerPositionX - chunkX;
        final int diffZ1 = playerPositionZ - chunkZ;
        if (diffX1 > gridUnitSize / 2 || diffX1 < -gridUnitSize / 2 + 1 || diffZ1 > gridUnitSize / 2
                || diffZ1 < -gridUnitSize / 2 + 1) {
            return true;
        }
        return false;
    }

    private static int VERTICES = 1 << 0;
    private static int TEXCOORDS = 1 << 1;
    private static int COLORS = 1 << 2;
    private static int INDICES = 1 << 3;
    private static int ALL = VERTICES | TEXCOORDS | COLORS | INDICES;

    private void createSubMesh(final Renderer renderer, final int xStart, final int yStart, final int zStart,
            final int xWidth, final int yWidth, final int zWidth, final int creationType) {
        final int modxStart = MathUtils.moduloPositive(xStart, width);
        final int modyStart = MathUtils.moduloPositive(yStart, height);
        final int modzStart = MathUtils.moduloPositive(zStart, width);

        final Pos pos = new Pos(modxStart / subMeshSize, modyStart / height, modzStart / subMeshSize);
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        final float[] vertices = (creationType & VERTICES) == VERTICES ? tmpVertices : null;
        final float[] texcoords = (creationType & TEXCOORDS) == TEXCOORDS ? tmpTexcoords : null;
        final float[] colors = (creationType & COLORS) == COLORS ? tmpColors : null;
        final int[] indices = (creationType & INDICES) == INDICES ? tmpIndices : null;
        geometryHandler.setVertices(vertices);
        geometryHandler.setTexcoords(texcoords);
        geometryHandler.setColors(colors);
        geometryHandler.setIndices(indices);

        final float[] verticesTransparent = (creationType & VERTICES) == VERTICES ? tmpVerticesTransparent : null;
        final float[] texcoordsTransparent = (creationType & TEXCOORDS) == TEXCOORDS ? tmpTexcoordsTransparent : null;
        final float[] colorsTransparent = (creationType & COLORS) == COLORS ? tmpColorsTransparent : null;
        final int[] indicesTransparent = (creationType & INDICES) == INDICES ? tmpIndicesTransparent : null;
        geometryHandlerTransparent.setVertices(verticesTransparent);
        geometryHandlerTransparent.setTexcoords(texcoordsTransparent);
        geometryHandlerTransparent.setColors(colorsTransparent);
        geometryHandlerTransparent.setIndices(indicesTransparent);

        // long t = System.currentTimeMillis();
        int vertexCount = 0;
        int indexCount = 0;
        int vertexCountTransparent = 0;
        int indexCountTransparent = 0;

        for (int x = xStart; x < xStart + xWidth; x++) {
            for (int y = yStart; y < yStart + yWidth; y++) {
                for (int z = zStart; z < zStart + zWidth; z++) {
                    final int block = getBlock(x, y, z);
                    if (block == 0) {
                        continue;
                    }

                    if (y < minY) {
                        minY = y;
                    }
                    if (y >= maxY) {
                        maxY = y;
                    }

                    final boolean isTransparent = blockUtil.getIsSemiTransparent(block);
                    final GeometryHandler currentGeometryHandler = isTransparent ? geometryHandlerTransparent
                            : geometryHandler;

                    if (!isTransparent) {
                        currentGeometryHandler.setStartIndexCount(indexCount);
                        currentGeometryHandler.setStartVertexCount(vertexCount);
                    } else {
                        currentGeometryHandler.setStartIndexCount(indexCountTransparent);
                        currentGeometryHandler.setStartVertexCount(vertexCountTransparent);
                    }
                    blockUtil.getGeometryProducer(block).generateBlock(block, currentGeometryHandler, this, blockUtil,
                            x, y, z);
                    if (!isTransparent) {
                        indexCount += currentGeometryHandler.getIndexCount();
                        vertexCount += currentGeometryHandler.getVertexCount();
                    } else {
                        indexCountTransparent += currentGeometryHandler.getIndexCount();
                        vertexCountTransparent += currentGeometryHandler.getVertexCount();
                    }
                }
            }
        }

        setMeshData(renderer, xStart, yStart, zStart, pos, indexCount, vertexCount, vertices, texcoords, colors,
                indices, minY, maxY);
        setMeshDataTransparent(renderer, xStart, yStart, zStart, pos, indexCountTransparent, vertexCountTransparent,
                verticesTransparent, texcoordsTransparent, colorsTransparent, indicesTransparent, minY, maxY);
    }

    private void setMeshData(final Renderer renderer, final int xStart, final int yStart, final int zStart,
            final Pos pos, final int indexCount, final int vertexCount, final float[] vertices,
            final float[] texcoords, final float[] colors, final int[] indices, final int minY, final int maxY) {
        if (indexCount == 0) {
            if (meshCache.containsKey(pos)) {
                final Mesh mesh = meshCache.get(pos);
                if (solidNode.hasChild(mesh)) {
                    solidNode.detachChild(mesh);
                }
            }
            return;
        }

        Mesh mesh;
        if (meshCache.containsKey(pos)) {
            mesh = meshCache.get(pos);
            if (!solidNode.hasChild(mesh)) {
                solidNode.attachChild(mesh);
            }
        } else {
            if (indices == null) {
                return;
            }

            mesh = new Mesh(xStart + "," + yStart + "," + zStart);
            mesh.getSceneHints().setLightCombineMode(LightCombineMode.Off);

            mesh.setModelBound(new BoundingBox(new Vector3(), subMeshSize / 2, height / 2, subMeshSize / 2));
            mesh.getSceneHints().setCullHint(CullHint.Dynamic);

            mesh.setRenderState(terrainTextureState);
            mesh.setRenderState(cullState);
            mesh.setRenderState(terrainBlendState);

            if (hasVBOSupport) {
                mesh.getSceneHints().setDataMode(DataMode.VBO);
            }

            solidNode.attachChild(mesh);
            meshCache.put(pos, mesh);
            mesh.getMeshData().setIndexMode(IndexMode.Triangles);
        }

        updateBuffers(renderer, xStart, zStart, indexCount, vertexCount, vertices, texcoords, colors, indices, minY,
                maxY, mesh);
    }

    private void setMeshDataTransparent(final Renderer renderer, final int xStart, final int yStart, final int zStart,
            final Pos pos, final int indexCount, final int vertexCount, final float[] vertices,
            final float[] texcoords, final float[] colors, final int[] indices, final int minY, final int maxY) {
        if (indexCount == 0) {
            if (meshCacheTransparent.containsKey(pos)) {
                final Mesh mesh = meshCacheTransparent.get(pos);
                if (transparentNode.hasChild(mesh)) {
                    transparentNode.detachChild(mesh);
                }
            }
            return;
        }

        Mesh mesh;
        if (meshCacheTransparent.containsKey(pos)) {
            mesh = meshCacheTransparent.get(pos);
            if (!transparentNode.hasChild(mesh)) {
                transparentNode.attachChild(mesh);
            }
        } else {
            if (indices == null) {
                return;
            }

            mesh = new Mesh(xStart + "," + yStart + "," + zStart);
            mesh.getSceneHints().setLightCombineMode(LightCombineMode.Off);

            mesh.setModelBound(new BoundingBox(new Vector3(), subMeshSize / 2, height / 2, subMeshSize / 2));
            mesh.getSceneHints().setCullHint(CullHint.Dynamic);

            mesh.setRenderState(terrainTextureState);
            mesh.setRenderState(windowCullState);
            mesh.setRenderState(transparentState);

            if (hasVBOSupport) {
                mesh.getSceneHints().setDataMode(DataMode.VBO);
            }

            transparentNode.attachChild(mesh);
            meshCacheTransparent.put(pos, mesh);
            mesh.getMeshData().setIndexMode(IndexMode.Triangles);
        }

        updateBuffers(renderer, xStart, zStart, indexCount, vertexCount, vertices, texcoords, colors, indices, minY,
                maxY, mesh);
    }

    private void updateBuffers(final Renderer renderer, final int xStart, final int zStart, final int indexCount,
            final int vertexCount, final float[] vertices, final float[] texcoords, final float[] colors,
            final int[] indices, final int minY, final int maxY, final Mesh mesh) {
        final int height = (maxY - minY) / 2 + 1;
        final int posY = (maxY + minY) / 2 + 1;
        final BoundingBox boundingBox = (BoundingBox) mesh.getModelBound();
        boundingBox.setYExtent(height);
        boundingBox.setCenter(xStart + subMeshSize / 2, posY, zStart + subMeshSize / 2);
        mesh.updateWorldBound(false);

        final MeshData meshData = mesh.getMeshData();

        if (vertices != null) {
            FloatBuffer vertexBuffer = meshData.getVertexBuffer();
            if (vertexBuffer != null && vertexBuffer.capacity() >= vertexCount * 3) {
                vertexBuffer.clear();
                if (hasVBOSupport) {
                    meshData.getVertexCoords().setNeedsRefresh(true);
                    // meshData.getVertexCoords().removeVBOID(ContextManager.getCurrentContext().getGlContextRep());
                }
            } else {
                if (hasVBOSupport && vertexBuffer != null) {
                    renderer.deleteVBOs(meshData.getVertexCoords());
                }
                vertexBuffer = BufferUtils.createFloatBuffer(vertexCount * 3);
                meshData.setVertexBuffer(vertexBuffer);
                if (hasVBOSupport) {
                    meshData.getVertexCoords().setVboAccessMode(VBOAccessMode.DynamicDraw);
                }
            }
            vertexBuffer.put(vertices, 0, vertexCount * 3);
        }

        if (texcoords != null) {
            FloatBuffer textureBuffer = meshData.getTextureBuffer(0);
            if (textureBuffer != null && textureBuffer.capacity() >= vertexCount * 2) {
                textureBuffer.clear();
                if (hasVBOSupport) {
                    meshData.getTextureCoords(0).setNeedsRefresh(true);
                    // meshData.getTextureCoords(0).removeVBOID(ContextManager.getCurrentContext().getGlContextRep());
                }
            } else {
                if (hasVBOSupport && textureBuffer != null) {
                    renderer.deleteVBOs(meshData.getTextureCoords(0));
                }
                textureBuffer = BufferUtils.createFloatBuffer(vertexCount * 2);
                meshData.setTextureBuffer(textureBuffer, 0);
                if (hasVBOSupport) {
                    meshData.getTextureCoords(0).setVboAccessMode(VBOAccessMode.DynamicDraw);
                }
            }
            textureBuffer.put(texcoords, 0, vertexCount * 2);
        }

        if (colors != null) {
            FloatBuffer colorBuffer = meshData.getTextureBuffer(1);
            if (colorBuffer != null && colorBuffer.capacity() >= vertexCount * 2) {
                colorBuffer.clear();
                if (hasVBOSupport) {
                    meshData.getTextureCoords(1).setNeedsRefresh(true);
                    // meshData.getColorCoords().removeVBOID(ContextManager.getCurrentContext().getGlContextRep());
                }
            } else {
                if (hasVBOSupport && colorBuffer != null) {
                    renderer.deleteVBOs(meshData.getTextureCoords(1));
                }
                colorBuffer = BufferUtils.createFloatBuffer(vertexCount * 2);
                meshData.setTextureBuffer(colorBuffer, 1);
                if (hasVBOSupport) {
                    meshData.getTextureCoords(1).setVboAccessMode(VBOAccessMode.DynamicDraw);
                }
            }
            colorBuffer.put(colors, 0, vertexCount * 2);
        }

        if (indices != null) {
            IntBuffer indexBuffer = (IntBuffer) meshData.getIndexBuffer();
            if (indexBuffer != null && indexBuffer.capacity() >= indexCount) {
                final int oldLimit = indexBuffer.limit();
                indexBuffer.clear();
                indexBuffer.limit(indexCount);
                if (hasVBOSupport) {
                    if (indexCount <= oldLimit) {
                        meshData.getIndices().setNeedsRefresh(true);
                    } else {
                        renderer.deleteVBOs(meshData.getIndices());
                    }
                }
            } else {
                if (hasVBOSupport && indexBuffer != null) {
                    renderer.deleteVBOs(meshData.getIndices());
                }
                indexBuffer = BufferUtils.createIntBuffer(indexCount);
                meshData.setIndexBuffer(indexBuffer);
                if (hasVBOSupport) {
                    meshData.getIndices().setVboAccessMode(VBOAccessMode.DynamicDraw);
                }
            }
            indexBuffer.put(indices, 0, indexCount);
        }
    }

    /**
     * Get the exact float used global lighting.
     * 
     * @param lighting
     * @return float
     */
    public float lookupLighting(final int lighting) {
        final int clampedLight = MathUtils.clamp(lighting, 0, 15);
        return clampedLight / 15f;
    }

    @Override
    public int getBlock(int x, final int y, int z) {
        if (testOutsideBounds(x, y, z)) {
            return 1;
        }

        x = MathUtils.moduloPositive(x, width);
        z = MathUtils.moduloPositive(z, width);

        return blocks[x + (y + z * height) * width] & 0xff;
    }

    private int getBlockFast(final int x, final int y, final int z) {
        return blocks[x + (y + z * height) * width] & 0xff;
    }

    @Override
    public void setBlock(final int x, final int y, final int z, final int blockId) {
        setBlock(x, y, z, blockId, BlockSide.Front);
    }

    @Override
    public void setBlock(final int x, final int y, final int z, final int blockId, final BlockSide orientation) {
        if (testOutsideBounds(x, y, z)) {
            return;
        }

        final BlockEditData data = new BlockEditData(new Pos(x, y, z), blockId, orientation);

        final int chunkX = MathUtils.floor((float) x / subMeshSize);
        final int chunkZ = MathUtils.floor((float) z / subMeshSize);
        chunkMailBox.add(new BlocksMessage(chunkX, chunkZ, data));
    }

    @Override
    public void setBlocks(final List<BlockEditData> blockList) {
        final Map<Coords, List<BlockEditData>> chunks = Maps.newHashMap();
        for (final BlockEditData data : blockList) {
            final int xx = data.pos.x;
            final int zz = data.pos.z;
            final int chunkX = MathUtils.floor((float) xx / subMeshSize);
            final int chunkZ = MathUtils.floor((float) zz / subMeshSize);
            final Coords coord = new Coords();
            coord.setX(chunkX);
            coord.setZ(chunkZ);

            List<BlockEditData> subData = chunks.get(coord);
            if (subData == null) {
                subData = Lists.newArrayList();
                chunks.put(coord, subData);
            }
            subData.add(data);
        }
        for (final Entry<Coords, List<BlockEditData>> entry : chunks.entrySet()) {
            final Coords coord = entry.getKey();
            final List<BlockEditData> list = entry.getValue();
            chunkMailBox.add(new BlocksMessage(coord.getX(), coord.getZ(), list));
        }
    }

    /**
     * Get block orientation at coordinate x, y, z.
     * 
     * @param x
     * @param y
     * @param z
     * @return Orientation
     */
    public BlockSide getBlockOrientation(final int x, final int y, final int z) {
        return BlockSide.values()[getBlockExtra(x, y, z)];
    }

    public int getBlockExtra(int x, final int y, int z) {
        if (testOutsideBounds(x, y, z)) {
            return 0;
        }

        x = MathUtils.moduloPositive(x, width);
        z = MathUtils.moduloPositive(z, width);

        return blockExtra[x + (y + z * height) * width] & 0x7;
    }

    private void setBlockInternal(final int xx, final int yy, final int zz, final int data, final BlockSide orientation) {
        if (testOutsideBounds(xx, yy, zz)) {
            return;
        }

        final int x = MathUtils.moduloPositive(xx, width);
        final int y = yy;
        final int z = MathUtils.moduloPositive(zz, width);

        blocks[x + (y + z * height) * width] = (byte) data;
        blockExtra[x + (y + z * height) * width] = (byte) orientation.ordinal();
    }

    private void setLightSolid(int x, final int y, int z, final boolean val) {
        x = MathUtils.moduloPositive(x, width);
        z = MathUtils.moduloPositive(z, width);
        isLightSolid.set(x + (y + z * height) * width, val);
    }

    private boolean isLightSolid(int x, final int y, int z) {
        x = MathUtils.moduloPositive(x, width);
        z = MathUtils.moduloPositive(z, width);
        return isLightSolid.get(x + (y + z * height) * width);
    }

    private boolean testOutsideBounds(final int x, final int y, final int z) {
        return y < 1 || y >= height || x < currentTileX - gridSize || x >= currentTileX + gridSize
                || z < currentTileZ - gridSize || z >= currentTileZ + gridSize;
    }

    private byte getLight(final byte[] buffer, int x, int y, int z) {
        x = MathUtils.moduloPositive(x, width);
        y = MathUtils.clamp(y, 0, height - 1);
        z = MathUtils.moduloPositive(z, width);

        return (byte) (buffer[x + (y + z * height) * width] & 0x0f);
    }

    private void setLight(final byte[] buffer, int x, int y, int z, final int data) {
        x = MathUtils.moduloPositive(x, width);
        y = MathUtils.clamp(y, 0, height - 1);
        z = MathUtils.moduloPositive(z, width);

        buffer[x + (y + z * height) * width] = (byte) (buffer[x + (y + z * height) * width] & 0xf0 | data & 0xf);
    }

    private byte getLightRead(final byte[] buffer, int x, int y, int z) {
        x = MathUtils.moduloPositive(x, width);
        y = MathUtils.clamp(y, 0, height - 1);
        z = MathUtils.moduloPositive(z, width);

        return (byte) (buffer[x + (y + z * height) * width] >> 4 & 0xf);
    }

    private void setLightRead(final byte[] buffer, int x, int y, int z, final int data) {
        x = MathUtils.moduloPositive(x, width);
        y = MathUtils.clamp(y, 0, height - 1);
        z = MathUtils.moduloPositive(z, width);

        buffer[x + (y + z * height) * width] = (byte) (buffer[x + (y + z * height) * width] & 0x0f | (data & 0xf) << 4);
    }

    private int getLightHeightmap(int x, int z) {
        if (testOutsideBounds(x, 5, z)) {
            return height;
        }

        x = MathUtils.moduloPositive(x, width);
        z = MathUtils.moduloPositive(z, width);

        return lightHeightmap[x + z * width];
    }

    private void setLightHeightmap(final int x, final int z, final short data) {
        lightHeightmap[x + z * width] = data;
    }

    private void initiateLighting(final Set<Pos> localLightOpenList, final int xStart, final int zStart,
            final int xEnd, final int zEnd, final int height) {
        for (int x = xStart; x < xEnd; x++) {
            for (int z = zStart; z < zEnd; z++) {
                final int modx = MathUtils.moduloPositive(x, width);
                final int modz = MathUtils.moduloPositive(z, width);

                int currentLight = MAX_LIGHT;
                for (int y = height - 1; y >= 0; y--) {
                    final int block = getBlockFast(modx, y, modz);
                    final boolean solid = blockUtil.getBlockType(block) == BlockType.Solid;
                    setLightSolid(x, y, z, false);
                    if (currentLight > 0) {
                        if (block != 0 && block != WATER && solid) {
                            setLightHeightmap(modx, modz, (short) y);
                            setLight(lightingWrite, x, y, z, 0);
                            setLight(localLightDataWrite, x, y, z, 0);
                            setLightSolid(x, y, z, true);
                            currentLight = 0;
                        } else {
                            setLight(lightingWrite, x, y, z, currentLight);
                            setLight(localLightDataWrite, x, y, z, 0);
                        }
                        if (block == WATER) {
                            currentLight -= 2;
                            if (currentLight < 0) {
                                currentLight = 0;
                            }
                        } else if (block != 0 && !solid) {
                            currentLight -= 1;
                            if (currentLight < 0) {
                                currentLight = 0;
                            }
                        }
                    } else if (block == 0 || block == WATER || !solid) {
                        setLight(lightingWrite, x, y, z, 0);
                        setLight(localLightDataWrite, x, y, z, 0);
                    } else {
                        setLight(lightingWrite, x, y, z, 0);
                        setLight(localLightDataWrite, x, y, z, 0);
                        setLightSolid(x, y, z, true);
                    }

                    if (blockUtil.isLocalLight(block)) {
                        setLight(localLightDataWrite, x, y, z, MAX_LIGHT);
                        localLightOpenList.add(new Pos(x, y, z));
                    }
                }
            }
        }
    }

    private void initiateLocalLightList(final Set<Pos> localLightOpenList, final int xStart, final int zStart,
            final int xEnd, final int zEnd, final int height) {
        for (int x = xStart; x < xEnd; x++) {
            for (int z = zStart; z < zEnd; z++) {
                final int modx = MathUtils.moduloPositive(x, width);
                final int modz = MathUtils.moduloPositive(z, width);

                for (int y = height - 1; y >= 0; y--) {
                    final int block = getBlockFast(modx, y, modz);
                    if (blockUtil.isLocalLight(block)) {
                        localLightOpenList.add(new Pos(x, y, z));
                    }
                }
            }
        }
    }

    private void initiateLightingOnly(final int xStart, final int zStart, final int xEnd, final int zEnd,
            final int height) {
        for (int x = xStart; x < xEnd; x++) {
            for (int z = zStart; z < zEnd; z++) {
                final int modx = MathUtils.moduloPositive(x, width);
                final int modz = MathUtils.moduloPositive(z, width);

                int currentLight = MAX_LIGHT;
                for (int y = height - 1; y >= 0; y--) {
                    final int block = getBlockFast(modx, y, modz);
                    final boolean solid = blockUtil.getBlockType(block) == BlockType.Solid;
                    setLightSolid(x, y, z, false);
                    if (currentLight > 0) {
                        if (block != 0 && block != WATER && solid) {
                            setLightHeightmap(modx, modz, (short) y);
                            setLight(lightingWrite, x, y, z, 0);
                            setLight(localLightDataWrite, x, y, z, 0);
                            setLightSolid(x, y, z, true);
                            currentLight = 0;
                        } else {
                            setLight(lightingWrite, x, y, z, currentLight);
                            setLight(localLightDataWrite, x, y, z, 0);
                        }
                        if (block == WATER) {
                            currentLight -= 2;
                            if (currentLight < 0) {
                                currentLight = 0;
                            }
                        } else if (block != 0 && !solid) {
                            currentLight -= 1;
                            if (currentLight < 0) {
                                currentLight = 0;
                            }
                        }
                    } else if (block == 0 || block == WATER || !solid) {
                        setLight(lightingWrite, x, y, z, 0);
                        setLight(localLightDataWrite, x, y, z, 0);
                    } else {
                        setLight(lightingWrite, x, y, z, 0);
                        setLight(localLightDataWrite, x, y, z, 0);
                        setLightSolid(x, y, z, true);
                    }
                }
            }
        }
    }

    private void fillQueue(final byte[] write, final Set<Pos> openList, final int xStart, final int zStart,
            final int xEnd, final int zEnd, final int height) {
        if (testOutsideBounds(xStart, 5, zStart)) {
            return;
        }
        for (int x = xStart; x < xEnd; x++) {
            for (int z = zStart; z < zEnd; z++) {
                final int yStart = getLightHeightmap(x, z) - 1;
                for (int y = yStart; y >= 0; y--) {
                    final byte currentLight = getLight(write, x, y, z);
                    if (currentLight <= LIGHT_STEP) {
                        continue;
                    }
                    addToLightQueue(openList, x, y, z);
                }
            }
        }
    }

    private void fillQueueLocal(final byte[] write, final Set<Pos> openList, final int xStart, final int zStart,
            final int xEnd, final int zEnd, final int height) {
        if (testOutsideBounds(xStart, 5, zStart)) {
            return;
        }
        for (int x = xStart; x < xEnd; x++) {
            for (int z = zStart; z < zEnd; z++) {
                for (int y = height - 1; y >= 0; y--) {
                    final byte currentLight = getLight(write, x, y, z);
                    if (currentLight <= LIGHT_STEP) {
                        continue;
                    }
                    addToLightQueue(openList, x, y, z);
                }
            }
        }
    }

    private void floodLighting(final Set<Pos> openList, final int xStart, final int zStart, final int xEnd,
            final int zEnd, final int height) {
        for (int x = xStart; x < xEnd; x++) {
            for (int z = zStart; z < zEnd; z++) {
                final int yStart = getLightHeightmap(x, z) - 1;

                if (testOutsideBounds(x, 5, z)) {
                    continue;
                }
                final int modx = MathUtils.moduloPositive(x, width);
                final int modz = MathUtils.moduloPositive(z, width);

                for (int y = yStart; y >= 0; y--) {
                    final int block = getBlockFast(modx, y, modz);
                    final boolean solid = blockUtil.getBlockType(block) == BlockType.Solid;
                    if (block != 0 && solid) {
                        continue;
                    }

                    int neighbourHeight;

                    // if (!testOutsideBounds(x + 1, 5, z)) {
                    neighbourHeight = getLightHeightmap(x + 1, z);
                    if (neighbourHeight < y) {
                        affectLightNode(lightingWrite, openList, x, y, z, getLight(lightingWrite, x + 1, y, z));
                        continue;
                    }
                    // }
                    // if (!testOutsideBounds(x - 1, 5, z)) {
                    neighbourHeight = getLightHeightmap(x - 1, z);
                    if (neighbourHeight < y) {
                        affectLightNode(lightingWrite, openList, x, y, z, getLight(lightingWrite, x - 1, y, z));
                        continue;
                    }
                    // }
                    // if (!testOutsideBounds(x, 5, z + 1)) {
                    neighbourHeight = getLightHeightmap(x, z + 1);
                    if (neighbourHeight < y) {
                        affectLightNode(lightingWrite, openList, x, y, z, getLight(lightingWrite, x, y, z + 1));
                        continue;
                    }
                    // }
                    // if (!testOutsideBounds(x, 5, z - 1)) {
                    neighbourHeight = getLightHeightmap(x, z - 1);
                    if (neighbourHeight < y) {
                        affectLightNode(lightingWrite, openList, x, y, z, getLight(lightingWrite, x, y, z - 1));
                        continue;
                    }
                    // }
                }
            }
        }
    }

    private void addToLightQueue(final Set<Pos> openList, final int x, final int y, final int z) {
        openList.add(new Pos(x, y, z));
    }

    private void djikstraLight(final byte[] write, Set<Pos> openList) {
        int iterations = 0;
        Set<Pos> backupList = Sets.newLinkedHashSet();
        while (!openList.isEmpty()) {
            final Iterator<Pos> iterator = openList.iterator();
            while (iterator.hasNext()) {
                final Pos openPos = iterator.next();
                iterator.remove();

                final int x = openPos.x;
                final int y = openPos.y;
                final int z = openPos.z;

                final byte currentLight = getLight(write, x, y, z);
                if (currentLight <= LIGHT_STEP) {
                    continue;
                }

                if (testLightExpand(write, x - 1, y, z, currentLight)) {
                    affectLightNode(write, backupList, x - 1, y, z, currentLight);
                }
                if (testLightExpand(write, x + 1, y, z, currentLight)) {
                    affectLightNode(write, backupList, x + 1, y, z, currentLight);
                }
                if (testLightExpand(write, x, y - 1, z, currentLight)) {
                    affectLightNode(write, backupList, x, y - 1, z, currentLight);
                }
                if (testLightExpand(write, x, y + 1, z, currentLight)) {
                    affectLightNode(write, backupList, x, y + 1, z, currentLight);
                }
                if (testLightExpand(write, x, y, z - 1, currentLight)) {
                    affectLightNode(write, backupList, x, y, z - 1, currentLight);
                }
                if (testLightExpand(write, x, y, z + 1, currentLight)) {
                    affectLightNode(write, backupList, x, y, z + 1, currentLight);
                }

                if (iterations++ > 50000) {
                    logger.warning("Too many light calcs, skipping: " + iterations + ", " + openList.size());
                    return;
                }
                if (iterations == 10000) {
                    logger.warning("Lots of light calcs: " + iterations + ", " + openList.size());
                }
                if (iterations == 30000) {
                    logger.warning("Scary amount of light calcs: " + iterations + ", " + openList.size());
                }
            }
            final Set<Pos> tmp = openList;
            openList = backupList;
            backupList = tmp;
        }
    }

    private void affectLightNode(final byte[] write, final Set<Pos> openList, final int x, final int y, final int z,
            final byte currentLight) {
        addToLightQueue(openList, x, y, z);
        final int light = currentLight - LIGHT_STEP;
        setLight(write, x, y, z, light < 0 ? 0 : light);
    }

    private boolean testLightExpand(final byte[] write, final int x, final int y, final int z, final byte currentLight) {
        if (testOutsideBounds(x, y, z)) {
            return false;
        }

        final int block = getBlock(x, y, z);
        final boolean solid = blockUtil.getBlockType(block) == BlockType.Solid;

        return (block == 0 || block == WATER || !solid) && getLight(write, x, y, z) < currentLight - LIGHT_STEP;
    }

    private void buildLightReadData(final byte[] writeGlobal, final byte[] writeLocal, final int xStart,
            final int zStart, final int xEnd, final int zEnd, final int height) {
        for (int x = xStart; x < xEnd + 1; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = zStart; z < zEnd + 1; z++) {
                    float dataGlobal = 0;
                    float dataLocal = 0;
                    float divider = 0;

                    if (!isLightSolid(x, y, z)) {
                        dataGlobal += getLight(writeGlobal, x, y, z);
                        dataLocal += getLight(writeLocal, x, y, z);
                        divider++;
                    }
                    if (!isLightSolid(x - 1, y, z)) {
                        dataGlobal += getLight(writeGlobal, x - 1, y, z);
                        dataLocal += getLight(writeLocal, x - 1, y, z);
                        divider++;
                    }
                    if (!isLightSolid(x, y, z - 1)) {
                        dataGlobal += getLight(writeGlobal, x, y, z - 1);
                        dataLocal += getLight(writeLocal, x, y, z - 1);
                        divider++;
                    }
                    if (!isLightSolid(x - 1, y, z - 1)) {
                        dataGlobal += getLight(writeGlobal, x - 1, y, z - 1);
                        dataLocal += getLight(writeLocal, x - 1, y, z - 1);
                        divider++;
                    }
                    if (y > 0) {
                        if (!isLightSolid(x, y - 1, z)) {
                            dataGlobal += getLight(writeGlobal, x, y - 1, z);
                            dataLocal += getLight(writeLocal, x, y - 1, z);
                            divider++;
                        }
                        if (!isLightSolid(x - 1, y - 1, z)) {
                            dataGlobal += getLight(writeGlobal, x - 1, y - 1, z);
                            dataLocal += getLight(writeLocal, x - 1, y - 1, z);
                            divider++;
                        }
                        if (!isLightSolid(x, y - 1, z - 1)) {
                            dataGlobal += getLight(writeGlobal, x, y - 1, z - 1);
                            dataLocal += getLight(writeLocal, x, y - 1, z - 1);
                            divider++;
                        }
                        if (!isLightSolid(x - 1, y - 1, z - 1)) {
                            dataGlobal += getLight(writeGlobal, x - 1, y - 1, z - 1);
                            dataLocal += getLight(writeLocal, x - 1, y - 1, z - 1);
                            divider++;
                        }
                    }

                    if (divider > 0) {
                        setLightRead(writeGlobal, x, y, z, (int) (dataGlobal / divider));
                        setLightRead(writeLocal, x, y, z, (int) (dataLocal / divider));
                    } else {
                        setLightRead(writeGlobal, x, y, z, 0);
                        setLightRead(writeLocal, x, y, z, 0);
                    }
                }
            }
        }
    }

    public int getHeight() {
        return height;
    }

    private final HitTester defaultPickingTester = new HitTester() {
        @Override
        public boolean isHit(final int blockId) {
            return blockUtil.getIsPickable(blockId);
        }
    };
    private final HitTester defaultHitTester = new HitTester() {
        @Override
        public boolean isHit(final int blockId) {
            return blockUtil.getIsCollidable(blockId);
        }
    };

    public void tracePicking(final ReadOnlyVector3 curpos, final ReadOnlyVector3 raydir, final int maxIterations,
            final IntersectionResult result) {
        new Tracer(this, defaultPickingTester, height).traceCollision(curpos, raydir, maxIterations, result);
    }

    public void traceCollision(final ReadOnlyVector3 curpos, final ReadOnlyVector3 raydir, final int maxIterations,
            final IntersectionResult result) {
        new Tracer(this, defaultHitTester, height).traceCollision(curpos, raydir, maxIterations, result);
    }

    /**
     * Stop all processing threads, and wait for them to finish.
     */
    public void stopThreads() {
        if (threadsStarted) {
            exit = true;

            logger.info("Stopping threads...");

            boolean wait = false;
            try {
                wait = exitLatch.await(20, TimeUnit.SECONDS);
            } catch (final InterruptedException e1) {
                e1.printStackTrace();
            }

            chunkMailBox.switchAndGet();
            chunkMailBox.switchAndGet();
            lightUpdateBox.switchAndGet();
            lightUpdateBox.switchAndGet();

            threadsStarted = false;

            logger.info("All threads done: " + wait);
        }
    }

    public float getGlobalLight() {
        return globalLight;
    }

    /**
     * Set global light value (0-15) for day/night simulation.
     * 
     * @param globalLight
     */
    public void setGlobalLight(final float globalLight) {
        this.globalLight = MathUtils.clamp(globalLight, 0f, 1f);
        updateLighting = true;
    }

    public WorldSettings getSettings() {
        return settings;
    }

    public BlockUtil getBlockUtil() {
        return blockUtil;
    }

    @Override
    public float getGlobalLighting(final int x, final int y, final int z) {
        final int lighting = getLightRead(lightingWrite, x, y, z);
        return lookupLighting(lighting);
    }

    @Override
    public float getLocalLighting(final int x, final int y, final int z) {
        final int lighting = getLightRead(localLightDataWrite, x, y, z);
        return lookupLighting(lighting);
    }

    public Texture getLightTexture() {
        return lightTexture;
    }

    public void setLightTexture(final Texture lightTexture) {
        this.lightTexture = lightTexture;
    }

    public ReadOnlyColorRGBA getCaveLight() {
        return caveLight;
    }

    public void setCaveLight(final ColorRGBA caveLight) {
        this.caveLight.set(caveLight);
        updateLighting = true;
    }

    public ReadOnlyColorRGBA getNightLight() {
        return nightLight;
    }

    public void setNightLight(final ColorRGBA nightLight) {
        this.nightLight.set(nightLight);
        updateLighting = true;
    }

    public ReadOnlyColorRGBA getDayLight() {
        return dayLight;
    }

    public void setDayLight(final ColorRGBA dayLight) {
        this.dayLight.set(dayLight);
        updateLighting = true;
    }

    public ReadOnlyColorRGBA getTorchLight() {
        return torchLight;
    }

    public void setTorchLight(final ColorRGBA torchLight) {
        this.torchLight.set(torchLight);
        updateLighting = true;
    }

    public int getCurrentlyLoadedChunksCount() {
        return isLoaded.size();
    }

    private abstract class UpdateMessage {
        int chunkX;
        int chunkZ;

        @Override
        public String toString() {
            return "UpdateMessage [chunkX=" + chunkX + ", chunkZ=" + chunkZ + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + chunkX;
            result = prime * result + chunkZ;
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final UpdateMessage other = (UpdateMessage) obj;
            if (chunkX != other.chunkX) {
                return false;
            }
            if (chunkZ != other.chunkZ) {
                return false;
            }
            return true;
        }
    }

    private class BlocksMessage extends UpdateMessage {
        List<BlockEditData> blockUpdates;

        public BlocksMessage(final int chunkX, final int chunkZ, final List<BlockEditData> blockUpdates) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.blockUpdates = blockUpdates;
        }

        public BlocksMessage(final int chunkX, final int chunkZ, final BlockEditData singleBlockUpdate) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            blockUpdates = Lists.newArrayList(singleBlockUpdate);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("BlocksMessage [chunkX=");
            builder.append(chunkX);
            builder.append(", chunkZ=");
            builder.append(chunkZ);
            builder.append(", blockUpdates=");
            builder.append(blockUpdates);
            builder.append("]");
            return builder.toString();
        }
    }

    private class ChunkMessage extends UpdateMessage {
        byte[] data;
        byte[] extra;

        public ChunkMessage(final int chunkX, final int chunkZ, final byte[] data, final byte[] extra) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.data = data;
            this.extra = extra;
        }
    }

    private class LightMessage extends UpdateMessage {
        public LightMessage(final int chunkX, final int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }

    private class RebuildMessage extends UpdateMessage {
        public RebuildMessage(final int chunkX, final int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }
}
