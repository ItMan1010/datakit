package com.itman.datakit.admin.component;

import com.alibaba.fastjson.JSON;
import com.itman.datakit.admin.common.api.RepairRequest;
import com.itman.datakit.admin.common.api.RepairResponse;
import com.itman.datakit.admin.common.config.DatakitConfig;
import com.itman.datakit.admin.common.dataqueue.ObjectQueue;
import com.itman.datakit.admin.common.dataqueue.QueueData;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.enums.DatakitObjectEnum;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.common.feignClient.DatakitFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.exception.DatakitError.ERROR_GET_OBJET_BY_TYPE_ID_FAIL;

/**
 * 具体业务对象操作流程包装
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatakitProcess {
    private final List<IObjectProcess> objectProcessList;
    private final DatakitFeignClient datakitFeignClient;
    private final CommonProcess commonProcess;
    private final DatakitConfig datakitConfig;

    private void threadSleep(Integer sleepTime) throws DatakitException {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            log.error("threadSleep=", e);
            Thread.currentThread().interrupt();
            throw new DatakitException("threadSleep", "Thread sleep InterruptedException!");
        }
    }


    public Object makeDatakitObject(Integer objectType, long objectId, Boolean tableMetaFlag) throws DatakitException {
        return objectProcessList.stream()
                .filter(x -> x.objectCurrent(DatakitObjectEnum.of(objectType)))
                .findFirst()
                .orElseThrow(() -> new DatakitException("createDatakitObject", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                .makeObjectFormat(objectId, tableMetaFlag);
    }

    public void createDatabaseTable(Integer objectType, long objectId) throws DatakitException {
        objectProcessList.stream()
                .filter(x -> x.objectCurrent(DatakitObjectEnum.of(objectType)))
                .findFirst()
                .orElseThrow(() -> new DatakitException("createDatabaseTable", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                .doBusinessCreateTable(makeDatakitObject(objectType, objectId, true));
    }

    public void initProcess(Integer filterFlag) {
        ObjectQueue.initProcess(datakitConfig.getOnceCrawNum(), filterFlag);
    }

    public void setAFinished() {
        ObjectQueue.aSetFinish();
    }

    @Async(A_THREAD_POOL_EXECUTOR)
    public void doBusinessInputQueue(CountDownLatch latch, final Long taskInstanceId, final Task task) {
        String error = null;
        try {
            objectProcessList.stream()
                    .filter(x -> x.objectCurrent(DatakitObjectEnum.of(task.getAObjectType())))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("doBusinessWithObjectA", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                    .doBusinessInputQueue(taskInstanceId, task);
        } catch (DatakitException cde) {
            log.error("cde=", cde);
            error = cde.getErrMsg();
        } catch (Exception e) {
            log.error("e=", e);
            error = e.getMessage();
        }

        if (!Objects.isNull(error)) {
            ObjectQueue.setWorkThreadFail();
            commonProcess.saveRunInfo(INFO_LEVEL_ERROR, taskInstanceId, "数据发送内存队列流程异常:" + error);
        }
        latch.countDown();
    }

    @Async(B_THREAD_POOL_EXECUTOR)
    public void doBusinessFromQueue(CountDownLatch latch, final TaskInstance taskInstance) {
        String error = null;
        try {
            while (true) {
                if (ObjectQueue.isWorkThreadFail()) {
                    throw new DatakitException("doBusinessFromQueue", "被通知中止退出处理");
                }

                //是否需要多线程控制
                List<QueueData> queueDataList = ObjectQueue.queuePoll(datakitConfig.getOnceCrawNum());
                if (CollectionUtils.isEmpty(queueDataList) && ObjectQueue.isAFinished()) {
                    break;
                }

                if (CollectionUtils.isEmpty(queueDataList)) {
                    threadSleep(1000);
                } else {
                    matchDataQueueDoBusiness(queueDataList, taskInstance);
                }
            }
        } catch (DatakitException cde) {
            log.error("cde={}", cde);
            error = cde.getErrMsg();
        } catch (Exception e) {
            log.error("e={}", e);
            error = e.getMessage();
        }

        if (!Objects.isNull(error)) {
            ObjectQueue.setWorkThreadFail();
            commonProcess.saveRunInfo(INFO_LEVEL_ERROR, taskInstance.getTaskInstanceId(), "从队列获取数据后流程处理异常:" + error);
        }
        latch.countDown();
    }

    private void matchDataQueueDoBusiness(List<QueueData> queueDataList, final TaskInstance taskInstance) throws DatakitException {
        //执行具体业务流程
        objectProcessList.stream()
                .filter(x -> x.objectCurrent(DatakitObjectEnum.of(taskInstance.getTask().getBObjectType())))
                .findFirst()
                .orElseThrow(() -> new DatakitException("doBusinessFromQueue", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                .matchDataQueueDoBusiness(queueDataList, taskInstance);

        //记录数累计
        ObjectQueue.setDataSumCount(taskInstance.getTaskInstanceId(), queueDataList.size());
        //累计金额字段
        if (!CollectionUtils.isEmpty(taskInstance.getTask().getSumFieldRelaIdMap())) {
            taskInstance.getTask().getSumFieldRelaIdMap().forEach((x, y) -> {
                for (QueueData iterator : queueDataList) {
                    if (iterator.getDataMap().containsKey(y)) {
                        ObjectQueue.setDataSumAmount(y, Long.parseLong(iterator.getDataMap().get(y)));
                    }
                }
            });
        }
    }

    @Async(B_THREAD_POOL_EXECUTOR)
    public void doBusinessLoadDataToMapCache(CountDownLatch latch, final long taskInstanceId, final Task task) {
        String error = null;
        try {
            objectProcessList.stream()
                    .filter(x -> x.objectCurrent(DatakitObjectEnum.of(task.getBObjectType())))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("loadDataFromB", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                    .doBusinessLoadDataToMapCache(task);
        } catch (DatakitException cde) {
            log.error("cde=", cde);
            error = cde.getErrMsg();
        } catch (Exception e) {
            log.error("e=", e);
            error = e.getMessage();
        }

        if (!Objects.isNull(error)) {
            ObjectQueue.setWorkThreadFail();
            commonProcess.saveRunInfo(INFO_LEVEL_ERROR, taskInstanceId, "数据加载内存队列异常:" + error);
        }
        latch.countDown();
    }

    public void unCompareDataObject(final TaskInstance taskInstance) throws DatakitException {
        Map<Long, String> fieldNameValueMap = objectProcessList.stream()
                .filter(x -> x.objectCurrent(DatakitObjectEnum.of(taskInstance.getTask().getBObjectType())))
                .findFirst()
                .orElseThrow(() -> new DatakitException("unCompareDataObject", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                .genFieldIdNameMap(taskInstance.getTask().getBObject());

        ObjectQueue.getDataObjectMap().forEach((key, value) -> {
            if (value.getComparedFlag().equals(COMPARED_FLAG_NO)) {
                TaskResult taskResult = new TaskResult();
                taskResult.setTaskInstanceId(taskInstance.getTaskInstanceId());
                taskResult.setCompareInfo("目标数据: " + key + "不存在于源数据");
                taskResult.setCompareFlag(COMPARE_RESULT_FLAG_A_LESS_B);
                taskResult.setRepairState(COMPARE_REPAIR_STATE_UNDO);

                RepairRequest repairRequest = new RepairRequest();
                repairRequest.setTaskInstanceId(taskInstance.getTaskInstanceId());
                repairRequest.setTaskId(taskInstance.getTaskId());
                repairRequest.setCompareFlag(taskResult.getCompareFlag());
                repairRequest.setCompareKey(key);
                repairRequest.setBObjectValue(new HashMap<>());
                value.getDataMap().forEach((x, y) -> {
                    if (fieldNameValueMap.containsKey(x)) {
                        repairRequest.getBObjectValue().put(fieldNameValueMap.get(x), y);
                    }
                });

                taskResult.setCompareData(JSON.toJSONString(repairRequest));
                ObjectQueue.putTaskResult(taskResult);
            }
        });
    }

    public List<TaskResult> getTaskInstanceResultList() {
        return ObjectQueue.getTaskResultList();
    }

    public String sendFeignMessage(final String repairUrl, final String repairMessageRequest) {
        try {
            String repairMessageResponse = datakitFeignClient.sentRepairMessage(new URI(repairUrl), repairMessageRequest);
            log.info("repairMessageResponse={}", repairMessageResponse);
            RepairResponse repairResponse = JSON.parseObject(repairMessageResponse, RepairResponse.class);
            if (!repairResponse.getResultCode().equals(SUCCESS)) {
                return repairResponse.getResultMsg();
            }
        } catch (Exception e) {
            log.error("e=", e);
            return "sendFeignMessage Exception";

        }
        return SUCCESS;
    }

    public void repairData(final RepairRequest repairRequest, final Task task) throws DatakitException {
        //目前规则以源端数据为准
        if (repairRequest.getCompareFlag().equals(COMPARE_RESULT_FLAG_A_MORE_B)) {
            QueueData queueData = new QueueData();
            queueData.setDataMap(repairRequest.getATaskFieldRelIdValueMap());
            //在B中删除多余数据
            objectProcessList.stream()
                    .filter(x -> x.objectCurrent(DatakitObjectEnum.of(task.getBObjectType())))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("repairData001", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                    .repairData(REPAIR_DATA_ACTION_INSERT, task, queueData);
        } else if (repairRequest.getCompareFlag().equals(COMPARE_RESULT_FLAG_A_LESS_B)) {
            QueueData queueData = new QueueData();
            queueData.setFieldNameValueMap(repairRequest.getBObjectValue());
            //在B中删除多余数据
            objectProcessList.stream()
                    .filter(x -> x.objectCurrent(DatakitObjectEnum.of(task.getBObjectType())))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("repairData002", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                    .repairData(REPAIR_DATA_ACTION_DELETE, task, queueData);
        } else if (repairRequest.getCompareFlag().equals(COMPARE_RESULT_FLAG_A_NOT_EQUAL_B)) {
            //todo 在B中直接更新A的数据
            throw new DatakitException("repairData003", "建设中...");
        }
    }

    public Map<Long, String> queryFieldIDNameMap(Integer objectType, Long objectId) throws DatakitException {
        return objectProcessList.stream()
                .filter(x -> x.objectCurrent(DatakitObjectEnum.of(objectType)))
                .findFirst()
                .orElseThrow(() -> new DatakitException("queryFieldIDNameMap", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                .queryFieldIDNameMap(objectId);
    }

    public String getObjectName(final Integer objectType, final Object objectFormat) throws DatakitException {
        return objectProcessList.stream()
                .filter(x -> x.objectCurrent(DatakitObjectEnum.of(objectType)))
                .findFirst()
                .orElseThrow(() -> new DatakitException("getObjectName", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                .getObjectName(objectFormat);
    }

    public String queryObjectName(final Integer objectType, final Long objectId) throws DatakitException {
        return objectProcessList.stream()
                .filter(x -> x.objectCurrent(DatakitObjectEnum.of(objectType)))
                .findFirst()
                .orElseThrow(() -> new DatakitException("queryObjectName", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                .queryObjectName(objectId);
    }

    public Integer getObjectOnLineFlag(final Integer objectType, final Object objectFormat) throws DatakitException {
        return objectProcessList.stream()
                .filter(x -> x.objectCurrent(DatakitObjectEnum.of(objectType)))
                .findFirst()
                .orElseThrow(() -> new DatakitException("getObjectOnLineFlag", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                .getObjectOnLineFlag(objectFormat);
    }

    public void createDatabaseTableByObject(final Integer objectType, final Object objectFormat) throws DatakitException {
        objectProcessList.stream()
                .filter(x -> x.objectCurrent(DatakitObjectEnum.of(objectType)))
                .findFirst()
                .orElseThrow(() -> new DatakitException("createDatabaseTable", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                .doBusinessCreateTable(objectFormat);
    }

    public void followUpObject(final Long taskInstanceId, final Task task) throws DatakitException {
        objectProcessList.stream()
                .filter(x -> x.objectCurrent(DatakitObjectEnum.of(task.getBObjectType())))
                .findFirst()
                .orElseThrow(() -> new DatakitException("followUpObject", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                .followUpObject(taskInstanceId, task);
    }

    public Map<String, Long> getObjectSumFieldRelIdList(final Integer objectType, final Task task) throws DatakitException {
        return objectProcessList.stream()
                .filter(x -> x.objectCurrent(DatakitObjectEnum.of(objectType)))
                .findFirst()
                .orElseThrow(() -> new DatakitException("getObjectSumFieldRelIdList", ERROR_GET_OBJET_BY_TYPE_ID_FAIL))
                .getObjectSumFieldRelIdList(task);
    }
}
