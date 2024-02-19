package com.itman.datakit.admin.common.dataroute;

public class RouteContext {
    private RouteContext() {
    }

    private static ThreadLocal<String> routeKey = new ThreadLocal<>();

    public static String getRouteKey() {
        return routeKey.get();
    }

    public static void setRouteKey(String key) {
        routeKey.set(key);
    }

    public static void removeRouteKey() {
        routeKey.remove();
    }
}
