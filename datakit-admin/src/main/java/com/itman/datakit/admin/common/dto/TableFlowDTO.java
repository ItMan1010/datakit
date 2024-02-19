package com.itman.datakit.admin.common.dto;

import lombok.Data;

import java.util.Date;

/**
 * 表流程实例参数
 *
 * @author: ItMan
 * @since: 2023/08/29  09:30
 */
@Data
public class TableFlowDTO {
    /**
     * 流程定义标识
     */
    private Long flowFormatId;
    /**
     * 流程名称
     */
    private String flowName;
    /**
     * 数据源
     */
    private String dataBase;
    /**
     * 备注
     */
    private String remark;
    /**
     * 生成时间
     */
    private Date createDate;
    /**
     * 发布标志：0下线、1在线
     */
    private Integer onLineFlag;
    /**
     * 状态时间
     */
    private Date stateDate;
}
