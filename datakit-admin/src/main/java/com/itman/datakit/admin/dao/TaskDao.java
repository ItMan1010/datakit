package com.itman.datakit.admin.dao;

import com.itman.datakit.admin.common.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;

@Mapper
public interface TaskDao {
    Integer insertTask(@Param("taskList") List<Task> taskList, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer insertTaskRepair(@Param("taskRepair") TaskRepair taskRepair, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer insertTaskFieldRela(@Param("dataBaseType") String dataBaseType, @Param("taskFieldRelaList") List<TaskFieldRela> taskFieldRelaList, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer insertTaskInstance(@Param("taskInstanceList") List<TaskInstance> taskInstanceList, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer deleteTaskInstance(@Param("taskInstanceId") Long taskInstanceId) throws DataAccessException;

    Integer deleteTaskResult(@Param("taskInstanceId") Long taskInstanceId) throws DataAccessException;

    TaskInstance selectTaskInstanceById(@Param("taskInstanceId") Long taskInstanceId) throws DataAccessException;

    List<TaskInstance> selectTaskInstanceByRunState(@Param("runState") Integer runState, @Param("sqlLimit") String sqlLimit) throws DataAccessException;

    List<TaskInstance> selectTaskInstanceByTaskId(@Param("taskId") Long taskId, @Param("runState") Integer runState) throws DataAccessException;

    List<TaskRepair> selectTaskRepairByRepairState(@Param("repairState") Integer repairState, @Param("sqlLimit") String sqlLimit) throws DataAccessException;

    List<TaskInstance> selectTaskInstanceByCondition(@Param("beginDate") String beginDate, @Param("endDate") String endDate, @Param("runState") Integer runState) throws DataAccessException;

    Integer updateTaskInstance(@Param("taskInstanceId") Long taskInstanceId, @Param("runState") Integer runState, @Param("oldRunState") Integer oldRunState, @Param("hostName") String hostName, @Param("hostIp") String hostIp, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateTaskRepair(@Param("taskRepairId") Long taskRepairId, @Param("repairState") Integer repairState, @Param("oldRepairState") Integer oldRepairState, @Param("hostName") String hostName, @Param("hostIp") String hostIp, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateTaskInstanceErrorMsg(@Param("taskInstanceId") Long taskInstanceId, @Param("runState") Integer runState, @Param("oldRunState") Integer oldRunState, @Param("errorMsg") String errorMsg, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateTaskRepairErrorMsg(@Param("taskRepairId") Long taskRepairId, @Param("repairState") Integer repairState, @Param("oldRepairState") Integer oldRepairState, @Param("errorMsg") String errorMsg, @Param("sysdate") String sysdate) throws DataAccessException;

    Task selectTaskByTaskId(@Param("taskId") Long taskId) throws DataAccessException;

    List<Task> selectTaskByTaskType(@Param("taskType") Integer taskType, @Param("taskId") Integer taskId) throws DataAccessException;

    List<Task> selectTaskByObjectId(@Param("objectRole") Integer objectRole, @Param("objectType") Integer objectType, @Param("objectId") Long objectId, @Param("onLineFlag") Integer onLineFlag) throws DataAccessException;

    Integer updateTaskByTaskId(@Param("task") Task task) throws DataAccessException;

    Integer deleteTaskByTaskId(@Param("taskId") Long taskId) throws DataAccessException;

    Integer deleteTaskFieldRelaByTaskId(@Param("taskId") Long taskId) throws DataAccessException;

    Integer deleteTaskFieldRelaById(@Param("taskFieldRelaId") Long taskFieldRelaId) throws DataAccessException;

    Integer updateTaskFieldRelaById(@Param("taskFieldRela") TaskFieldRela taskFieldRela) throws DataAccessException;

    List<TaskFieldRela> selectTaskFieldRelaByTaskId(@Param("taskId") Long taskId) throws DataAccessException;

    Integer insertTaskResult(@Param("dataBaseType") String dataBaseType, @Param("taskResultList") List<TaskResult> taskResultList, @Param("sysdate") String sysdate) throws DataAccessException;

    TaskResult selectTaskResultById(@Param("taskResultId") Long taskResultId) throws DataAccessException;

    List<TaskResult> selectTaskResultByTaskInstanceId(@Param("taskInstanceId") Long taskInstanceId, @Param("repairState") Integer repairState) throws DataAccessException;

    Integer updateTaskResultRepairInfo(@Param("taskResultId") Long taskResultId, @Param("repairState") Integer repairState, @Param("oldRepairState") Integer oldRepairState, @Param("repairInfo") String repairInfo, @Param("taskRepairId") Long taskRepairId, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateTaskOnLineFlagById(@Param("taskId") Long taskId, @Param("onLineFlag") Integer onLineFlag) throws DataAccessException;

}
