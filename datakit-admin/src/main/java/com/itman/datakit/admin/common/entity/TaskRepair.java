package com.itman.datakit.admin.common.entity;

import lombok.Data;

import java.util.Date;

/**
 * 任务比较数据修复
 *
 * @author: ItMan
 * @since: 2023/07/26  11:05
 */
@Data
public class TaskRepair extends BaseEntity {
    /**
     * 修复运行标识ID
     */
    private Long taskRepairId;
    /**
     * 任务实例标识ID
     */
    private Long taskInstanceId;
    /**
     * 修复运行主机名称
     */
    private String hostName;
    /**
     * 修复运行主机IP
     */
    private String hostIp;
    /**
     * 修复状态
     */
    private Integer repairState;
    /**
     * 修复时间
     */
    private Date repairStateDate;
}
