package com.itman.datakit.admin.common.dto;

import lombok.Data;

/**
 * @author: ItMan
 * @since: 2023/08/29  09:30
 */
@Data
public class TableRowsDTO {
    /**
     * 发布标志：0下线、1在线
     */
    private Integer onLineFlag;
    /**
     * 任务选择对象标志
     */
    private String taskSelectFlag;
}
