package com.itman.datakit.admin.common.entity;

import lombok.Data;

/**
 * TODO
 *
 * @author: ItMan
 * @since: 2023/12/27  22:27
 */
@Data
public class TableTransform {
    private String sourceTableName;
    private String targetTableName;
    private String stateName;
    private Integer selectFlag;
}
