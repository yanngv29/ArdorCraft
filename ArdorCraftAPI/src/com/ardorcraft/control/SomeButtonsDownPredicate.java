/**
 * 
 */
package com.ardorcraft.control;

import com.google.common.base.Predicate;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.logical.TwoInputStates;

/**
 * @author nathan
 *
 */
public class SomeButtonsDownPredicate implements
    Predicate<TwoInputStates>
{
    private ButtonSet buttons;
    
    public SomeButtonsDownPredicate(ButtonSet buttonsParam)
    {
        buttons = buttonsParam;
    }

    @Override
    public boolean apply(TwoInputStates states)
    {
        InputState current = states.getCurrent();
        
        for(Key currentKey : buttons.keys)
        {
            if(current.getKeyboardState().isDown(currentKey))
            {
                return true;
            }
        }
        
        for(MouseButton currentButton : buttons.mouseButtons)
        {
            if(current.getMouseState().getButtonState(currentButton) == ButtonState.DOWN)
            {
                return true;
            }
        }
        // TODO Auto-generated method stub
        return false;
    }

}
