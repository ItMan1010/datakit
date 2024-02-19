package com.itman.datakit.admin.common.entity;

import lombok.Data;

import java.util.List;


/**
 * 表定义对象
 *
 * @author: ItMan
 * @since: 2023/08/17  09:54
 */
@Data
public class TableFlowFormat extends BaseEntity {
    /**
     * 表流程
     */
    private Long flowFormatId;
    /**
     * 表流程名称
     */
    private String flowName;
    /**
     * 数据源
     */
    private String sourceDataBase;
    /**
     * 数据源
     */
    private String targetDataBase;
    /**
     * 备注
     */
    private String remark;
    /**
     * 发布标志：0下线、1在线
     */
    private Integer onLineFlag;
    /**
     * 流程节点，数据查询后节点
     */
    private List<TableFlowNode> tableFlowNodeList;

    //-------------界面扩展使用-------------------------------
    private String onLineFlagName;
    private String sourceJdbcUrl;
    private String targetJdbcUrl;
    private String tableFlowFormatJason;
    /**
     * 表流程动作：0无动作、1新增、2删除
     */
    private Integer tableFlowAction;
    /**
     * 表流程动作：0无动作、1新增、2删除
     */
    private Integer tableFlowNodeAction;
    private Long tableFlowNodeId;
    private String tableFlowNodeTableName;
    //-------------界面扩展使用-------------------------------

}
