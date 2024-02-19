package com.itman.datakit.admin.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 数据库类型的枚举定义
 */
@Getter
@AllArgsConstructor
public enum DataBaseTypeEnum {

    /**
     * MySQL数据库
     */
    MYSQL(1, "mysql",
            "jdbc:(?<type>[a-z]+)://(?<host>[a-zA-Z0-9-//.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)?"),
    /**
     * Oracle数据库
     */
    ORACLE(2, "oracle",
            "jdbc:(?<type>[a-z]+):thin:@(?<host>[a-zA-Z0-9-//.]+):(?<port>[0-9]+):(?<database>[a-zA-Z0-9_]+)?"),
    /**
     * PostgreSQL数据库
     */
    POSTGRESQL(3, "postgresql",
            "jdbc:(?<type>[a-z]+)://(?<host>[a-zA-Z0-9-//.]+):(?<port>[0-9]+)/(?<database>[a-zA-Z0-9_]+)?");

    private int id;
    private String name;
    private String urlPattern;

    public static DataBaseTypeEnum of(String name) {
        if (!StringUtils.isEmpty(name)) {
            for (DataBaseTypeEnum type : DataBaseTypeEnum.values()) {
                if (type.getName().equalsIgnoreCase(name)) {
                    return type;
                }
            }
        }

        throw new IllegalArgumentException("cannot find enum name: " + name);
    }

    public boolean isLikePostgres() {
        return POSTGRESQL.equals(this);
    }

    public boolean isLikeOracle() {
        return ORACLE.equals(this);
    }

    public boolean isLikeMysql() {
        return MYSQL.equals(this);
    }
}
