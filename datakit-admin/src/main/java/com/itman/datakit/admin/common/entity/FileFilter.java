package com.itman.datakit.admin.common.entity;

import lombok.Data;

/**
 * 文件过滤规则对象
 *
 * @author: ItMan
 * @since: 2023/08/16  11:05
 */
@Data
public class FileFilter extends BaseEntity {
    /**
     * 文件过滤标识
     */
    private Long fileFilterId;
    /**
     * 文件定义标识
     */
    private Long fileFormatId;
    /**
     * 文件字段标识
     */
    private Long fileFieldId;
    /**
     * 符号标识：1(等于=)、2(大于号>)、3(小于号<)、4(大于等于号>=)、5(小于等于号<=)
     */
    private Integer symbolId;
    /**
     * 同一组条件是与关系，不同组条件是或关系
     */
    private Integer symbolGroup;
    /**
     * 字段值
     */
    private String fileFieldValue;
}
