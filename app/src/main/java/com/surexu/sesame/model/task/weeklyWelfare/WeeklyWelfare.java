package com.surexu.sesame.model.task.weeklyWelfare;

import com.surexu.sesame.data.ModelFields;
import com.surexu.sesame.data.modelFieldExt.BooleanModelField;
import com.surexu.sesame.model.task.rewardSupport.IsolatedRewardTask;
import com.surexu.sesame.util.Log;
import java.math.BigDecimal;
import org.json.JSONArray;
import org.json.JSONObject;

public final class WeeklyWelfare extends IsolatedRewardTask {
    private static final String INDEX = "[{\"chInfo\":\"goldbill\",\"modeBitMask\":513}]";
    private static final String PREFIX = "com.alipay.finaggexpbff.needle.weeklyWelfare.";
    private BooleanModelField signIn;
    private BooleanModelField weeklyPrize;

    @Override
    public String getName() {
        return "黄金票每周福利";
    }

    @Override
    protected void addFields(ModelFields modelFields) {
        BooleanModelField booleanModelField = new BooleanModelField("signIn", "每日签到", false);
        this.signIn = booleanModelField;
        modelFields.addField(booleanModelField);
        BooleanModelField booleanModelField2 = new BooleanModelField("weeklyPrize", "第七日签到后领取周奖励", false);
        this.weeklyPrize = booleanModelField2;
        modelFields.addField(booleanModelField2);
    }

    private JSONObject today(JSONObject jSONObject) throws Exception {
        JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("result");
        JSONObject jSONObjectOptJSONObject2 = jSONObjectOptJSONObject == null ? null : jSONObjectOptJSONObject.optJSONObject("upsertData");
        JSONObject jSONObjectOptJSONObject3 = jSONObjectOptJSONObject2 == null ? null : jSONObjectOptJSONObject2.optJSONObject("sign");
        JSONArray jSONArrayOptJSONArray = jSONObjectOptJSONObject3 == null ? null : jSONObjectOptJSONObject3.optJSONArray("timeline");
        if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() > 31) {
            return null;
        }
        JSONObject jSONObject2 = null;
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObjectOptJSONObject4 = jSONArrayOptJSONArray.optJSONObject(i);
            if (jSONObjectOptJSONObject4 != null && Boolean.TRUE.equals(jSONObjectOptJSONObject4.opt("isToday"))) {
                if (jSONObject2 != null || !(jSONObjectOptJSONObject4.opt("signed") instanceof Boolean)) {
                    return null;
                }
                jSONObject2 = jSONObjectOptJSONObject4;
            }
        }
        return jSONObject2;
    }

    @Override
    protected void execute(IsolatedRewardTask.Run run) throws Exception {
        JSONObject jSONObject = today(run.query("com.alipay.finaggexpbff.needle.weeklyWelfare.index", INDEX));
        if (jSONObject == null) {
            Log.record(getName() + "：没有唯一且明确的今日签到状态");
            return;
        }
        if (jSONObject.getBoolean("signed")) {
            Log.record(getName() + "：今日已签到");
            return;
        }
        if (!this.signIn.getValue().booleanValue()) {
            Log.record(getName() + "：可签到，签到开关未开启");
            return;
        }
        String strOptString = jSONObject.optString("basePrizeNum", "");
        String strOptString2 = jSONObject.optString("prizeNum", "");
        int iOptInt = jSONObject.optInt("day", -1);
        if (!strOptString.matches("[0-9]{1,9}(\\.[0-9]{1,4})?") || !strOptString2.matches("[0-9]{1,9}(\\.[0-9]{1,4})?") || iOptInt < 1 || iOptInt > 7) {
            Log.record(getName() + "：签到奖励格式不明确，本轮结束");
            return;
        }
        if (run.onceToday("sign", "com.alipay.finaggexpbff.needle.weeklyWelfare.trigger", new JSONArray().put(new JSONObject().put("basePrize", new BigDecimal(strOptString)).put("prizeNum", strOptString2).put("type", "SIGN")).toString(), new IsolatedRewardTask.Allowed() {
            @Override
            public final boolean isAllowed() {
                return WeeklyWelfare.this.signIn.getValue().booleanValue();
            }
        }) == null) {
            return;
        }
        Log.record(getName() + "：签到接口返回成功");
        if (iOptInt == 7 && this.weeklyPrize.getValue().booleanValue()) {
            JSONObject jSONObject2 = today(run.query("com.alipay.finaggexpbff.needle.weeklyWelfare.index", INDEX));
            if (jSONObject2 == null || jSONObject2.optInt("day", -1) != 7 || !jSONObject2.getBoolean("signed")) {
                Log.record(getName() + "：第七日签到未确认，停止领取周奖励");
                return;
            }
            if (run.onceToday("weeklyPrize", "com.alipay.finaggexpbff.needle.weeklyWelfare.trigger", "[{\"type\":\"SIGN_PRIZE\"}]", new IsolatedRewardTask.Allowed() {
                @Override
                public final boolean isAllowed() {
                    return WeeklyWelfare.this.weeklyPrize.getValue().booleanValue();
                }
            }) != null) {
                Log.record(getName() + "：周奖励接口返回成功");
            }
        }
    }
}
