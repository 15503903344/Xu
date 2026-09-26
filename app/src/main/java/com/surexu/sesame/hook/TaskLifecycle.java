package com.surexu.sesame.hook;

import com.surexu.sesame.data.task.ModelTask;

public final class TaskLifecycle {

    public static final class Freeze {
    }

    public static void thaw(Freeze freeze) {
    }

    private TaskLifecycle() {
    }

    public static Freeze freezeIfIdle() {
        if (ModelTask.isAllTaskIdle()) {
            return new Freeze();
        }
        return null;
    }
}
