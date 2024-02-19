package com.itman.datakit.admin.common.dataroute;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DruidProperties {
    private String dataBase;
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private String initialSize;
    private String minIdle;
    private String maxActive;
    private String validationQuery;
    private String testOnBorrow;
    private String testWhileIdle;
    private String timeBetweenEvictionRunsMillis;
    private String encode;

    private String urlDbType;
    private String urlHost;
    private String urlPort;
    private String urlDatabase;
}
