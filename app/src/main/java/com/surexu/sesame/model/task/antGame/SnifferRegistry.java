package com.surexu.sesame.model.task.antGame;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SnifferRegistry {
    private static final Map<String, IGameSniffer> sniffers = new LinkedHashMap<>();

    private static final SnifferRegistry INSTANCE = new SnifferRegistry();

    private SnifferRegistry() {
        register(DglsSniffer.INSTANCE);
        register(XjskpSniffer.INSTANCE);
        register(HnycSniffer.INSTANCE);
        register(LmctSniffer.INSTANCE);
        register(ZgwhxSniffer.INSTANCE);
    }

    public static SnifferRegistry getInstance() {
        return INSTANCE;
    }

    private void register(IGameSniffer sniffer) {
        sniffers.put(sniffer.getAppId(), sniffer);
    }

    public String detectEnvironment(Map<String, String> params, String url, Map<String, String> headers) {
        String joined = String.join(",", params.values());
        String referer = headers.get("referer");
        if (referer == null) {
            referer = "";
        }
        String lowerCase = (joined + url + referer).toLowerCase(Locale.ROOT);
        if (lowerCase.contains("senlin") || lowerCase.contains("mayisenlin")) {
            return "AntForest";
        }
        if (lowerCase.contains("zhuangyuan") || lowerCase.contains("antfarm")) {
            return "AntFarm";
        }
        return (lowerCase.contains("haiyang") || lowerCase.contains("ocean")) ? "AntOcean" : "GameCenter";
    }

    public IGameSniffer getSniffer(String appId) {
        return sniffers.get(appId);
    }

    public String quickExtract(String key, String source) {
        Matcher matcher = Pattern.compile("(?i)(?:" + key + ")[\"=:\\s]*([^&\" \n]+)").matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
