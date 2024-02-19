package com.itman.datakit.admin.service.impl;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.itman.datakit.admin.common.constants.DataBaseTypeEnum;
import com.itman.datakit.admin.common.dataroute.DruidConfig;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.common.tablemeta.TableColumnBean;
import com.itman.datakit.admin.common.tablemeta.TableMetaProcess;
import com.itman.datakit.admin.component.CommonProcess;
import com.itman.datakit.admin.component.DatakitProcess;
import com.itman.datakit.admin.dao.ObjectTableDao;
import com.itman.datakit.admin.plugins.ITable;
import com.itman.datakit.admin.service.TableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.enums.DatakitObjectEnum.DATAKIT_OBJECT_TABLE;
import static com.itman.datakit.admin.common.util.ChangeNameUtil.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class TableServiceImpl implements TableService {
    private final ObjectTableDao objectTableDao;
    private final DatakitProcess datakitProcess;
    private final TableMetaProcess tableMetaProcess;
    private final CommonProcess commonProcess;
    private final DruidConfig druidConfig;

    private final List<ITable> dataBaseList;

    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void createTableInstance(TableFormat tableFormat) throws DatakitException {
        if (StringUtils.isEmpty(tableFormat.getTableName())) {
            throw new DatakitException("createTableInstance001", " tableName is null ! ");
        }

        tableFormat.setTableFormatId(commonProcess.querySequence());
        //刚生成无效状态即下线状态
        tableFormat.setOnLineFlag(ON_LINE_FLAG_OFF_LINE);
        if (!objectTableDao.insertTable(tableFormat, getSqlSystemDate()).equals(1)) {
            throw new DatakitException("createTableInstance002", " insertTable error ! ");
        }

        if (CollectionUtils.isEmpty(tableFormat.getTableFieldList())) {
            throw new DatakitException("createTableInstance003", " getTableFieldList is null ! ");
        }

        List<TableField> tableFieldList = new ArrayList<>();
        for (TableField iterator : tableFormat.getTableFieldList()) {
            if (!StringUtils.isEmpty(iterator.getFieldName()) && iterator.getFieldType() != null) {
                iterator.setTableFieldId(commonProcess.querySequence());
                iterator.setTableFormatId(tableFormat.getTableFormatId());
                tableFieldList.add(iterator);
            }
        }

        if (!objectTableDao.insertTableFieldList(druidConfig.getUrlDbType("db0"), tableFieldList, getSqlSystemDate()).equals(tableFieldList.size())) {
            throw new DatakitException("createTableInstance004", " insertTable error ! ");
        }
    }


    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void modifyTableInstance(TableFormat tableFormat) throws DatakitException {
        if (StringUtils.isEmpty(tableFormat.getTableName())) {
            throw new DatakitException("modifyTableInstance001", " tableName is null ! ");
        }

        //更新表信息
        if (!objectTableDao.updateTableFormatById(tableFormat).equals(1)) {
            throw new DatakitException("modifyTableInstance002", " updateTableFormatById error ! ");
        }

        if (!CollectionUtils.isEmpty(tableFormat.getTableFieldList())) {
            tableFormat.getTableFieldList().forEach(x -> {
                if (x.getTableFieldId() == null) {
                    x.setTableFieldId(-1L);
                }
            });
        }

        //字段删除
        modifyTableInstanceFieldListDelete(tableFormat);

        //字段插入
        modifyTableInstanceFieldListInsert(tableFormat);
    }

    public void modifyTableInstanceFieldListDelete(TableFormat tableFormat) throws DatakitException {
        List<TableField> tableFieldListDelete = new ArrayList<>();
        List<TableField> tableFieldListCompared = objectTableDao.selectTableField(tableFormat.getTableFormatId());
        if (!CollectionUtils.isEmpty(tableFieldListCompared)) {
            for (TableField iterator : tableFieldListCompared) {
                if (tableFormat.getTableFieldList() == null ||
                        tableFormat.getTableFieldList().stream().filter(x ->
                                x.getTableFieldId().equals(iterator.getTableFieldId())).collect(Collectors.toList()).size() == 0) {
                    tableFieldListDelete.add(iterator);
                }
            }
        }

        //删除表字段行
        for (TableField iterator : tableFieldListDelete) {
            if (!objectTableDao.deleteTableFieldById(iterator.getTableFieldId()).equals(1)) {
                throw new DatakitException("updateTableInstance003", " deleteTableFieldById error ! ");
            }
        }
    }

    public void modifyTableInstanceFieldListInsert(TableFormat tableFormat) throws DatakitException {
        if (!CollectionUtils.isEmpty(tableFormat.getTableFieldList())) {
            List<TableField> tableFieldListInsert = new ArrayList<>();
            for (TableField iterator : tableFormat.getTableFieldList()) {
                if (iterator.getTableFieldId() > 0L) {
                    //字段更新操作
                    if (!objectTableDao.updateTableFieldById(iterator).equals(1)) {
                        throw new DatakitException("updateTableInstance", " updateTableFieldById error ! ");
                    }
                } else if (iterator.getTableFieldId().equals(-1L)) {
                    //字段新增操作
                    iterator.setTableFieldId(commonProcess.querySequence());
                    iterator.setTableFormatId(tableFormat.getTableFormatId());
                    tableFieldListInsert.add(iterator);
                }
            }

            //表字段插入操作
            if (!CollectionUtils.isEmpty(tableFieldListInsert) &&
                    !objectTableDao.insertTableFieldList(druidConfig.getUrlDbType("db0"), tableFieldListInsert, getSqlSystemDate()).equals(tableFieldListInsert.size())) {
                throw new DatakitException("updateTableInstance", " insertTableFieldList error ! ");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void deleteTableInstance(Long tableFormatId) {
        objectTableDao.deleteTableFormatById(tableFormatId);
        objectTableDao.deleteTableFieldByTableFormatId(tableFormatId);
    }

    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void copyTableInstance(Long tableFormatId) throws DatakitException {
        TableFormat tableFormat = getTableInfo(tableFormatId);
        if (Objects.isNull(tableFormat)) {
            throw new DatakitException("copyTableInstance", "查询表对象失败!");
        }
        tableFormat.setTableName("copy_" + tableFormat.getTableName());
        createTableInstance(tableFormat);
    }

    @Override
    public void updateTableFormatOnLineFlagById(Long tableFormatId, Integer onLineFlag) {
        objectTableDao.updateTableFormatOnLineFlagById(tableFormatId, onLineFlag);
    }

    @Override
    public PageInfo<TableFormat> selectTableFormatByOnLineFlagPage(Integer pageNum, Integer PageSize, Integer onLineFlag) {
        //开启分页功能，设置每页显示条数
        PageMethod.startPage(pageNum, PageSize);
        List<TableFormat> tableFormatList = objectTableDao.selectTableFormatByOnLineFlag(onLineFlag);
        if (!CollectionUtils.isEmpty(tableFormatList)) {
            tableFormatList.forEach(iterator -> {
                iterator.setOnLineFlagName(changeOnLineFlagName(iterator.getOnLineFlag()));
                iterator.setJdbcUrl(druidConfig.getJdbcUrl(iterator.getDataBase()));
            });
        }
        //获取分页相关数据，设置导航分页的页码数
        return new PageInfo<>(tableFormatList, 3);
    }

    @Override
    public PageInfo<TableField> selectTableFieldPage(Integer pageNum, Integer PageSize, Long tableFormatId) {
        //开启分页功能，设置每页显示条数
        PageMethod.startPage(pageNum, PageSize);
        List<TableField> tableFieldList = objectTableDao.selectTableField(tableFormatId);
        //获取分页相关数据，设置导航分页的页码数
        return new PageInfo<>(tableFieldList, 3);
    }

    @Override
    public TableFormat selectTableFormatById(Long tableFormatId) {
        return objectTableDao.selectTableFormatById(tableFormatId);
    }

    public TableFormat getTableInfo(Long tableFormatId) {
        TableFormat tableFormat = selectTableFormatById(tableFormatId);
        if (Objects.isNull(tableFormat)) {
            return null;
        }
        PageInfo<TableField> pageInfo = selectTableFieldPage(1, 100, tableFormatId);
        tableFormat.setTableFieldList(pageInfo.getList());
        return tableFormat;
    }


    @Override
    public void createDatabaseTable(Long tableFormatId) throws DatakitException {
        datakitProcess.createDatabaseTable(DATAKIT_OBJECT_TABLE.getValue(), tableFormatId);
    }

    @Override
    public void checkDatabaseTable(TableFormat tableFormat) throws DatakitException {
        //根据表名查询数据库表信息
        List<TableColumnBean> tableColumnsList = tableMetaProcess.getTableColumns(tableFormat.getDataBase(), tableFormat.getTableName());
        if (CollectionUtils.isEmpty(tableColumnsList)) {
            throw new DatakitException("checkDatabaseTable001", "表【" + tableFormat.getTableName() + "】在数据库中不存在!");
        }

        if (tableColumnsList.size() != tableFormat.getTableFieldList().size()) {
            throw new DatakitException("checkDatabaseTable002", "表字段个数不一致!");
        }

        for (TableField iterator : tableFormat.getTableFieldList()) {
            List<TableColumnBean> tableColumnsListTemp = tableColumnsList.stream().filter(x ->
                    x.getColumnName().equalsIgnoreCase(iterator.getFieldName())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(tableColumnsListTemp)) {
                throw new DatakitException("checkDatabaseTable003", "字段:" + iterator.getFieldName() + "在数据表里不存在!");
            }
//            //检查字段类型和长度是否一致
//            if (!TABLE_FIELD_TYPE_NAME_MAP.get(iterator.getFieldType()).equalsIgnoreCase(tableColumnsListTemp.get(0).getColumnType())) {
//                throw new DatakitException("checkDatabaseTable", "字段:" + iterator.getFieldName() + "类型属性不一致!");
//            }

            //mysql8.0开始开始不显示数字型类型字段长度，故没必要再校验
//            if (!iterator.getFieldType().equals(TABLE_FIELD_TYPE_DATETIME) && !iterator.getFieldLength().equals(tableColumnsListTemp.get(0).getColumnLength())) {
//                throw new DatakitException("checkDatabaseTable", "字段:" + iterator.getFieldName() + "长度属性不一致!");
//            }

            if (!iterator.getNullAble().equals(tableColumnsListTemp.get(0).getNullAble())) {
                throw new DatakitException("checkDatabaseTable004", "字段:" + iterator.getFieldName() + "不可空属性不一致!");
            }
        }
    }


    @Override
    public void syncDatabaseTable(TableFormat tableFormat) throws DatakitException {
        //根据表名查询数据库表信息
        List<TableColumnBean> tableColumnsList = tableMetaProcess.getTableColumns(tableFormat.getDataBase(), tableFormat.getTableName());
        if (CollectionUtils.isEmpty(tableColumnsList)) {
            throw new DatakitException("checkDatabaseTable", "查询数据表失败!");
        }

        if (!CollectionUtils.isEmpty(tableFormat.getTableFieldList())) {
            tableFormat.getTableFieldList().clear();
        } else {
            tableFormat.setTableFieldList(new ArrayList<>());
        }

        Map<String, Integer> FIELD_NAME_TYPE_MAP = getFieldNameTypeMap(tableFormat.getDataBase());
        tableColumnsList.forEach(x -> {
            TableField tableField = new TableField();
            tableField.setFieldName(x.getColumnName());
            tableField.setFieldType(FIELD_NAME_TYPE_MAP.get(x.getColumnType().toLowerCase()));
            tableField.setFieldLength(x.getColumnLength());
            tableField.setNullAble(x.getNullAble());
            tableField.setKeyFlag(x.isKeyFlag() ? 1 : 0);
            tableFormat.getTableFieldList().add(tableField);
        });
    }

    public List<String> getTableNameList(String dataBase) throws DatakitException {
        return tableMetaProcess.getTableNameList(dataBase);
    }

    public void tableTransform(String sourceTableName, String sourceDb, String targetDb) throws DatakitException {
        TableFormat tableFormat = new TableFormat();
        tableFormat.setDataBase(sourceDb);
        tableFormat.setTableName(sourceTableName);
        syncDatabaseTable(tableFormat);
        tableFormat.setDataBase(targetDb);
        datakitProcess.createDatabaseTableByObject(DATAKIT_OBJECT_TABLE.getValue(), tableFormat);
    }

    private String getSqlSystemDate() {
        try {
            return dataBaseList.stream()
                    .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType("db0"))))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("getSqlSystemDate", "getSqlSystemDate:不能数据类型匹配出对应数据配置"))
                    .getSqlSystemDate();
        } catch (DatakitException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, String> getFieldTypeNameMergeMap(String dataBase) {
        try {
            return dataBaseList.stream()
                    .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType(dataBase))))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("getFieldTypeNameMap", "getFieldTypeNameMap:不能数据类型匹配出对应数据配置"))
                    .getFieldTypeNameMergeMap();
        } catch (DatakitException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> getFieldNameTypeMap(String dataBase) {
        try {
            return dataBaseList.stream()
                    .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType(dataBase))))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("getFieldTypeNameMap", "getFieldNameTypeMap:不能数据类型匹配出对应数据配置"))
                    .getFieldNameTypeMap();
        } catch (DatakitException e) {
            throw new RuntimeException(e);
        }
    }
}
