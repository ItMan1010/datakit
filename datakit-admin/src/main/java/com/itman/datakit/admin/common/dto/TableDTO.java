package com.itman.datakit.admin.common.dto;

import lombok.Data;

import java.util.Date;

/**
 * 查询任务运行实例参数
 *
 * @author: ItMan
 * @since: 2023/08/29  09:30
 */
@Data
public class TableDTO {
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
