package com.itman.datakit.admin.common.tablemeta;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TableColumnBean {
    private String dbName;
    private String tableName;
    private String columnName;
    private String autoIncrement;
    private String generatedColumn;
    private String columnType;
    private int columnLength;
    private String columnRemark;
    private boolean keyFlag;
    private boolean indexFlag;
    private int nullAble;
}

