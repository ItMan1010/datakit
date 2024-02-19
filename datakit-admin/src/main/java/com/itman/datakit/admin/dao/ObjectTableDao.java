package com.itman.datakit.admin.dao;

import com.itman.datakit.admin.common.entity.TableField;
import com.itman.datakit.admin.common.entity.TableFormat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;

@Mapper
public interface ObjectTableDao {
    Integer insertDataList(String dataBase, @Param("insertSql") String insertSql) throws DataAccessException;

    List<Map<String, Object>> selectDataList(String dataBase, @Param("selectSql") String selectSql) throws DataAccessException;

    Integer deleteData(String dataBase, @Param("deleteSql") String deleteSql) throws DataAccessException;

    Integer executeTableSql(String dataBase, @Param("executeSql") String executeSql) throws DataAccessException;

    List<TableField> selectTableField(@Param("tableFormatId") Long tableFormatId) throws DataAccessException;

    TableField selectTableFieldById(@Param("tableFieldId") Long tableFieldId) throws DataAccessException;

    TableFormat selectTableFormatByName(@Param("tableName") String tableName) throws DataAccessException;

    TableFormat selectTableFormatById(@Param("tableFormatId") Long tableFormatId) throws DataAccessException;

    List<TableFormat> selectTableFormatByOnLineFlag(@Param("onLineFlag") Integer onLineFlag) throws DataAccessException;

    Integer deleteTableFormatById(@Param("tableFormatId") Long tableFormatId) throws DataAccessException;

    Integer updateTableFormatById(@Param("tableFormat") TableFormat tableFormat) throws DataAccessException;

    Integer updateTableFormatOnLineFlagById(@Param("tableFormatId") Long tableFormatId, @Param("onLineFlag") Integer onLineFlag) throws DataAccessException;

    Integer insertTable(@Param("tableFormat") TableFormat tableFormat, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateTableFieldById(@Param("tableField") TableField tableField) throws DataAccessException;

    Integer insertTableFieldList(@Param("dataBaseType") String dataBaseType, @Param("tableFieldList") List<TableField> tableFieldList, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer deleteTableFieldById(@Param("tableFieldId") Long tableFieldId) throws DataAccessException;

    Integer deleteTableFieldByTableFormatId(@Param("tableFormatId") Long tableFormatId) throws DataAccessException;
}
