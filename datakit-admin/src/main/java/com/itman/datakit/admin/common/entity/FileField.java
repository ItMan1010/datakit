package com.itman.datakit.admin.common.entity;

import lombok.Data;

@Data
public class FileField extends BaseEntity {
    /**
     * 文件字段唯一标识
     */
    private Long fileFieldId;
    /**
     * 文件标识
     */
    private Long fileFormatId;
    /**
     * 字段所属标志：1 文件特殊行、2 文件数据正文
     */
    private Integer belongFlag;
    /**
     * 字段所属值，根据belongFlag取不同值
     */
    private Long belongId;
    /**
     * 可以约定一个名称
     * 如果不填，自动按顺序编排：fieldName01、fieldName02、fieldName03...
     */
    private String fieldName;
    /**
     * 如果数据体定义splitFlag=1，每个字段固定长度
     */
    private Integer fixWidth;
    /**
     * 行记录字段占位，从1开始，1、2、3...
     */
    private Integer position;
    /**
     * 用于特殊行定义：1表示该字段记录总行数
     */
    private Integer sumLineFlag;
    /**
     * 如果不为空,记录行体用于累加字段名称
     */
    private String sumFieldName;
}
