package com.itman.datakit.admin.common.entity;

import lombok.Data;

import java.util.Date;

/**
 * 任务实例结果明细对象
 *
 * @author: ItMan
 * @since: 2023/08/21  17:53
 */
@Data
public class TaskResult extends BaseEntity {
    /**
     * 任务运行明细主键标识ID
     */
    private Long taskResultId;
    /**
     * 任务实例标识ID
     */
    private Long taskInstanceId;
    /**
     * 任务运行信息
     */
    private String taskResult;
    /**
     * 比较结果标志 1:A比B多;2:A比B少；3:AB指定字段值不一致
     */
    private Integer compareFlag;
    /**
     * 比较结果信息
     */
    private String compareInfo;
    /**
     * 比较对象数据信息
     */
    private String compareData;
    /**
     * 平帐状态:0待平帐、1平帐成功、2平帐失败
     */
    private Integer repairState;
    /**
     * 平帐处理信息
     */
    private String repairInfo;
    /**
     * 平帐时间
     */
    private Date repairStateDate;


    //-------------界面扩展使用-------------------------------
    private String compareFlagName;
    private String repairStateName;
    //-------------界面扩展使用-------------------------------
}
