package com.itman.datakit.admin.common.dataqueue;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 队列对象
 *
 * @author: ItMan
 * @since: 2023/07/31  16:06
 */
@Getter
@Setter
public class QueueData {
    /**
     * 1:对象数据转换业务标准格式：key=taskFieldRelaId、value=对象数据字段值
     */
    private Map<Long, String> dataMap;
    /**
     * 比对标志，0未比较、1已比较
     */
    private Integer comparedFlag;
    /**
     * 参与比较key
     */
    private String comparedKey;
    /**
     * 对象字段名称和值映射
     */
    private Map<String, String> fieldNameValueMap;
}
