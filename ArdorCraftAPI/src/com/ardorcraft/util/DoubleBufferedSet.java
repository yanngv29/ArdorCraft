/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.util;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Utility class used by the mailbox update system.
 * 
 * @param <T>
 */
public class DoubleBufferedSet<T> {
    private Set<T> frontSet = Sets.newHashSet();
    private Set<T> backSet = Sets.newHashSet();

    /**
     * The add method can be called at any point.
     * 
     * @param t
     */
    public void add(final T t) {
        synchronized (backSet) {
            backSet.add(t);
        }
    }

    /**
     * The switchAndGet call and it's returned list has to be accessed sequencially.
     * 
     * @return The Set
     */
    public Set<T> switchAndGet() {
        if (backSet.isEmpty()) {
            return Collections.emptySet();
        }
        synchronized (backSet) {
            final Set<T> tmp = backSet;
            backSet = frontSet;
            frontSet = tmp;
            backSet.clear();
            return frontSet;
        }
    }
}
