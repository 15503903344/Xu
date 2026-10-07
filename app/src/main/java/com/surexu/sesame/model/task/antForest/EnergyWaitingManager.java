package com.surexu.sesame.model.task.antForest;

import com.surexu.sesame.util.Log;
import com.surexu.sesame.util.TimeUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class EnergyWaitingManager {

    private static final String TAG = "EnergyWaitingManager";
    private static final long ACTIVATION_THRESHOLD_MS = 240000L;
    private static final long MIN_INTERVAL_MS = 500L;
    private static final long WAKE_AHEAD_MIN_MS = 5000L;
    private static final long WAKE_AHEAD_MAX_MS = 10001L;
    private static final long CLEAN_COOLDOWN_MS = 60000L;
    private static final long MAX_WAIT_TIME_MS = 120L * 60000L;

    private static final EnergyWaitingManager INSTANCE = new EnergyWaitingManager();

    private final ConcurrentHashMap<String, EnergyWaitingTask> waitingTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();
    private final Set<String> persistedTaskIds = ConcurrentHashMap.newKeySet();
    private final Object taskMutex = new Object();
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r, TAG);
        t.setDaemon(true);
        return t;
    });

    private EnergyCollectCallback energyCollectCallback;
    private volatile long lastCleanTime;
    private volatile long lastExecuteTime;

    public interface EnergyWaitingTaskRestoreCallback {
        boolean onRestore(EnergyWaitingTask task);
    }

    public static EnergyWaitingManager getInstance() {
        return INSTANCE;
    }

    public void setEnergyCollectCallback(EnergyCollectCallback callback) {
        this.energyCollectCallback = callback;
    }

    public void init(EnergyWaitingTaskRestoreCallback restoreCallback) {
        try {
            List<EnergyWaitingTask> restored = EnergyWaitingPersistence.getInstance().loadTasks();
            for (EnergyWaitingTask task : restored) {
                if (restoreCallback != null && restoreCallback.onRestore(task)) {
                    addWaitingTask(task.getUserId(), task.getUserName(), task.getBubbleId(), task.getProduceTime(), task.getFromTag(), task.getShieldEndTime(), task.getBombEndTime(), null, task.isCanDouble(), task.getFullEnergy(), true);
                }
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
    }

    public boolean addWaitingTask(String userId, String userName, long bubbleId, long produceTime, String fromTag, long shieldEndTime, long bombEndTime, JSONObject userHomeObj, boolean canDouble, int fullEnergy, boolean forceAccept) {
        long maxWaitTimeMs = getMaxWaitTimeMs();
        long remaining = produceTime - System.currentTimeMillis();
        if (remaining > maxWaitTimeMs && !forceAccept) {
            cancelAndRemoveTask(userId + "_" + bubbleId, produceTime);
            return false;
        }
        addTaskInternal(userId, userName, bubbleId, produceTime, fromTag, shieldEndTime, bombEndTime, userHomeObj, canDouble, fullEnergy, forceAccept);
        return true;
    }

    private void addTaskInternal(String userId, String userName, long bubbleId, long produceTime, String fromTag, long shieldEndTime, long bombEndTime, JSONObject userHomeObj, boolean canDouble, int fullEnergy, boolean forceAccept) {
        String taskId = userId + "_" + bubbleId;
        synchronized (taskMutex) {
            try {
                EnergyWaitingTask existing = waitingTasks.get(taskId);
                long currentShieldEndTime = shieldEndTime;
                long currentBombEndTime = bombEndTime;
                if (userHomeObj != null) {
                    long[] protection = parseProtection(userHomeObj);
                    currentShieldEndTime = protection[0];
                    currentBombEndTime = protection[1];
                }
                if (existing != null && !existing.isCancelled() && existing.getProduceTime() != produceTime && !existing.isSelf()) {
                    if (shouldSkipWaitingDueToProtection(userHomeObj, produceTime, currentShieldEndTime, currentBombEndTime)) {
                        removeTaskRecord(taskId, produceTime);
                        cancelRunningTask(taskId);
                        waitingTasks.remove(taskId);
                        Log.record("取消蹲点[" + taskId + "]：保护罩未过期，无需更新");
                        return;
                    }
                    existing = null;
                }
                EnergyWaitingTask task = new EnergyWaitingTask(userId, userName, bubbleId, produceTime, fromTag, 0, 3, currentShieldEndTime, currentBombEndTime, fromTag.contains("PK") && !userName.startsWith("PK好友|"), canDouble, false, fullEnergy);
                EnergyWaitingTask oldTask = waitingTasks.put(taskId, task);
                if (oldTask != null && !oldTask.isCancelled()) {
                    cancelRunningTask(taskId);
                    Log.record("更新蹲点[" + task.getUserTypeTag() + userName + "]：成熟时间[" + TimeUtil.getCommonDate(produceTime) + "]");
                } else {
                    Log.record("添加蹲点[" + task.getUserTypeTag() + userName + "]：成熟时间[" + TimeUtil.getCommonDate(produceTime) + "]");
                }
                long remaining = produceTime - System.currentTimeMillis();
                if (remaining > MAX_WAIT_TIME_MS && !forceAccept) {
                    removeTaskRecord(taskId, produceTime);
                    cancelRunningTask(taskId);
                    waitingTasks.remove(taskId);
                    return;
                }
                if (remaining < ACTIVATION_THRESHOLD_MS || forceAccept) {
                    removeTaskRecord(taskId, produceTime);
                    persistedTaskIds.add(taskId);
                    scheduleTask(task);
                    requestSavePersistence();
                } else {
                    persistedTaskIds.add(taskId);
                    EnergyWaitingPersistence.getInstance().updateSingleTask(task);
                }
            } catch (Throwable t) {
                Log.printStackTrace(t);
            }
        }
    }

    private long[] parseProtection(JSONObject userHomeObj) {
        long shieldEndTime = 0L;
        long bombEndTime = 0L;
        try {
            JSONArray props = userHomeObj.optJSONArray("usingUserPropsNew");
            if (props != null) {
                for (int i = 0; i < props.length(); i++) {
                    JSONObject prop = props.optJSONObject(i);
                    if (prop == null) {
                        continue;
                    }
                    String propGroup = prop.optString("propGroup");
                    long endTime = prop.optLong("endTime");
                    if ("shield".equals(propGroup)) {
                        shieldEndTime = Math.max(shieldEndTime, endTime);
                    } else if ("energyBombCard".equals(propGroup)) {
                        bombEndTime = Math.max(bombEndTime, endTime);
                    }
                }
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return new long[]{shieldEndTime, bombEndTime};
    }

    private boolean shouldSkipWaitingDueToProtection(JSONObject userHomeObj, long produceTime, long shieldEndTime, long bombEndTime) {
        if (userHomeObj == null) {
            return false;
        }
        return produceTime < Math.max(shieldEndTime, bombEndTime);
    }

    private void scheduleTask(EnergyWaitingTask task) {
        try {
            long preciseTime = calculatePreciseCollectTime(task);
            long now = System.currentTimeMillis();
            long waitTime = preciseTime - now;
            long delay;
            if (waitTime <= 0) {
                delay = 0;
            } else if (waitTime > WAKE_AHEAD_MAX_MS) {
                delay = waitTime - ThreadLocalRandom.current().nextLong(WAKE_AHEAD_MIN_MS, WAKE_AHEAD_MAX_MS);
            } else {
                delay = waitTime;
            }
            ScheduledFuture<?> future = scheduler.schedule(() -> executePreciseWaitingTask(task), Math.max(delay, 0), TimeUnit.MILLISECONDS);
            runningTasks.put(task.getTaskId(), future);
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
    }

    private void executePreciseWaitingTask(EnergyWaitingTask task) {
        try {
            if (task.isCancelled()) {
                cleanupCancelledTask(task);
                return;
            }
            synchronized (taskMutex) {
                long now = System.currentTimeMillis();
                long interval = ThreadLocalRandom.current().nextLong(MIN_INTERVAL_MS, 1501L);
                long last = lastExecuteTime;
                if (last != 0 && now - last < interval) {
                    long remaining = calculatePreciseCollectTime(task) - now;
                    if (remaining > 0) {
                        long extraDelay = interval - (now - last);
                        ScheduledFuture<?> future = scheduler.schedule(() -> executePreciseWaitingTask(task), Math.max(extraDelay, 0), TimeUnit.MILLISECONDS);
                        runningTasks.put(task.getTaskId(), future);
                        return;
                    }
                }
                lastExecuteTime = now;
                EnergyCollectCallback callback = energyCollectCallback;
                if (callback != null) {
                    callback.onPreRevalidateProp(task.getUserId(), task.getFromTag());
                }
                long preciseTime = calculatePreciseCollectTime(task);
                long currentTime = System.currentTimeMillis();
                if (preciseTime > currentTime + 1500L) {
                    long delay = preciseTime - currentTime;
                    ScheduledFuture<?> future = scheduler.schedule(() -> executePreciseWaitingTask(task), delay, TimeUnit.MILLISECONDS);
                    runningTasks.put(task.getTaskId(), future);
                    return;
                }
                List<EnergyWaitingTask> batch = new ArrayList<>();
                batch.add(task);
                Iterator<Map.Entry<String, EnergyWaitingTask>> it = waitingTasks.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, EnergyWaitingTask> entry = it.next();
                    EnergyWaitingTask other = entry.getValue();
                    if (!Objects.equals(other.getTaskId(), task.getTaskId()) && !other.isCancelled()) {
                        long otherPrecise = calculatePreciseCollectTime(other);
                        if (Math.abs(otherPrecise - preciseTime) < MIN_INTERVAL_MS) {
                            batch.add(other);
                        }
                    }
                }
                if (batch.size() > 1) {
                    Log.record("初始合并：检测到[" + task.getUserName() + "]有 " + batch.size() + " 个任务时机相近，合并为一个协程处理");
                }
                long startTime = System.currentTimeMillis();
                CollectResult result = callback != null ? callback.collectUserEnergyForWaiting(batch) : CollectResult.failure("无回调");
                long responseTime = System.currentTimeMillis() - startTime;
                if (result.isSuccess()) {
                    if (callback != null) {
                        callback.addToTotalCollected(result.getEnergyCount());
                    }
                    UserEnergyPatternManager.getInstance().updateUserPattern(task.getUserId(), result, responseTime);
                    for (EnergyWaitingTask batchTask : batch) {
                        removeTaskRecord(batchTask.getTaskId(), batchTask.getProduceTime());
                        cancelRunningTask(batchTask.getTaskId());
                        waitingTasks.remove(batchTask.getTaskId());
                    }
                } else {
                    EnergyWaitingTask retried = task.withRetry();
                    long timeToTarget = task.getProduceTime() - System.currentTimeMillis();
                    if (SmartRetryStrategy.shouldRetry(retried.getRetryCount(), result.getError(), timeToTarget)) {
                        long retryDelay = SmartRetryStrategy.getRetryDelay(retried.getRetryCount(), result.getError());
                        EnergyWaitingTask updated = new EnergyWaitingTask(retried.getUserId(), retried.getUserName(), retried.getBubbleId(), retried.getProduceTime(), retried.getFromTag(), retried.getRetryCount(), retried.getMaxRetries(), retried.getShieldEndTime(), retried.getBombEndTime(), retried.isPk(), retried.isCanDouble(), retried.isCancelled(), retried.getFullEnergy());
                        waitingTasks.put(updated.getTaskId(), updated);
                        ScheduledFuture<?> future = scheduler.schedule(() -> executePreciseWaitingTask(updated), Math.max(retryDelay, 0), TimeUnit.MILLISECONDS);
                        runningTasks.put(updated.getTaskId(), future);
                        Log.record("蹲点[" + task.getUserName() + "]收取失败，重试[" + updated.getRetryCount() + "]");
                    } else {
                        for (EnergyWaitingTask batchTask : batch) {
                            removeTaskRecord(batchTask.getTaskId(), batchTask.getProduceTime());
                            cancelRunningTask(batchTask.getTaskId());
                            waitingTasks.remove(batchTask.getTaskId());
                        }
                        Log.record("蹲点[" + task.getUserName() + "]收取失败，放弃");
                    }
                }
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
    }

    private void cleanupCancelledTask(EnergyWaitingTask task) {
        synchronized (taskMutex) {
            removeTaskRecord(task.getTaskId(), task.getProduceTime());
            cancelRunningTask(task.getTaskId());
            waitingTasks.remove(task.getTaskId());
        }
    }

    public boolean hasWaitingTask(String userId, long bubbleId, long produceTime) {
        synchronized (taskMutex) {
            EnergyWaitingTask task = waitingTasks.get(userId + "_" + bubbleId);
            return task != null && !task.isCancelled() && task.getProduceTime() == produceTime;
        }
    }

    public void cancelAndRemoveTask(String taskId, long produceTime) {
        synchronized (taskMutex) {
            EnergyWaitingTask task = waitingTasks.get(taskId);
            if (task != null && produceTime > 0 && task.getProduceTime() != produceTime) {
                return;
            }
            cancelRunningTask(taskId);
            waitingTasks.remove(taskId);
            persistedTaskIds.remove(taskId);
            removeTaskRecord(taskId, produceTime);
        }
    }

    public void removeTaskRecord(String taskId, long produceTime) {
        synchronized (taskMutex) {
            persistedTaskIds.remove(taskId);
            EnergyWaitingPersistence.getInstance().removeTask(taskId, produceTime);
        }
    }

    private void cancelRunningTask(String taskId) {
        ScheduledFuture<?> future = runningTasks.remove(taskId);
        if (future != null) {
            future.cancel(true);
        }
    }

    public void cleanExpiredTasks(boolean enableRevalidation) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanTime < CLEAN_COOLDOWN_MS) {
            return;
        }
        lastCleanTime = currentTime;
        synchronized (taskMutex) {
            Iterator<Map.Entry<String, EnergyWaitingTask>> it = waitingTasks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, EnergyWaitingTask> entry = it.next();
                EnergyWaitingTask task = entry.getValue();
                if (task.isCancelled()) {
                    it.remove();
                    cancelRunningTask(entry.getKey());
                    removeTaskRecord(entry.getKey(), task.getProduceTime());
                } else if (enableRevalidation && energyCollectCallback != null) {
                    energyCollectCallback.onPreRevalidateProp(task.getUserId(), task.getFromTag());
                    if (task.isCancelled()) {
                        it.remove();
                        cancelRunningTask(entry.getKey());
                        removeTaskRecord(entry.getKey(), task.getProduceTime());
                    }
                }
            }
        }
    }

    public void cleanPkTasks() {
        synchronized (taskMutex) {
            Iterator<Map.Entry<String, EnergyWaitingTask>> it = waitingTasks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, EnergyWaitingTask> entry = it.next();
                if (entry.getValue().isPk()) {
                    it.remove();
                    cancelRunningTask(entry.getKey());
                    persistedTaskIds.remove(entry.getKey());
                }
            }
            EnergyWaitingPersistence.getInstance().removePkTasks();
        }
    }

    public void clearAllTasks(boolean wipeDisk) {
        synchronized (taskMutex) {
            for (ScheduledFuture<?> future : runningTasks.values()) {
                future.cancel(true);
            }
            runningTasks.clear();
            waitingTasks.clear();
            persistedTaskIds.clear();
            if (wipeDisk) {
                EnergyWaitingPersistence.getInstance().physicalWipeAllTasks();
            }
        }
    }

    public Collection<EnergyWaitingTask> getWaitingTasksInternal() {
        synchronized (taskMutex) {
            return new ArrayList<>(waitingTasks.values());
        }
    }

    public boolean isUnderProtection(JSONObject userHomeObj, EnergyWaitingTask task) {
        long[] protection = parseProtection(userHomeObj);
        return task.getProduceTime() < Math.max(protection[0], protection[1]);
    }

    public long calculatePreciseCollectTime(EnergyWaitingTask task) {
        if (task.isSelf()) {
            return task.getProduceTime();
        }
        long protectionEndTime = task.getProtectionEndTime();
        long now = System.currentTimeMillis();
        if (protectionEndTime > now) {
            return protectionEndTime + 100L;
        }
        return task.getProduceTime();
    }

    private long getMaxWaitTimeMs() {
        return MAX_WAIT_TIME_MS;
    }

    private void requestSavePersistence() {
        try {
            synchronized (taskMutex) {
                EnergyWaitingPersistence.getInstance().saveTasks(waitingTasks);
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
    }
}
