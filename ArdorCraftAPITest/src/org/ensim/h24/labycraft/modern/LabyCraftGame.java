/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package org.ensim.h24.labycraft.modern;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.swing.WindowConstants;

import org.ensim.h24.labycraft.entity.Entity;
import org.ensim.h24.labycraft.entity.EntityWithPhysics;

import com.ardor3d.framework.Canvas;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseManager;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyHeldCondition;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.MouseButtonPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.NormalsMode;
import com.ardor3d.scenegraph.shape.Pyramid;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.ardorcraft.base.ArdorCraftGame;
import com.ardorcraft.base.CanvasRelayer;
import com.ardorcraft.collision.IntersectionResult;
import com.ardorcraft.data.Pos;
import com.ardorcraft.generators.DataGenerator;
import com.ardorcraft.network.LocalServerConnection;
import com.ardorcraft.network.LocalServerDataHandler;
import com.ardorcraft.objects.QuadBox;
import com.ardorcraft.objects.SkyDome;
import com.ardorcraft.player.PlayerWithPhysics;
import com.ardorcraft.util.BlockUtil;
import com.ardorcraft.util.geometryproducers.BoxProducer;
import com.ardorcraft.util.geometryproducers.MeshProducer;
import com.ardorcraft.voxel.Voxelator;
import com.ardorcraft.world.BlockSide;
import com.ardorcraft.world.BlockType;
import com.ardorcraft.world.BlockWorld;
import com.ardorcraft.world.IServerConnection;
import com.ardorcraft.world.WorldSettings;
import com.google.common.base.Predicate;

/**
 * A bigger example that will grow over time...
 */
public class LabyCraftGame implements ArdorCraftGame {

    private BlockWorld blockWorld;
    private final int tileSize = 16;
    private final int height = 150;
    private double farPlane = 10000.0;
    private double startTime = -1;
    private ArrayList<Pos> doorPos = new ArrayList<Pos>();
    private ArrayList<Entity> entities= new ArrayList<Entity>();
    
    private final int startX = 100,startY = 51,startZ = 100;
    private final double DayTimeLength = 60;//= 60*10;
    private final double NightTimeLength = 30; //= 60*7;
    private final double OpenDoorTime = (DayTimeLength + NightTimeLength)*0.35;
    private final double SunRisingTime = (DayTimeLength + NightTimeLength)*0.3;
    private final double CloseDoorTime = (DayTimeLength + NightTimeLength)*0.80;
    private final double SunDimmingTime = (DayTimeLength + NightTimeLength)*0.85;
    private final int doorBlockType = 64;
    private final IntersectionResult intersectionResult = new IntersectionResult();

    private final FogState fogState = new FogState();
    private final ColorRGBA fogColor = new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f);
    private final ColorRGBA topColor = new ColorRGBA(0.5f, 0.6f, 1.0f, 1.0f);
    private boolean doorIsClosed = true;
    private CanvasRelayer canvas;
    private Node root;
    private Camera camera;
    private PlayerWithPhysics player;
    private LogicalLayer logicalLayer = null;
    private Node worldNode;
    private Node textNode;
    private SkyDome skyDome;
    private QuadBox selectionBox;

    private int blockType = 1;
    private float globalLight = 1f;
    private boolean isInWater = false;
    private final int[] blockTypeLookup = new int[] {
            1, 47, 4, 5, 20, 95, 12, 45, 48, 50
    };

    @Override
    public void update(final ReadOnlyTimer timer) {
        player.update(blockWorld, timer);
        updateEntities(timer,player.getPosition());
        blockWorld.tracePicking(player.getPosition(), player.getDirection(), 50, intersectionResult);
        if (intersectionResult.hit) {
            final Pos hitPos = intersectionResult.pos;
            selectionBox.setTranslation(hitPos.x + 0.5, hitPos.y + 0.5, hitPos.z + 0.5);
        }

        camera.setLocation(player.getPosition());
        camera.setDirection(player.getDirection());
        camera.setUp(player.getUp());
        camera.setLeft(player.getLeft());

        skyDome.setTranslation(player.getPosition());
        updateWorld(timer);
        updateDayLight(timer);
        updateFog(player.getPosition());
        updateText(timer);
        
        

        // The infinite world update
        blockWorld.updatePlayer(player.getPosition(), player.getDirection());
        blockWorld.update(timer);
    }

    private void updateEntities(ReadOnlyTimer timer,Vector3 playerPos) {
		for(Entity e : entities) {
			e.setPlayerPosition(playerPos);
			
			e.update(blockWorld, timer);
		}
		
	}

	private void updateWorld(ReadOnlyTimer timer) {
       	if ( startTime == -1) {
    		startTime = timer.getTimeInSeconds();
    		return;
    	}

    	double currentTime = timer.getTimeInSeconds();
    	double timeOfTheDay = (currentTime - startTime)%(DayTimeLength + NightTimeLength);
    	if (!doorIsClosed && timeOfTheDay> CloseDoorTime ) {
    		for(Pos pos : doorPos){
    			final BlockSide orientation = getOrientation(doorBlockType);
    			blockWorld.setBlock(pos.x, pos.y, pos.z, doorBlockType, orientation);
    			blockWorld.setBlock(pos.x, pos.y+1, pos.z, doorBlockType, orientation);
    		}
    		doorIsClosed=false;
    	}
    	
    	if (doorIsClosed && timeOfTheDay > OpenDoorTime ) {
    		for(Pos pos : doorPos){
    		blockWorld.setBlock(pos.x, pos.y, pos.z, 0);
    		blockWorld.setBlock(pos.x, pos.y+1, pos.z, 0);
    		}
    		doorIsClosed=true;
    	}
    }
    private void updateText(ReadOnlyTimer timer) {
    	if ( startTime == -1) {
    		startTime = timer.getTimeInSeconds();
    		return;
    	}

    	double currentTime = timer.getTimeInSeconds();
    	double timeOfTheDay = (currentTime - startTime)%(DayTimeLength + NightTimeLength);
    	
    	if (timeOfTheDay >= 10 && timeOfTheDay < 11){
			textNode.detachAllChildren();
			createText("+", canvas.getCanvasRenderer().getCamera().getWidth() / 2 - 5, canvas.getCanvasRenderer()
	                .getCamera().getHeight() / 2 - 10);
    	}
		if (timeOfTheDay> CloseDoorTime ) {
			textNode.detachAllChildren();
			createText("Its Night", 10, 10);
			createText("+", canvas.getCanvasRenderer().getCamera().getWidth() / 2 - 5, canvas.getCanvasRenderer()
	                .getCamera().getHeight() / 2 - 10);
			//System.out.println("Time of the day =  Its Night");
		}
		if (timeOfTheDay> OpenDoorTime) {
			textNode.detachAllChildren();
			createText("+", canvas.getCanvasRenderer().getCamera().getWidth() / 2 - 5, canvas.getCanvasRenderer()
	                .getCamera().getHeight() / 2 - 10);
		//	System.out.println("Time of the day =  END Of  Night");
		}		
		
	}

	private void updateDayLight(ReadOnlyTimer timer) {
    	if ( startTime == -1) {
    		startTime = timer.getTimeInSeconds();
    		return;
    	}
    	double currentTime = timer.getTimeInSeconds();
    	double timeOfTheDay = (currentTime - startTime)%(DayTimeLength + NightTimeLength);
    	if( timeOfTheDay > CloseDoorTime  || timeOfTheDay < SunRisingTime){
    		globalLight = 0;
    		//System.out.println("Light To ZERO"); 
    	}
    	if( timeOfTheDay > OpenDoorTime  && timeOfTheDay < CloseDoorTime){
    		globalLight = 1;
    		//System.out.println("Light To UN");
    	}
    	if ( timeOfTheDay >= CloseDoorTime  && timeOfTheDay <= SunDimmingTime)
    		globalLight = (float)(1 +( (timeOfTheDay - CloseDoorTime)/(CloseDoorTime - SunDimmingTime)));
    	if ( timeOfTheDay <= OpenDoorTime  && timeOfTheDay >= SunRisingTime)
    		globalLight = (float)(( (timeOfTheDay - SunRisingTime)/(OpenDoorTime - SunRisingTime)));
    	blockWorld.setGlobalLight(globalLight);
    	//System.out.println("Time of the day = "+ timeOfTheDay + " light = "+ globalLight);
	}
    

	@Override
    public void render(final Renderer renderer) {
        // root.draw(renderer);

        // Taking over the drawing to draw in specific order without performance
        // hogging renderqueue sorting...
        skyDome.draw(renderer);
        worldNode.draw(renderer);
        if (intersectionResult.hit) {
            selectionBox.draw(renderer);
        }
        textNode.draw(renderer);
    }

    @Override
    public void init(final Node root, final CanvasRelayer canvas, final LogicalLayer logicalLayer,
            final PhysicalLayer physicalLayer, final MouseManager mouseManager) {
        this.root = root;
        this.canvas = canvas;
        this.logicalLayer = logicalLayer;

        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    LabyCraftGame.class, "com/ardorcraft/resources"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        canvas.setTitle("ArdorCraft API Example - RealGame.java");

        final SelectDialog dialog = new SelectDialog();
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        DataGenerator dataGenerator = null;
        try {
            dataGenerator = (DataGenerator) dialog.getSelectedGenerator().newInstance();
        	//dataGenerator = new LabyDataGenerator(20, 1);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        if ( dataGenerator instanceof LabyDataGenerator) {
        	doorPos = ((LabyDataGenerator)dataGenerator).getDoorsPos();
        }
        final String texture = dialog.getSelectedTexture();
        final int textureTileSize = dialog.getSelectedTextureSize();
        final boolean doOverwriteMap = dialog.getIsOverwriteMap();
        final int gridSize = dialog.getViewDistance();

        farPlane = (gridSize - 1) / 2 * tileSize;

        camera = canvas.getCanvasRenderer().getCamera();
        camera.setFrustumPerspective(75.0, (float) camera.getWidth() / (float) camera.getHeight(), 0.1, farPlane);

        setupFog();

        // Create player object
        player = new PlayerWithPhysics(logicalLayer);
        player.getPosition().set(startX, startY, startZ);
        player.setWalking(true);

        registerTriggers(logicalLayer, mouseManager);

        // Map file to use
        final File worldFileSource = new File(dialog.getSelectedGenerator().getSimpleName() + "_Map.acr");
        // Uncomment this if you want to start your mapfile from scratch each run...
        if (doOverwriteMap && worldFileSource.exists()) {
            worldFileSource.delete();
        }

        // Create main blockworld handler
        final WorldSettings settings = new WorldSettings();

        // Here you can load any terrain texture you wish (should contain 16x16 tiles).
        // Just make sure you set the correct tilesize, that is, the subtexture size in pixels.
        settings.setTerrainTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, texture));
        settings.setTerrainTextureTileSize(textureTileSize);

        settings.setWaterTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "water.png"));
        settings.setTileSize(tileSize);
        settings.setTileHeight(height);
        settings.setGridSize(gridSize);

        final IServerConnection serverConnection = new LocalServerConnection(new LocalServerDataHandler(tileSize,
                height, gridSize, dataGenerator, worldFileSource));
        settings.setServerConnection(serverConnection);

        blockWorld = new BlockWorld(settings);

        // Set block 45 (brickblock) to be a pyramid drawn with the meshproducer
        final BlockUtil blockUtil = blockWorld.getBlockUtil();
        final int blockId = 45;
        blockUtil.setBlockMapping(blockId, 7, 0); // brick block tile coords
        blockUtil.setBlockType(blockId, BlockType.Transparent); // Not covering the entire box block = not solid
        final Mesh mesh = new Pyramid("pyramid", 1.0, 1.0);
        final MeshProducer meshProducer = new MeshProducer(mesh);
        meshProducer.createOrientations(); // create all permutation rotations of the mesh
        meshProducer.setTransformTextureCoords(true); // transform 0-1 texcoords to the specific tile
        blockUtil.setGeometryProducer(blockId, meshProducer);

        worldNode = blockWorld.getWorldNode();
        root.attachChild(worldNode);

        skyDome = new SkyDome("Dome", 8, 8, 10);
        root.attachChild(skyDome);

        textNode = new Node("text");
        root.attachChild(textNode);
        createText("+", canvas.getCanvasRenderer().getCamera().getWidth() / 2 - 5, canvas.getCanvasRenderer()
                .getCamera().getHeight() / 2 - 10);
        createText("[Y/H] Change time of day", 10, 10);
        createText("[V] Voxelate a mesh at current target pos", 10, 30);
        createText("[F] Fly/Walk", 10, 50);
        createText("[0..9] Select blocktype (9=torch)", 10, 70);
        createText("[LMB/RMB] Add/Remove block", 10, 90);

        // Create box to show selected box
        selectionBox = new QuadBox("SelectionBox", new Vector3(), 0.501, 0.501, 0.501);
        selectionBox.getSceneHints().setNormalsMode(NormalsMode.Off);
        selectionBox.setDefaultColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 0.4f));
        selectionBox.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        final BlendState bs = new BlendState();
        bs.setBlendEnabled(true);
        selectionBox.setRenderState(bs);
        final WireframeState ws = new WireframeState();
        ws.setLineWidth(2);
        selectionBox.setRenderState(ws);
        selectionBox.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        root.attachChild(selectionBox);

        updateLighting();

        blockWorld.startThreads();
    }

    private void createText(final String text, final int x, final int y) {
        final BasicText info = BasicText.createDefaultTextLabel("Text2", text, 16);
        info.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        info.setTranslation(new Vector3(x, y, 0));
        textNode.attachChild(info);
    }

    private void updateLighting() {
        final float light = globalLight * 0.9f + 0.1f;
        final ReadOnlyColorRGBA newColor = new ColorRGBA(fogColor).multiplyLocal(light);
        fogState.setColor(newColor);
        skyDome.getMidColor().set(newColor);
        skyDome.getTopColor().set(topColor).multiplyLocal(light);
        skyDome.updateColors();

        GameTaskQueueManager.getManager(ContextManager.getCurrentContext()).getQueue(GameTaskQueue.RENDER)
                .enqueue(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        canvas.getCanvasRenderer().getRenderer().setBackgroundColor(newColor);
                        return true;
                    }
                });
    }

    private void setupFog() {
        fogState.setDensity(1.0f);
        fogState.setEnabled(true);
        fogState.setEnd((float) farPlane);
        fogState.setStart((float) farPlane / 3.0f);
        fogState.setDensityFunction(FogState.DensityFunction.Linear);
        fogState.setQuality(FogState.Quality.PerPixel);
        root.setRenderState(fogState);
    }

    private void updateFog(final Vector3 position) {
        final int block = blockWorld.getBlock((int) position.getX(), (int) (position.getY() + 0.15),
                (int) position.getZ());
        if (block == BlockWorld.WATER && !isInWater) {
            isInWater = true;
            fogColor.set(0.1f, 0.13f, 0.25f, 1.0f);
            topColor.set(0.15f, 0.2f, 0.35f, 1.0f);
            fogState.setStart(0);
            fogState.setEnd((float) farPlane / 8);
            updateLighting();
        } else if (block == 0 && isInWater) {
            isInWater = false;
            fogColor.set(1.0f, 1.0f, 1.0f, 1.0f);
            topColor.set(0.5f, 0.6f, 1.0f, 1.0f);
            fogState.setEnd((float) farPlane);
            fogState.setStart((float) farPlane / 3.0f);
            updateLighting();
        }
    }

    private void registerTriggers(final LogicalLayer logicalLayer, final MouseManager mouseManager) {
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.LEFT),
                new TriggerAction() {
                    @Override
                    public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                        addBlock();
                    }
                }));

        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.RIGHT),
                new TriggerAction() {
                    @Override
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        removeBlock();
                    }
                }));

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                player.setWalking(!player.isWalking());
            }
        }));

        logicalLayer.registerTrigger(new InputTrigger(new KeyHeldCondition(Key.SPACE), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                player.jump();
            }
        }));

        final Predicate<TwoInputStates> numberPressed = new Predicate<TwoInputStates>() {
            @Override
            public boolean apply(final TwoInputStates states) {
                final char keyChar = states.getCurrent().getKeyboardState().getKeyEvent().getKeyChar();
                if (Character.isDigit(keyChar)) {
                    blockType = blockTypeLookup[Character.digit(keyChar, 10)];
                    return true;
                }
                return false;
            }
        };
        logicalLayer.registerTrigger(new InputTrigger(numberPressed, new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {}
        }));

        logicalLayer.registerTrigger(new InputTrigger(new KeyHeldCondition(Key.Y), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                globalLight = (float) Math.min(globalLight + tpf * 0.4, 1);
                blockWorld.setGlobalLight(globalLight);
                updateLighting();
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyHeldCondition(Key.H), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                globalLight = (float) Math.max(globalLight - tpf * 0.4, 0);
                blockWorld.setGlobalLight(globalLight);
                updateLighting();
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyHeldCondition(Key.M), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                createMob();
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.V), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                if (intersectionResult.hit) {
                    final Pos addPos = intersectionResult.oldPos;
                    final Voxelator voxelator = new Voxelator(blockWorld, 50, 50, 50);
                    voxelator.voxelate(addPos, new Teapot(), 1.0f, 43);
                }
            }
        }));

        if (mouseManager.isSetGrabbedSupported()) {
            mouseManager.setGrabbed(GrabbedState.GRABBED);
        }
    }

    @Override
    public void destroy() {
        blockWorld.stopThreads();
    }

    @Override
    public void resize(final int newWidth, final int newHeight) {}
    
    
    private void createMob() {
    	 if (intersectionResult.hit) {
             final Pos addPos = intersectionResult.oldPos;
             if (!player.isPlayerSpace(addPos))  {
	             EntityWithPhysics ewp = new EntityWithPhysics(logicalLayer, addPos);
	             final BlockSide orientation = getOrientation(blockType);
	             blockWorld.setBlock((int)addPos.x, (int)addPos.y, (int)addPos.z, 86,orientation);
	             entities.add(ewp);
    	 }
    	 }
    }

    private void addBlock() {
        if (intersectionResult.hit) {
            final Pos addPos = intersectionResult.oldPos;
            if (!player.isPlayerSpace(addPos)) {
                final BlockSide orientation = getOrientation(blockType);
                blockWorld.setBlock(addPos.x, addPos.y, addPos.z, blockType, orientation);
            }
        }
    }

    private void removeBlock() {
        if (intersectionResult.hit) {
            final Pos deletePos = intersectionResult.pos;
            blockWorld.setBlock(deletePos.x, deletePos.y, deletePos.z, 0);

            final int aboveBlock = blockWorld.getBlock(deletePos.x, deletePos.y + 1, deletePos.z);
            if (aboveBlock > 99 && aboveBlock < 108) {
                blockWorld.setBlock(deletePos.x, deletePos.y + 1, deletePos.z, 0);
            }
        }
    }

    private BlockSide getOrientation(final int blockId) {
        BlockSide orientation = BlockSide.Front;

        if (blockWorld.getBlockUtil().getGeometryProducer(blockId) instanceof BoxProducer) {
            final Vector3 dir = player.getDirection();
            if (Math.abs(dir.getX()) > Math.abs(dir.getZ())) {
                if (dir.getX() < 0) {
                    orientation = BlockSide.Right;
                } else {
                    orientation = BlockSide.Left;
                }
            } else {
                if (dir.getZ() < 0) {
                    orientation = BlockSide.Front;
                } else {
                    orientation = BlockSide.Back;
                }
            }
        } else {
            final int xDir = intersectionResult.oldPos.x - intersectionResult.pos.x;
            final int yDir = intersectionResult.oldPos.y - intersectionResult.pos.y;
            final int zDir = intersectionResult.oldPos.z - intersectionResult.pos.z;
            if (xDir != 0) {
                if (xDir > 0) {
                    orientation = BlockSide.Right;
                } else {
                    orientation = BlockSide.Left;
                }
            } else if (yDir != 0) {
                if (yDir > 0) {
                    orientation = BlockSide.Bottom;
                } else {
                    orientation = BlockSide.Top;
                }
            } else if (zDir != 0) {
                if (zDir > 0) {
                    orientation = BlockSide.Front;
                } else {
                    orientation = BlockSide.Back;
                }
            }
        }

        return orientation;
    }
}
