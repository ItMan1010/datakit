package com.itman.datakit.admin.service.impl;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.itman.datakit.admin.common.constants.DataBaseTypeEnum;
import com.itman.datakit.admin.common.dataroute.DruidConfig;
import com.itman.datakit.admin.common.entity.TableFlowFormat;
import com.itman.datakit.admin.common.entity.TableFlowNode;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.common.tablemeta.TableColumnBean;
import com.itman.datakit.admin.common.tablemeta.TableMetaProcess;
import com.itman.datakit.admin.component.CommonProcess;
import com.itman.datakit.admin.dao.ObjectTableFlowDao;
import com.itman.datakit.admin.plugins.ITable;
import com.itman.datakit.admin.service.TableFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.util.ChangeNameUtil.changeOnLineFlagName;


@Slf4j
@Service
@RequiredArgsConstructor
public class TableFlowServiceImpl implements TableFlowService {
    private final ObjectTableFlowDao objectTableFlowDao;
    private final TableMetaProcess tableMetaProcess;
    private final CommonProcess commonProcess;
    private final DruidConfig druidConfig;
    private final List<ITable> dataBaseList;

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

    @Override
    public PageInfo<TableFlowFormat> selectTableFlowByOnLineFlagPage(Integer pageNum, Integer PageSize, Integer onLineFlag) {
        //开启分页功能，设置每页显示条数
        PageMethod.startPage(pageNum, PageSize);
        List<TableFlowFormat> tableFormatList = objectTableFlowDao.selectTableFlowByOnLineFlag(onLineFlag);
        if (!CollectionUtils.isEmpty(tableFormatList)) {
            tableFormatList.forEach(iterator -> {
                iterator.setOnLineFlagName(changeOnLineFlagName(iterator.getOnLineFlag()));
                iterator.setSourceJdbcUrl(druidConfig.getJdbcUrl(iterator.getSourceDataBase()));
                iterator.setTargetJdbcUrl(druidConfig.getJdbcUrl(iterator.getTargetDataBase()));
            });
        }
        //获取分页相关数据，设置导航分页的页码数
        return new PageInfo<>(tableFormatList, 3);
    }

    @Override
    public TableFlowFormat selectTableFlowById(Long flowFormatId) {
        TableFlowFormat tableFlowFormat = objectTableFlowDao.selectTableFlowById(flowFormatId);
        if (!Objects.isNull(tableFlowFormat)) {
            tableFlowFormat.setTableFlowNodeList(objectTableFlowDao.selectTableFlowNodeById(flowFormatId));
        }
        return tableFlowFormat;
    }

    @Override
    public void updateTableFlowOnLineFlagById(Long flowFormatId, Integer onLineFlag) {
        objectTableFlowDao.updateTableFlowOnLineFlagById(flowFormatId, onLineFlag);
    }

    @Override
    public List<String> getDatabaseTableFieldNameList(String dataBase, String tableName) throws DatakitException {
        //根据表名查询数据库表信息
        List<String> filedNameList = new ArrayList<>();
        List<TableColumnBean> tableColumnsList = tableMetaProcess.getTableColumns(dataBase, tableName);
        if (!CollectionUtils.isEmpty(tableColumnsList)) {
            filedNameList = tableColumnsList.stream().map(x -> x.getColumnName()).collect(Collectors.toList());
        }
        return filedNameList;
    }


    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void createTableFlow(TableFlowFormat tableFlowFormat) throws DatakitException {
        if (StringUtils.isEmpty(tableFlowFormat.getFlowName())) {
            throw new DatakitException("createTableFlow001", "流程名称不能为空");
        }

        tableFlowFormat.setFlowFormatId(commonProcess.querySequence());
        //刚生成无效状态即下线状态
        tableFlowFormat.setOnLineFlag(ON_LINE_FLAG_OFF_LINE);
        if (!objectTableFlowDao.insertTableFlowFormat(tableFlowFormat, getSqlSystemDate()).equals(1)) {
            throw new DatakitException("createTableFlow002", " insertTableFlowFormat error ! ");
        }

        insertTableFlowNodeList(tableFlowFormat.getFlowFormatId(), tableFlowFormat.getTableFlowNodeList());
    }

    void tableFlowNodeTreeToList(List<TableFlowNode> sourceTableFlowNodeList, List<TableFlowNode> targetTableFlowNodeList) {
        if (CollectionUtils.isEmpty(sourceTableFlowNodeList)) {
            return;
        }
        sourceTableFlowNodeList.forEach(x -> targetTableFlowNodeList.add(x));
        sourceTableFlowNodeList.forEach(x -> tableFlowNodeTreeToList(x.getChildren(), targetTableFlowNodeList));
    }


    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void updateTableFlow(TableFlowFormat tableFlowFormat) throws DatakitException {
        if (StringUtils.isEmpty(tableFlowFormat.getFlowName())) {
            throw new DatakitException("updateTableFlow", "流程名称不能为空");
        }

        //刚生成无效状态即下线状态
        tableFlowFormat.setOnLineFlag(ON_LINE_FLAG_OFF_LINE);
        objectTableFlowDao.updateTableFlowFormatById(tableFlowFormat);

        //先删除，在查询
        objectTableFlowDao.deleteTableFlowNodeByFlowFormatId(tableFlowFormat.getFlowFormatId());

        insertTableFlowNodeList(tableFlowFormat.getFlowFormatId(), tableFlowFormat.getTableFlowNodeList());
    }

    void insertTableFlowNodeList(Long flowFormatId, List<TableFlowNode> tableFlowNodeList) throws DatakitException {
        if (!CollectionUtils.isEmpty(tableFlowNodeList)) {
            tableFlowNodeList.forEach(x -> {
                if (Objects.isNull(x.getSelectedFieldName())) {
                    x.setSelectedFieldName("-1");
                }
                if (Objects.isNull(x.getSelectedParentFieldName())) {
                    x.setSelectedParentFieldName("-1");
                }
            });
            List<TableFlowNode> insertTableFlowNodeList = new ArrayList<>();
            tableFlowNodeTreeToList(tableFlowNodeList, insertTableFlowNodeList);
            insertTableFlowNodeList.forEach(x -> x.setFlowFormatId(flowFormatId));
            if (!objectTableFlowDao.insertTableFlowNodeList(druidConfig.getUrlDbType("db0"), insertTableFlowNodeList, getSqlSystemDate()).equals(insertTableFlowNodeList.size())) {
                throw new DatakitException("createTableFlow", " insertTableFlowNodeList error ! ");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void deleteTableFlow(Long flowFormatId) {
        objectTableFlowDao.deleteTableFlowById(flowFormatId);
        objectTableFlowDao.deleteTableFlowNodeByFlowFormatId(flowFormatId);
    }
}
