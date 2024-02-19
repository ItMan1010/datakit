package com.itman.datakit.admin.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 文件类型的枚举定义
 */
@Getter
@AllArgsConstructor
public enum FileTypeEnum {

    /**
     * 文本文件
     */
    TEXT(1, "Text"),

    /**
     * Excel
     */
    EXCEL(2, "Excel");

    private Integer id;
    private String name;

    public static boolean exists(String name) {
        return Arrays.stream(values()).anyMatch(item -> item.name().equalsIgnoreCase(name));
    }

    public static FileTypeEnum of(Integer id) {
        if (!Objects.isNull(id)) {
            for (FileTypeEnum type : FileTypeEnum.values()) {
                if (type.getId().equals(id)) {
                    return type;
                }
            }
        }

        throw new IllegalArgumentException("cannot find enum id: " + id);
    }
}
