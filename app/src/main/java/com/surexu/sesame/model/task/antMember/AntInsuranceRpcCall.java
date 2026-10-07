package com.surexu.sesame.model.task.antMember;

import com.surexu.sesame.hook.ApplicationHook;
import org.json.JSONObject;

public class AntInsuranceRpcCall {
    public static String queryMultiSceneWaitToGainList() {
        return ApplicationHook.requestString("com.alipay.insgiftbff.insgiftMain.queryMultiSceneWaitToGainList", "[{\"entrance\":\"jkj_zhima_dairy66\",\"eventToWaitParamDTO\":{\"giftProdCode\":\"GIFT_UNIVERSAL_COVERAGE\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]},\"helpChildParamDTO\":{\"giftProdCode\":\"GIFT_HEALTH_GOLD_CHILD\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]},\"priorityChannelParamDTO\":{\"giftProdCode\":\"GIFT_UNIVERSAL_COVERAGE\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]},\"signInParamDTO\":{\"giftProdCode\":\"GIFT_UNIVERSAL_COVERAGE\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]}}]");
    }

    public static String gainMyAndFamilySumInsured(JSONObject jSONObject) {
        return ApplicationHook.requestString("com.alipay.insgiftbff.insgiftMain.gainMyAndFamilySumInsured", "[" + jSONObject + "]");
    }

    public static String queryAvailableNum() {
        return ApplicationHook.requestString("com.alipay.insmarketingbff.lottery.queryAvailableNum", "[{\"planId\":\"INSP10723155\",\"scene\":\"INSIOP_BUILD_SCENE_1000100100192_@alipay/insiop-lottery-image-draw\"}]");
    }

    public static String lotteryDraw() {
        return ApplicationHook.requestString("com.alipay.insmarketingbff.lottery.draw", "[{\"extParams\":{\"componentType\":\"insiop-lottery-draw-image\"},\"planId\":\"INSP10723155\",\"scene\":\"INSIOP_BUILD_SCENE_1000100100192_@alipay/insiop-lottery-image-draw\"}]");
    }

    public static String queryUserAccountInfo(String str) {
        return ApplicationHook.requestString("com.alipay.insmarketingbff.point.queryUserAccountInfo", "[{\"channel\":\"HiChat\",\"pointProdCode\":\"" + str + "\",\"pointUnitType\":\"COUNT\"}]");
    }

    public static String oneStopPlanTriggerExchangeDetail(String str, String str2) {
        return ApplicationHook.requestString("com.alipay.insmarketingbff.onestop.planTrigger", "[{\"extParams\":{\"itemId\":\"" + str2 + "\"},\"planCode\":\"" + str + "\",\"planOperateCode\":\"exchangeDetail\"}]");
    }

    public static String oneStopPlanTriggerExchange(String str, String str2, int i) {
        return ApplicationHook.requestString("com.alipay.insmarketingbff.onestop.planTrigger", "[{\"extParams\":{\"itemId\":\"" + str2 + "\",\"pointAmount\":\"" + i + "\"},\"planCode\":\"" + str + "\",\"planOperateCode\":\"exchange\"}]");
    }

    public static String querySignInProcess(String str, String str2) {
        return ApplicationHook.requestString("com.alipay.insmarketingbff.bean.querySignInProcess", "[{\"appletId\":\"" + str + "\",\"scene\":\"" + str2 + "\"}]");
    }

    public static String beanQuerySignInProcess() {
        return querySignInProcess("AP16242232", "INS_BLUE_BEAN_SIGN");
    }

    public static String beanSignInTrigger() {
        return ApplicationHook.requestString("com.alipay.insmarketingbff.bean.signInTrigger", "[{\"appletId\":\"AP16242232\",\"scene\":\"INS_BLUE_BEAN_SIGN\"}]");
    }

    public static String beanExchangeDetail(String str) {
        return oneStopPlanTriggerExchangeDetail("bluebean_onestop", str);
    }

    public static String beanExchange(String str, int i) {
        return oneStopPlanTriggerExchange("bluebean_onestop", str, i);
    }
}
