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

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.InputState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.logical.TwoInputStates;
import com.google.common.base.Predicate;

/**
 * A condition that is true if a given button was pressed when going from the previous input state to the current one.
 */
@Immutable
public final class MouseButtonHeldCondition implements Predicate<TwoInputStates> {
    private final MouseButton _button;

    /**
     * Construct a new MouseButtonHeldCondition.
     * 
     * @param button
     *            the button that should be pressed to trigger this condition
     * @throws NullPointerException
     *             if the button is null
     */
    public MouseButtonHeldCondition(final MouseButton button) {
        if (button == null) {
            throw new NullPointerException();
        }

        _button = button;
    }

    public boolean apply(final TwoInputStates states) {
        final InputState currentState = states.getCurrent();

        if (currentState == null || !currentState.getMouseState().hasButtonState(ButtonState.DOWN)) {
            return false;
        }

        return currentState.getMouseState().getButtonState(_button) == ButtonState.DOWN;
    }
}
