/**
 * 
 */
package com.ardorcraft.control;

import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;

import java.util.Collection;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * @author Nathan
 *
 *  A set of keys or mouse buttons to map to a function
 *  for creating configurable controls.
 */
public class ButtonSet
{
    /**
     * The set of keys in the set.
     */
    @XmlElementWrapper(name="Keys")
    @XmlElement(name="Key")
    public TreeSet<Key> keys;
    
    /**
     * The set of mouse buttons in the set.
     */
    @XmlElementWrapper(name="MouseButtons")
    @XmlElement(name="Button")
    public TreeSet<MouseButton> mouseButtons;
    
    /**
     * Default constructor just creates
     * an empty set.
     */
    public ButtonSet()
    {
        keys = new TreeSet<Key>();
        mouseButtons = new TreeSet<MouseButton>();
    }
    
    /**
     * Create a union of a number of
     * button sets.
     * @param buttonSets
     *      The buttonsets to combine
     */
    public ButtonSet(ButtonSet...buttonSets)
    {
        keys = new TreeSet<Key>();
        mouseButtons = new TreeSet<MouseButton>();
        
        for(ButtonSet set : buttonSets)
        {
            keys.addAll(set.keys);
            mouseButtons.addAll(set.mouseButtons);
        }
    }
    
    /**
     * Constructs a set containing the
     * union of the sets passed in.
     * @param buttonSets
     *      The sets to combine.
     */
    public ButtonSet(Collection<ButtonSet> buttonSets)
    {
        keys = new TreeSet<Key>();
        mouseButtons = new TreeSet<MouseButton>();
        
        for(ButtonSet set : buttonSets)
        {
            keys.addAll(set.keys);
            mouseButtons.addAll(set.mouseButtons);
        }
    }
}
