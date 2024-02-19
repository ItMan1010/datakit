package com.itman.datakit.admin.common.dto;

import lombok.Data;

/**
 * 查询任务运行实例参数
 *
 * @author: ItMan
 * @since: 2023/08/29  09:30
 */
@Data
public class TaskInstanceDTO {
    /**
     * 查询开始时间
     */
    String beginDate;
    /**
     * 查询结束时间
     */
    String endDate;
    /**
     * 运行状态
     */
    Integer runState;
}
