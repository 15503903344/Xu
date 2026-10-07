package com.surexu.sesame.model.task.antGame;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LmctSniffer implements IGameSniffer {
    public static final LmctSniffer INSTANCE = new LmctSniffer();
    private static final String appId = "2060170000365923";

    private LmctSniffer() {
    }

    @Override
    public Map<String, String> extract(String url, Map<String, String> headers, String body) {
        if (!url.contains("microfun.cn") && !url.contains("akbing.com")) {
            return null;
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        Pattern openIdPattern = Pattern.compile("(?i)(?:openID|device_id)[\"\\\\: =]+([a-zA-Z0-9][a-zA-Z0-9_-]{15,})");
        Pattern tokenPattern = Pattern.compile("(?i)token[\"\\\\: =]+([a-zA-Z0-9]{5,})");
        Pattern userIdPattern = Pattern.compile("(?i)userid[\"\\\\: =]+([a-zA-Z0-9]{5,})");
        Pattern channelPattern = Pattern.compile("(?i)channel[\"\\\\: =]+([a-zA-Z0-9_-]{5,})");
        Matcher matcher = openIdPattern.matcher(body);
        if (matcher.find() && matcher.group(1) != null) {
            result.put("openid", matcher.group(1));
        }
        String combined = url + body;
        matcher = tokenPattern.matcher(combined);
        if (matcher.find() && matcher.group(1) != null) {
            result.put("token", matcher.group(1));
        }
        matcher = userIdPattern.matcher(combined);
        if (matcher.find() && matcher.group(1) != null) {
            result.put("userid", matcher.group(1));
        }
        matcher = channelPattern.matcher(body);
        if (matcher.find() && matcher.group(1) != null) {
            result.put("channel", matcher.group(1));
        }
        if (result.containsKey("openid")) {
            return result;
        }
        return null;
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
