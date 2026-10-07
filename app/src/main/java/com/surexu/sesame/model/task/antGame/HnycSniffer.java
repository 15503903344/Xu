package com.surexu.sesame.model.task.antGame;

import java.util.Collections;
import java.util.Map;

public final class HnycSniffer implements IGameSniffer {
    public static final HnycSniffer INSTANCE = new HnycSniffer();
    private static final String appId = "2021004163668677";

    private HnycSniffer() {
    }

    @Override
    public Map<String, String> extract(String url, Map<String, String> headers, String body) {
        if (!url.contains("hnycgames.cn") && !url.contains("babigame.cn")) {
            return null;
        }
        String extracted = SnifferRegistry.getInstance().quickExtract("yxtChannelUserId", url + body);
        if (extracted == null) {
            extracted = SnifferRegistry.getInstance().quickExtract("wxOpenID", url + body);
        }
        if (extracted == null) {
            return null;
        }
        return Collections.singletonMap("yxtChannelUserId", extracted);
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
