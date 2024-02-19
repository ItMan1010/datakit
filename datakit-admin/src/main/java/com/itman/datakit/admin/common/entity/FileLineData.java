package com.itman.datakit.admin.common.entity;

import lombok.Data;

import java.util.Map;

/**
 * 文件数据对象
 *
 * @author: ItMan
 * @since: 2023/08/18  10:50
 */
@Data
public class FileLineData {
    /**
     * 行数数据类型：1 文件特殊行、2 文件数据正文
     */
    private Integer lineDataType;
    /**
     * 字段标识ID对应文件数据
     */
    private Map<Long, String> lineDataMap;
    /**
     * 所在文件行数
     */
    private Integer lineNumber;
}
