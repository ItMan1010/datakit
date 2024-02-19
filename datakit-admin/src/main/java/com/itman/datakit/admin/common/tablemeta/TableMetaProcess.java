package com.itman.datakit.admin.common.tablemeta;

import com.itman.datakit.admin.common.dataroute.DruidConfig;
import com.itman.datakit.admin.common.exception.DatakitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class TableMetaProcess {
    private final TableMetaGenerator tableMetaGenerator;
    private final DruidConfig druidConfig;

    public List<TableColumnBean> getTableColumns(String dataBase, String tableName) throws DatakitException {
        Optional.ofNullable(druidConfig.getDruid()).orElseThrow(() -> new DatakitException("getTableColumns", dataBase + "数据源为空!"));
        String dataBaseType = druidConfig.getUrlDbType(dataBase);
        String schemaName = druidConfig.getSchema(dataBase);
        String userName = druidConfig.getUserName(dataBase);
        Optional.ofNullable(schemaName).orElseThrow(() -> new DatakitException("getTableColumns", dataBase + "获取jdbcUrl失败"));
        return tableMetaGenerator.getTableColumns(dataBase, dataBaseType, schemaName, userName, tableName);
    }

    public List<String> getTableNameList(String dataBase) throws DatakitException {
        Optional.ofNullable(druidConfig.getDruid()).orElseThrow(() -> new DatakitException("getTableNameList", dataBase + "数据源为空!"));
        String dataBaseType = druidConfig.getUrlDbType(dataBase);
        String schemaName = druidConfig.getSchema(dataBase);
        String userName = druidConfig.getUserName(dataBase);
        Optional.ofNullable(schemaName).orElseThrow(() -> new DatakitException("getTableNameList", dataBase + "获取jdbcUrl失败"));
        return tableMetaGenerator.getTableNameList(dataBase, dataBaseType, schemaName, userName);
    }
}
