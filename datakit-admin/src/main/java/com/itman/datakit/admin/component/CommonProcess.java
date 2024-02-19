package com.itman.datakit.admin.component;

import com.itman.datakit.admin.common.constants.DataBaseTypeEnum;
import com.itman.datakit.admin.common.dataroute.DruidConfig;
import com.itman.datakit.admin.common.entity.RunInfo;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.dao.CommonDao;
import com.itman.datakit.admin.plugins.ITable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;

/**
 * TODO
 *
 * @author: ItMan
 * @since: 2023/12/22  10:33
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonProcess {
    private final CommonDao commonDao;
    private final DruidConfig druidConfig;
    private final List<ITable> dataBaseList;

    public Long querySequence() {
        //统一使用一个序列值
        return commonDao.querySequence(druidConfig.getUrlDbType("db0"), "seq_datakit_id");
    }


    public void saveRunInfo(Integer infoLevel, Long taskInstanceId, String runInfo) {
        String message = null;
        if (runInfo.length() >= 1000) {
            message = Thread.currentThread().getName() + "--" + runInfo.substring(0, 1000);
        } else {
            message = Thread.currentThread().getName() + "--" + runInfo;
        }

        RunInfo taskRun = new RunInfo();
        taskRun.setInfoLevel(infoLevel);
        taskRun.setRunInfo(message);
        taskRun.setTaskInstanceId(taskInstanceId);
        taskRun.setRunInfoId(querySequence());
        List<RunInfo> taskRunList = new ArrayList<>();
        taskRunList.add(taskRun);
        commonDao.insertTaskRun(taskRunList, getSqlSystemDate());
    }

    public void saveMonitor(Long taskInstanceId, Integer dataSumCount) {
        List<RunInfo> runInfoList = commonDao.selectRunInfoByIdAndLevel(taskInstanceId, INFO_LEVEL_MONITOR);
        if (CollectionUtils.isEmpty(runInfoList)) {
            saveRunInfo(INFO_LEVEL_MONITOR, taskInstanceId, "数据实时处理总量【" + dataSumCount + "】");
        } else {
            String runInfo = Thread.currentThread().getName() + "--" + "数据实时获取总量【" + dataSumCount + "】";
            commonDao.updateTaskRunInfoById(runInfoList.get(0).getRunInfoId(), runInfo);
        }
    }

    public List<RunInfo> selectRunInfoByTaskInstanceId(Long taskInstanceId) {
        return commonDao.selectRunInfoByTaskInstanceId(taskInstanceId);
    }

    private String getSqlSystemDate() {
        try {
            return dataBaseList.stream()
                    .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType("db0"))))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("getSqlSystemDate", "can not match DataBaseTypeEnum!"))
                    .getSqlSystemDate();
        } catch (DatakitException e) {
            throw new RuntimeException(e);
        }
    }
}
