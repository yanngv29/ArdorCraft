package org.ensim.h24.labycraft.modern;

import java.util.ArrayList;

import org.ensim.h24.labycraft.modern.generator.NiceCustomDataGenerator;

import com.ardor3d.math.MathUtils;
import com.ardorcraft.data.Pos;
import com.ardorcraft.generators.DataGenerator;
import com.ardorcraft.world.BlockWorld;
import com.ardorcraft.world.WorldModifier;


public class LabyDataGenerator implements DataGenerator {

    private int nrLayers = 1;
    protected int waterHeight = 2;
	private NiceCustomDataGenerator niceDataGenerator = new NiceCustomDataGenerator(1);
	//labySize doit être impair
	private final int labySize = 201;
	
	public LabyDataGenerator() {
		//super(1, 2);
		mazeMap= maze.generate();
		limiteInsideDoors();
		maze.debug();
	}
	
	private ArrayList<Pos> doorPos = new ArrayList<Pos>();
	
	public ArrayList<Pos> getDoorsPos() {
		return doorPos;
	}
	private void limiteInsideDoors() {
		int freeZoneLimite = labySize/3;
		int i = freeZoneLimite;
		boolean doorOpen = false;
		for(int j=freeZoneLimite; j<(freeZoneLimite*2); j++)
		{
				if( !doorOpen && mazeMap[(i-1)*labySize+j]==Maze.BLANK) {
					mazeMap[i*labySize+j]=Maze.FREEZONEDOOR;
					doorPos.add(new Pos(j,51,i));
					doorOpen = true;
				}
				else mazeMap[i*labySize+j]=Maze.WALL;

		}
		doorOpen = false;
		i = freeZoneLimite*2;	
		for(int j=freeZoneLimite; j<(freeZoneLimite*2); j++)
		{
			if( !doorOpen && mazeMap[(i+1)*labySize+j]==Maze.BLANK) {
				mazeMap[i*labySize+j]=Maze.FREEZONEDOOR;
				doorPos.add(new Pos(j,51,i));
				doorOpen = true;
			}
			else mazeMap[i*labySize+j]=Maze.WALL;

		}
		doorOpen = false;
		int j = freeZoneLimite;
		for(i=freeZoneLimite; i<(freeZoneLimite*2); i++)
		{
				if( !doorOpen && mazeMap[(i)*labySize+(j-1)]==Maze.BLANK) {
					mazeMap[i*labySize+j]=Maze.FREEZONEDOOR;
					doorPos.add(new Pos(i,50,j));
					doorOpen = true;
				}
				else mazeMap[i*labySize+j]=Maze.WALL;

		}
		doorOpen = false;
		j = freeZoneLimite*2;
		for(i=freeZoneLimite; i<(freeZoneLimite*2); i++)
		{
				if( !doorOpen && mazeMap[(i)*labySize+(j+1)]==Maze.BLANK) {
					mazeMap[i*labySize+j]=Maze.FREEZONEDOOR;
					doorPos.add(new Pos(i,50,j));
					doorOpen = true;
				}
				else mazeMap[i*labySize+j]=Maze.WALL;

		}		
		
	}

	public LabyDataGenerator(int nrLayers, int waterHeight) {
		//super(nrLayers, waterHeight);
		mazeMap= maze.generate();
		maze.debug();
		this.nrLayers = nrLayers;
		this.waterHeight = waterHeight;
	}
	
	
	
	
	@Override
    public void generateChunk(final int xStart, final int zStart, final int xEnd, final int zEnd, int spacing,
	            final int height, final WorldModifier blockScene) {
			niceDataGenerator.generateChunk(xStart, zStart, xEnd, zEnd, spacing, height, blockScene);
	        for (int x = xStart; x < xEnd; x++) {
	            for (int z = zStart; z < zEnd; z++) {
	                generateColumn(x, z, height, blockScene);
	            }
	        }
	    }

	    private void generateColumn(final int x, final int z, final int height, final WorldModifier blockScene) {
	        int startHeight = 1;
	        blockScene.setBlock(x, 0, z, 4);
	        if ( isLaby(x,0,z)) {
	        for (int i = 0; i < nrLayers; i++) {
	            final int localHeight = Math.max(0, getLayerHeight(i, x, startHeight, z, blockScene,height));
	            final int type = getLayerType(i, x, z, blockScene);

	            for (int y = startHeight; y < startHeight + localHeight && y < height; y++) {
	                if (!isCave(x, y, z, blockScene)) {
	                    blockScene.setBlock(x, y, z, type);
	                } else if (y < waterHeight) {
	                    blockScene.setBlock(x, y, z, BlockWorld.WATER);
	                } else {
	                    blockScene.setBlock(x, y, z, 0);
	                }
	            }
	            startHeight += localHeight;
	        }
	        for (int y = startHeight; y < height; y++) {
	            if (y < waterHeight) {
	                blockScene.setBlock(x, y, z, BlockWorld.WATER);
	            } else {
	                blockScene.setBlock(x, y, z, 0);
	            }
	        }
	        } else {
	        	
	        }
	       
	    }
	    

	
	public boolean isCave(int x, int y, int z, WorldModifier blockScene) {
		// TODO Auto-generated method stub
		return false;
	}

	
	public int getLayerType(int layer, int x, int z, WorldModifier blockScene) {
		// TODO Auto-generated method stub
		if (isWall(layer, x, 0, z)) {
			return 4;
		} else if (isDoor(layer, x, 0, z)) {
			return 64;
		}
		return MathUtils.rand.nextInt(3) + 1;
	}

	
	public int getLayerHeight(int layer, int x, int y, int z,
			WorldModifier blockScene, int heightMax) {
		
		int height = getCurrentHeightFor(blockScene,x,z,heightMax);
		if (isWall(layer, x, y, z)) {
			height += 4;
//			if (y == 1) 
//					System.out.println("IS WALL layer="+layer+" x="+x+" y="+y+" z="+z);
		} else if (isDoor(layer, x, 0, z)) {
			return 54;
		}
		
		return height;
	}
	private int getCurrentHeightFor(WorldModifier blockScene,int x, int z,int heightMax) {
		for(int i=heightMax-1;i>0;i-- ) {
			if (blockScene.getBlock(x, i, z)> 0)
				return i;
		}
		return 0;
	}
	private boolean isWall(int layer, int x, int y, int z) {
		if ( x >= 0 && z >= 0  && z<labySize && x<labySize) {
				byte wallOrNot = mazeMap[z*(labySize)+x];
				if (wallOrNot == Maze.WALL)
					return true;
		}
		return false;
	}
	
	private boolean isDoor(int layer, int x, int y, int z) {
		if ( x >= 0 && z >= 0  && z<labySize && x<labySize) {
				byte wallOrNot = mazeMap[z*(labySize)+x];
				if (wallOrNot == Maze.FREEZONEDOOR)
					return true;
		}
		return false;
	}
	private boolean isLaby( int x, int y, int z) {
		if ( x >= 0 && z >= 0  && z<labySize && x<labySize) {
			return true;
		}
		return false;
	}
	
	
	
	private Maze maze = new Maze(labySize,labySize);
	byte[] mazeMap; 
	
}
