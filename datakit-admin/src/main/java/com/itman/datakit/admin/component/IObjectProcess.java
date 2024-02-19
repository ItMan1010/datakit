package com.itman.datakit.admin.component;

import com.itman.datakit.admin.common.dataqueue.QueueData;
import com.itman.datakit.admin.common.entity.Task;
import com.itman.datakit.admin.common.entity.TaskInstance;
import com.itman.datakit.admin.common.enums.DatakitObjectEnum;
import com.itman.datakit.admin.common.exception.DatakitException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IObjectProcess {
    /**
     * 业务对象匹配
     *
     * @param objectEnum 业务对象类型定义:文件、表
     * @return Boolean 是否标识
     */
    Boolean objectCurrent(DatakitObjectEnum objectEnum);

    /**
     * 业务对象组装
     *
     * @param objectFormatId 业务对象标识
     * @return Object 统一业务对象
     * @throws DatakitException
     */
    Object makeObjectFormat(Long objectFormatId, Boolean tableMetaFlag) throws DatakitException;

    /**
     * 1：来源端业务数据同步到内存队列
     * 2：多线程并发处理
     *
     * @param task 任务结构
     */
    void doBusinessInputQueue(final Long taskInstanceId, final Task task) throws DatakitException, IOException;

    /**
     * 从内存队列获取对象数据进行后续业务流程处理
     * 业务流程包括：1数据转换、2数据稽核、其他后续扩展。。。
     *
     * @param objectDataList
     * @param taskInstance
     * @throws DatakitException
     */
    void matchDataQueueDoBusiness(List<QueueData> objectDataList, final TaskInstance taskInstance) throws DatakitException;

    /**
     * 1:目标端业务数据同步到本地内存Map缓存
     * 2:多线程处理加
     *
     * @param task 任务对象
     */
    void doBusinessLoadDataToMapCache(final Task task) throws DatakitException;

    /**
     * 根据业务对象定义生成对应数据库表结构
     *
     * @param objectFormat 统一业务对象
     * @throws DatakitException
     */
    void doBusinessCreateTable(final Object objectFormat) throws DatakitException;

    /**
     * 生成业务对象定义字段标识和名称
     *
     * @param objectFormat
     * @return
     * @throws DatakitException
     */
    Map<Long, String> genFieldIdNameMap(final Object objectFormat) throws DatakitException;

    /**
     * 业务对象数据修订处理
     *
     * @param repairAction
     * @param task
     * @param queueData
     * @throws DatakitException
     */
    void repairData(final Integer repairAction, final Task task, final QueueData queueData) throws DatakitException;

    /**
     * 查询业务对象定义字段标识和名称
     *
     * @param objectId
     * @return
     */
    Map<Long, String> queryFieldIDNameMap(Long objectId);

    String getObjectName(final Object objectFormat);

    String queryObjectName(final Long objectId);

    Integer getObjectOnLineFlag(final Object objectFormat);

    void followUpObject(final Long taskInstanceId, final Task task) throws DatakitException;

    Map<String, Long> getObjectSumFieldRelIdList(final Task task);
}
