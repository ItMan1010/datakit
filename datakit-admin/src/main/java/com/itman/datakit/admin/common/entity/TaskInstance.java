package com.itman.datakit.admin.common.entity;

import lombok.Data;

import java.util.Date;

/**
 * 任务实例对象
 *
 * @author: ItMan
 * @since: 2023/07/26  10:10
 */
@Data
public class TaskInstance extends BaseEntity {
    /**
     * 任务实例主键标识ID
     */
    private Long taskInstanceId;
    /**
     * 任务标识ID
     */
    private Long taskId;
    /**
     * 任务对象
     */
    private Task task;
    /**
     * 任务类型
     */
    private Integer taskType;
    /**
     * 任务运行信息
     */
    private String taskMsg;
    /**
     * 任务实例运行主机名称
     */
    private String hostName;
    /**
     * 任务实例运行主机IP
     */
    private String hostIp;
    /**
     * 任务类型名称
     */
    private String taskTypeName;
    /**
     * 任务实例运行状态
     */
    private Integer runState;
    /**
     * 任务实例运行时间
     */
    private Date runStateDate;
    /**
     * 运行状态名称
     */
    private String runStateName;
}
