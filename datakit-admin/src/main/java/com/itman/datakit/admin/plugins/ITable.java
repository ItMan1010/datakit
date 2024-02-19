package com.itman.datakit.admin.plugins;

import com.itman.datakit.admin.common.constants.DataBaseTypeEnum;
import com.itman.datakit.admin.common.exception.DatakitException;

import java.util.Map;

public interface ITable {
    /**
     * 数据库类型匹配
     */
    Boolean dataBaseCurrent(DataBaseTypeEnum dataBaseTypeEnum);

    /**
     * 组装字段语句
     *
     * @param fieldName
     * @param fieldType
     * @param fieldLength
     * @param nullAble
     * @return
     */
    String makeTableFieldSql(String fieldName, Integer fieldType, Integer fieldLength, Integer nullAble) throws DatakitException;

    String makeSelectSql(String tableName, String sqlColumns, String sqlWhere, Long beginRow, Integer tableSelectCount);

    String stringToDate(String columnValue);

    String dateToString(String columnName);

    String makeSqlLimit(Integer pageRow, Integer pageCount);

    String getSqlSystemDate();

    Integer getFiledTypeByName(String filedName) throws DatakitException;

    Map<Integer, String> getFieldTypeNameMap();
    Map<Integer, String> getFieldTypeNameMergeMap();
    Map<String, Integer> getFieldNameTypeMap();
}
