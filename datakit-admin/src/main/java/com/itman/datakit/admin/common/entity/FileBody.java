package com.itman.datakit.admin.common.entity;

import lombok.Data;

import java.util.List;

/**
 * 文件数据数据体定义, 主要是每一行规则记录
 *
 * @author: ItMan
 * @since: 2023/08/16  11:05
 */
@Data
public class FileBody extends BaseEntity {
    /**
     * 文件体主键ID
     */
    private Long fileBodyId;
    /**
     * 文件唯一标识
     */
    private Long fileFormatId;
    /**
     * 间隔标记符：(1)固定长度、(2)竖线|、(3)逗号，、(4)与符号&
     */
    private Integer splitFlag;
    /**
     * 行记录字段定义
     */
    private List<FileField> fileFieldList;
    /**
     * 固定开始行: null不存在
     */
    private Integer fixBeginLine;
    /**
     * 固定结束行: null不存在
     */
    private Integer fixEndLine;

    //-------------界面扩展使用-------------------------------
    private String splitFlagName;
    //-------------界面扩展使用-------------------------------
}
