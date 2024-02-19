package com.itman.datakit.admin.common.dto;

import lombok.Data;

/**
 * 任务明细参数
 *
 * @author: ItMan
 * @since: 2023/08/29  09:30
 */
@Data
public class TaskRowsDTO {
    /**
     * 任务类型
     */
    private Integer taskType;
    /**
     * 任务ID
     */
    private Integer taskId;
}
