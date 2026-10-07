package com.surexu.sesame.model.task.antGame;

import java.util.Map;

public interface IGameSniffer {
    Map<String, String> extract(String url, Map<String, String> headers, String body);

    String getAppId();

    String resolveScene(Map<String, String> params, String url, Map<String, String> headers);
}
