package com.itman.datakit.admin.common.entity;

import lombok.Data;

import java.util.Date;

/**
 * 实体类基本信息
 *
 * @author: ItMan
 * @since: 2023/10/15  19:00
 */

@Data
public class BaseEntity {
    /**
     * 生成时间
     */
    private Date createDate;
    /**
     * 记录状态：0无效、1有效
     */
    /**
     * 状态时间
     */
    private Date stateDate;
}
