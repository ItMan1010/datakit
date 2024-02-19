package com.itman.datakit.admin.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum ObjectTaskEnum {
    /**
     * 数据同步(文件->表、文件->文件、表->文件、表->表)
     */
    OBJECT_TASK_TYPE_DATA_EXCHANGE(1, "数据同步"),
    /**
     * 数据比较(文件<->表、文件<->文件、表<->文件、表<->表)
     */
    OBJECT_TASK_TYPE_DATA_COMPARE(2, "数据比较"),
    /**
     * 流程迁移
     */
    OBJECT_TASK_TYPE_TABLE_FLOW_SYNC(3, "流程迁移"),
    ;

    private int value;
    private String description;

    public static ObjectTaskEnum of(Integer v) {
        if (!Objects.isNull(v)) {
            for (ObjectTaskEnum type : ObjectTaskEnum.values()) {
                if (v.intValue() == type.getValue()) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("no such value: " + v);
    }
}
