package com.itman.datakit.admin.plugins.impl;

import com.itman.datakit.admin.common.constants.DataBaseTypeEnum;
import com.itman.datakit.admin.common.enums.TableFieldTypeEnum;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.plugins.AbstractTable;
import com.itman.datakit.admin.plugins.ITable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

import static com.itman.datakit.admin.common.constants.DataBaseTypeEnum.ORACLE;
import static com.itman.datakit.admin.common.enums.TableFieldTypeEnum.*;
import static com.itman.datakit.admin.common.util.CommonUtil.makeTableFieldNullFlag;

/**
 * Oracle数据库特性处理类
 *
 * @author: ItMan
 * @since: 2023/12/07  09:58
 */
@Slf4j
@Component
public class OracleTableImpl extends AbstractTable implements ITable {

    public static final String ORACLE_INTEGER_NUMBER = "number";

    @Override
    public Boolean dataBaseCurrent(DataBaseTypeEnum dataBaseTypeEnum) {
        return dataBaseTypeEnum.equals(ORACLE);
    }

    public OracleTableImpl() throws DatakitException {
        //自定类型规则：(1)名称都必须统一小写、(2)必须配置上所有已经定义标准类型，如果没有则找最接近合理的
        super.fieldTypeEnumMapPut(TYPE_01_INTEGER_1, ORACLE_INTEGER_NUMBER);
        super.fieldTypeEnumMapPut(TYPE_02_INTEGER_2, ORACLE_INTEGER_NUMBER);
        super.fieldTypeEnumMapPut(TYPE_03_INTEGER_3, ORACLE_INTEGER_NUMBER);
        super.fieldTypeEnumMapPut(TYPE_04_INTEGER_4, ORACLE_INTEGER_NUMBER);
        super.fieldTypeEnumMapPut(TYPE_05_INTEGER_8, ORACLE_INTEGER_NUMBER);
        super.fieldTypeEnumMapPut(TYPE_06_STRING_INDEFINITE, "varchar2");
        super.fieldTypeEnumMapPut(TYPE_07_DATETIME_8, "timestamp");
        super.fieldTypeEnumMapPut(TYPE_08_TIMESTAMP_4, "timestamp");

        super.fieldTypeEnumMapChange();
    }

    @Override
    public String makeTableFieldSql(String fieldName, Integer fieldType, Integer fieldLength, Integer nullAble) throws DatakitException {
        if (TableFieldTypeEnum.of(fieldType).getIfLength() == 0) {
            return String.format("%s %s %s", fieldName, super.TABLE_FIELD_TYPE_NAME_MAP.get(fieldType), makeTableFieldNullFlag(nullAble));
        } else {
            return String.format("%s %s(%d) %s", fieldName, super.TABLE_FIELD_TYPE_NAME_MAP.get(fieldType), fieldLength, makeTableFieldNullFlag(nullAble));
        }
    }


    @Override
    public String makeSelectSql(String tableName, String sqlColumns, String sqlWhere, Long beginRow, Integer tableSelectCount) {
        if (!StringUtils.isEmpty(sqlWhere)) {
            return String.format("select %s from (select %s,rownum as rn from %s where %s and rownum <= %s ) where rn >= %s ", sqlColumns, sqlColumns, tableName, sqlWhere, beginRow + tableSelectCount, beginRow);
        } else {
            return String.format("select %s from (select %s,rownum as rn from %s where rownum <= %s ) where rn >= %s ", sqlColumns, sqlColumns, tableName, beginRow + tableSelectCount, beginRow);
        }
    }

    @Override
    public String stringToDate(String columnValue) {
        if (columnValue.length() == 20) {
            return "to_timestamp('" + columnValue + "','YYYYMMDDHH24MISSFF6')";
        } else {
            return "to_timestamp('" + columnValue + "','YYYYMMDDHH24MISS')";
        }
    }

    @Override
    public String dateToString(String columnName) {
        return "date_format(" + columnName + ",'%Y%m%d%H%i%s%f') " + columnName;
    }

    @Override
    public String makeSqlLimit(Integer pageRow, Integer pageCount) {
        return " ";
    }

    @Override
    public String getSqlSystemDate() {
        return "sysdate";
    }

    @Override
    public Integer getFiledTypeByName(String filedName) throws DatakitException {
        if (!super.TABLE_FIELD_NAME_MERGE_TYPE_MAP.containsKey(filedName.toLowerCase())) {
            throw new DatakitException("getFiledTypeByName", "根据字段类型名称匹配定义类型失败");
        }
        return super.TABLE_FIELD_NAME_MERGE_TYPE_MAP.get(filedName.toLowerCase());
    }

    @Override
    public Map<Integer, String> getFieldTypeNameMap() {
        return super.TABLE_FIELD_TYPE_NAME_MAP;
    }

    @Override
    public Map<Integer, String> getFieldTypeNameMergeMap() {
        return super.TABLE_FIELD_TYPE_NAME_MERGE_MAP;
    }

    @Override
    public Map<String, Integer> getFieldNameTypeMap() {
        return super.TABLE_FIELD_NAME_MERGE_TYPE_MAP;
    }
}
