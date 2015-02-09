package org.ensim.h24.labycraft.entity;

import com.ardor3d.math.Vector3;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardorcraft.world.BlockWorld;

public interface Entity {

	public void update(final BlockWorld blockScene, final ReadOnlyTimer timer);

	public void setPlayerPosition(Vector3 playerPos);
	public Vector3 getPosition();
}
