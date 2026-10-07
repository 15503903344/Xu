package com.surexu.sesame.model.task.antGame;

import java.util.Collections;
import java.util.Map;

public final class DglsSniffer implements IGameSniffer {
    public static final DglsSniffer INSTANCE = new DglsSniffer();
    private static final String appId = "2021005139650112";

    private DglsSniffer() {
    }

    @Override
    public Map<String, String> extract(String url, Map<String, String> headers, String body) {
        if (!url.contains("haligame.com") && !url.contains("shenzhenyuren.com")) {
            return null;
        }
        String extracted = SnifferRegistry.getInstance().quickExtract("userId", body);
        if (extracted == null) {
            extracted = SnifferRegistry.getInstance().quickExtract("open_id", body + url);
        }
        if (extracted == null) {
            return null;
        }
        return Collections.singletonMap("open_id", extracted);
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
