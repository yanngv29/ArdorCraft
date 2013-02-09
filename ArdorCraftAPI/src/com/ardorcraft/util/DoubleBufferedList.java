/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.util;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * Utility class used by the mailbox update system.
 * 
 * @param <T>
 */
public final class DoubleBufferedList<T> {
    private List<T> frontList = Lists.newArrayList();
    private List<T> backList = Lists.newArrayList();

    /**
     * The add method can be called at any point.
     * 
     * @param t
     */
    public void add(final T t) {
        if (t != null) {
            synchronized (backList) {
                backList.add(t);
            }
        }
    }

    /**
     * The switchAndGet call and it's returned list has to be accessed sequencially.
     * 
     * @return The list
     */
    public List<T> switchAndGet() {
        if (backList.isEmpty()) {
            return Collections.emptyList();
        }
        synchronized (backList) {
            final List<T> tmp = backList;
            backList = frontList;
            frontList = tmp;
            backList.clear();
            return frontList;
        }
    }

    public boolean isEmpty() {
        return backList.isEmpty();
    }
}
