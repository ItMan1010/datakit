package com.itman.datakit.admin.common.dataroute;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;

public class RouteChoose extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return RouteContext.getRouteKey();
    }

    public RouteChoose(DataSource defaultTargetDataSource, Map<Object, Object> targetDataSources) {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }
}
