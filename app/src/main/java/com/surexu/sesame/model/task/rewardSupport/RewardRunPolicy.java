package com.surexu.sesame.model.task.rewardSupport;

import java.util.Objects;

public final class RewardRunPolicy {
    public static boolean mayQuery(long j, long j2) {
        return j >= j2;
    }

    private RewardRunPolicy() {
    }

    public static boolean sameAccount(String str, String str2) {
        return (str == null || str.isEmpty() || !Objects.equals(str, str2)) ? false : true;
    }

    public static boolean mayAttempt(String str, String str2) {
        return (str == null || str.isEmpty() || str.equals(str2)) ? false : true;
    }

    public static boolean mayCheckIn(String str) {
        return "CHECK_IN".equals(str);
    }
}
