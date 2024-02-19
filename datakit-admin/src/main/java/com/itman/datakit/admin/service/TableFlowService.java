package com.itman.datakit.admin.service;


import com.github.pagehelper.PageInfo;
import com.itman.datakit.admin.common.entity.TableFlowFormat;
import com.itman.datakit.admin.common.exception.DatakitException;

import java.util.List;

public interface TableFlowService {
    PageInfo<TableFlowFormat> selectTableFlowByOnLineFlagPage(Integer pageNum, Integer PageSize, Integer onLineFlag);

    TableFlowFormat selectTableFlowById(Long flowFormatId);

    void updateTableFlowOnLineFlagById(Long flowFormatId, Integer onLineFlag);

    List<String> getDatabaseTableFieldNameList(String dataBase, String tableName) throws DatakitException;

    void createTableFlow(TableFlowFormat tableFlowFormat) throws DatakitException;

    void updateTableFlow(TableFlowFormat tableFlowFormat) throws DatakitException;

    void deleteTableFlow(Long flowFormatId);
}
