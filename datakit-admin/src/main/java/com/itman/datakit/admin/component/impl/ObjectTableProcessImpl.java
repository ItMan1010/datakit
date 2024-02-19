package com.itman.datakit.admin.component.impl;

import com.itman.datakit.admin.common.config.DatakitConfig;
import com.itman.datakit.admin.common.constants.DataBaseTypeEnum;
import com.itman.datakit.admin.common.dataqueue.ObjectQueue;
import com.itman.datakit.admin.common.dataqueue.QueueData;
import com.itman.datakit.admin.common.dataroute.DruidConfig;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.enums.DatakitObjectEnum;
import com.itman.datakit.admin.common.enums.ObjectTaskEnum;
import com.itman.datakit.admin.common.enums.TableFieldTypeEnum;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.common.tablemeta.TableColumnBean;
import com.itman.datakit.admin.common.tablemeta.TableMetaProcess;
import com.itman.datakit.admin.component.AbstractObjectProcess;
import com.itman.datakit.admin.component.IObjectProcess;
import com.itman.datakit.admin.dao.ObjectTableDao;
import com.itman.datakit.admin.plugins.ITable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.enums.DatakitObjectEnum.DATAKIT_OBJECT_TABLE;
import static com.itman.datakit.admin.common.enums.TableFieldTypeEnum.*;
import static com.itman.datakit.admin.common.util.CommonUtil.genMapKey;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObjectTableProcessImpl extends AbstractObjectProcess implements IObjectProcess {
    private final ObjectTableDao tableDao;
    private final TableMetaProcess tableMetaProcess;
    private final DruidConfig druidConfig;
    private final List<ITable> dataBaseList;
    private final DatakitConfig datakitConfig;

    @Override
    public Boolean objectCurrent(DatakitObjectEnum objectEnum) {
        return objectEnum.equals(DATAKIT_OBJECT_TABLE);
    }


    /**
     * 执行组合后批量插入sql语句
     *
     * @param tableFormat
     * @param dataRecordList
     */
    private void makeDataInsertSql(TableFormat tableFormat, List<Map<String, String>> dataRecordList) {
        if (CollectionUtils.isEmpty(dataRecordList)) {
            return;
        }
        List<String> dataRowList = dataRecordList.stream().map(x -> makeInsertRow(tableFormat.getDataBase(), tableFormat.getTableFieldList(), x)).collect(Collectors.toList());
        StringBuilder insertSql = new StringBuilder();
        if (druidConfig.getUrlDbType(tableFormat.getDataBase()).equalsIgnoreCase("oracle")) {
            insertSql.append("insert all ");
            for (String iterator : dataRowList) {
                insertSql.append("into ");
                insertSql.append(tableFormat.getTableName());
                insertSql.append(" (");
                insertSql.append(tableFormat.getInsertSqlColumns());
                insertSql.append(") values ");
                insertSql.append(iterator);
            }
            insertSql.append(" select 1 from dual ");
        } else {
            insertSql.append("insert into ");
            insertSql.append(tableFormat.getTableName());
            insertSql.append(" (");
            insertSql.append(tableFormat.getInsertSqlColumns());
            insertSql.append(") values ");
            insertSql.append(dataRowList.stream().collect(Collectors.joining(",")));
        }
        tableDao.insertDataList(tableFormat.getDataBase(), insertSql.toString());
    }

    /**
     * 根据字段组合插入sql语句
     *
     * @param dataBase
     * @param tableFieldList
     * @param dataRow
     * @return 插入sql语句
     */
    private String makeInsertRow(String dataBase, List<TableField> tableFieldList, Map<String, String> dataRow) {
        List<String> dataColumnList = new ArrayList<>();
        for (TableField iterator : tableFieldList) {
            Object columnValue = null;
            if (dataRow.containsKey(iterator.getFieldName())) {
                columnValue = dataRow.get(iterator.getFieldName());
            } else if (dataRow.containsKey(iterator.getFieldName().toLowerCase())) {
                columnValue = dataRow.get(iterator.getFieldName().toLowerCase());
            }

            if (!Objects.isNull(columnValue)) {
                if (TableFieldTypeEnum.of(iterator.getFieldType()).getClassify() == TABLE_FIELD_TYPE_CLASSIFY_DATETIME) {
                    dataColumnList.add(changeStringToDate(dataBase, columnValue.toString()));
                } else if (TableFieldTypeEnum.of(iterator.getFieldType()).getClassify() == TABLE_FIELD_TYPE_CLASSIFY_STRING) {
                    dataColumnList.add("'" + columnValue + "'");
                } else {
                    dataColumnList.add(columnValue.toString());
                }
            } else {
                dataColumnList.add("null");
            }
        }
        return "( " + dataColumnList.stream().collect(Collectors.joining(",")) + ")";
    }

    private String changeStringToDate(String dataBase, String columnValue) {
        try {
            return dataBaseList.stream()
                    .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType(dataBase))))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("changeStringToDate", "changeStringToDate:根据数据库类型匹配数据库配置失败"))
                    .stringToDate(columnValue);
        } catch (DatakitException e) {
            throw new RuntimeException(e);
        }
    }

    private String makeInsertSqlColumns(List<TableField> tableFieldList) {
        return tableFieldList.stream().map(TableField::getFieldName).collect(Collectors.joining(","));
    }

    private String makeSelectSqlColumns(String dataBase, List<TableField> tableFieldList) throws DatakitException {
        List<String> sourceTableColumnList = new ArrayList<>();
        for (TableField iterator : tableFieldList) {
            String fieldName = iterator.getFieldName();

            if (TableFieldTypeEnum.of(iterator.getFieldType()).getClassify() == TABLE_FIELD_TYPE_CLASSIFY_DATETIME) {
                fieldName = changeDateToString(dataBase, fieldName);
            }
            sourceTableColumnList.add(fieldName);
        }

        return sourceTableColumnList.stream().collect(Collectors.joining(","));
    }

    String changeDateToString(String dataBase, String columnName) throws DatakitException {
        return dataBaseList.stream()
                .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType(dataBase))))
                .findFirst()
                .orElseThrow(() -> new DatakitException("changeDateToString", "changeDateToString:根据数据库类型匹配数据库配置失败"))
                .dateToString(columnName);
    }

    @Override
    public Object makeObjectFormat(Long objectFormatId, Boolean tableMetaFlag) throws DatakitException {
        return makeTableFormat(objectFormatId, tableMetaFlag);
    }

    private TableFormat makeTableFormat(Long tableFormatId, Boolean tableMetaFlag) throws DatakitException {
        TableFormat tableFormat = tableDao.selectTableFormatById(tableFormatId);
        if (Objects.isNull(tableFormat)) {
            throw new DatakitException("makeTableFormat", "tableFormat is null!");
        }

        tableFormat.setTableFieldList(tableDao.selectTableField(tableFormatId));
        if (!tableFormat.getTableFieldList().isEmpty()) {
            tableFormat.getTableFieldList().forEach(x ->
                    x.setFieldName(x.getFieldName().toUpperCase()));
        }

        if (tableMetaFlag != null && tableMetaFlag) {
            List<TableColumnBean> tableColumnsList = tableMetaProcess.getTableColumns(tableFormat.getDataBase(), tableFormat.getTableName());
            if (!CollectionUtils.isEmpty(tableColumnsList)) {
                tableFormat.setTableColumnsList(tableColumnsList);
                tableFormat.setInsertSqlColumns(makeInsertSqlColumns(tableFormat.getTableFieldList()));
                tableFormat.setSelectSqlColumns(makeSelectSqlColumns(tableFormat.getDataBase(), tableFormat.getTableFieldList()));
                tableFormat.setTableColumnsByNameMap(tableColumnsList.stream().collect(Collectors.toMap(TableColumnBean::getColumnName, obj -> obj)));
            }
        }

        if (!CollectionUtils.isEmpty(tableFormat.getTableFieldList())) {
            tableFormat.setTableFieldByIdMap(tableFormat.getTableFieldList().stream().collect(Collectors.toMap(TableField::getTableFieldId, obj -> obj)));
            Map<String, TableField> tableFieldByNameMap = tableFormat.getTableFieldList().stream().collect(Collectors.toMap(TableField::getFieldName, obj -> obj));
            tableFormat.setTableFieldByNameMap(new HashMap<>(tableFieldByNameMap.size()));
            tableFieldByNameMap.forEach((x, y) -> tableFormat.getTableFieldByNameMap().put(x.toLowerCase(), y));
        }
        return tableFormat;
    }

    private void objectDataIntoTable(List<QueueData> objectDataList, final Task task) throws DatakitException {
        TableFormat tableFormat = (TableFormat) task.getBObject();
        if (CollectionUtils.isEmpty(tableFormat.getTableColumnsList())) {
            throw new DatakitException("objectDataIntoTable", "表实例字段信息为空!");
        }
        //数据结构映射转换
        makeDataInsertSql(tableFormat, changeObjectDataByTableFormatB(objectDataList, task));
    }

    /**
     * 把内存队列获取标准转换格式数据转换成业务对象B表格式的数据
     *
     * @param objectDataList
     * @param task
     * @return
     * @throws DatakitException
     */
    private List<Map<String, String>> changeObjectDataByTableFormatB(List<QueueData> objectDataList, final Task task) throws DatakitException {
        List<Map<String, String>> dataTableDataList = new ArrayList<>();
        TableFormat tableFormat = (TableFormat) task.getBObject();

        for (QueueData objectDataIterator : objectDataList) {
            //循环文件格式字段，根据字段匹配映射字段值
            Map<String, String> tableFieldValueMap = new HashMap<>();
            for (TableField tableFieldIterator : tableFormat.getTableFieldList()) {
                List<TaskFieldRela> taskFieldRelaList = task.getTaskFieldRelaList().
                        stream().filter(x -> x.getBObjectFieldId().equals(tableFieldIterator.getTableFieldId())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(taskFieldRelaList)) {
                    //可能存在一个表部分字段没有参与关联业务处理,所有匹配不到
                    continue;
                }

                //这里匹配不到不应该报错,应为源端数据加载空字段不会写入map集合中,则会匹配不到
                if (!objectDataIterator.getDataMap().containsKey(taskFieldRelaList.get(0).getTaskFieldRelaId())) {
                    continue;
                }

                tableFieldValueMap.put(tableFieldIterator.getFieldName(), objectDataIterator.getDataMap().get(taskFieldRelaList.get(0).getTaskFieldRelaId()));
            }

            if (!CollectionUtils.isEmpty(tableFieldValueMap)) {
                dataTableDataList.add(tableFieldValueMap);
            } else {
                //一个字段匹配不到就报错
                throw new DatakitException("changeObjectDataByTableFormatB", "根据目标结构匹配字段失败!");
            }
        }

        return dataTableDataList;
    }

    public void matchDataQueueDoBusiness(List<QueueData> objectDataList, final TaskInstance taskInstance) throws DatakitException {
        switch (ObjectTaskEnum.of(taskInstance.getTask().getTaskType())) {
            case OBJECT_TASK_TYPE_DATA_EXCHANGE:
                objectDataIntoTable(objectDataList, taskInstance.getTask());
                break;
            case OBJECT_TASK_TYPE_DATA_COMPARE:
                super.objectDataCompareToMapCache(objectDataList, taskInstance.getTaskId(), taskInstance.getTaskInstanceId());
                break;
            default:
                break;
        }
    }

    @Override
    public void doBusinessInputQueue(final Long taskInstanceId, final Task task) throws DatakitException {
        selectTableDataInputQueue(task);
    }

    private void selectTableDataInputQueue(final Task task) throws DatakitException {
        TableFormat tableFormat = (TableFormat) task.getAObject();

        while (true) {
            String sqlSelect = makeSelectSql(tableFormat.getDataBase(), tableFormat.getTableName(), tableFormat.getSelectSqlColumns(), tableFormat.getTableWhere(), ObjectQueue.getBeginRowNum(datakitConfig.getTableSelectCount()));
            List<Map<String, Object>> datalist = tableDao.selectDataList(tableFormat.getDataBase(), sqlSelect);
            if (CollectionUtils.isEmpty(datalist)) {
                break;
            }

            List<QueueData> dataQueueList = new ArrayList<>();
            for (Map<String, Object> iterator : datalist) {
                dataQueueList.add(doTaskFieldRelaIdMapped(iterator, task));
            }

            ObjectQueue.putDataIntoQueue(dataQueueList);
        }
    }

    private String makeSelectSql(String dataBase, String tableName, String sqlColumns, String sqlWhere, Long beginRow) throws DatakitException {
        return dataBaseList.stream()
                .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType(dataBase))))
                .findFirst()
                .orElseThrow(() -> new DatakitException("makeSelectSql", "can not match DataBaseTypeEnum!"))
                .makeSelectSql(tableName, sqlColumns, sqlWhere, beginRow, datakitConfig.getTableSelectCount());
    }

    private QueueData doTaskFieldRelaIdMapped(Map<String, Object> tableFieldMap, final Task task) {
        TableFormat tableFormatA = (TableFormat) task.getAObject();
        Map<Long, String> objectDataMapped = new HashMap<>();
        Map<String, String> fieldNameValueMap = new HashMap<>();
        List<String> mapKeyList = new ArrayList<>();

        Iterator<Map.Entry<String, Object>> iterator = tableFieldMap.entrySet().iterator();
        Map.Entry<String, Object> entry;
        while (iterator.hasNext()) {
            entry = iterator.next();
            String tableFieldName = entry.getKey();
            String tableFieldValue = entry.getValue().toString();

            if (tableFormatA.getTableFieldByNameMap().containsKey(tableFieldName.toLowerCase())) {
                List<TaskFieldRela> taskFieldRelaList = task.getTaskFieldRelaList().
                        stream().filter(x -> x.getAObjectFieldId().equals(tableFormatA.getTableFieldByNameMap().get(tableFieldName.toLowerCase()).getTableFieldId())).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(taskFieldRelaList)) {
                    objectDataMapped.put(taskFieldRelaList.get(0).getTaskFieldRelaId(), tableFieldValue);
                    fieldNameValueMap.put(tableFieldName.toLowerCase(), tableFieldValue);
                    if (taskFieldRelaList.get(0).getCompareFlag() != null && taskFieldRelaList.get(0).getCompareFlag().equals(COMPARE_ABLE_FLAG_YES)) {
                        mapKeyList.add(tableFieldValue);
                    }
                }
            }
        }

        QueueData dataObject = new QueueData();
        dataObject.setDataMap(objectDataMapped);
        dataObject.setFieldNameValueMap(fieldNameValueMap);
        dataObject.setComparedKey(genMapKey(mapKeyList));
        return dataObject;
    }

    @Override
    public void doBusinessLoadDataToMapCache(final Task task) throws DatakitException {
        selectTableDataToMapCache(task);
    }

    private void selectTableDataToMapCache(final Task task) throws DatakitException {
        TableFormat tableFormat = (TableFormat) task.getBObject();

        while (true) {
            String selectSql = makeSelectSql(tableFormat.getDataBase(), tableFormat.getTableName(), tableFormat.getSelectSqlColumns(), null, ObjectQueue.getBeginRowNum(datakitConfig.getTableSelectCount()));
            List<Map<String, Object>> datalist = tableDao.selectDataList(tableFormat.getDataBase(), selectSql);
            if (CollectionUtils.isEmpty(datalist)) {
                break;
            }

            for (Map<String, Object> iterator : datalist) {
                putDataIntoMap(iterator, task.getTaskFieldRelaList(), tableFormat.getTableFieldByIdMap(), tableFormat.getTableFieldByNameMap());
            }
        }
    }

    private void putDataIntoMap(final Map<String, Object> dataMap, final List<TaskFieldRela> taskFieldRelaList, final Map<Long, TableField> tableFieldByIdMap, final Map<String, TableField> tableFieldByNameMap) throws DatakitException {
        Map<String, Object> filedNameMap = new HashMap<>(dataMap.size());
        dataMap.forEach((x, y) -> filedNameMap.put(x.toLowerCase(), y));

        //生成参与比较的key值
        List<String> mapKeyList = new ArrayList<>();
        for (TaskFieldRela taskFieldRelaIterator : taskFieldRelaList) {
            if (taskFieldRelaIterator.getCompareFlag() != null &&
                    taskFieldRelaIterator.getCompareFlag().equals(COMPARE_ABLE_FLAG_YES) &&
                    tableFieldByIdMap.containsKey(taskFieldRelaIterator.getBObjectFieldId()) &&
                    filedNameMap.containsKey(tableFieldByIdMap.get(taskFieldRelaIterator.getBObjectFieldId()).getFieldName().toLowerCase())) {
                Object object = filedNameMap.get(tableFieldByIdMap.get(taskFieldRelaIterator.getBObjectFieldId()).getFieldName().toLowerCase());
                mapKeyList.add(object.toString());
            }
        }

        if (CollectionUtils.isEmpty(mapKeyList)) {
            throw new DatakitException("selectTableDataToMapCache", "表数据匹配比较key失败!");
        }

        Map<Long, String> dataObjetMap = new HashMap<>();
        filedNameMap.forEach((x, y) -> {
            Long tableFieldId = tableFieldByNameMap.get(x).getTableFieldId();
            dataObjetMap.put(tableFieldId, y.toString());
        });

        ObjectQueue.putDataIntoMap(genMapKey(mapKeyList), dataObjetMap);
    }

    public void doBusinessCreateTable(final Object objectFormat) throws DatakitException {
        //获取对象定义
        TableFormat tableFormat = (TableFormat) objectFormat;
        if (!CollectionUtils.isEmpty(tableFormat.getTableColumnsList())) {
            throw new DatakitException("doBusinessCreateTable", " 该表在数据库已经存在!");
        }

        List<String> fieldsList = new ArrayList<>();
        List<String> keyList = new ArrayList<>();
        for (TableField x : tableFormat.getTableFieldList()) {
            fieldsList.add(makeTableFieldSql(tableFormat.getDataBase(), x.getFieldName(), x.getFieldType(), x.getFieldLength(), x.getNullAble()));
            if (x.getKeyFlag().equals(1)) {
                keyList.add(x.getFieldName());
            }
        }

        String tableFieldSql = fieldsList.stream().collect(Collectors.joining(","));
        String createTableSql = String.format("create table %s ( %s )", tableFormat.getTableName(), tableFieldSql);
        try {
            tableDao.executeTableSql(tableFormat.getDataBase(), createTableSql);
        } catch (Exception e) {
            throw new DatakitException("doBusinessCreateTable", e.getMessage());
        }

        if (!CollectionUtils.isEmpty(keyList)) {
            String keyFieldSql = keyList.stream().collect(Collectors.joining(","));
            String addKeySql = String.format("ALTER TABLE %s ADD CONSTRAINT %s PRIMARY KEY (%s)", tableFormat.getTableName(), tableFormat.getTableName() + "_key", keyFieldSql);
            try {
                tableDao.executeTableSql(tableFormat.getDataBase(), addKeySql);
            } catch (Exception e) {
                throw new DatakitException("doBusinessCreateTable", e.getMessage());
            }
        }
    }

    String makeTableFieldSql(String dataBaseType, String fieldName, Integer fieldType, Integer fieldLength, Integer nullAble) throws DatakitException {
        return dataBaseList.stream()
                .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType(dataBaseType))))
                .findFirst()
                .orElseThrow(() -> new DatakitException("createDatakitObject", "dataBaseType=" + dataBaseType + " can not match dataBase!"))
                .makeTableFieldSql(fieldName, fieldType, fieldLength, nullAble);
    }

    public Map<Long, String> genFieldIdNameMap(final Object objectFormat) {
        Map<Long, String> fieldIdNameMap = new HashMap<>();
        TableFormat tableFormat = (TableFormat) objectFormat;
        tableFormat.getTableFieldList().forEach(x -> fieldIdNameMap.put(x.getTableFieldId(), x.getFieldName()));
        return fieldIdNameMap;
    }

    public void repairData(final Integer repairAction, final Task task, final QueueData queueData) throws DatakitException {
        if (repairAction.equals(REPAIR_DATA_ACTION_INSERT)) {
            List<QueueData> queueDataList = new ArrayList<>();
            queueDataList.add(queueData);
            objectDataIntoTable(queueDataList, task);
        } else if (repairAction.equals(REPAIR_DATA_ACTION_DELETE)) {
            //删除操作
            TableFormat tableFormat = (TableFormat) task.getBObject();
            Map<Long, String> fieldIdNameMap = genFieldIdNameMap(task.getBObject());
            AtomicReference<String> deleteSql = new AtomicReference<>("delete from " + tableFormat.getTableName() + " where 1=1");
            task.getTaskFieldRelaList().forEach(x -> {
                if (x.getCompareFlag() != null && x.getCompareFlag().equals(COMPARE_ABLE_FLAG_YES)) {
                    String fieldName = fieldIdNameMap.get(x.getBObjectFieldId());
                    deleteSql.set(deleteSql + " and " + fieldName + "='" + queueData.getFieldNameValueMap().get(fieldName) + "'");
                }
            });

            tableDao.deleteData(tableFormat.getDataBase(), deleteSql.get());
        }
    }

    public Map<Long, String> queryFieldIDNameMap(Long objectId) {
        List<TableField> tableFieldList = tableDao.selectTableField(objectId);
        if (!CollectionUtils.isEmpty(tableFieldList)) {
            Map<Long, String> fieldNameMap = new HashMap<>(tableFieldList.size());
            tableFieldList.forEach(x -> fieldNameMap.put(x.getTableFieldId(), x.getFieldName()));
            return fieldNameMap;
        }
        return null;
    }

    public String getObjectName(final Object objectFormat) {
        if (!Objects.isNull(objectFormat)) {
            return ((TableFormat) objectFormat).getTableName();
        }
        return null;
    }

    public String queryObjectName(final Long objectId) {
        if (!Objects.isNull(objectId)) {
            TableFormat tableFormat = tableDao.selectTableFormatById(objectId);
            if (!Objects.isNull(tableFormat)) {
                return tableFormat.getTableName();
            }
        }
        return null;
    }

    public Integer getObjectOnLineFlag(final Object objectFormat) {
        if (!Objects.isNull(objectFormat)) {
            return ((TableFormat) objectFormat).getOnLineFlag();
        }
        return null;
    }

    public void followUpObject(final Long taskInstanceId, final Task task) {
        //nothing to do
    }

    public Map<String, Long> getObjectSumFieldRelIdList(final Task task) {
        //nothing to do
        return null;
    }
}
