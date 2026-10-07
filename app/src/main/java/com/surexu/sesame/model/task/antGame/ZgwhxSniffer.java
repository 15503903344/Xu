package com.surexu.sesame.model.task.antGame;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ZgwhxSniffer implements IGameSniffer {
    public static final ZgwhxSniffer INSTANCE = new ZgwhxSniffer();
    private static final String appId = "2021005132680209";

    private ZgwhxSniffer() {
    }

    @Override
    public Map<String, String> extract(String url, Map<String, String> headers, String body) {
        if (!url.contains("dianwanshidai.com")) {
            return null;
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        String openId = SnifferRegistry.getInstance().quickExtract("openid", body + url);
        if (openId != null) {
            result.put("openid", openId);
        }
        String appIdHeader = headers.get("app-id");
        if (appIdHeader != null) {
            result.put("app_id", appIdHeader);
        }
        String bgid = headers.get("bgid");
        if (bgid != null) {
            result.put("bgid", bgid);
        }
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    @Override
    public String getAppId() {
        return appId;
    }

    @Override
    public String resolveScene(Map<String, String> params, String url, Map<String, String> headers) {
        return SnifferRegistry.getInstance().detectEnvironment(params, url, headers);
    }
}
