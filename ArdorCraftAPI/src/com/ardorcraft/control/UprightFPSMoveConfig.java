package com.ardorcraft.control;

import com.ardor3d.input.Key;
import java.util.ArrayList;
import javax.xml.bind.annotation.XmlRootElement;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.ardor3d.input.logical.TwoInputStates;

@XmlRootElement(name="Controls")
public class UprightFPSMoveConfig
{
    public enum defaultControls
    {
        LeftHanded,
        RightHanded
    }
    
    public ButtonSet MoveLeft = new ButtonSet();
    public ButtonSet MoveRight = new ButtonSet();
    public ButtonSet MoveForward = new ButtonSet();
    public ButtonSet MoveBack = new ButtonSet();
    public ButtonSet TurnLeft = new ButtonSet();
    public ButtonSet TurnRight = new ButtonSet();
    public ButtonSet TurnUp = new ButtonSet();
    public ButtonSet TurnDown = new ButtonSet();
    
    public UprightFPSMoveConfig()
    {
        
    }
    
    public UprightFPSMoveConfig(defaultControls config)
    {
        TurnLeft.keys.add(Key.LEFT);
        TurnRight.keys.add(Key.RIGHT);
        TurnUp.keys.add(Key.UP);
        TurnDown.keys.add(Key.DOWN);
        
        if(config == defaultControls.LeftHanded)
        {
            MoveLeft.keys.add(Key.J);
            MoveRight.keys.add(Key.L);
            MoveForward.keys.add(Key.I);
            MoveBack.keys.add(Key.K);
        }
        else
        {
            MoveForward.keys.add(Key.W);
            MoveLeft.keys.add(Key.A);
            MoveBack.keys.add(Key.S);
            MoveRight.keys.add(Key.D);
        }
    }
    
    Predicate<TwoInputStates> createSomeMoveKeysDownPredicate()
    {
        ArrayList<Predicate<TwoInputStates> > 
            tempPredicates =
            new ArrayList<Predicate<TwoInputStates> >();
        
        tempPredicates.add(
                new SomeButtonsDownPredicate(MoveForward));
        tempPredicates.add(
                new SomeButtonsDownPredicate(MoveLeft));
        tempPredicates.add(
                new SomeButtonsDownPredicate(MoveBack));
        tempPredicates.add(
                new SomeButtonsDownPredicate(MoveRight));
        
        return Predicates.or(tempPredicates);
    }
    
    Predicate<TwoInputStates> createSomeTurnKeyPredicate()
    {
        ArrayList<Predicate<TwoInputStates> > 
            tempPredicates =
            new ArrayList<Predicate<TwoInputStates> >();
        
        tempPredicates.add(
                new SomeButtonsDownPredicate(TurnUp));
        tempPredicates.add(
                new SomeButtonsDownPredicate(TurnLeft));
        tempPredicates.add(
                new SomeButtonsDownPredicate(TurnDown));
        tempPredicates.add(
                new SomeButtonsDownPredicate(TurnRight));
        
        return Predicates.or(tempPredicates);
    }
}
