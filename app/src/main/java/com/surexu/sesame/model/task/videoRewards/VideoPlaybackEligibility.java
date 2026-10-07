package com.surexu.sesame.model.task.videoRewards;

public final class VideoPlaybackEligibility {
    private VideoPlaybackEligibility() {
    }

    public static boolean qualifies(String str, String str2, long j, long j2, long j3) {
        return str != null && str2 != null && !str.isEmpty() && str.equals(str2) && j2 > 0 && j >= 0 && j3 >= 0 && j >= Math.min(j2, j3) && j <= j2 + 2000;
    }
}
