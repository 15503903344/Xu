package com.surexu.sesame.model.task.antForest;

import com.surexu.sesame.util.Log;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserEnergyPatternManager {

    private static final String TAG = "UserEnergyPatternManager";
    private static final long PATTERN_EXPIRE_MS = 2592000000L;
    private static final long ACTIVE_WINDOW_MS = 86400000L;
    private static final ConcurrentHashMap<String, UserEnergyPattern> userPatterns = new ConcurrentHashMap<>();

    private static final UserEnergyPatternManager INSTANCE = new UserEnergyPatternManager();

    public static UserEnergyPatternManager getInstance() {
        return INSTANCE;
    }

    public void cleanupExpiredPatterns() {
        long currentTime = System.currentTimeMillis();
        LinkedHashMap<String, UserEnergyPattern> expired = new LinkedHashMap<>();
        for (Map.Entry<String, UserEnergyPattern> entry : userPatterns.entrySet()) {
            if (currentTime - entry.getValue().getLastCollectTime() > PATTERN_EXPIRE_MS) {
                expired.put(entry.getKey(), entry.getValue());
            }
        }
        Set<String> keySet = expired.keySet();
        Iterator<String> it = keySet.iterator();
        while (it.hasNext()) {
            userPatterns.remove(it.next());
        }
        if (!keySet.isEmpty()) {
            Log.i("清理过期用户模式数据：" + keySet.size() + "个用户");
        }
    }

    public UserEnergyPattern getUserPattern(String userId) {
        UserEnergyPattern pattern = userPatterns.get(userId);
        return pattern == null ? new UserEnergyPattern(userId, 0.0d, 0L, 0L, false) : pattern;
    }

    public void updateUserPattern(String userId, CollectResult result, long responseTime) {
        UserEnergyPattern pattern = getUserPattern(userId);
        long currentTime = System.currentTimeMillis();
        double collectSuccessRate = result.isSuccess() ? (pattern.getCollectSuccessRate() * 0.9d) + 0.1d : pattern.getCollectSuccessRate() * 0.9d;
        long avgResponseTime = pattern.getAvgResponseTime();
        if (responseTime > 0) {
            avgResponseTime = (long) ((responseTime * 0.2d) + (pattern.getAvgResponseTime() * 0.8d));
        }
        long lastCollectTime = currentTime;
        boolean isActive = currentTime - pattern.getLastCollectTime() < ACTIVE_WINDOW_MS;
        if (!result.isSuccess()) {
            lastCollectTime = pattern.getLastCollectTime();
        }
        UserEnergyPattern updated = new UserEnergyPattern(userId, collectSuccessRate, avgResponseTime, lastCollectTime, isActive);
        userPatterns.put(userId, updated);
        Log.i("更新用户[" + userId + "]模式：成功率[" + String.format("%.2f", collectSuccessRate) + "] 响应时间[" + avgResponseTime + "ms] 活跃[" + isActive + "]");
    }
}
