package com.itman.datakit.admin.common.entity;

import lombok.Data;

import java.util.List;

/**
 * 文件的特殊行：如行头、行尾
 *
 * @author: ItMan
 * @since: 2023/08/17  09:54
 */
@Data
public class FileSpecial extends BaseEntity {
    /**
     * 文件行主键ID
     */
    private Long fileSpecialId;
    /**
     * 文件格式主键ID
     */
    private Long fileFormatId;
    /**
     * 文件行数据行分隔符：
     * (1)固定长度、(2)竖线|、(3)逗号，、(4)与符号&
     */
    private Integer splitFlag;
    /**
     * 行记录字段定义,保持顺序
     */
    private List<FileField> fileFieldList;
    /**
     * 一般文件第一行为文件头：记录文件汇总信息
     * 如果为-1，表示行尾
     */
    private Integer fixLinePosition;
    /**
     * 备注
     */
    private String remark;


    //-------------界面扩展使用-------------------------------
    private String splitFlagName;
    //-------------界面扩展使用-------------------------------
}
