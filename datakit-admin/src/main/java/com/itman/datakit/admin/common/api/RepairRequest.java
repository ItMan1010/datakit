package com.itman.datakit.admin.common.api;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 数据修订请求参数
 *
 * @author: ItMan
 * @since: 2023/11/27  14:42
 */
@Data
public class RepairRequest implements Serializable {
    private static final long serialVersionUID = 1865981403129616196L;
    private Long taskInstanceId;
    private Long taskId;
    private Integer compareFlag;
    private String compareKey;
    private Map<Long, String> aTaskFieldRelIdValueMap;
    private Map<String, String> aObjectValue;
    private Map<String, String> bObjectValue;
}
