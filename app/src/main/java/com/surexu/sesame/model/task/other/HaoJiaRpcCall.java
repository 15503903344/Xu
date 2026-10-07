package com.surexu.sesame.model.task.other;

import com.surexu.sesame.hook.ApplicationHook;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

public final class HaoJiaRpcCall {
    private static final String CHANNEL = "jiaofei_card_promo";
    private static final String OPERATION_PARAM_ID = "independent_component_program2023082800847098";

    private HaoJiaRpcCall() {
    }

    public static String querySignIn() throws JSONException {
        return request("independent_component_sign_in_00966139_independent_component_sign_in_recall", null);
    }

    public static String doSignIn(String str) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("code", str);
        return request("independent_component_sign_in_00966139_independent_component_sign_in", jSONObject);
    }

    public static String queryTaskList() throws JSONException {
        return request("independent_component_task_reward_00793835_independent_component_task_reward_query", null);
    }

    public static String applyTask(String str) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("code", str);
        return request("independent_component_task_reward_00793835_independent_component_task_reward_apply", jSONObject);
    }

    private static String request(String str, JSONObject jSONObject) throws JSONException {
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("channel", CHANNEL);
        if (jSONObject != null) {
            Iterator<String> itKeys = jSONObject.keys();
            while (itKeys.hasNext()) {
                String next = itKeys.next();
                jSONObject2.put(next, jSONObject.get(next));
            }
        }
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put(str, jSONObject2);
        JSONObject jSONObject4 = new JSONObject();
        jSONObject4.put("channel", CHANNEL);
        jSONObject4.put("components", jSONObject3);
        jSONObject4.put("operationParamIdentify", OPERATION_PARAM_ID);
        jSONObject4.put("source", "jiaofei");
        return ApplicationHook.requestString("alipay.imasp.program.programInvoke", "[" + jSONObject4 + "]");
    }
}
