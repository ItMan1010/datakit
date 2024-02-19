package com.itman.datakit.admin.service;


import com.github.pagehelper.PageInfo;
import com.itman.datakit.admin.common.api.RepairRequest;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.exception.DatakitException;

import java.util.List;
import java.util.Map;

public interface TaskService {

    /**
     * 数据转换：文件->表、文件->文件、表->表、表->文件
     *
     * @param taskInstance 任务实例对象
     * @throws DatakitException
     */
    void exchangeObjectDataAToB(final TaskInstance taskInstance) throws DatakitException;

    /**
     * 数据比较
     * 处理逻辑：通过纯内存稽核比对，先加载数据，考虑到内存数量大小，B按数据块存储，数据块使用完后释放，重新加载数据块
     *
     * @param taskInstance 任务实例对象
     * @throws DatakitException
     */
    void compareObjectDataAndB(final TaskInstance taskInstance) throws DatakitException;

    /**
     * 根据比较结果数据进行数据请求处理
     *
     * @param taskRepair 任务实例对象
     * @throws DatakitException
     */
    void repairRequest(final TaskRepair taskRepair) throws DatakitException;

    /**
     * 根据比较结果数据进行数据修复处理
     *
     * @param repairRequest 数据修复请求串
     * @throws DatakitException
     */
    void repairData(final RepairRequest repairRequest) throws DatakitException;

    PageInfo<TaskInstance> selectTaskInstancePage(Integer pageNum, Integer PageSize, String beginDate, String endDate, Integer state);

    TaskInstance selectTaskInstanceById(Long taskInstanceId);

    List<TaskResult> selectTaskResultByTaskInstanceId(Long taskInstanceId);

    List<RunInfo> selectRunInfoByTaskInstanceId(Long taskInstanceId);

    PageInfo<Task> selectTaskPage(Integer pageNum, Integer PageSize, Integer taskType, Integer taskId);

    Task makeTaskInfoById(Long taskId) throws DatakitException;

    /**
     * 配置生成新任务
     *
     * @param task
     * @throws DatakitException
     */
    void createTask(Task task) throws DatakitException;

    void createTaskRepair(Long taskInstanceId) throws DatakitException;
    /**
     * 生成任务运行实例
     *
     * @param taskId
     * @throws DatakitException
     */
    void createTaskInstance(Long taskId) throws DatakitException;

    void deleteTaskInstance(Long taskInstanceId) throws DatakitException;

    void stopTaskInstance(Long taskInstanceId) throws DatakitException;

    void updateTaskInstance(Long taskInstanceId) throws DatakitException;

    void deleteTask(Long taskId) throws DatakitException;
    void copyTask(Long taskId) throws DatakitException;
    void modifyTask(Task task) throws DatakitException;

    Map<Long, String> queryFieldIDNameMap(Integer objectType, Long objectId) throws DatakitException;

    void checkTaskOnLineByObjectId(Integer objectType, Long objectId) throws DatakitException;

    void updateTaskOnLineFlagById(Long taskId, Integer onLineFlag);

    void checkTaskObjectOnLine(Long taskId) throws DatakitException;

    void checkTaskOffLine(Long taskId) throws DatakitException;
}
