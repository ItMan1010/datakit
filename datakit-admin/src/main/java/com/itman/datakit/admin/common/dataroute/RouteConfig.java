package com.itman.datakit.admin.common.dataroute;

import com.alibaba.druid.pool.DruidDataSource;
import com.itman.datakit.admin.common.constants.DataBaseTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RouteConfig {
    private final DruidConfig druidConfig;

    private Properties changeToProperties(DruidProperties druidProperties) {
        Properties properties = new Properties();
        properties.setProperty("druid.name", druidProperties.getUsername());
        properties.setProperty("druid.url", druidProperties.getUrl());
        properties.setProperty("druid.username", druidProperties.getUsername());
        properties.setProperty("druid.password", druidProperties.getPassword());
        properties.setProperty("druid.testWhileIdle", druidProperties.getTestWhileIdle());
        properties.setProperty("druid.testOnBorrow", druidProperties.getTestOnBorrow());
        properties.setProperty("druid.validationQuery", druidProperties.getValidationQuery());
        properties.setProperty("druid.driverClassName", druidProperties.getDriverClassName());
        properties.setProperty("druid.initialSize", druidProperties.getInitialSize());
        properties.setProperty("druid.minIdle", druidProperties.getMinIdle());
        properties.setProperty("druid.maxActive", druidProperties.getMaxActive());
        return properties;
    }

    void parseJdbcUrl(DruidProperties druidProperties) {
        for (DataBaseTypeEnum type : DataBaseTypeEnum.values()) {
            if (parseJdbcUrl(type.getUrlPattern(), druidProperties)) {
                return;
            }
        }
    }

    Boolean parseJdbcUrl(String pattern, DruidProperties druidProperties) {
        Pattern namePattern = Pattern.compile(pattern);
        Matcher dateMatcher = namePattern.matcher(druidProperties.getUrl());
        Boolean parserFlag = false;
        while (dateMatcher.find()) {
            druidProperties.setUrlDbType(dateMatcher.group("type"));
            druidProperties.setUrlHost(dateMatcher.group("host"));
            druidProperties.setUrlPort(dateMatcher.group("port"));
            druidProperties.setUrlDatabase(dateMatcher.group("database"));
            parserFlag = true;
        }
        return parserFlag;
    }

    @Bean
    @Primary
    public RouteChoose dataSource() {
        Map<Object, Object> druidDataSourceMap = new HashMap<>(druidConfig.getDruid().size());
        for (Integer i = 0; i < druidConfig.getDruid().size(); i++) {
            DruidDataSource druidDataSource = new DruidDataSource();
            druidDataSource.configFromPropety(changeToProperties(druidConfig.getDruid().get(i)));
            druidDataSourceMap.put("db" + i, druidDataSource);
            druidConfig.getDruid().get(i).setDataBase("db" + i);
            parseJdbcUrl(druidConfig.getDruid().get(i));
        }
        return new RouteChoose((DruidDataSource) druidDataSourceMap.get("db0"), druidDataSourceMap);
    }
}
