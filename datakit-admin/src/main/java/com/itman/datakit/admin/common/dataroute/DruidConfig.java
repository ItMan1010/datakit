package com.itman.datakit.admin.common.dataroute;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO
 *
 * @author: ItMan
 * @since: 2023/12/01  17:22
 */
@Component
@ConfigurationProperties(prefix = "spring.datasource")
@Data
public class DruidConfig {
    public List<DruidProperties> druid;

    public String getUrlDbType(String dataBaseType) {
        List<DruidProperties> druidPropertiesList = druid.stream().
                filter(x -> x.getDataBase().equals(dataBaseType)).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(druidPropertiesList)) {
            return druidPropertiesList.get(0).getUrlDbType();
        } else {
            return null;
        }
    }

    public String getSchema(String dataBaseType) {
        List<DruidProperties> druidPropertiesList = druid.stream().
                filter(x -> x.getDataBase().equals(dataBaseType)).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(druidPropertiesList)) {
            return druidPropertiesList.get(0).getUrlDatabase();
        } else {
            return null;
        }
    }

    public String getUserName(String dataBaseType) {
        List<DruidProperties> druidPropertiesList = druid.stream().
                filter(x -> x.getDataBase().equals(dataBaseType)).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(druidPropertiesList)) {
            return druidPropertiesList.get(0).getUsername();
        } else {
            return null;
        }
    }

    public String getJdbcUrl(String dataBaseType) {
        List<DruidProperties> druidPropertiesList = druid.stream().
                filter(x -> x.getDataBase().equals(dataBaseType)).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(druidPropertiesList)) {
            return druidPropertiesList.get(0).getUrl();
        } else {
            return null;
        }
    }
}
