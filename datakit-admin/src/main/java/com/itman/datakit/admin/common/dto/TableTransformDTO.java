package com.itman.datakit.admin.common.dto;

import com.itman.datakit.admin.common.entity.TableTransform;
import lombok.Data;

import java.util.List;

/**
 * @author: ItMan
 * @since: 2023/08/29  09:30
 */
@Data
public class TableTransformDTO {
    /**
     * 源数据库
     */
    private String sourceDb;
    /**
     * 目标数据库
     */
    private String targetDb;
    /**
     * 源库所有表
     */
    private List<TableTransform> tableTransformList;
    /**
     * 操作动作:
     */
    private Integer action;
}
