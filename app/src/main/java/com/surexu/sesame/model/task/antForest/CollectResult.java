package com.surexu.sesame.model.task.antForest;

import lombok.Getter;

@Getter
public class CollectResult {

    private final boolean success;
    private final int energyCount;
    private final String error;

    public CollectResult(boolean success, int energyCount, String error) {
        this.success = success;
        this.energyCount = energyCount;
        this.error = error;
    }

    public static CollectResult success(int energyCount) {
        return new CollectResult(true, energyCount, null);
    }

    public static CollectResult failure(String error) {
        return new CollectResult(false, 0, error);
    }
}
