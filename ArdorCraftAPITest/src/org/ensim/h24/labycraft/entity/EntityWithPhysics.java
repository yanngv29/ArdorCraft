
package org.ensim.h24.labycraft.entity;

import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardorcraft.control.FlyControl;
import com.ardorcraft.control.WalkControl;
import com.ardorcraft.data.Pos;
import com.ardorcraft.pathfinding.ConstrainedAStar;
import com.ardorcraft.pathfinding.PathNode;
import com.ardorcraft.pathfinding.PathResult;
import com.ardorcraft.player.PlayerWithCollision;
import com.ardorcraft.world.BlockWorld;

/**
 * A player implementation with walk/fly modes and gravity and jumping.
 */
public class EntityWithPhysics extends PlayerWithCollision implements Entity {
    private boolean jump = false;
    private double time;
    private final double stepTime = 1.0/2.0;//1.0 / 100.0;
    private boolean walking = false;
    private final Vector3 damping = new Vector3(0.85, 1.0, 0.85);
    private final double gravity = -0.3;
    private final double jumpForce = 9;
    private final double speed = 0.9;
    private Vector3 playerPosition;
    private final Vector3 OldPosition = new Vector3(0.85, 1.0, 0.85);

    public EntityWithPhysics(final LogicalLayer logicalLayer, Pos addPos) {
    	currentLocation.set(addPos.x, addPos.y, addPos.z);
       
    }

    @Override
    public void update(final BlockWorld blockScene, final ReadOnlyTimer timer) {
        final double tpf = timer.getTimePerFrame();

        time += tpf;

        keepAboveGround(blockScene);

        int ticks = 0;
        while (time > stepTime) {
            final double step = stepTime;
            OldPosition.set(getPosition());
            
            getVelocity().addLocal(getAcceleration().multiply(speed, null));
            if (walking) {
                getVelocity().addLocal(0, gravity, 0);
                if (jump && isOnGround()) {
                    getVelocity().addLocal(0, jumpForce, 0);
                }
            }
            getPosition().addLocal(getVelocity().multiply(step, null));
            ConstrainedAStar pathFinder = new ConstrainedAStar(blockScene, 18); 
            PathResult pathResult = pathFinder.findPath((int)getPosition().getX(), (int)getPosition().getY(), (int)getPosition().getZ(),
            		(int)playerPosition.getX(), (int)playerPosition.getY(), (int)playerPosition.getZ());
            if (pathResult != null && pathResult.getPath().size() >1) {
            	PathNode pn = pathResult.getPath().get(1);
            
            	getPosition().set(pn.getX(), pn.getY(), pn.getZ());
            	System.out.println("newPOS="+getPosition());
            }
            getVelocity().multiplyLocal(damping);

            // Update collision etc
            super.update(blockScene, timer);
            
           if (!OldPosition.equals(getPosition())) {
            	blockScene.setBlock((int)OldPosition.getX(),(int) OldPosition.getY(), (int)OldPosition.getZ(), 0);
            	blockScene.setBlock((int)getPosition().getX(),(int) getPosition().getY(), (int)getPosition().getZ(), 86);
            	System.out.println("changement de position du monstre OLD="+OldPosition+ " new="+getPosition());
            }

            time -= stepTime;
            ticks++;
            jump = false;
        }
        if (ticks > 0) {
            getAcceleration().set(0, 0, 0);
        }
        jump = false;
    }

    private void keepAboveGround(final BlockWorld blockScene) {
        final int X = (int) MathUtils.floor(position.getX());
        final int Y = (int) MathUtils.floor(position.getY());
        final int Z = (int) MathUtils.floor(position.getZ());

        int block = blockScene.getBlock(X, Y, Z);
        if (block != 0 && blockScene.getBlockUtil().getIsCollidable(block)) {
            for (int y = Y; y < blockScene.getHeight(); y++) {
                block = blockScene.getBlock(X, y, Z);
                if (block == 0 || !blockScene.getBlockUtil().getIsCollidable(block)) {
                    position.setY(y + 2);
                    break;
                }
            }
        }
    }

    double t = 0;

    public void jump() {
        if (!jump && isOnGround()) {
            jump = true;
        }
    }

    public boolean isWalking() {
        return walking;
    }

    public void setWalking(final boolean walking) {
        this.walking = walking;
    
    }

    public void enableControls() {
        setWalking(walking);
    }

	@Override
	public void setPlayerPosition(Vector3 playerPos) {
		playerPosition = playerPos;
		
	}
}
