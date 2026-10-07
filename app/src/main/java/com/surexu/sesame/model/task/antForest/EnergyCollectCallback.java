package com.surexu.sesame.model.task.antForest;

import java.util.Collection;
import java.util.List;

public interface EnergyCollectCallback {

    CollectResult collectUserEnergyForWaiting(List<EnergyWaitingTask> tasks);

    void addToTotalCollected(int energyCount);

    long getWaitingCollectDelay();

    void onPreRevalidateProp(String userId, String fromTag);

    void onSmartPropDecision(Collection<EnergyWaitingTask> allWaitingTasks);
}
