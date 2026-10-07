package com.surexu.sesame.model.task.videoRewards;

import com.surexu.sesame.hook.ApplicationHook;

public final class VideoRewardsRpcCall {
    private VideoRewardsRpcCall() {
    }

    public static String queryWallet() {
        return ApplicationHook.requestString("alipay.content.interact.task.wallet.v2", "[{\"pageIndex\":1,\"pageSize\":10,\"walletTab\":\"available\"}]", 1, -1);
    }

    public static String reserve() {
        return ApplicationHook.requestString("alipay.content.interact.task.reserve", "[{\"sourcePage\":\"\",\"taskType\":\"reserve\"}]", 1, -1);
    }
}
