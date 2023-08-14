package com.yeonfish.phone2webcam.common;

import kotlin.jvm.JvmStatic;

public class ApiConfig {
    @JvmStatic
    public static String getApiUrl(String url) {
        StringBuilder apiUrl = new StringBuilder();
        apiUrl.append(url);
        return apiUrl.toString();
    }
}
