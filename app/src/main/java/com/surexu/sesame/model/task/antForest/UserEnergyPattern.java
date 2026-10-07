package com.surexu.sesame.model.task.antForest;

import lombok.Getter;

@Getter
public class UserEnergyPattern {

    private final String userId;
    private final double collectSuccessRate;
    private final long avgResponseTime;
    private final long lastCollectTime;
    private final boolean isActiveUser;

    public UserEnergyPattern(String userId, double collectSuccessRate, long avgResponseTime, long lastCollectTime, boolean isActiveUser) {
        this.userId = userId;
        this.collectSuccessRate = collectSuccessRate;
        this.avgResponseTime = avgResponseTime;
        this.lastCollectTime = lastCollectTime;
        this.isActiveUser = isActiveUser;
    }

    public UserEnergyPattern copy(String userId, double collectSuccessRate, long avgResponseTime, long lastCollectTime, boolean isActiveUser) {
        return new UserEnergyPattern(userId, collectSuccessRate, avgResponseTime, lastCollectTime, isActiveUser);
    }
}
