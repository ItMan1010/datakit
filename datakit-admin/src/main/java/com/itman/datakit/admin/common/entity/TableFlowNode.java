package com.itman.datakit.admin.common.entity;

import com.itman.datakit.admin.common.tablemeta.TableColumnBean;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 表定义节点
 *
 * @author: itMan
 * @since: 2024/01/17  22:06
 */

@Data
public class TableFlowNode {
    /**
     * 流程节点标识
     */
    private Long flowNodeId;
    /**
     * 流程定义标识
     */
    private Long flowFormatId;
    /**
     * 表名称
     */
    private String tableName;
    /**
     * 条件查询字段名称
     */
    private String selectedFieldName;
    private List<String> fieldNameList;
    /**
     * 条件关联父节点字段
     */
    private String selectedParentFieldName;
    private List<String> parentFieldNameList;

    /**
     * 父节点标识
     */
    private Long parentFlowNodeId;
    /**
     * 儿子节点
     */
    private List<TableFlowNode> children = new ArrayList<>();
    /**
     * 拼装的字段串
     */
    private String selectSqlColumns;
    private String insertSqlColumns;
    private List<TableColumnBean> tableColumns;
    private List<TableField> tableFieldList;

}