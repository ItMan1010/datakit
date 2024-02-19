package com.itman.datakit.admin.service.impl;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.itman.datakit.admin.common.api.RepairRequest;
import com.itman.datakit.admin.common.config.DatakitConfig;
import com.itman.datakit.admin.common.constants.DataBaseTypeEnum;
import com.itman.datakit.admin.common.constants.DatakitConstant;
import com.itman.datakit.admin.common.dataqueue.ObjectQueue;
import com.itman.datakit.admin.common.dataroute.DruidConfig;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.enums.DatakitObjectEnum;
import com.itman.datakit.admin.common.enums.ObjectTaskEnum;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.common.util.IpUtil;
import com.itman.datakit.admin.component.CommonProcess;
import com.itman.datakit.admin.component.DatakitProcess;
import com.itman.datakit.admin.dao.TaskDao;
import com.itman.datakit.admin.plugins.ITable;
import com.itman.datakit.admin.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.enums.DatakitObjectEnum.*;
import static com.itman.datakit.admin.common.util.ChangeNameUtil.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final DatakitProcess datakitProcess;
    private final TaskDao taskDao;
    private final DatakitConfig datakitConfig;
    private final CommonProcess commonProcess;
    private final List<ITable> dataBaseList;
    private final DruidConfig druidConfig;

    @Scheduled(fixedDelay = 3000)
    @Async("ScheduleTask")
    public void scheduledObjectTask() {
        try {
            log.info("scheduledObjectTask------->");

            //正常任务处理
            dispatchTaskInstance();

            //数据修复任务处理
            dispatchTaskRepair();
        } catch (Exception e) {
            log.error("---------Exception=", e);
        }
    }

    private void dispatchTaskInstance() {
        List<TaskInstance> taskInstanceList = taskDao.selectTaskInstanceByRunState(TASK_INSTANCE_RUN_STATE_INIT, makeSqlLimit(0, 1));
        if (!CollectionUtils.isEmpty(taskInstanceList) && taskInstanceList.get(0).getTaskId() != null) {
            if (taskInstanceList.get(0).getRunState().equals(TASK_INSTANCE_RUN_STATE_INIT)) {
                Integer updateCount = taskDao.updateTaskInstance(taskInstanceList.get(0).getTaskInstanceId(),
                        DatakitConstant.TASK_INSTANCE_RUN_STATE_MOVE, TASK_INSTANCE_RUN_STATE_INIT,
                        IpUtil.getHostName(), IpUtil.getIp(), getSqlSystemDate());
                if (!updateCount.equals(1)) {
                    log.info("dispatchRecoveryFlowTask : have been done by other host thread, taskInstanceId={}", taskInstanceList.get(0).getTaskInstanceId());
                    return;
                }

                //加载任务执行,只处理任务开始时间之前数据
                executeTaskInstance(taskInstanceList.get(0));
            }
        } else {
            log.debug(" there is no undo task instance !");
        }
    }

    private void dispatchTaskRepair() {
        List<TaskRepair> taskRepairList = taskDao.selectTaskRepairByRepairState(TASK_INSTANCE_RUN_STATE_INIT, makeSqlLimit(0, 1));
        if (!CollectionUtils.isEmpty(taskRepairList) && taskRepairList.get(0).getTaskRepairId() != null) {
            if (taskRepairList.get(0).getRepairState().equals(TASK_INSTANCE_RUN_STATE_INIT)) {
                Integer updateCount = taskDao.updateTaskRepair(taskRepairList.get(0).getTaskRepairId(),
                        DatakitConstant.TASK_INSTANCE_RUN_STATE_MOVE, TASK_INSTANCE_RUN_STATE_INIT,
                        IpUtil.getHostName(), IpUtil.getIp(), getSqlSystemDate());
                if (!updateCount.equals(1)) {
                    log.info("dispatchTaskRepair : have been done by other host thread, taskRepairId={}", taskRepairList.get(0).getTaskRepairId());
                    return;
                }

                //加载任务执行,只处理任务开始时间之前数据
                executeTaskRepair(taskRepairList.get(0));
            }
        } else {
            log.debug(" there is no undo task repair !");
        }
    }

    /**
     * 组装任务对象信息
     *
     * @param taskId 任务标识
     * @return Task 任务对象
     * @throws DatakitException
     */
    private Task makeTask(Long taskId) throws DatakitException {
        Task task = taskDao.selectTaskByTaskId(taskId);
        if (Objects.isNull(task)) {
            throw new DatakitException("makeTask", " task is null!");
        }

        //组装业务A对象
        task.setAObject(datakitProcess.makeDatakitObject(task.getAObjectType(), task.getAObjectId(), true));

        //组装业务B对象
        task.setBObject(datakitProcess.makeDatakitObject(task.getBObjectType(), task.getBObjectId(), true));

        //组装对象间字段关联
        task.setTaskFieldRelaList(taskDao.selectTaskFieldRelaByTaskId(task.getTaskId()));

        //获取B对象如果是文件有字段汇总标识关联字段标识
        task.setSumFieldRelaIdMap(datakitProcess.getObjectSumFieldRelIdList(task.getBObjectType(), task));

        return task;
    }

    /**
     * 封装任务对象字段名称
     *
     * @param task
     * @return
     */
    private Task matchTaskObjectFieldName(Task task) throws DatakitException {
        if (!Objects.isNull(task.getAObjectType()) && !Objects.isNull(task.getAObject())) {
            Map<Long, String> aObjectFieldIDNameMap = datakitProcess.queryFieldIDNameMap(task.getAObjectType(), task.getAObjectId());
            if (!CollectionUtils.isEmpty(task.getTaskFieldRelaList()) && !CollectionUtils.isEmpty(aObjectFieldIDNameMap)) {
                task.getTaskFieldRelaList().forEach(x -> {
                    if (aObjectFieldIDNameMap.containsKey(x.getAObjectFieldId())) {
                        x.setAObjectFieldName(aObjectFieldIDNameMap.get(x.getAObjectFieldId()));
                    }
                });
            }

            task.setAObjectName(datakitProcess.getObjectName(task.getAObjectType(), task.getAObject()));
        }
        if (!Objects.isNull(task.getBObjectType()) && !Objects.isNull(task.getBObject())) {
            Map<Long, String> bObjectFieldIDNameMap = datakitProcess.queryFieldIDNameMap(task.getBObjectType(), task.getBObjectId());
            if (!CollectionUtils.isEmpty(task.getTaskFieldRelaList()) && !CollectionUtils.isEmpty(bObjectFieldIDNameMap)) {
                task.getTaskFieldRelaList().forEach(x -> {
                    if (bObjectFieldIDNameMap.containsKey(x.getBObjectFieldId())) {
                        x.setBObjectFieldName(bObjectFieldIDNameMap.get(x.getBObjectFieldId()));
                    }
                });
            }
            task.setBObjectName(datakitProcess.getObjectName(task.getBObjectType(), task.getBObject()));
        }

        return task;
    }

    public Task makeTaskInfoById(Long taskId) throws DatakitException {
        //转换字段名称
        return matchTaskObjectFieldName(makeTask(taskId));
    }

    private void executeTaskInstance(final TaskInstance taskInstance) {
        try {
            taskInstance.setTask(makeTask(taskInstance.getTaskId()));

            switch (ObjectTaskEnum.of(taskInstance.getTask().getTaskType())) {
                case OBJECT_TASK_TYPE_DATA_EXCHANGE:
                case OBJECT_TASK_TYPE_TABLE_FLOW_SYNC:
                    exchangeObjectDataAToB(taskInstance);
                    break;
                case OBJECT_TASK_TYPE_DATA_COMPARE:
                    compareObjectDataAndB(taskInstance);
                    break;
                default:
                    break;
            }

            updateTaskInstanceSuccess(taskInstance.getTaskInstanceId());
        } catch (DatakitException cde) {
            log.error("{}", cde);
            updateTaskInstanceError(taskInstance.getTaskInstanceId(), cde.getMessage());
        } catch (Exception e) {
            log.error("{}", e);
            updateTaskInstanceError(taskInstance.getTaskInstanceId(), "unknown exception!");
        }
    }

    Integer updateTaskInstanceSuccess(Long taskInstanceId) {
        return taskDao.updateTaskInstanceErrorMsg(taskInstanceId, DatakitConstant.TASK_INSTANCE_RUN_STATE_FINISH, DatakitConstant.TASK_INSTANCE_RUN_STATE_MOVE, "运行成功 !", getSqlSystemDate());
    }

    Integer updateTaskInstanceError(Long taskInstanceId, String errorMessage) {
        return taskDao.updateTaskInstanceErrorMsg(taskInstanceId, DatakitConstant.TASK_INSTANCE_RUN_STATE_ERROR, DatakitConstant.TASK_INSTANCE_RUN_STATE_MOVE, errorMessage, getSqlSystemDate());
    }


    private void executeTaskRepair(final TaskRepair taskRepair) {
        try {
            repairRequest(taskRepair);

            updateTaskRepairSuccess(taskRepair.getTaskRepairId());
        } catch (DatakitException cde) {
            log.error("{}", cde);
            updateTaskRepairError(taskRepair.getTaskRepairId(), cde.getMessage());
        } catch (Exception e) {
            log.error("{}", e);
            updateTaskRepairError(taskRepair.getTaskRepairId(), "unknown exception!");
        }
    }

    Integer updateTaskRepairSuccess(Long taskRepairId) {
        return taskDao.updateTaskRepairErrorMsg(taskRepairId, DatakitConstant.TASK_INSTANCE_RUN_STATE_FINISH, DatakitConstant.TASK_INSTANCE_RUN_STATE_MOVE, "task success !", getSqlSystemDate());
    }

    Integer updateTaskRepairError(Long taskRepairId, String errorMessage) {
        return taskDao.updateTaskRepairErrorMsg(taskRepairId, DatakitConstant.TASK_INSTANCE_RUN_STATE_ERROR, DatakitConstant.TASK_INSTANCE_RUN_STATE_MOVE, errorMessage, getSqlSystemDate());
    }

    @Override
    public void exchangeObjectDataAToB(final TaskInstance taskInstance) throws DatakitException {
        commonProcess.saveRunInfo(INFO_LEVEL_INFO, taskInstance.getTaskInstanceId(), "任务开始");

        datakitProcess.initProcess(null);

        doBusinessFromAToB(taskInstance);

        if (ObjectQueue.isWorkThreadFail()) {
            commonProcess.saveRunInfo(INFO_LEVEL_INFO, taskInstance.getTaskInstanceId(), "任务失败结束");
        } else {
            commonProcess.saveRunInfo(INFO_LEVEL_INFO, taskInstance.getTaskInstanceId(), "任务正常结束");
        }
    }

    private void saveTaskResult(Long taskInstanceId, String resultInfo) throws DatakitException {
        TaskResult taskResult = new TaskResult();
        taskResult.setTaskResult(resultInfo);
        taskResult.setTaskInstanceId(taskInstanceId);
        taskResult.setTaskResultId(commonProcess.querySequence());
        List<TaskResult> taskResultList = new ArrayList<>();
        taskResultList.add(taskResult);
        taskDao.insertTaskResult(druidConfig.getUrlDbType("db0"), taskResultList, getSqlSystemDate());
    }

    private void doBusinessFromAToB(final TaskInstance taskInstance) throws DatakitException {
        commonProcess.saveMonitor(taskInstance.getTaskInstanceId(), 0);

        //获取默认线程数
        Integer threadCountA = datakitConfig.getAThreadCount();
        Integer threadCountB = datakitConfig.getBThreadCount();

        if (!Objects.isNull(taskInstance.getTask().getOrderFlag()) && taskInstance.getTask().getOrderFlag().equals(1)) {
            //单线程写入保证数据顺序
            threadCountA = 1;
            threadCountB = 1;
        }

        //文件加载处理必须单线程
        if (DatakitObjectEnum.of(taskInstance.getTask().getAObjectType()).equals(DATAKIT_OBJECT_FILE)) {
            threadCountA = 1;
        }

        commonProcess.saveRunInfo(INFO_LEVEL_INFO, taskInstance.getTaskInstanceId(), "源端线程加载数据发送队列开始,线程数:" + threadCountA);

        final CountDownLatch countDownLatchA = new CountDownLatch(threadCountA);
        //业务对象A加载数据发送内存队列
        doBusinessWithObjectA(countDownLatchA, threadCountA, taskInstance.getTaskInstanceId(), taskInstance.getTask());

        //业务对象B从内存队列获取数据进行业务流程处理
        commonProcess.saveRunInfo(INFO_LEVEL_INFO, taskInstance.getTaskInstanceId(), "目标端线程从队列获取数据业务处理开始,线程数:" + threadCountB);

        final CountDownLatch countDownLatchB = new CountDownLatch(threadCountB);
        doBusinessWithObjectB(countDownLatchB, threadCountB, taskInstance);

        try {
            log.info("countDownLatchA await");
            countDownLatchA.await();
            //设置A完整状态
            datakitProcess.setAFinished();
            log.info("countDownLatchA end");
        } catch (InterruptedException e) {
            log.error("e=", e);
            throw new DatakitException("countDownLatchA", " countDownLatchA InterruptedException");
        }

        commonProcess.saveRunInfo(INFO_LEVEL_INFO, taskInstance.getTaskInstanceId(), "源端数据加载发送队列结束,线程数:" + threadCountA);

        try {
            log.info("countDownLatchB await");
            countDownLatchB.await();
            log.info("countDownLatchB end");
        } catch (InterruptedException e) {
            log.error("countDownLatchB InterruptedException=", e);
            throw new DatakitException("countDownLatchB", " countDownLatchB InterruptedException");
        }
        commonProcess.saveRunInfo(INFO_LEVEL_INFO, taskInstance.getTaskInstanceId(), "目标端从队列获取数据业务处理结束,线程数:" + threadCountB);

        if (!ObjectQueue.isWorkThreadFail()) {
            //进行善后处理
            datakitProcess.followUpObject(taskInstance.getTaskInstanceId(), taskInstance.getTask());

            commonProcess.saveMonitor(taskInstance.getTaskInstanceId(), ObjectQueue.getDataSumCount(taskInstance.getTaskInstanceId()));
        }

        ObjectQueue.finishProcess(taskInstance.getTaskInstanceId());
    }

    private void doBusinessWithObjectA(CountDownLatch countDownLatchA, final Integer threadCount, final Long taskInstanceId, final Task task) throws DatakitException {
        if (DatakitObjectEnum.of(task.getAObjectType()).equals(DATAKIT_OBJECT_FILE) && !threadCount.equals(1)) {
            throw new DatakitException("getDataFromA", "doBusinessWithObjectA must have only one thread !");
        }

        for (int i = 0; i < threadCount; i++) {
            datakitProcess.doBusinessInputQueue(countDownLatchA, taskInstanceId, task);
        }
    }

    private void doBusinessWithObjectB(CountDownLatch countDownLatchB, final Integer threadCount, final TaskInstance taskInstance) throws DatakitException {
        //实现多线程处理
        for (int i = 0; i < threadCount; i++) {
            datakitProcess.doBusinessFromQueue(countDownLatchB, taskInstance);
        }
    }

    @Override
    public void compareObjectDataAndB(final TaskInstance taskInstance) throws DatakitException {
        commonProcess.saveRunInfo(INFO_LEVEL_INFO, taskInstance.getTaskInstanceId(), "数据比较:目标端数据加载缓存开始");

        datakitProcess.initProcess(null);

        //加载业务对象B端的数据插入本地Map缓存中
        loadDataFromB(taskInstance.getTaskInstanceId(), taskInstance.getTask());

        commonProcess.saveRunInfo(INFO_LEVEL_INFO, taskInstance.getTaskInstanceId(), "数据比较:目标端数据加载缓存完成");

        datakitProcess.initProcess(1);

        //多线程批量比较
        doBusinessFromAToB(taskInstance);

        //处理剩下B端未比较数据
        datakitProcess.unCompareDataObject(taskInstance);

        if (!CollectionUtils.isEmpty(datakitProcess.getTaskInstanceResultList())) {
            datakitProcess.getTaskInstanceResultList().forEach(iterator -> {
                iterator.setTaskInstanceId(taskInstance.getTaskInstanceId());
                iterator.setTaskResult("数据明细差异:");
                iterator.setTaskResultId(commonProcess.querySequence());
            });
            taskDao.insertTaskResult(druidConfig.getUrlDbType("db0"), datakitProcess.getTaskInstanceResultList(), getSqlSystemDate());
            saveTaskResult(taskInstance.getTaskInstanceId(), "数据差异汇总:差异数[" + datakitProcess.getTaskInstanceResultList().size() + "]");
        } else {
            saveTaskResult(taskInstance.getTaskInstanceId(), "数据差异汇总:无差异");
        }
    }

    /**
     * 多线加载业务对象B端数据上传内存map结构
     *
     * @param task
     */
    private void loadDataFromB(final long taskInstanceId, final Task task) throws DatakitException {
        final CountDownLatch countDownLatchB = new CountDownLatch(datakitConfig.getBThreadCount());

        for (int i = 0; i < datakitConfig.getBThreadCount(); i++) {
            datakitProcess.doBusinessLoadDataToMapCache(countDownLatchB, taskInstanceId, task);
        }

        try {
            log.info("countDownLatchB await");
            countDownLatchB.await();
            log.info("countDownLatchB end");
        } catch (InterruptedException e) {
            log.error("{}", e);
            Thread.currentThread().interrupt();
            throw new DatakitException("loadDataFromB", " countDownLatchB InterruptedException");
        }
    }

    @Override
    public void repairRequest(final TaskRepair taskRepair) throws DatakitException {
        //查询待修订的数据
        List<TaskResult> taskResultList = new ArrayList<>();
        taskResultList.addAll(taskDao.selectTaskResultByTaskInstanceId(taskRepair.getTaskInstanceId(), COMPARE_REPAIR_STATE_UNDO));
        taskResultList.addAll(taskDao.selectTaskResultByTaskInstanceId(taskRepair.getTaskInstanceId(), COMPARE_REPAIR_STATE_FAIL));
        if (!CollectionUtils.isEmpty(taskResultList)) {
            //查询数据订正类型
            TaskInstance taskInstance = taskDao.selectTaskInstanceById(taskRepair.getTaskInstanceId());
            Optional.ofNullable(taskInstance).orElseThrow(() -> new DatakitException("repairObjectData001", "taskInstance is null!"));

            Task task = taskDao.selectTaskByTaskId(taskInstance.getTaskId());
            Optional.ofNullable(task).orElseThrow(() -> new DatakitException("repairObjectData002", " task is null!"));
            Optional.ofNullable(task.getRepairUrl()).orElseThrow(() -> new DatakitException("repairObjectData003", " repairUrl is null!"));

            for (TaskResult iterator : taskResultList) {
                //平帐发送队列或http请求
                String errorInfo = datakitProcess.sendFeignMessage(task.getRepairUrl(), iterator.getCompareData());
                if (errorInfo.equals(SUCCESS)) {
                    taskDao.updateTaskResultRepairInfo(iterator.getTaskResultId(), COMPARE_REPAIR_STATE_SUCCESS, iterator.getRepairState(), "repair success", taskRepair.getTaskRepairId(), getSqlSystemDate());
                } else {
                    taskDao.updateTaskResultRepairInfo(iterator.getTaskResultId(), COMPARE_REPAIR_STATE_FAIL, iterator.getRepairState(), errorInfo, taskRepair.getTaskRepairId(), getSqlSystemDate());
                }
            }
        }
    }

    @Override
    public PageInfo<TaskInstance> selectTaskInstancePage(Integer pageNum, Integer PageSize, String beginDate, String endDate, Integer runState) {
        //开启分页功能，设置每页显示条数
        PageMethod.startPage(pageNum, PageSize);
        List<TaskInstance> taskInstanceList = taskDao.selectTaskInstanceByCondition(makeSqlDate(beginDate + "000000"), makeSqlDate(endDate + "235959"), runState);
        if (!CollectionUtils.isEmpty(taskInstanceList)) {
            for (TaskInstance iterator : taskInstanceList) {
                iterator.setRunStateName(changeTaskInstanceRunStateName(iterator.getRunState()));
                Task task = taskDao.selectTaskByTaskId(iterator.getTaskId());
                if (!Objects.isNull(task)) {
                    iterator.setTaskType(task.getTaskType());
                    iterator.setTaskTypeName(ObjectTaskEnum.of(task.getTaskType()).getDescription());
                }
            }
        }
        //获取分页相关数据，设置导航分页的页码数
        return new PageInfo<>(taskInstanceList, 3);
    }

    @Override
    public TaskInstance selectTaskInstanceById(Long taskInstanceId) {
        TaskInstance taskInstance = taskDao.selectTaskInstanceById(taskInstanceId);
        if (!Objects.isNull(taskInstance)) {
            taskInstance.setRunStateName(changeTaskInstanceRunStateName(taskInstance.getRunState()));
            Task task = taskDao.selectTaskByTaskId(taskInstance.getTaskId());
            if (!Objects.isNull(task)) {
                taskInstance.setTaskType(task.getTaskType());
                taskInstance.setTaskTypeName(ObjectTaskEnum.of(task.getTaskType()).getDescription());
            }
        }
        return taskInstance;
    }

    @Override
    public List<TaskResult> selectTaskResultByTaskInstanceId(Long taskInstanceId) {
        List<TaskResult> taskResultList = taskDao.selectTaskResultByTaskInstanceId(taskInstanceId, null);
        return taskResultList;
    }

    @Override
    public List<RunInfo> selectRunInfoByTaskInstanceId(Long taskInstanceId) {
        return commonProcess.selectRunInfoByTaskInstanceId(taskInstanceId);
    }

    public PageInfo<Task> selectTaskPage(Integer pageNum, Integer PageSize, Integer taskType, Integer taskId) {
        //开启分页功能，设置每页显示条数
        PageMethod.startPage(pageNum, PageSize);
        List<Task> taskList = taskDao.selectTaskByTaskType(taskType, taskId);
        if (!CollectionUtils.isEmpty(taskList)) {
            taskList.forEach(x -> {
                x.setOnLineFlagName(changeOnLineFlagName(x.getOnLineFlag()));
                x.setTaskTypeName(ObjectTaskEnum.of(x.getTaskType()).getDescription());
                x.setAObjectTypeName(changeObjectTypeName(x.getAObjectType()));
                x.setBObjectTypeName(changeObjectTypeName(x.getBObjectType()));
                try {
                    x.setAObjectName(datakitProcess.queryObjectName(x.getAObjectType(), x.getAObjectId()));
                    x.setBObjectName(datakitProcess.queryObjectName(x.getBObjectType(), x.getBObjectId()));
                } catch (DatakitException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        //获取分页相关数据，设置导航分页的页码数
        return new PageInfo<>(taskList, 3);
    }

    public void createTaskInstance(Long taskId) throws DatakitException {
        Optional.ofNullable(taskId).orElseThrow(() -> new DatakitException("createTaskInstance", " taskId is null !"));
        TaskInstance taskInstance = new TaskInstance();
        taskInstance.setTaskInstanceId(commonProcess.querySequence());
        taskInstance.setTaskId(taskId);
        taskInstance.setRunState(TASK_INSTANCE_RUN_STATE_INIT);
        List<TaskInstance> taskInstanceList = new ArrayList<>();
        taskInstanceList.add(taskInstance);
        if (!taskDao.insertTaskInstance(taskInstanceList, getSqlSystemDate()).equals(1)) {
            throw new DatakitException("createTaskInstance", " insertTaskInstance fail! ");
        }
    }

    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void createTask(Task task) throws DatakitException {
        Optional.ofNullable(task).orElseThrow(() -> new DatakitException("createTask001", " task is null ! "));
        task.setTaskId(commonProcess.querySequence());
        task.setOnLineFlag(ON_LINE_FLAG_OFF_LINE);
        List<Task> taskList = new ArrayList<>();
        taskList.add(task);
        if (!taskDao.insertTask(taskList, getSqlSystemDate()).equals(1)) {
            throw new DatakitException("createTask002", " insertTask fail! ");
        }

        createTaskFieldRelaList(task);
    }

    public void createTaskFieldRelaList(Task task) throws DatakitException {
        if (!CollectionUtils.isEmpty(task.getTaskFieldRelaList())) {
            List<TaskFieldRela> taskFieldRelaListInsert = new ArrayList<>();

            getTaskFieldRelaListInsert(task.getTaskId(), task.getTaskFieldRelaList(), taskFieldRelaListInsert);

            if (CollectionUtils.isEmpty(taskFieldRelaListInsert)) {
                throw new DatakitException("createTaskFieldRelaList001", " 请设置对象字段映射配对! ");
            }

            if (!taskDao.insertTaskFieldRela(druidConfig.getUrlDbType("db0"), taskFieldRelaListInsert, getSqlSystemDate()).equals(taskFieldRelaListInsert.size())) {
                throw new DatakitException("createTaskFieldRelaList002", " insertTaskFieldRela fail! ");
            }
        }
    }

    private void getTaskFieldRelaListInsert(Long taskId, List<TaskFieldRela> taskFieldRelaList, List<TaskFieldRela> taskFieldRelaListInsert) throws DatakitException {
        for (TaskFieldRela iterator : taskFieldRelaList) {
            if ((iterator.getAObjectFieldId() <= 0 && iterator.getBObjectFieldId() > 0) ||
                    (iterator.getAObjectFieldId() > 0 && iterator.getBObjectFieldId() < 0)) {
                throw new DatakitException("getTaskFieldRelaListInsert", " 请正确配对映射对象字段! ");
            }

            if (iterator.getAObjectFieldId() > 0 && iterator.getBObjectFieldId() > 0) {
                iterator.setTaskFieldRelaId(commonProcess.querySequence());
                iterator.setTaskId(taskId);
                taskFieldRelaListInsert.add(iterator);
            }
        }
    }


    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void createTaskRepair(Long taskInstanceId) throws DatakitException {
        Optional.ofNullable(taskInstanceId).orElseThrow(() -> new DatakitException("createTaskRepair", "createTaskRepair:任务实例标识ID为空"));
        TaskRepair taskRepair = new TaskRepair();
        taskRepair.setTaskRepairId(commonProcess.querySequence());
        taskRepair.setTaskInstanceId(taskInstanceId);
        taskRepair.setRepairState(TASK_INSTANCE_RUN_STATE_INIT);
        if (!taskDao.insertTaskRepair(taskRepair, getSqlSystemDate()).equals(1)) {
            throw new DatakitException("createTask", " createTaskRepair fail! ");
        }
    }


    @Transactional(rollbackFor = DatakitException.class)
    public void deleteTaskInstance(Long taskInstanceId) throws DatakitException {
        Optional.ofNullable(taskInstanceId).orElseThrow(() -> new DatakitException("deleteTaskInstance", "deleteTaskInstance:任务实例标识ID为空"));
        if (!taskDao.deleteTaskInstance(taskInstanceId).equals(1)) {
            throw new DatakitException("deleteTaskInstance", "deleteTaskInstance：删除任务实例数据失败");
        }
        taskDao.deleteTaskResult(taskInstanceId);
    }

    @Transactional(rollbackFor = DatakitException.class)
    public void stopTaskInstance(Long taskInstanceId) throws DatakitException {
        Optional.ofNullable(taskInstanceId).orElseThrow(() -> new DatakitException("stopTaskInstance001", "stopTaskInstance:任务实例标识ID为空"));
        TaskInstance taskInstance = taskDao.selectTaskInstanceById(taskInstanceId);
        if (!Objects.isNull(taskInstance)) {
            if (!taskInstance.getRunState().equals(TASK_INSTANCE_RUN_STATE_MOVE)) {
                throw new DatakitException("stopTaskInstance002", "任务不在进行中不能中止操作");
            }

            ObjectQueue.setWorkThreadFail();
            commonProcess.saveRunInfo(INFO_LEVEL_WARN, taskInstanceId, "通知任务中止处理");
            //为了防止异常不能结束的状态
            if (!taskDao.updateTaskInstance(taskInstanceId, TASK_INSTANCE_RUN_STATE_FINISH, taskInstance.getRunState(), null, null, getSqlSystemDate()).equals(1)) {
                throw new DatakitException("stopTaskInstance003", " stopTaskInstance:更新任务实例数据失败");
            }
        }
    }


    public void updateTaskInstance(Long taskInstanceId) throws DatakitException {
        Optional.ofNullable(taskInstanceId).orElseThrow(() -> new DatakitException("updateTaskInstance001", "updateTaskInstance:任务实例标识ID为空"));
        TaskInstance taskInstance = taskDao.selectTaskInstanceById(taskInstanceId);
        if (Objects.isNull(taskInstance)) {
            throw new DatakitException("updateTaskInstance002", "查无任务实例标识ID【" + taskInstanceId + "】数据");
        }

        if (!taskDao.updateTaskInstance(taskInstanceId, TASK_INSTANCE_RUN_STATE_INIT, taskInstance.getRunState(), null, null, getSqlSystemDate()).equals(1)) {
            throw new DatakitException("updateTaskInstance003", " deleteTaskInstance fail! ");
        }
    }

    @Transactional(rollbackFor = DatakitException.class)
    public void deleteTask(Long taskId) throws DatakitException {
        Optional.ofNullable(taskId).orElseThrow(() -> new DatakitException("deleteTask", "taskId is null!"));
        if (!taskDao.deleteTaskByTaskId(taskId).equals(1)) {
            throw new DatakitException("deleteTask", " deleteTaskByTaskId fail! ");
        }
        taskDao.deleteTaskFieldRelaByTaskId(taskId);
    }

    @Transactional(rollbackFor = DatakitException.class)
    public void copyTask(Long taskId) throws DatakitException {
        Task task = makeTask(taskId);
        task.setRemark("copy from taskId【" + taskId + "】");
        createTask(task);
    }

    @Transactional(rollbackFor = DatakitException.class)
    public void modifyTask(Task task) throws DatakitException {
        taskDao.updateTaskByTaskId(task);

        modifyTaskFieldRelaList(task);

        //delete操作
        deleteTaskFieldRelaList(task);
    }

    public void modifyTaskFieldRelaList(Task task) throws DatakitException {
        if (!CollectionUtils.isEmpty(task.getTaskFieldRelaList())) {
            List<TaskFieldRela> taskFieldRelaListInsert = new ArrayList<>();
            for (TaskFieldRela iterator : task.getTaskFieldRelaList()) {
                if (iterator.getTaskFieldRelaId().equals(-1L)) {
                    iterator.setTaskFieldRelaId(commonProcess.querySequence());
                    iterator.setTaskId(task.getTaskId());
                    taskFieldRelaListInsert.add(iterator);
                } else {
                    taskDao.updateTaskFieldRelaById(iterator);
                }
            }

            if (!CollectionUtils.isEmpty(taskFieldRelaListInsert) && !taskDao.insertTaskFieldRela(druidConfig.getUrlDbType("db0"), taskFieldRelaListInsert, getSqlSystemDate()).equals(taskFieldRelaListInsert.size())) {
                throw new DatakitException("updateTask", " insertTaskFieldRela fail! ");
            }
        }
    }

    public void deleteTaskFieldRelaList(Task task) {
        //delete操作
        List<TaskFieldRela> taskFieldRelaList = taskDao.selectTaskFieldRelaByTaskId(task.getTaskId());
        if (!CollectionUtils.isEmpty(taskFieldRelaList)) {
            for (TaskFieldRela iterator : taskFieldRelaList) {
                if (task.getTaskFieldRelaList() == null ||
                        task.getTaskFieldRelaList().stream().filter(x -> x.getTaskFieldRelaId().equals(iterator.getTaskFieldRelaId())).collect(Collectors.toList()).size() == 0) {
                    taskDao.deleteTaskFieldRelaById(iterator.getTaskFieldRelaId());
                }
            }
        }
    }

    @Override
    public void repairData(final RepairRequest repairRequest) throws DatakitException {
        Task task = makeTask(repairRequest.getTaskId());
        datakitProcess.repairData(repairRequest, task);
    }

    @Override
    public Map<Long, String> queryFieldIDNameMap(Integer objectType, Long objectId) throws DatakitException {
        return datakitProcess.queryFieldIDNameMap(objectType, objectId);
    }

    @Override
    public void checkTaskOnLineByObjectId(Integer objectType, Long objectId) throws DatakitException {
        List<Task> taskAList = taskDao.selectTaskByObjectId(1, objectType, objectId, ON_LINE_FLAG_ON_LINE);
        if (!CollectionUtils.isEmpty(taskAList)) {
            throw new DatakitException("checkTaskStateByObjectId", "请先下线关联任务ID：" + taskAList.stream().map(x -> x.getTaskId().toString()).collect(Collectors.joining(",")));
        }
        List<Task> taskBList = taskDao.selectTaskByObjectId(2, objectType, objectId, ON_LINE_FLAG_ON_LINE);
        if (!CollectionUtils.isEmpty(taskBList)) {
            throw new DatakitException("checkTaskStateByObjectId", "请先下线关联任务ID：" + taskBList.stream().map(x -> x.getTaskId().toString()).collect(Collectors.joining(",")));
        }
    }

    @Override
    public void updateTaskOnLineFlagById(Long taskId, Integer onLineFlag) {
        taskDao.updateTaskOnLineFlagById(taskId, onLineFlag);
    }

    public void checkTaskObjectOnLine(Long taskId) throws DatakitException {
        Task task = makeTask(taskId);
        if (!Objects.isNull(task)) {
            if (!Objects.isNull(task.getAObject())) {
                if (!datakitProcess.getObjectOnLineFlag(task.getAObjectType(), task.getAObject()).equals(ON_LINE_FLAG_ON_LINE)) {
                    throw new DatakitException("checkTaskOnLine", "请先申请源对象上线");
                }
            }

            if (!Objects.isNull(task.getBObject())) {
                if (!datakitProcess.getObjectOnLineFlag(task.getBObjectType(), task.getBObject()).equals(ON_LINE_FLAG_ON_LINE)) {
                    throw new DatakitException("checkTaskOnLine", "请先申请目标对象上线");
                }
            }
        }
    }

    public void checkTaskOffLine(Long taskId) throws DatakitException {
        List<TaskInstance> stateMoveList = taskDao.selectTaskInstanceByTaskId(taskId, TASK_INSTANCE_RUN_STATE_MOVE);
        if (!CollectionUtils.isEmpty(stateMoveList)) {
            throw new DatakitException("checkTaskOffLine", "有正在处理任务实例:" + stateMoveList.stream().map(x -> x.getTaskInstanceId().toString()).collect(Collectors.joining(",")));
        }

        List<TaskInstance> stateInitList = taskDao.selectTaskInstanceByTaskId(taskId, TASK_INSTANCE_RUN_STATE_INIT);
        if (!CollectionUtils.isEmpty(stateInitList)) {
            throw new DatakitException("checkTaskOffLine", "有待处理任务实例:" + stateInitList.stream().map(x -> x.getTaskInstanceId().toString()).collect(Collectors.joining(",")));
        }
    }

    private String makeSqlLimit(Integer pageRow, Integer pageCount) {
        try {
            return dataBaseList.stream()
                    .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType("db0"))))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("makeSqlLimit", "makeSqlLimit:根据数据库类型匹配数据库失败"))
                    .makeSqlLimit(pageRow, pageCount);
        } catch (DatakitException e) {
            throw new RuntimeException(e);
        }
    }

    private String makeSqlDate(String date) {
        try {
            return dataBaseList.stream()
                    .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType("db0"))))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("makeSqlDate", "makeSqlDate:根据数据库类型匹配数据库失败"))
                    .stringToDate(date);
        } catch (DatakitException e) {
            throw new RuntimeException(e);
        }
    }

    private String getSqlSystemDate() {
        try {
            return dataBaseList.stream()
                    .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType("db0"))))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("getSqlSystemDate", "getSqlSystemDate:根据数据库类型匹配数据库失败"))
                    .getSqlSystemDate();
        } catch (DatakitException e) {
            throw new RuntimeException(e);
        }
    }
}
