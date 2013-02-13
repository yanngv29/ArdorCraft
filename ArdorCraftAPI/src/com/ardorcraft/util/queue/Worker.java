/**
 * Copyright (c) 2011-2012 Ardor Labs AB.
 *
 * This file is part of the ArdorCraft API, developed by Rikard Herlitz.
 */

package com.ardorcraft.util.queue;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.ardor3d.renderer.Renderer;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * The Worker handles the chunk update Tasks, making sure they are prioritized, and doesnt spend too much time per
 * frame.
 */
public class Worker {
    private final List<Task> taskList = Lists.newLinkedList();
    private final ReentrantLock lock = new ReentrantLock();
    private long executionTime = 10;
    private boolean needsSorting = false;
    private final Coords playerCoords = new Coords();

    public void enqueue(final Task task) {
        task.setPlayerCoords(playerCoords);

        lock.lock();
        try {
            taskList.add(task);
            needsSorting = true;
        } finally {
            lock.unlock();
        }
    }

    private final Set<Task> taskCleaner = Sets.newHashSet();

    public void execute(final Renderer renderer) {
        final long beginTime = System.currentTimeMillis();
        long elapsedTime = 0;

        int size = 0;

        lock.lock();
        try {
            size = taskList.size();
            if (size > 0 && needsSorting) {
                Collections.sort(taskList);
                needsSorting = false;

                taskCleaner.clear();
                final Iterator<Task> iterator = taskList.iterator();
                while (iterator.hasNext()) {
                    final Task task = iterator.next();
                    final int bucket = task.getBucket();
                    if ((bucket == 2 || bucket == 3) && taskCleaner.contains(task)) {
                        iterator.remove();
                    } else if (bucket == 2 || bucket == 3) {
                        taskCleaner.add(task);
                    }
                }
                size = taskList.size();
            }
        } finally {
            lock.unlock();
        }

        if (size > 0) {
            int tasksFinished = 0;
            for (int i = 0; i < size && elapsedTime < executionTime; i++) {
                taskList.get(i).execute(renderer);
                // System.out.println(taskList.get(i));
                tasksFinished++;
                elapsedTime = System.currentTimeMillis() - beginTime;
            }

            lock.lock();
            try {
                for (int i = 0; i < tasksFinished; i++) {
                    taskList.remove(0);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(final long executionTime) {
        this.executionTime = executionTime;
    }

    public void setPlayerCoords(final int playerX, final int playerZ) {
        playerCoords.setX(playerX);
        playerCoords.setZ(playerZ);
    }
}
