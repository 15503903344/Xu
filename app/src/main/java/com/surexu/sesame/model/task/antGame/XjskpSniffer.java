package com.surexu.sesame.model.task.antGame;

import java.util.Collections;
import java.util.Map;

public final class XjskpSniffer implements IGameSniffer {
    public static final XjskpSniffer INSTANCE = new XjskpSniffer();
    private static final String appId = "2060170000353846";

    private XjskpSniffer() {
    }

    @Override
    public Map<String, String> extract(String url, Map<String, String> headers, String body) {
        if (!url.contains("scgame.com.cn") && !url.contains("hnycgames.cn")) {
            return null;
        }
        String extracted = SnifferRegistry.getInstance().quickExtract("uin", url + body);
        if (extracted == null) {
            extracted = SnifferRegistry.getInstance().quickExtract("AuthUin", url + body);
        }
        if (extracted == null) {
            return null;
        }
        return Collections.singletonMap("uin", extracted);
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
