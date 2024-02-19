package com.itman.datakit.admin.common.entity;

import lombok.Data;

/**
 * 表字段定义
 *
 * @author: ItMan
 * @since: 2023/08/17  09:54
 */
@Data
public class TableField extends BaseEntity {
    /**
     * 表字段唯一标识
     */
    private Long tableFieldId;
    /**
     * 所属表标识
     */
    private Long tableFormatId;
    /**
     * 表字段名称
     */
    private String fieldName;
    /**
     * 表字段类型
     */
    private Integer fieldType;
    /**
     * 表字段长度
     */
    private Integer fieldLength;
    /**
     * 表字段可空
     */
    private Integer nullAble;
    /**
     * 主键标识:0非主键、1主键
     */
    private Integer keyFlag;
}
