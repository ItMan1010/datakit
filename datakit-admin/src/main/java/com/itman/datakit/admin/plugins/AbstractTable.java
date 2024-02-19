package com.itman.datakit.admin.plugins;

import com.itman.datakit.admin.common.enums.TableFieldTypeEnum;
import com.itman.datakit.admin.common.exception.DatakitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.itman.datakit.admin.common.enums.TableFieldTypeEnum.values;

/**
 * TODO
 *
 * @author: ItMan
 * @since: 2023/12/09  20:04
 */
@Slf4j
@Component
public abstract class AbstractTable {
    private Map<TableFieldTypeEnum, String> TABLE_FIELD_TYPE_ENUM_MAP = new HashMap<>();
    protected Map<Integer, String> TABLE_FIELD_TYPE_NAME_MAP = new HashMap<>();
    protected Map<Integer, String> TABLE_FIELD_TYPE_NAME_MERGE_MAP = new HashMap<>();
    protected Map<String, Integer> TABLE_FIELD_NAME_MERGE_TYPE_MAP = new HashMap<>();

    public void fieldTypeEnumMapPut(TableFieldTypeEnum tableFieldTypeEnum, String fieldType) {
        TABLE_FIELD_TYPE_ENUM_MAP.put(tableFieldTypeEnum, fieldType);
    }

    public void fieldTypeEnumMapChange() throws DatakitException {
        if (TABLE_FIELD_TYPE_ENUM_MAP.size() != values().length) {
            throw new DatakitException("MysqlTableImpl", "字段类型与datakit定义不一致");
        }

        Map<String, TableFieldTypeEnum> tableFieldTypeEnumMap = new HashMap<>();
        TABLE_FIELD_TYPE_ENUM_MAP.forEach((x, y) -> {
            TABLE_FIELD_TYPE_NAME_MAP.put(x.getTypeId(), y);
            if (tableFieldTypeEnumMap.containsKey(y)) {
                //相同匹配最大字节数类型字段
                if (x.getByteSize() > tableFieldTypeEnumMap.get(y).getByteSize()) {
                    tableFieldTypeEnumMap.put(y, x);
                }
            } else {
                tableFieldTypeEnumMap.put(y, x);
            }
        });

        tableFieldTypeEnumMap.forEach((x, y) -> TABLE_FIELD_TYPE_NAME_MERGE_MAP.put(y.getTypeId(), x));
        tableFieldTypeEnumMap.forEach((x, y) -> TABLE_FIELD_NAME_MERGE_TYPE_MAP.put(x, y.getTypeId()));
    }

}
