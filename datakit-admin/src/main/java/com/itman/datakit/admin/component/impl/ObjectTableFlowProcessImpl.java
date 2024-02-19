package com.itman.datakit.admin.component.impl;

import com.itman.datakit.admin.common.constants.DataBaseTypeEnum;
import com.itman.datakit.admin.common.dataqueue.QueueData;
import com.itman.datakit.admin.common.dataroute.DruidConfig;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.enums.DatakitObjectEnum;
import com.itman.datakit.admin.common.enums.ObjectTaskEnum;
import com.itman.datakit.admin.common.enums.TableFieldTypeEnum;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.common.tablemeta.TableMetaProcess;
import com.itman.datakit.admin.component.AbstractObjectProcess;
import com.itman.datakit.admin.component.IObjectProcess;
import com.itman.datakit.admin.dao.ObjectTableFlowDao;
import com.itman.datakit.admin.plugins.ITable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.enums.DatakitObjectEnum.DATAKIT_OBJECT_TABLE_FLOW;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObjectTableFlowProcessImpl extends AbstractObjectProcess implements IObjectProcess {
    private final ObjectTableFlowDao tableFlowDao;
    private final TableMetaProcess tableMetaProcess;
    private final DruidConfig druidConfig;
    private final List<ITable> dataBaseList;

    @Override
    public Boolean objectCurrent(DatakitObjectEnum objectEnum) {
        return objectEnum.equals(DATAKIT_OBJECT_TABLE_FLOW);
    }

    @Override
    public Object makeObjectFormat(Long objectFormatId, Boolean tableMetaFlag) throws DatakitException {
        return makeTableFlowFormat(objectFormatId, tableMetaFlag);
    }

    private TableFlowFormat makeTableFlowFormat(Long tableFlowFormatId, Boolean tableMetaFlag) throws DatakitException {
        TableFlowFormat tableFlowFormat = tableFlowDao.selectTableFlowById(tableFlowFormatId);
        if (Objects.isNull(tableFlowFormat)) {
            throw new DatakitException("makeTableFlowFormat", "表流程对象查询为空");
        }

        tableFlowFormat.setTableFlowNodeList(tableFlowDao.selectTableFlowNodeById(tableFlowFormatId));

        if (!CollectionUtils.isEmpty(tableFlowFormat.getTableFlowNodeList())) {
            for (TableFlowNode iterator : tableFlowFormat.getTableFlowNodeList()) {
                iterator.setTableColumns(tableMetaProcess.getTableColumns(tableFlowFormat.getSourceDataBase(), iterator.getTableName()));
                if (Objects.isNull(iterator.getTableColumns())) {
                    throw new DatakitException("makeTableFlowFormat", "查询表" + iterator.getTableColumns() + "失败");
                }

                List<TableField> tableFieldList = new ArrayList<>(iterator.getTableColumns().size());
                iterator.getTableColumns().forEach(x -> {
                    TableField tableField = new TableField();
                    tableField.setFieldName(x.getColumnName());
                    tableField.setFieldType(getFiledTypeByName(tableFlowFormat.getSourceDataBase(), x.getColumnType()));
                    tableFieldList.add(tableField);
                });

                iterator.setTableFieldList(tableFieldList);
                iterator.setSelectSqlColumns(makeSqlColumns(tableFlowFormat.getSourceDataBase(), iterator.getTableFieldList()));
                iterator.setInsertSqlColumns(tableFieldList.stream().map(TableField::getFieldName).collect(Collectors.joining(",")));
            }
        }

        return tableFlowFormat;
    }

    private String makeSqlColumns(String dataBase, List<TableField> tableFieldList) throws DatakitException {
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
                .orElseThrow(() -> new DatakitException("changeDateToString", "changeDateToString：根据数据库类型匹配数据库失败"))
                .dateToString(columnName);
    }

    @Override
    public void matchDataQueueDoBusiness(List<QueueData> objectDataList, final TaskInstance taskInstance) throws DatakitException {
        switch (ObjectTaskEnum.of(taskInstance.getTask().getTaskType())) {
            case OBJECT_TASK_TYPE_TABLE_FLOW_SYNC:
                objectDataDoFlowSync(objectDataList, taskInstance.getTask());
                break;
            default:
                break;
        }
    }

    private void objectDataDoFlowSync(List<QueueData> objectDataList, final Task task) throws DatakitException {
        TableFlowFormat tableFlowFormat = (TableFlowFormat) task.getBObject();
        if (CollectionUtils.isEmpty(tableFlowFormat.getTableFlowNodeList())) {
            throw new DatakitException("objectDataDoFlowSync", "表流程节点为空!");
        }

        //根据格式转换出B根节点字段名称和值
        makeDataFlowSync(tableFlowFormat, changeObjectDataByTableFormatB(objectDataList, task));
    }

    /**
     * 做流程同步
     *
     * @param tableFlowFormat
     * @param dataRecordList
     */
    private void makeDataFlowSync(TableFlowFormat tableFlowFormat, List<Map<String, String>> dataRecordList) throws DatakitException {
        if (CollectionUtils.isEmpty(dataRecordList)) {
            return;
        }

        for (Map<String, String> iterator : dataRecordList) {
            List<String> insertSqlList = new ArrayList<>();
            List<Map<String, String>> dataMaplist = new ArrayList<>(1);
            dataMaplist.add(iterator);
            recurFlowNode(tableFlowFormat.getSourceDataBase(), tableFlowFormat.getTargetDataBase(), tableFlowFormat.getTableFlowNodeList(), -1L, dataMaplist, insertSqlList);

            if (!CollectionUtils.isEmpty(insertSqlList)) {
                //统一事务流程处理
                insertTableByTransaction(tableFlowFormat.getTargetDataBase(), insertSqlList);
            }
        }
    }

    void recurFlowNode(String sourceDataBase, String targetDataBase, List<TableFlowNode> flowNodeList, Long parentFlowNodeId, List<Map<String, String>> parentDatalist, List<String> insertSqlList) throws DatakitException {
        //匹配parentFlowConfigId下的所有节点配置实例
        List<TableFlowNode> flowNodeListByFilter = flowNodeList.stream().filter(x -> x.getParentFlowNodeId().equals(parentFlowNodeId)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(flowNodeListByFilter)) {
            return;
        }

        //组装每个节点配置实例的sql语句并获取实际记录数
        for (TableFlowNode iterator : flowNodeListByFilter) {
            //根据父节点实例数据记录数个执行
            for (Map dataIterator : parentDatalist) {
                List<Map<String, String>> datalist = new ArrayList<>();
                makeSqlFlowNode(sourceDataBase, targetDataBase, iterator, dataIterator, datalist, insertSqlList);
                //递归下节点数据
                recurFlowNode(sourceDataBase, targetDataBase, flowNodeList, iterator.getFlowNodeId(), datalist, insertSqlList);
            }
        }
    }

    void makeSqlFlowNode(String sourceDataBase, String targetDataBase, TableFlowNode flowNode, Map parentDataMap, List<Map<String, String>> dataRowlist, List<String> insertSqlList) throws DatakitException {
        //组装查询sql
        if (!parentDataMap.containsKey(flowNode.getSelectedFieldName().toUpperCase())) {
            throw new DatakitException("makeSqlFlowNode", "根据字段名称匹配数据失败");
        }

        String fieldValue = String.valueOf(parentDataMap.get(flowNode.getSelectedFieldName().toUpperCase()));
        String selectSql = String.format(" select %s from %s where %s = '%s'", flowNode.getSelectSqlColumns(), flowNode.getTableName(), flowNode.getSelectedFieldName().toUpperCase(), fieldValue);

        //执行查询sql
        List<Map<String, Object>> selectedDataRowList = tableFlowDao.selectDataList(sourceDataBase, selectSql);
        if (CollectionUtils.isEmpty(selectedDataRowList)) {
            //没有数据直接返回
            return;
        }

        selectedDataRowList.forEach(iterator -> {
            Map<String, String> dataMap = new HashMap<>(iterator.size());
            iterator.forEach((x, y) -> dataMap.put(x, y.toString()));
            dataRowlist.add(dataMap);
        });

        //组装insert sql(插入主数据库)
        String insertSql = makeDataInsertSql(targetDataBase, flowNode.getTableName(), flowNode.getInsertSqlColumns(), flowNode.getTableFieldList(), dataRowlist);
        insertSqlList.add(insertSql);
    }

    @Transactional(rollbackFor = DatakitException.class)
    public void insertTableByTransaction(String dataBase, List<String> insertSqlList) throws DatakitException {
        for (String insertSqlIterator : insertSqlList) {
            if (tableFlowDao.insertDataList(dataBase, insertSqlIterator).equals(0)) {
                throw new DatakitException("insertTableByTransaction", "插入表失败");
            }
        }
    }

    public String makeDataInsertSql(String targetDataBase, String tableName, String insertSqlColumns, List<TableField> tableFieldList, List<Map<String, String>> dataRecordList) {
        StringBuffer insertSql = new StringBuffer();
        insertSql.append("insert into ");
        insertSql.append(tableName);
        insertSql.append("(");
        insertSql.append(insertSqlColumns);
        insertSql.append(")values");
        List<String> dataRowList = dataRecordList.stream().map(x -> makeInsertRow(targetDataBase, tableFieldList, x)).collect(Collectors.toList());
        insertSql.append(dataRowList.stream().collect(Collectors.joining(",")));
        return insertSql.toString();
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
                    .orElseThrow(() -> new DatakitException("changeStringToDate", "changeStringToDate:根据数据库类型匹配数据库失败"))
                    .stringToDate(columnValue);
        } catch (DatakitException e) {
            throw new RuntimeException(e);
        }
    }

    private Integer getFiledTypeByName(String dataBase, String filedName) {
        try {
            return dataBaseList.stream()
                    .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType(dataBase))))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("getFiledTypeByName", "getFiledTypeByName:根据数据库类型匹配数据库失败"))
                    .getFiledTypeByName(filedName);
        } catch (DatakitException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 获取内存队标准格式数据转换目标端(B)成业务对象表格式的数据
     *
     * @param objectDataList
     * @param task
     * @return
     * @throws DatakitException
     */
    private List<Map<String, String>> changeObjectDataByTableFormatB(List<QueueData> objectDataList, final Task task) throws DatakitException {
        List<Map<String, String>> dataTableDataList = new ArrayList<>();
        TableFlowFormat tableFlowFormat = (TableFlowFormat) task.getBObject();

        for (QueueData objectDataIterator : objectDataList) {
            //循环获取行数据，匹配数据量对应的根节点
            Map<String, String> tableFieldValueMap = new HashMap<>();

            getRootNodeFieldName(objectDataIterator.getDataMap(), tableFieldValueMap, tableFlowFormat.getTableFlowNodeList(), task.getTaskFieldRelaList());

            if (!CollectionUtils.isEmpty(tableFieldValueMap)) {
                dataTableDataList.add(tableFieldValueMap);
            }
        }

        return dataTableDataList;
    }

    private void getRootNodeFieldName(Map<Long, String> dataMap, Map<String, String> tableFieldValueMap, final List<TableFlowNode> tableFlowNodeList, final List<TaskFieldRela> taskFieldRelaList) throws DatakitException {
        for (TableFlowNode tableFlowNodeIterator : tableFlowNodeList) {
            //寻找流程更节点进对应字段
            if (tableFlowNodeIterator.getParentFlowNodeId().equals(-1L)) {
                List<TaskFieldRela> taskFieldRelaListFilter = taskFieldRelaList.
                        stream().filter(x -> x.getBObjectFieldId().equals(tableFlowNodeIterator.getFlowNodeId())).collect(Collectors.toList());

                if (CollectionUtils.isEmpty(taskFieldRelaListFilter)) {
                    throw new DatakitException("getRootNodeFieldName001", "根据根节点标识寻找不到关联ID!");
                }

                if (!dataMap.containsKey(taskFieldRelaListFilter.get(0).getTaskFieldRelaId())) {
                    throw new DatakitException("getRootNodeFieldName002", "根据关联ID找不到数据字段!");
                }

                tableFieldValueMap.put(tableFlowNodeIterator.getSelectedFieldName(), dataMap.get(taskFieldRelaListFilter.get(0).getTaskFieldRelaId()));

                //根节点只会有一个
                return;
            }
        }
    }


    @Override
    public void doBusinessInputQueue(final Long taskInstanceId, final Task task) {
        //todo
    }


    @Override
    public void doBusinessLoadDataToMapCache(final Task task) {
        //todo
    }

    @Override
    public void doBusinessCreateTable(Object objectFormat) {
        //todo
    }

    @Override
    public Map<Long, String> genFieldIdNameMap(final Object objectFormat) {
        Map<Long, String> fieldIdNameMap = new HashMap<>();
        TableFormat tableFormat = (TableFormat) objectFormat;
        tableFormat.getTableFieldList().forEach(x -> fieldIdNameMap.put(x.getTableFieldId(), x.getFieldName()));
        return fieldIdNameMap;
    }

    @Override
    public void repairData(final Integer repairAction, final Task task, final QueueData queueData) {
        //nothing to do
    }

    @Override
    public Map<Long, String> queryFieldIDNameMap(Long objectId) {
        List<TableFlowNode> tableFlowNodeList = tableFlowDao.selectTableFlowNodeById(objectId);
        if (!CollectionUtils.isEmpty(tableFlowNodeList)) {
            Map<Long, String> fieldNameMap = new HashMap<>(1);
            tableFlowNodeList.forEach(x -> {
                if (x.getParentFlowNodeId().equals(-1L)) {
                    fieldNameMap.put(x.getFlowNodeId(), x.getSelectedFieldName());
                }
            });
            return fieldNameMap;
        }
        return null;
    }

    @Override
    public String getObjectName(final Object objectFormat) {
        if (!Objects.isNull(objectFormat)) {
            return ((TableFlowFormat) objectFormat).getFlowName();
        }
        return null;
    }

    @Override
    public String queryObjectName(final Long objectId) {
        if (!Objects.isNull(objectId)) {
            TableFlowFormat tableFlowFormat = tableFlowDao.selectTableFlowById(objectId);
            if (!Objects.isNull(tableFlowFormat)) {
                return tableFlowFormat.getFlowName();
            }
        }
        return null;
    }

    @Override
    public Integer getObjectOnLineFlag(final Object objectFormat) {
        if (!Objects.isNull(objectFormat)) {
            return ((TableFlowFormat) objectFormat).getOnLineFlag();
        }
        return null;
    }

    @Override
    public void followUpObject(final Long taskInstanceId, final Task task) {
        //nothing to do
    }

    @Override
    public Map<String, Long> getObjectSumFieldRelIdList(final Task task) {
        //nothing to do
        return null;
    }
}
