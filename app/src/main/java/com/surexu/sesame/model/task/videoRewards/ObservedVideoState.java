package com.surexu.sesame.model.task.videoRewards;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ObservedVideoState {
    public final String contentId;
    public final long currentMs;
    public final long durationMs;
    public final boolean ended;
    public final boolean paused;

    private ObservedVideoState(String str, long j, long j2, boolean z, boolean z2) {
        this.contentId = str;
        this.currentMs = j;
        this.durationMs = j2;
        this.paused = z;
        this.ended = z2;
    }

    public static ObservedVideoState fromValues(String str, long j, long j2, boolean z, boolean z2) {
        if (str == null || str.isEmpty() || j < 0 || j2 <= 0 || j > 2000 + j2) {
            return null;
        }
        return new ObservedVideoState(str, j, j2, z, z2);
    }

    public static ObservedVideoState fromPageState(JSONObject jSONObject, String str) {
        JSONArray jSONArrayOptJSONArray;
        if (jSONObject == null || str == null || str.isEmpty() || (jSONArrayOptJSONArray = jSONObject.optJSONArray("videos")) == null || jSONArrayOptJSONArray.length() > 4) {
            return null;
        }
        ObservedVideoState observedVideoState = null;
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject != null && str.equals(jSONObjectOptJSONObject.optString("key"))) {
                long jOptLong = jSONObjectOptJSONObject.optLong("currentMs", -1L);
                long jOptLong2 = jSONObjectOptJSONObject.optLong("durationMs", -1L);
                if (jOptLong < 0 || jOptLong2 <= 0 || jOptLong > 2000 + jOptLong2 || observedVideoState != null) {
                    return null;
                }
                observedVideoState = new ObservedVideoState(str, jOptLong, jOptLong2, jSONObjectOptJSONObject.optBoolean("paused", true), jSONObjectOptJSONObject.optBoolean("ended", false));
            }
        }
        return observedVideoState;
    }

    public boolean qualifies(long j) {
        if (!this.paused) {
            String str = this.contentId;
            if (VideoPlaybackEligibility.qualifies(str, str, this.currentMs, this.durationMs, j)) {
                return true;
            }
        }
        return false;
    }
}
