package com.surexu.sesame.model.task.antForest;

import com.surexu.sesame.util.FileUtil;
import com.surexu.sesame.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EnergyWaitingPersistence {

    private static final String TAG = "EnergyWaitingPersistence";
    private static final long MAX_TASK_AGE_MS = 28800000L;
    private static final String FILE_NAME = "energy_waiting_tasks.json";
    private static final EnergyWaitingPersistence INSTANCE = new EnergyWaitingPersistence();

    public static EnergyWaitingPersistence getInstance() {
        return INSTANCE;
    }

    private File getStoreFile() {
        return new File(FileUtil.MAIN_DIRECTORY_FILE, FILE_NAME);
    }

    private List<EnergyWaitingTask> loadTasksInternal() {
        List<EnergyWaitingTask> tasks = new ArrayList<>();
        try {
            File storeFile = getStoreFile();
            if (storeFile == null || !storeFile.exists()) {
                return tasks;
            }
            String content = FileUtil.readFromFile(storeFile);
            if (content == null || content.isEmpty()) {
                return tasks;
            }
            JSONArray array = new JSONArray(content);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null) {
                    EnergyWaitingTask task = fromJson(obj);
                    if (task != null) {
                        tasks.add(task);
                    }
                }
            }
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return tasks;
    }

    private void updateDataStoreInternal(List<EnergyWaitingTask> tasks) {
        try {
            JSONArray array = new JSONArray();
            for (EnergyWaitingTask task : tasks) {
                array.put(toJson(task));
            }
            FileUtil.write2File(array.toString(), getStoreFile());
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
    }

    public void saveTasks(Map<String, EnergyWaitingTask> tasks) {
        synchronized (this) {
            updateDataStoreInternal(new ArrayList<>(tasks.values()));
        }
    }

    public void removeTask(String taskId, long produceTime) {
        synchronized (this) {
            List<EnergyWaitingTask> tasks = loadTasksInternal();
            Iterator<EnergyWaitingTask> it = tasks.iterator();
            while (it.hasNext()) {
                EnergyWaitingTask task = it.next();
                String id = task.getUserId() + "_" + task.getBubbleId();
                if (id.equals(taskId)) {
                    if (produceTime > 0 && task.getProduceTime() != produceTime) {
                        continue;
                    }
                    it.remove();
                }
            }
            updateDataStoreInternal(tasks);
        }
    }

    public void updateSingleTask(EnergyWaitingTask task) {
        synchronized (this) {
            List<EnergyWaitingTask> tasks = loadTasksInternal();
            String taskId = task.getTaskId();
            Iterator<EnergyWaitingTask> it = tasks.iterator();
            while (it.hasNext()) {
                EnergyWaitingTask existing = it.next();
                if (existing.getTaskId().equals(taskId)) {
                    it.remove();
                }
            }
            tasks.add(task);
            updateDataStoreInternal(tasks);
        }
    }

    public List<EnergyWaitingTask> loadTasks() {
        synchronized (this) {
            return loadTasksInternal();
        }
    }

    public void physicalWipeAllTasks() {
        synchronized (this) {
            try {
                File storeFile = getStoreFile();
                if (storeFile != null && storeFile.exists()) {
                    List<EnergyWaitingTask> tasks = loadTasksInternal();
                    if (!tasks.isEmpty()) {
                        storeFile.delete();
                        Log.record("已物理抹除所有森林持久化任务");
                    }
                }
            } catch (Throwable t) {
                Log.printStackTrace(t);
            }
        }
    }

    public void removePkTasks() {
        synchronized (this) {
            List<EnergyWaitingTask> tasks = loadTasksInternal();
            Iterator<EnergyWaitingTask> it = tasks.iterator();
            while (it.hasNext()) {
                if (it.next().isPk()) {
                    it.remove();
                }
            }
            updateDataStoreInternal(tasks);
        }
    }

    public void validateAndRestoreTasks(EnergyWaitingManager.EnergyWaitingTaskRestoreCallback addTaskCallback) {
        synchronized (this) {
            List<EnergyWaitingTask> tasks = loadTasksInternal();
            long currentTime = System.currentTimeMillis();
            Iterator<EnergyWaitingTask> it = tasks.iterator();
            while (it.hasNext()) {
                EnergyWaitingTask task = it.next();
                if (currentTime - task.getProduceTime() > MAX_TASK_AGE_MS) {
                    it.remove();
                    continue;
                }
                if (addTaskCallback != null) {
                    boolean accepted = addTaskCallback.onRestore(task);
                    if (!accepted) {
                        it.remove();
                    }
                }
            }
            updateDataStoreInternal(tasks);
        }
    }

    private JSONObject toJson(EnergyWaitingTask task) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("userId", task.getUserId());
            obj.put("userName", task.getUserName());
            obj.put("bubbleId", task.getBubbleId());
            obj.put("produceTime", task.getProduceTime());
            obj.put("fromTag", task.getFromTag());
            obj.put("retryCount", task.getRetryCount());
            obj.put("maxRetries", task.getMaxRetries());
            obj.put("shieldEndTime", task.getShieldEndTime());
            obj.put("bombEndTime", task.getBombEndTime());
            obj.put("isPk", task.isPk());
            obj.put("canDouble", task.isCanDouble());
            obj.put("isCancelled", task.isCancelled());
            obj.put("fullEnergy", task.getFullEnergy());
        } catch (Throwable t) {
            Log.printStackTrace(t);
        }
        return obj;
    }

    private EnergyWaitingTask fromJson(JSONObject obj) {
        try {
            return new EnergyWaitingTask(
                    obj.optString("userId"),
                    obj.optString("userName"),
                    obj.optLong("bubbleId"),
                    obj.optLong("produceTime"),
                    obj.optString("fromTag"),
                    obj.optInt("retryCount"),
                    obj.optInt("maxRetries"),
                    obj.optLong("shieldEndTime"),
                    obj.optLong("bombEndTime"),
                    obj.optBoolean("isPk"),
                    obj.optBoolean("canDouble"),
                    obj.optBoolean("isCancelled"),
                    obj.optInt("fullEnergy"));
        } catch (Throwable t) {
            Log.printStackTrace(t);
            return null;
        }
    }
}
