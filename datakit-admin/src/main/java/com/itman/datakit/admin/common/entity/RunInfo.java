package com.itman.datakit.admin.common.entity;

import lombok.Data;

import java.util.Date;

/**
 * 任务运行信息
 *
 * @author: ItMan
 * @since: 2023/08/21  17:53
 */
@Data
public class RunInfo extends BaseEntity {
    /**
     * 任务运行主键标识ID
     */
    private Long runInfoId;
    /**
     * 任务实例标识ID
     */
    private Long taskInstanceId;
    /**
     * 日志等级
     */
    private Integer infoLevel;
    /**
     * 任务运行信息
     */
    private String runInfo;
    /**
     * 生成时间
     */
    private Date createDate;
    /**
     * 日志等级名称
     */
    private String infoLevelName;
}
