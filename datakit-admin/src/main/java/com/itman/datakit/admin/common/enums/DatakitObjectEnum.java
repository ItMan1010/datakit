// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/datakit
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package com.itman.datakit.admin.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum DatakitObjectEnum {
    DATAKIT_OBJECT_FILE(1, "文件"),
    DATAKIT_OBJECT_TABLE(2, "表"),
    DATAKIT_OBJECT_TABLE_FLOW(3, "流程"),
    ;

    private int value;
    private String description;

    public static DatakitObjectEnum of(Integer v) {
        if (!Objects.isNull(v)) {
            for (DatakitObjectEnum type : DatakitObjectEnum.values()) {
                if (v.intValue() == type.getValue()) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("no such value: " + v);
    }
}
