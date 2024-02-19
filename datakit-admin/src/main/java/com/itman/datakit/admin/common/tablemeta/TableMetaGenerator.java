package com.itman.datakit.admin.common.tablemeta;

import com.itman.datakit.admin.common.constants.DataBaseTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class TableMetaGenerator {
    // 自动引入(前提是 搭建了spring boot+mybatis架构
    private final SqlSessionFactory sqlSessionFactory;

    public static final String COLUMN_NAME = "COLUMN_NAME";

    public void judgeKey(String keyColumn, TableColumnBean columnBean) {
        columnBean.setKeyFlag((!StringUtils.isEmpty(keyColumn) && keyColumn.equalsIgnoreCase(columnBean.getColumnName())) ? true : false);
    }

    public void judgeIndex(List<String> indexColumns, TableColumnBean columnBean) {
        for (String indexColumn : indexColumns) {
            if (indexColumn != null && indexColumn.equalsIgnoreCase(columnBean.getColumnName())) {
                columnBean.setIndexFlag(true);
            }
        }
    }

    public List<TableColumnBean> getTableMeta(String dataBase, String dataBaseType, String schemaName, String userName, String tableName) {
        List<TableColumnBean> columnList = new ArrayList<>();
        SqlSession sqlSession = null;
        try {
            sqlSession = sqlSessionFactory.openSession();
            // 获取连接、获取元数据
            DatabaseMetaData metaData = sqlSession.getConnection().getMetaData();
            String catalog = null;
            String schema = null;
            String tableNameNew = null;

            if (DataBaseTypeEnum.of(dataBaseType).isLikeMysql()) {
                catalog = schemaName;
                tableNameNew = tableName.toLowerCase();
            } else if (DataBaseTypeEnum.of(dataBaseType).isLikePostgres()) {
                catalog = schemaName;
                schema = "public";
                tableNameNew = tableName.toLowerCase();
            } else if (DataBaseTypeEnum.of(dataBaseType).isLikeOracle()) {
                schema = userName.toUpperCase();
                tableNameNew = tableName.toUpperCase();
            } else {
                schema = schemaName;
                tableNameNew = tableName.toLowerCase();
            }

            ResultSet primaryKeys = metaData.getPrimaryKeys(catalog, schema, tableNameNew);
            String keyColumn = null;
            while (primaryKeys.next()) {
                if (primaryKeys.getString(COLUMN_NAME) != null) {
                    keyColumn = primaryKeys.getString(COLUMN_NAME);
                    break;
                }
            }
            primaryKeys.close();

            //获取索引
            List<String> indexColumns = new ArrayList<>();
            ResultSet indexInfo = metaData.getIndexInfo(catalog, schema, tableNameNew, false, false);
            while (indexInfo.next()) {
                if (indexInfo.getString(COLUMN_NAME) != null) {
                    String indexColumn = indexInfo.getString(COLUMN_NAME);
                    indexColumns.add(indexColumn);
                }
            }
            indexInfo.close();

            // 获取 表字段
            ResultSet columns = metaData.getColumns(catalog, schema, tableNameNew, null);
            while (columns.next()) {
                TableColumnBean columnBean = TableColumnBean.builder().
                        columnName(columns.getString(COLUMN_NAME).toUpperCase()).
                        columnType(columns.getString("TYPE_NAME")).
                        columnLength(columns.getInt("COLUMN_SIZE")).
                        nullAble(columns.getInt("NULLABLE")).
                        build();
                judgeKey(keyColumn, columnBean);
                judgeIndex(indexColumns, columnBean);
                columnList.add(columnBean);
            }
            columns.close();
        } catch (SQLException e) {
            log.error("getTableMeta SQLException=", e);
        } catch (Exception e) {
            log.error("getTableMeta Exception=", e);
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
        return columnList;
    }

    public List<TableColumnBean> getTableColumns(String dataBase, String dataBaseType, String schemaName, String userName, String tableName) {
        return getTableMeta(dataBase, dataBaseType, schemaName, userName, tableName);
    }

    public List<String> getTableNameList(String dataBase, String dataBaseType, String schemaName, String userName) {
        List<String> tableNameList = new ArrayList<>();
        SqlSession sqlSession = null;
        try {
            sqlSession = sqlSessionFactory.openSession();
            // 获取连接、获取元数据
            DatabaseMetaData metaData = sqlSession.getConnection().getMetaData();
            String catalog = null;
            String schema = null;

            if (DataBaseTypeEnum.of(dataBaseType).isLikeMysql()) {
                catalog = schemaName;
            } else if (DataBaseTypeEnum.of(dataBaseType).isLikePostgres()) {
                catalog = schemaName;
                schema = "public";
            } else if (DataBaseTypeEnum.of(dataBaseType).isLikeOracle()) {
                schema = userName.toUpperCase();
            } else {
                schema = schemaName;
            }

            ResultSet tables = metaData.getTables(catalog, schema, null, new String[]{"TABLE"});
            while (tables.next()) {
                tableNameList.add(tables.getString(3).toLowerCase());
            }
        } catch (SQLException e) {
            log.error("getTableMeta SQLException=", e);
        } catch (Exception e) {
            log.error("getTableMeta Exception=", e);
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
        return tableNameList;
    }
}

