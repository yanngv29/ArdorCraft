/**
 * 
 */
package com.ardorcraft.control;

import com.ardor3d.input.logical.LogicalLayer;

/**
 * @author Nathan
 *
 */
public interface ITriggerGroup
{

    /**
     * Add the triggers to the specified
     * logical layer.
     * @param layer
     *      The logical layer to add
     *      the triggers to.
     */
    void AddToLayer(LogicalLayer layer);
    
    /**
     * Removes the trigger from the
     * specified logival layer
     * @param layer
     *      The logical layer to
     *      remove the triggers from.
     */
    void RemoveFromLayer(LogicalLayer layer);
    
}
