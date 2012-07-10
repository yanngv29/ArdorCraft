/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardorcraft.control;

import com.ardor3d.framework.Canvas;
import com.ardor3d.input.Key;
import com.ardor3d.input.KeyboardState;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TriggerConditions;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardorcraft.player.PlayerBase;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class WalkControl {

	private final Vector3 _upAxis = new Vector3();
	private double _mouseRotateSpeed = .003;
	private double _moveSpeed = 0.8;
	private double _keyRotateSpeed = 2.25;
	private final Matrix3 _workerMatrix = new Matrix3();
	private final Vector3 _workerStoreA = new Vector3();
	private InputTrigger _mouseTrigger;
	private InputTrigger _keyTrigger;
	private final LogicalLayer layer;
	private final PlayerBase player;

	public WalkControl(final PlayerBase player, final LogicalLayer layer,
			final ReadOnlyVector3 upAxis) {
		_upAxis.set(upAxis);
		this.layer = layer;
		this.player = player;
	}

	public ReadOnlyVector3 getUpAxis() {
		return _upAxis;
	}

	public void setUpAxis(final ReadOnlyVector3 upAxis) {
		_upAxis.set(upAxis);
	}

	public double getMouseRotateSpeed() {
		return _mouseRotateSpeed;
	}

	public void setMouseRotateSpeed(final double speed) {
		_mouseRotateSpeed = speed;
	}

	public double getMoveSpeed() {
		return _moveSpeed;
	}

	public void setMoveSpeed(final double speed) {
		_moveSpeed = speed;
	}

	public double getKeyRotateSpeed() {
		return _keyRotateSpeed;
	}

	public void setKeyRotateSpeed(final double speed) {
		_keyRotateSpeed = speed;
	}

	protected void move(final KeyboardState kb, final double tpf) {
		// MOVEMENT
		int moveFB = 0, strafeLR = 0;
		if (kb.isDown(Key.W)) {
			moveFB += 1;
		}
		if (kb.isDown(Key.S)) {
			moveFB -= 1;
		}
		if (kb.isDown(Key.A)) {
			strafeLR += 1;
		}
		if (kb.isDown(Key.D)) {
			strafeLR -= 1;
		}

		if (moveFB != 0 || strafeLR != 0) {
			final Vector3 loc = _workerStoreA.zero();
			if (moveFB == 1) {
				loc.addLocal(player.getDirection());
			} else if (moveFB == -1) {
				loc.subtractLocal(player.getDirection());
			}
			if (strafeLR == 1) {
				loc.addLocal(player.getLeft());
			} else if (strafeLR == -1) {
				loc.subtractLocal(player.getLeft());
			}
			loc.setY(0);
			loc.normalizeLocal();
			player.getAcceleration().set(loc).multiplyLocal(_moveSpeed);

			// TODO
			// player.getTargetVelocity().x = loc.getX() * 80 *
			// PhysicsSystem.TIMESTEP;
			// player.getTargetVelocity().z = loc.getZ() * 80 *
			// PhysicsSystem.TIMESTEP;
		}

		if (kb.isDown(Key.SPACE)) {
			// TODO
			// player.jump();
		}

		// ROTATION
		int rotX = 0, rotY = 0;
		if (kb.isDown(Key.UP)) {
			rotY -= 1;
		}
		if (kb.isDown(Key.DOWN)) {
			rotY += 1;
		}
		if (kb.isDown(Key.LEFT)) {
			rotX += 1;
		}
		if (kb.isDown(Key.RIGHT)) {
			rotX -= 1;
		}
		if (rotX != 0 || rotY != 0) {
			rotate(rotX * _keyRotateSpeed / _mouseRotateSpeed * tpf, rotY
					* _keyRotateSpeed / _mouseRotateSpeed * tpf);
		}
	}

	protected void rotate(final double dx, final double dy) {

		if (dx != 0) {
			_workerMatrix.fromAngleNormalAxis(_mouseRotateSpeed * dx,
					_upAxis != null ? _upAxis : player.getUp());
			_workerMatrix.applyPost(player.getLeft(), _workerStoreA);
			player.getLeft().set(_workerStoreA);
			_workerMatrix.applyPost(player.getDirection(), _workerStoreA);
			player.getDirection().set(_workerStoreA);
			_workerMatrix.applyPost(player.getUp(), _workerStoreA);
			player.getUp().set(_workerStoreA);
		}

		if (dy != 0) {
			_workerMatrix.fromAngleNormalAxis(_mouseRotateSpeed * dy,
					player.getLeft());
			_workerMatrix.applyPost(player.getLeft(), _workerStoreA);
			player.getLeft().set(_workerStoreA);
			_workerMatrix.applyPost(player.getDirection(), _workerStoreA);
			player.getDirection().set(_workerStoreA);
			_workerMatrix.applyPost(player.getUp(), _workerStoreA);
			player.getUp().set(_workerStoreA);
		}

		player.normalize();
	}

	/**
	 * @param layer
	 *            the logical layer to register with
	 * @param upAxis
	 *            the up axis of the camera
	 * @param dragOnly
	 *            if true, mouse input will only rotate the camera if one of the
	 *            mouse buttons (left, center or right) is down.
	 * @return a new FlyControl object
	 */
	public static WalkControl setupTriggers(final PlayerBase player,
			final LogicalLayer layer, final ReadOnlyVector3 upAxis,
			final boolean dragOnly) {

		final WalkControl control = new WalkControl(player, layer, upAxis);
		control.setupKeyboardTriggers(layer);
		control.setupMouseTriggers(layer, dragOnly);
		return control;
	}

	/**
	 * Deregister the triggers of the given FlyControl from the given
	 * LogicalLayer.
	 * 
	 * @param layer
	 * @param control
	 */
	public static void removeTriggers(final LogicalLayer layer,
			final WalkControl control) {
		if (control._mouseTrigger != null) {
			layer.deregisterTrigger(control._mouseTrigger);
		}
		if (control._keyTrigger != null) {
			layer.deregisterTrigger(control._keyTrigger);
		}
	}

	public void setupMouseTriggers(final LogicalLayer layer,
			final boolean dragOnly) {
		final WalkControl control = this;
		// Mouse look
		final Predicate<TwoInputStates> someMouseDown = Predicates.or(
				TriggerConditions.leftButtonDown(), Predicates.or(
						TriggerConditions.rightButtonDown(),
						TriggerConditions.middleButtonDown()));
		final Predicate<TwoInputStates> dragged = Predicates.and(
				TriggerConditions.mouseMoved(), someMouseDown);
		final TriggerAction dragAction = new TriggerAction() {

			// Test boolean to allow us to ignore first mouse event. First event
			// can wildly vary based on platform.
			private boolean firstPing = true;

			@Override
			public void perform(final Canvas source,
					final TwoInputStates inputStates, final double tpf) {
				final MouseState mouse = inputStates.getCurrent()
						.getMouseState();
				if (mouse.getDx() != 0 || mouse.getDy() != 0) {
					if (!firstPing) {
						control.rotate(-mouse.getDx(), -mouse.getDy());
					} else {
						firstPing = false;
					}
				}
			}
		};

		_mouseTrigger = new InputTrigger(dragOnly ? dragged
				: TriggerConditions.mouseMoved(), dragAction);
		layer.registerTrigger(_mouseTrigger);
	}

	public Predicate<TwoInputStates> setupKeyboardTriggers(
			final LogicalLayer layer) {

		final WalkControl control = this;

		// WASD control
		final Predicate<TwoInputStates> keysHeld = new Predicate<TwoInputStates>() {
			Key[] keys = new Key[] { Key.W, Key.A, Key.S, Key.D, Key.LEFT,
					Key.RIGHT, Key.UP, Key.DOWN, Key.SPACE };

			@Override
			public boolean apply(final TwoInputStates states) {
				for (final Key k : keys) {
					if (states.getCurrent() != null
							&& states.getCurrent().getKeyboardState().isDown(k)) {
						return true;
					}
				}
				return false;
			}
		};

		final TriggerAction moveAction = new TriggerAction() {
			@Override
			public void perform(final Canvas source,
					final TwoInputStates inputStates, final double tpf) {
				control.move(inputStates.getCurrent().getKeyboardState(), tpf);
			}
		};
		_keyTrigger = new InputTrigger(keysHeld, moveAction);
		layer.registerTrigger(_keyTrigger);
		return keysHeld;
	}

	public InputTrigger getKeyTrigger() {
		return _keyTrigger;
	}

	public InputTrigger getMouseTrigger() {
		return _mouseTrigger;
	}

	public void enable() {
		if (!layer.getTriggers().contains(_keyTrigger)) {
			layer.registerTrigger(_keyTrigger);
		}
		if (!layer.getTriggers().contains(_mouseTrigger)) {
			layer.registerTrigger(_mouseTrigger);
		}
	}

	public void disable() {
		if (layer.getTriggers().contains(_keyTrigger)) {
			layer.deregisterTrigger(_keyTrigger);
		}
		if (layer.getTriggers().contains(_mouseTrigger)) {
			layer.deregisterTrigger(_mouseTrigger);
		}
	}
}
