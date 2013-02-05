/**
 * 
 */
package com.ardorcraft.control;

import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import java.util.ArrayList;

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
    @XmlElementWrapper(name="Keys")
    @XmlElement(name="Key")
    public ArrayList<Key> keys =
        new ArrayList<Key>();
    
    @XmlElementWrapper(name="MouseButtons")
    @XmlElement(name="Button")
    public ArrayList<MouseButton> mouseButtons =
        new ArrayList<MouseButton>();
}
