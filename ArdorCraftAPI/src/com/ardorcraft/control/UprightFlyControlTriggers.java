package com.ardorcraft.control;

import com.ardor3d.framework.Canvas;
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

/**
 * 
 * Triggers for flying that enforce
 * an upright position.
 * @author nathan
 *
 */
public class UprightFlyControlTriggers implements ITriggerGroup
{

    private final Vector3 upAxis = new Vector3();
    private double mouseRotateSpeed = .005;
    private double moveSpeed = 15;
    private double keyRotateSpeed = 2.25;
    private final Matrix3 workerMatrix = new Matrix3();
    private final Vector3 workerStoreA = new Vector3();
    private final InputTrigger moveTrigger;
    private final InputTrigger turnTrigger;
    private final PlayerBase player;
    
    // The first mouse state is pretty random,
    // so arrange to ignore it.  I'm not sure
    // this should be there, but I'm not sure
    // if Ardor3D allows not firing the
    // triggers on the first update.
    private boolean firstPing = true;
    
    private Predicate<TwoInputStates> mouseTurnPredicate;
    
    private SomeButtonsDownPredicate moveLeftPredicate;
    private SomeButtonsDownPredicate moveRightPredicate;
    private SomeButtonsDownPredicate moveForwardPredicate;
    private SomeButtonsDownPredicate moveBackPredicate;
    private SomeButtonsDownPredicate turnLeftPredicate;
    private SomeButtonsDownPredicate turnRightPredicate;
    private SomeButtonsDownPredicate turnUpPredicate;
    private SomeButtonsDownPredicate turnDownPredicate;
    private UprightFPSMoveConfig moveConfig;

    /**
     * Creates a trigger set for flying
     * in an upright first person shooter.
     * @param playerParam
     *      The player object to update
     * @param upAxisParam
     *      The up axis for the player
     * @param mouseLookDragOnly
     *      Whether the player needs to
     *      press a mouse button to
     *      turn.
     * @param moveConfigParam
     *      The keys and possibly mouse
     *      buttons to manage movement.
     */
    public UprightFlyControlTriggers(
            final PlayerBase playerParam,
            final ReadOnlyVector3 upAxisParam,
            final boolean mouseLookDragOnly,
            final UprightFPSMoveConfig moveConfigParam)
    {
        player = playerParam;
        upAxis.set(upAxisParam);
        moveConfig = moveConfigParam;
        
        moveLeftPredicate =
                new SomeButtonsDownPredicate(moveConfig.MoveLeft);
        moveRightPredicate =
                new SomeButtonsDownPredicate(moveConfig.MoveRight);
        moveForwardPredicate =
                new SomeButtonsDownPredicate(moveConfig.MoveForward);
        moveBackPredicate =
                new SomeButtonsDownPredicate(moveConfig.MoveBack);
        turnLeftPredicate =
                new SomeButtonsDownPredicate(moveConfig.TurnLeft);
        turnRightPredicate =
                new SomeButtonsDownPredicate(moveConfig.TurnRight);
        turnUpPredicate =
                new SomeButtonsDownPredicate(moveConfig.TurnUp);
        turnDownPredicate =
                new SomeButtonsDownPredicate(moveConfig.TurnDown);
          
        moveTrigger = CreateMoveTrigger();
        
        mouseTurnPredicate = CreateMouseTurnPredicate(
                mouseLookDragOnly);
                
        turnTrigger = CreateTurnTrigger();
    }
    
    /**
     * Create a trigger to map move keys
     * to the move the player
     * @return
     *      The trigger.
     */
    private InputTrigger CreateMoveTrigger()
    {

        final UprightFlyControlTriggers control = this;
        
        final Predicate<TwoInputStates> keysHeld =
                Predicates.or(moveConfig.createSomeMoveKeysDownPredicates());
        
        final TriggerAction moveAction = new TriggerAction() {
            @Override
            public void perform(final Canvas source,
                    final TwoInputStates inputStates, final double tpf) {
                control.move(inputStates, tpf);
            }
        };
        
        return new InputTrigger(keysHeld, moveAction);
    }
    
    /**
     * Create a predicate for mouse movements
     * @param mouseTurnDragOnly
     *      True if looking requres
     *      dragging
     * @return
     *      The predicate for mouse turning.
     */
    private Predicate<TwoInputStates> CreateMouseTurnPredicate(
            boolean mouseTurnDragOnly)
    {
        
        // Mouse look
        final Predicate<TwoInputStates> someMouseDown = Predicates.or(
                TriggerConditions.leftButtonDown(), Predicates.or(
                        TriggerConditions.rightButtonDown(),
                        TriggerConditions.middleButtonDown()));
        final Predicate<TwoInputStates> dragged = Predicates.and(
                TriggerConditions.mouseMoved(), someMouseDown);
        
        final Predicate<TwoInputStates> mouseLookUpdate =
                mouseTurnDragOnly
                    ? dragged
                    : TriggerConditions.mouseMoved();

        return mouseLookUpdate;
        
    }
    
    /**
     * Creates a trigger for managing looking
     * and turning.
     * @param mouseLookDragOnly
     * @return
     *      The trigger for the player
     *      to look and turn.
     */
    private InputTrigger CreateTurnTrigger()
    {

        final UprightFlyControlTriggers control = this;
        
        final Predicate<TwoInputStates> keysHeld =
                Predicates.or(moveConfig.createSomeTurnKeyPredicates());
        
        final Predicate<TwoInputStates> turnPredicate =
                Predicates.or(keysHeld, mouseTurnPredicate);
        
        final TriggerAction turnAction = new TriggerAction() {

            @Override
            public void perform(final Canvas source,
                    final TwoInputStates inputStates, final double tpf) {
                control.turn(inputStates, tpf);
            }
        };
        
        return new InputTrigger(turnPredicate, turnAction);
        
    }
    
    /**
     * Add the triggers to the logical layer
     */
    @Override
    public void AddToLayer(LogicalLayer layer)
    {
        layer.registerTrigger(moveTrigger);
        layer.registerTrigger(turnTrigger);
    }

    /**
     * Remove the triggers from the logical layer
     */
    @Override
    public void RemoveFromLayer(LogicalLayer layer)
    {
        layer.deregisterTrigger(moveTrigger);
        layer.deregisterTrigger(turnTrigger);
    }

    /**
     * Move the player according to the
     * triggers
     * @param state
     *      The input state.
     * @param tpf
     *      The time per frame.
     */
    protected void move(
            final TwoInputStates state,
            final double tpf)
    {
        // MOVEMENT
        int moveFB = 0, strafeLR = 0;
        if (moveForwardPredicate.apply(state)) {
            moveFB += 1;
        }
        if (moveBackPredicate.apply(state)) {
            moveFB -= 1;
        }
        if (moveLeftPredicate.apply(state)) {
            strafeLR += 1;
        }
        if (moveRightPredicate.apply(state)) {
            strafeLR -= 1;
        }

        if (moveFB != 0 || strafeLR != 0) {
            final Vector3 loc = new Vector3(Vector3.ZERO);
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
            loc.normalizeLocal().multiplyLocal(moveSpeed * tpf)
                    .addLocal(player.getPosition());
            player.getPosition().set(loc);
        }
    }

    /**
     * Turns the player
     * @param state
     *      The input state for the
     *      turn
     * @param tpf
     *      The time per frame for
     *      the last frame.
     */
    protected void turn(
            final TwoInputStates state,
            final double tpf)
    {

        final double keyboardRotationFactor =
                keyRotateSpeed / mouseRotateSpeed * tpf;
        
        // ROTATION
        double rotX = 0, rotY = 0;
        
        if (turnUpPredicate.apply(state)) {
            rotY -= keyboardRotationFactor;
        }
        if (turnDownPredicate.apply(state)) {
            rotY += keyboardRotationFactor;
        }
        if (turnLeftPredicate.apply(state)) {
            rotX += keyboardRotationFactor;
        }
        if (turnRightPredicate.apply(state)) {
            rotX -= keyboardRotationFactor;
        }
        
        if(mouseTurnPredicate.apply(state))
        {
            if(firstPing)
            {
                firstPing = false;
            }
            else
            {
                final MouseState mouse = state.getCurrent()
                        .getMouseState();
                rotX -= mouse.getDx();
                rotY -= mouse.getDy();
            }
        }
        
        rotate(rotX, rotY);
    }

    /**
     * Rotate the player by the desired
     * amounts
     * @param dx
     *      The x angle to rotate
     * @param dy
     *      The y angle to rotate
     */
    protected void rotate(final double dx, final double dy) {

        if (dx != 0) {
            workerMatrix.fromAngleNormalAxis(mouseRotateSpeed * dx,
                    upAxis != null ? upAxis : player.getUp());
            workerMatrix.applyPost(player.getLeft(), workerStoreA);
            player.getLeft().set(workerStoreA);
            workerMatrix.applyPost(player.getDirection(), workerStoreA);
            player.getDirection().set(workerStoreA);
            workerMatrix.applyPost(player.getUp(), workerStoreA);
            player.getUp().set(workerStoreA);
        }

        if (dy != 0) {
            workerMatrix.fromAngleNormalAxis(mouseRotateSpeed * dy,
                    player.getLeft());
            workerMatrix.applyPost(player.getLeft(), workerStoreA);
            player.getLeft().set(workerStoreA);
            workerMatrix.applyPost(player.getDirection(), workerStoreA);
            player.getDirection().set(workerStoreA);
            workerMatrix.applyPost(player.getUp(), workerStoreA);
            player.getUp().set(workerStoreA);
        }

        player.normalize();
        
    }

}
