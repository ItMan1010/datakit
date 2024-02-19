package com.itman.datakit.admin.service;


import com.github.pagehelper.PageInfo;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.exception.DatakitException;

import java.util.List;
import java.util.Map;

public interface TableService {

    PageInfo<TableFormat> selectTableFormatByOnLineFlagPage(Integer pageNum, Integer PageSize, Integer onLineFlag);

    PageInfo<TableField> selectTableFieldPage(Integer pageNum, Integer PageSize, Long tableFormatId);

    void createTableInstance(TableFormat tableFormat) throws DatakitException;

    void modifyTableInstance(TableFormat tableFormat) throws DatakitException;

    void deleteTableInstance(Long tableFormatId) throws DatakitException;

    void copyTableInstance(Long tableFormatId) throws DatakitException;

    TableFormat selectTableFormatById(Long tableFormatId);

    void createDatabaseTable(Long tableFormatId) throws DatakitException;

    void checkDatabaseTable(TableFormat tableFormat) throws DatakitException;

    void syncDatabaseTable(TableFormat tableFormat) throws DatakitException;

    void updateTableFormatOnLineFlagById(Long tableFormatId, Integer onLineFlag);

    List<String> getTableNameList(String dataBase) throws DatakitException;

    void tableTransform(String sourceTableName, String sourceDb, String targetDb) throws DatakitException;

    Map<Integer, String> getFieldTypeNameMergeMap(String dataBase);
}
