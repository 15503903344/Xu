package com.surexu.sesame.model.task.antMember;

import com.surexu.sesame.util.Log;
import com.surexu.sesame.util.MessageUtil;
import com.surexu.sesame.util.Status;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class AntInsurance {
    private static final String TAG = "AntInsurance";

    public static void executeTask(Set<String> set) {
        if (set.contains("beanSignIn")) {
            beanSignIn();
        }
        if (set.contains("beanExchangeBubbleBoost")) {
            beanExchange("IT20230214000700069722");
        }
        if (set.contains("beanExchangeGoldenTicket")) {
            beanExchange("IT20240322000100086304");
        }
        if (set.contains("gainSumInsured")) {
            lotteryDraw();
            gainSumInsured();
        }
    }

    private static void gainSumInsured() {
        try {
            JSONObject jSONObject = new JSONObject(AntInsuranceRpcCall.queryMultiSceneWaitToGainList());
            if (MessageUtil.checkSuccess(TAG, jSONObject).booleanValue()) {
                JSONObject jSONObject2 = jSONObject.getJSONObject("data");
                Iterator<String> itKeys = jSONObject2.keys();
                while (itKeys.hasNext()) {
                    Object obj = jSONObject2.get(itKeys.next());
                    if (obj instanceof JSONArray) {
                        JSONArray jSONArray = (JSONArray) obj;
                        for (int i = 0; i < jSONArray.length(); i++) {
                            gainMyAndFamilySumInsured(jSONArray.getJSONObject(i));
                        }
                    } else if (obj instanceof JSONObject) {
                        JSONObject jSONObject3 = (JSONObject) obj;
                        if (jSONObject3.length() != 0) {
                            gainMyAndFamilySumInsured(jSONObject3);
                        }
                    }
                }
            }
        } catch (Throwable th) {
            String str = TAG;
            Log.i(str, "gainSumInsured err:");
            Log.printStackTrace(str, th);
        }
    }

    private static void gainMyAndFamilySumInsured(JSONObject jSONObject) {
        if (jSONObject == null || jSONObject.optInt("sendType", 2) != 1) {
            return;
        }
        try {
            jSONObject.put("entrance", "jkj_zhima_dairy66");
            JSONObject jSONObject2 = new JSONObject(AntInsuranceRpcCall.gainMyAndFamilySumInsured(jSONObject));
            if (MessageUtil.checkSuccess(TAG, jSONObject2).booleanValue()) {
                Log.other("蚂蚁保障🛡️领取保障金#获得[" + jSONObject2.getJSONObject("data").getJSONObject("gainSumInsuredDTO").optString("gainSumInsuredYuan") + "元保额]");
            }
        } catch (Throwable th) {
            String str = TAG;
            Log.i(str, "gainMyAndFamilySumInsured err:");
            Log.printStackTrace(str, th);
        }
    }

    private static void lotteryDraw() {
        if (Status.hasFlagToday("insurance::lotteryDraw").booleanValue()) {
            return;
        }
        try {
            JSONObject jSONObject = new JSONObject(AntInsuranceRpcCall.queryAvailableNum());
            String str = TAG;
            if (MessageUtil.checkSuccess(str, jSONObject).booleanValue()) {
                if (jSONObject.getJSONObject("result").getInt("num") == 3) {
                    JSONObject jSONObject2 = new JSONObject(AntInsuranceRpcCall.lotteryDraw());
                    if (!MessageUtil.checkSuccess(str, jSONObject2).booleanValue()) {
                        return;
                    }
                    JSONArray jSONArray = jSONObject2.getJSONArray("result");
                    for (int i = 0; i < jSONArray.length(); i++) {
                        Log.other("蚂蚁保障🛡️天天领取保障福利#获得[" + jSONArray.getJSONObject(i).getString("prizeName") + "]");
                    }
                }
                Status.flagToday("insurance::lotteryDraw");
            }
        } catch (Throwable th) {
            String str2 = TAG;
            Log.i(str2, "lotteryDraw err:");
            Log.printStackTrace(str2, th);
        }
    }

    private static void beanSignIn() {
        try {
            JSONObject jSONObject = new JSONObject(AntInsuranceRpcCall.beanQuerySignInProcess());
            String str = TAG;
            if (MessageUtil.checkSuccess(str, jSONObject).booleanValue() && jSONObject.getJSONObject("result").getBoolean("canPush")) {
                JSONObject jSONObject2 = new JSONObject(AntInsuranceRpcCall.beanSignInTrigger());
                if (MessageUtil.checkSuccess(str, jSONObject2).booleanValue()) {
                    Log.other("蚂蚁保障🛡️安心豆签到#获得[" + jSONObject2.getJSONObject("result").getJSONArray("prizeSendOrderDTOList").getJSONObject(0).getString("prizeName") + "]");
                }
            }
        } catch (Throwable th) {
            String str2 = TAG;
            Log.i(str2, "beanSignIn err:");
            Log.printStackTrace(str2, th);
        }
    }

    private static void beanExchange(String str) {
        try {
            JSONObject jSONObject = new JSONObject(AntInsuranceRpcCall.queryUserAccountInfo("INS_BLUE_BEAN"));
            String str2 = TAG;
            if (MessageUtil.checkSuccess(str2, jSONObject).booleanValue()) {
                int i = jSONObject.getJSONObject("result").getInt("userCurrentPoint");
                JSONObject jSONObject2 = new JSONObject(AntInsuranceRpcCall.beanExchangeDetail(str));
                if (MessageUtil.checkSuccess(str2, jSONObject2).booleanValue()) {
                    JSONObject jSONObject3 = jSONObject2.getJSONObject("result").getJSONObject("rspContext").getJSONObject("params").getJSONObject("exchangeDetail");
                    String string = jSONObject3.getString("itemName");
                    JSONObject jSONObject4 = jSONObject3.getJSONObject("itemExchangeConsultDTO");
                    int i2 = jSONObject4.getInt("realConsumePointAmount");
                    if (jSONObject4.getBoolean("canExchange") && i2 <= i && MessageUtil.checkSuccess(str2, new JSONObject(AntInsuranceRpcCall.beanExchange(str, i2))).booleanValue()) {
                        Log.other("蚂蚁保障🛡️安心豆兑换[" + string + "]#消耗[" + i2 + "安心豆]");
                    }
                }
            }
        } catch (Throwable th) {
            String str3 = TAG;
            Log.i(str3, "beanExchange err:");
            Log.printStackTrace(str3, th);
        }
    }
}
