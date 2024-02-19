package com.itman.datakit.admin.common.entity;

import com.itman.datakit.admin.common.tablemeta.TableColumnBean;
import lombok.Data;

import java.util.List;
import java.util.Map;


/**
 * 表定义对象
 *
 * @author: ItMan
 * @since: 2023/08/17  09:54
 */
@Data
public class TableFormat extends BaseEntity {
    /**
     * 表实例ID
     */
    private Long tableFormatId;
    /**
     * 表名称
     */
    private String tableName;
    /**
     * 过滤条件
     */
    private String tableWhere;
    /**
     * 表字段定义
     */
    List<TableColumnBean> tableColumnsList;
    Map<String, TableColumnBean> tableColumnsByNameMap;
    /**
     * 表插入字段串sql
     */
    private String insertSqlColumns;
    /**
     * 表字段定义
     */
    List<TableField> tableFieldList;
    Map<Long, TableField> tableFieldByIdMap;
    Map<String, TableField> tableFieldByNameMap;
    /**
     * 表查询字段串sql
     */
    private String selectSqlColumns;

    private String dataBase;
    /**
     * 发布标志：0下线、1在线
     */
    private Integer onLineFlag;


    //-------------界面扩展使用-------------------------------
    private String onLineFlagName;
    private String jdbcUrl;

    /**
     * 表页面字段操作标识：1新增、2删除
     */
    private Integer tableFieldAction;
    /**
     * 表页面操作行体字段索引值
     */
    private Integer tableFieldIndex;
    /**
     * 表操作标识：1保存、2校验
     */
    private Integer tableAction;
    //-------------界面扩展使用-------------------------------

}
