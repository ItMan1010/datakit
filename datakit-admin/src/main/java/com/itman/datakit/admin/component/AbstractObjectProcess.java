package com.itman.datakit.admin.component;

import com.alibaba.fastjson.JSON;
import com.itman.datakit.admin.common.api.RepairRequest;
import com.itman.datakit.admin.common.dataqueue.ObjectQueue;
import com.itman.datakit.admin.common.dataqueue.QueueData;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.exception.DatakitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;

@Slf4j
@Component
public abstract class AbstractObjectProcess {
    /**
     * 获取分发过来数据转换key后和Map数据进行比较
     *
     * @param queueDataList
     * @param taskId
     * @param taskInstanceId
     * @throws DatakitException
     */
    public void objectDataCompareToMapCache(List<QueueData> queueDataList, final Long taskId, final Long taskInstanceId) {
        Map<String, QueueData> dataRecordMap = new HashMap<>();

        queueDataList.forEach(iterator -> {
            if (!Objects.isNull(iterator.getComparedKey())) {
                //把队列数据转成实际数据
                dataRecordMap.put(iterator.getComparedKey(), iterator);
            }
        });
        objectDataCompareResult(dataRecordMap, taskInstanceId, taskId);
    }

    public void objectDataCompareResult(Map<String, QueueData> dataRecordMap, final Long taskInstanceId, final Long taskId) {
        Iterator<Map.Entry<String, QueueData>> iterator = dataRecordMap.entrySet().iterator();
        Map.Entry<String, QueueData> entry;
        while (iterator.hasNext()) {
            entry = iterator.next();
            if (ObjectQueue.getDataObjectMap().containsKey(entry.getKey())) {
                ObjectQueue.getDataObjectMap().get(entry.getKey()).setComparedFlag(COMPARED_FLAG_YES);
            } else {
                TaskResult taskResult = new TaskResult();
                taskResult.setCompareInfo("源数据: " + entry.getKey() + "不存在目标数据中!");
                taskResult.setTaskInstanceId(taskInstanceId);
                taskResult.setCompareFlag(COMPARE_RESULT_FLAG_A_MORE_B);
                taskResult.setRepairState(COMPARE_REPAIR_STATE_UNDO);

                RepairRequest repairRequest = new RepairRequest();
                repairRequest.setTaskInstanceId(taskInstanceId);
                repairRequest.setTaskId(taskId);
                repairRequest.setCompareFlag(taskResult.getCompareFlag());
                repairRequest.setCompareKey(entry.getKey());
                repairRequest.setAObjectValue(entry.getValue().getFieldNameValueMap());
                repairRequest.setATaskFieldRelIdValueMap(entry.getValue().getDataMap());
                taskResult.setCompareData(JSON.toJSONString(repairRequest));
                ObjectQueue.putTaskResult(taskResult);
            }
        }
    }

    public Integer getDataSumCount(Long taskInstanceId) {
        return ObjectQueue.getDataSumCount(taskInstanceId);
    }

    public Long getDataSumAmount(Long sumFieldRelaId) {
        return ObjectQueue.getDataSumAmount(sumFieldRelaId);
    }

}
