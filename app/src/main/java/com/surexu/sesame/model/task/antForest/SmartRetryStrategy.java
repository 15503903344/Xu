package com.surexu.sesame.model.task.antForest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SmartRetryStrategy {

    private static final List<Long> retryDelays = Arrays.asList(10000L, 30000L, 60000L, 180000L);
    private static final long MAX_RETRY_DELAY = 180000L;
    private static final long MIN_TIME_TO_TARGET = 10000L;

    public static long getRetryDelay(int retryCount, String lastError) {
        long baseDelay = (retryCount < 0 || retryCount >= retryDelays.size()) ? MAX_RETRY_DELAY : retryDelays.get(retryCount);
        double multiplier = 1.0d;
        if (lastError != null) {
            if (lastError.contains("网络")) {
                multiplier = 2.0d;
            } else if (lastError.contains("频繁")) {
                multiplier = 3.0d;
            } else if (lastError.contains("保护")) {
                multiplier = 1.0d;
            }
        }
        long jitter = ThreadLocalRandom.current().nextLong(-2000L, 2001L);
        return (long) (baseDelay * multiplier) + jitter;
    }

    public static boolean shouldRetry(int retryCount, String error, long timeToTarget) {
        if (retryCount >= 3 || timeToTarget < MIN_TIME_TO_TARGET) {
            return false;
        }
        if (error != null && (error.contains("网络") || error.contains("临时"))) {
            return true;
        }
        return (error == null || !error.contains("保护")) && retryCount < 2;
    }
}
