package com.itman.datakit.admin.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;

/**
 * 统一映射数据库字段类型
 */
@Getter
@AllArgsConstructor
public enum TableFieldTypeEnum {
    /**
     * 比如:
     * mysql:tinyint
     */
    TYPE_01_INTEGER_1(1, 1, TABLE_FIELD_TYPE_IF_LENGTH_NO, TABLE_FIELD_TYPE_CLASSIFY_INTEGER, "整型单字节"),
    /**
     * 比如:
     * mysql:smallint
     * postgres:int2
     */
    TYPE_02_INTEGER_2(2, 2, TABLE_FIELD_TYPE_IF_LENGTH_NO, TABLE_FIELD_TYPE_CLASSIFY_INTEGER, "整型2字节"),
    /**
     * 比如:
     * mysql:mediumint
     */
    TYPE_03_INTEGER_3(3, 3, TABLE_FIELD_TYPE_IF_LENGTH_NO, TABLE_FIELD_TYPE_CLASSIFY_INTEGER, "整型3字节"),
    /**
     * 比如:
     * mysql:integer
     * postgres:int4
     */
    TYPE_04_INTEGER_4(4, 4, TABLE_FIELD_TYPE_IF_LENGTH_NO, TABLE_FIELD_TYPE_CLASSIFY_INTEGER, "整型4字节"),
    /**
     * 比如:
     * mysql:bigint
     * postgres:int8
     */
    TYPE_05_INTEGER_8(5, 8, TABLE_FIELD_TYPE_IF_LENGTH_NO, TABLE_FIELD_TYPE_CLASSIFY_INTEGER, "整型8字节"),

    /**
     * 比如:
     * mysql:varchar
     * postgres:varchar
     */
    TYPE_06_STRING_INDEFINITE(6, -1, TABLE_FIELD_TYPE_IF_LENGTH_YES, TABLE_FIELD_TYPE_CLASSIFY_STRING, "可变字符串"),

    /**
     * 比如:
     * mysql:datetime
     * postgres:
     */
    TYPE_07_DATETIME_8(7, 8, TABLE_FIELD_TYPE_IF_LENGTH_NO, TABLE_FIELD_TYPE_CLASSIFY_DATETIME, "日期和时间值"),

    /**
     * 比如:
     * mysql:timestamp
     * postgres:timestamp
     */
    TYPE_08_TIMESTAMP_4(8, 4, 0, TABLE_FIELD_TYPE_CLASSIFY_DATETIME, "日期和时间值"),


    ;

    /**
     * 字段类型ID
     */
    private int typeId;
    /**
     * 字段类型默认字节数,-1没有默认
     */
    private int byteSize;
    /**
     * 字段是否需要指定长度：1指定、0不指定
     */
    private int ifLength;
    /**
     * 字段类型分类：1整型、2字符串、3时间
     */
    private int classify;
    /**
     * 字段类型描述
     */
    private String description;

    public static TableFieldTypeEnum of(Integer v) {
        if (!Objects.isNull(v)) {
            for (TableFieldTypeEnum type : TableFieldTypeEnum.values()) {
                if (v.intValue() == type.getTypeId()) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("no such value: " + v);
    }
}
