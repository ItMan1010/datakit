package com.itman.datakit.admin.common.api;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据修订响应参数
 *
 * @author: ItMan
 * @since: 2023/11/27  14:45
 */
@Data
public class RepairResponse implements Serializable {

    private static final long serialVersionUID = 1865981403129616196L;
    /**
     * 返回码
     */
    private String resultCode;
    /**
     * 返回信息
     */
    private String resultMsg;
}
