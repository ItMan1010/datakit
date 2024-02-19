package com.itman.datakit.admin.dao;

import com.itman.datakit.admin.common.entity.RunInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;

@Mapper
public interface CommonDao {
    Long querySequence(@Param("dataBaseType") String dataBaseType, @Param("sequenceName") String sequenceName);

    Integer insertTaskRun(@Param("taskRunList") List<RunInfo> taskRunList, @Param("sysdate") String sysdate) throws DataAccessException;

    List<RunInfo> selectRunInfoByTaskInstanceId(@Param("taskInstanceId") Long taskInstanceId) throws DataAccessException;

    List<RunInfo> selectRunInfoByIdAndLevel(@Param("taskInstanceId") Long taskInstanceId, @Param("infoLevel") Integer infoLevel) throws DataAccessException;

    Integer updateTaskRunInfoById(@Param("runInfoId") Long runInfoId, @Param("runInfo") String runInfo) throws DataAccessException;
}
