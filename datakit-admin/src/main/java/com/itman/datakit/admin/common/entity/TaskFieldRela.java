package com.itman.datakit.admin.common.entity;

import lombok.Data;

/**
 * 业务对象字段映射关联
 *
 * @author: ItMan
 * @since: 2023/07/26  11:07
 */
@Data
public class TaskFieldRela extends BaseEntity {
    private Long taskFieldRelaId;
    private Long taskId;
    private Long aObjectFieldId;
    private Long bObjectFieldId;
    private Integer compareFlag;

    //-------------界面扩展使用-------------------------------
    private String aObjectFieldName;
    private String bObjectFieldName;
    //-------------界面扩展使用-------------------------------
}
