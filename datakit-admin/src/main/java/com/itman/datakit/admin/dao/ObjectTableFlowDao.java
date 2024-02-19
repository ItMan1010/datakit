package com.itman.datakit.admin.dao;

import com.itman.datakit.admin.common.entity.TableFlowFormat;
import com.itman.datakit.admin.common.entity.TableFlowNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;

@Mapper
public interface ObjectTableFlowDao {
    List<Map<String, Object>> selectDataList(String dataBase, @Param("selectSql") String selectSql) throws DataAccessException;

    Integer insertDataList(String dataBase, @Param("insertSql") String insertSql) throws DataAccessException;

    List<TableFlowFormat> selectTableFlowByOnLineFlag(@Param("onLineFlag") Integer onLineFlag) throws DataAccessException;

    TableFlowFormat selectTableFlowById(@Param("flowFormatId") Long flowFormatId) throws DataAccessException;

    List<TableFlowNode> selectTableFlowNodeById(@Param("flowFormatId") Long flowFormatId) throws DataAccessException;

    Integer updateTableFlowOnLineFlagById(@Param("flowFormatId") Long flowFormatId, @Param("onLineFlag") Integer onLineFlag) throws DataAccessException;

    Integer insertTableFlowFormat(@Param("tableFlowFormat") TableFlowFormat tableFlowFormat, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer deleteTableFlowNodeByFlowFormatId(@Param("flowFormatId") Long flowFormatId) throws DataAccessException;

    Integer insertTableFlowNodeList(@Param("dataBaseType") String dataBaseType, @Param("tableFlowNodeList") List<TableFlowNode> tableFlowNodeList, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateTableFlowFormatById(@Param("tableFlowFormat") TableFlowFormat tableFlowFormat) throws DataAccessException;

    Integer deleteTableFlowById(@Param("flowFormatId") Long flowFormatId) throws DataAccessException;
}
