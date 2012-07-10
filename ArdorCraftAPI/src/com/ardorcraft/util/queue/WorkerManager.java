/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.util.queue;

public final class WorkerManager {
    private static final Worker worker = new Worker();

    private WorkerManager() {

    }

    public static Worker getWorker() {
        return worker;
    }
}
