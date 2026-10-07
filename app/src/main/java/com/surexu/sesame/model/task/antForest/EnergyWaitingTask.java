package com.surexu.sesame.model.task.antForest;

import com.surexu.sesame.util.idMap.UserIdMap;

import lombok.Getter;

@Getter
public class EnergyWaitingTask {

    private final String userId;
    private final String userName;
    private final long bubbleId;
    private final long produceTime;
    private final String fromTag;
    private final int retryCount;
    private final int maxRetries;
    private final long shieldEndTime;
    private final long bombEndTime;
    private final boolean isPk;
    private final boolean canDouble;
    private final boolean isCancelled;
    private final int fullEnergy;
    private final String taskId;

    public EnergyWaitingTask(String userId, String userName, long bubbleId, long produceTime, String fromTag, int retryCount, int maxRetries, long shieldEndTime, long bombEndTime, boolean isPk, boolean canDouble, boolean isCancelled, int fullEnergy) {
        this.userId = userId;
        this.userName = userName;
        this.bubbleId = bubbleId;
        this.produceTime = produceTime;
        this.fromTag = fromTag;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.shieldEndTime = shieldEndTime;
        this.bombEndTime = bombEndTime;
        this.isPk = isPk;
        this.canDouble = canDouble;
        this.isCancelled = isCancelled;
        this.fullEnergy = fullEnergy;
        this.taskId = userId + "_" + bubbleId;
    }

    public EnergyWaitingTask copy(String userId, String userName, long bubbleId, long produceTime, String fromTag, int retryCount, int maxRetries, long shieldEndTime, long bombEndTime, boolean isPk, boolean canDouble, boolean isCancelled, int fullEnergy) {
        return new EnergyWaitingTask(userId, userName, bubbleId, produceTime, fromTag, retryCount, maxRetries, shieldEndTime, bombEndTime, isPk, canDouble, isCancelled, fullEnergy);
    }

    public EnergyWaitingTask withRetry() {
        return copy(userId, userName, bubbleId, produceTime, fromTag, retryCount + 1, maxRetries, shieldEndTime, bombEndTime, isPk, canDouble, isCancelled, fullEnergy);
    }

    public long getProtectionEndTime() {
        return Math.max(shieldEndTime, bombEndTime);
    }

    public boolean hasProtection(long currentTime) {
        return shieldEndTime > currentTime || bombEndTime > currentTime;
    }

    public boolean isSelf() {
        return java.util.Objects.equals(userId, UserIdMap.getCurrentUid());
    }

    public String getUserTypeTag() {
        return isSelf() ? "⭐️主号|" : "好友|";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EnergyWaitingTask)) {
            return false;
        }
        EnergyWaitingTask task = (EnergyWaitingTask) other;
        return java.util.Objects.equals(userId, task.userId)
                && java.util.Objects.equals(userName, task.userName)
                && bubbleId == task.bubbleId
                && produceTime == task.produceTime
                && java.util.Objects.equals(fromTag, task.fromTag)
                && retryCount == task.retryCount
                && maxRetries == task.maxRetries
                && shieldEndTime == task.shieldEndTime
                && bombEndTime == task.bombEndTime
                && isPk == task.isPk
                && canDouble == task.canDouble
                && isCancelled == task.isCancelled
                && fullEnergy == task.fullEnergy;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (int) (bubbleId ^ (bubbleId >>> 32));
        result = 31 * result + (int) (produceTime ^ (produceTime >>> 32));
        result = 31 * result + (fromTag != null ? fromTag.hashCode() : 0);
        result = 31 * result + retryCount;
        result = 31 * result + maxRetries;
        result = 31 * result + (int) (shieldEndTime ^ (shieldEndTime >>> 32));
        result = 31 * result + (int) (bombEndTime ^ (bombEndTime >>> 32));
        result = 31 * result + (isPk ? 1 : 0);
        result = 31 * result + (canDouble ? 1 : 0);
        result = 31 * result + (isCancelled ? 1 : 0);
        result = 31 * result + fullEnergy;
        return result;
    }
}
