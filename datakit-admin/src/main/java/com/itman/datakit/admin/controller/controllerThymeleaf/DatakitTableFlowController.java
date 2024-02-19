package com.itman.datakit.admin.controller.controllerThymeleaf;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.itman.datakit.admin.common.dataroute.DruidConfig;
import com.itman.datakit.admin.common.dto.TableFlowDTO;
import com.itman.datakit.admin.common.dto.TableFlowRowsDTO;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.component.CommonProcess;
import com.itman.datakit.admin.service.TableFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.util.ChangeNameUtil.changeOnLineFlagName;
import static com.itman.datakit.admin.common.util.ResultModelUtil.resultModel;

@Slf4j
@Controller
@RequestMapping("/datakit")
@RequiredArgsConstructor
public class DatakitTableFlowController {
    private final TableFlowService tableFlowService;
    private final DruidConfig druidConfig;
    private final CommonProcess commonProcess;

    /**
     * 表流程动作：1删除、2上线、3下线、4复制
     */
    private static final Integer TABLE_FLOW_ROW_ACTION_DELETE = 1;
    private static final Integer TABLE_FLOW_ROW_ACTION_ONLINE = 2;
    private static final Integer TABLE_FLOW_ROW_ACTION_OFFLINE = 3;
    private static final Integer TABLE_FLOW_ROW_ACTION_COPY = 4;

    Map<String, String> getDataBaseMap() {
        Map<String, String> dataBaseMap = new HashMap<>(druidConfig.getDruid().size());
        druidConfig.getDruid().forEach(x -> dataBaseMap.put(x.getDataBase(), x.getDataBase() + " 【" + x.getUrl() + "】"));
        return dataBaseMap;
    }


    /**
     * 表流程记录操作
     * http://127.0.0.1:9193/datakit/table/flow/rows/operate?tableFlowFormatId=11&onLineFlag=1&&action=1
     */
    @GetMapping(path = "/table/flow/rows/operate")
    public String tableFlowRowsOperate(Model model,
                                       @RequestParam("flowFormatId") Long flowFormatId,
                                       @RequestParam(required = false, name = "onLineFlag") Integer onLineFlag,
                                       @RequestParam(required = false, name = "action") Integer action) {
        try {
            if (action.equals(TABLE_FLOW_ROW_ACTION_DELETE)) {
                //删除记录
                tableFlowService.deleteTableFlow(flowFormatId);
            } else if (action.equals(TABLE_FLOW_ROW_ACTION_ONLINE)) {
                //上线
                tableFlowService.updateTableFlowOnLineFlagById(flowFormatId, ON_LINE_FLAG_ON_LINE);
            } else if (action.equals(TABLE_FLOW_ROW_ACTION_OFFLINE)) {
                //下线：先校验，校验关联的任务配置表是否都是下线状态
                //taskService.checkTaskOnLineByObjectId(DATAKIT_OBJECT_TABLE.getValue(), tableFlowFormatId);

                tableFlowService.updateTableFlowOnLineFlagById(flowFormatId, ON_LINE_FLAG_OFF_LINE);
            } else if (action.equals(TABLE_FLOW_ROW_ACTION_COPY)) {
                //todo
                throw new DatakitException("tableFlowRowsOperate", "暂不实现复制功能!");
            }
        } catch (DatakitException e) {
            log.error("tableFlowRowsOperate error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }

        TableFlowRowsDTO tableFlowRowsDTO = new TableFlowRowsDTO();
        tableFlowRowsDTO.setOnLineFlag(onLineFlag);
        return tableFlowRows(model, 1, tableFlowRowsDTO);
    }

    public String tableFlowRows(Model model, Integer pageNum, TableFlowRowsDTO tableFlowRowsDTO) {
        tableFlowRowsDTO = Optional.ofNullable(tableFlowRowsDTO).orElse(new TableFlowRowsDTO());
        tableFlowRowsDTO.setOnLineFlag(Optional.ofNullable(tableFlowRowsDTO.getOnLineFlag()).orElse(-1));
        PageInfo<TableFlowFormat> pageInfo = tableFlowService.selectTableFlowByOnLineFlagPage(pageNum, WEB_PAGE_SIZE, tableFlowRowsDTO.getOnLineFlag());
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute("pageLeftSize", (pageInfo.getPageSize() - pageInfo.getSize()));
        model.addAttribute("tableFlowRowsDTO", tableFlowRowsDTO);
        return "tableFlow/tableFlowRowsPage";
    }

    /**
     * 查询表流程记录
     * http://127.0.0.1:9193/datakit/tableFlowRows/query?pageNum=1&onLineFlag=1
     */
    @GetMapping(path = "/tableFlowRows/query")
    public String tableFlowRowsQuery(Model model,
                                     @RequestParam(required = false, name = "pageNum") Integer pageNum,
                                     @RequestParam(required = false, name = "onLineFlag") Integer onLineFlag) {
        TableFlowRowsDTO tableFlowRowsDTO = new TableFlowRowsDTO();
        tableFlowRowsDTO.setOnLineFlag(onLineFlag);
        return tableFlowRows(model, Optional.ofNullable(pageNum).orElse(1), tableFlowRowsDTO);
    }


    /**
     * 查询表流程信息
     * http://127.0.0.1:9193/datakit/table/flow/query?flowFormatId=1&pageNum=1&viewFlag=2
     */
    @GetMapping(path = "/table/flow/query")
    public String tableFlowQuery(Model model,
                                 @RequestParam("flowFormatId") Long flowFormatId,
                                 @RequestParam("pageNum") Integer pageNum,
                                 @RequestParam(required = false, name = "viewFlag") Integer viewFlag) {
        TableFlowDTO tableFlowDTO = new TableFlowDTO();
        tableFlowDTO.setFlowFormatId(flowFormatId);
        return tableFlow(model, tableFlowDTO, viewFlag);
    }

    /**
     * 表流程处理
     *
     * @param model
     * @param tableFlowDTO
     * @return
     */
    public String tableFlow(Model model, TableFlowDTO tableFlowDTO, Integer viewFlag) {
        TableFlowFormat tableFlowFormat = null;

        try {
            viewFlag = Optional.ofNullable(viewFlag).orElse(VIEW_FLAG_SHOW);
            tableFlowFormat = tableFlowService.selectTableFlowById(tableFlowDTO.getFlowFormatId());
            tableFlowFormat = Optional.ofNullable(tableFlowFormat).orElse(new TableFlowFormat());
            if (tableFlowFormat.getOnLineFlag() != null) {
                tableFlowFormat.setOnLineFlagName(changeOnLineFlagName(tableFlowFormat.getOnLineFlag()));
            }
            tableFlowFormat.setFlowFormatId(Optional.ofNullable(tableFlowFormat.getFlowFormatId()).orElse(-1L));

            if (!CollectionUtils.isEmpty(tableFlowFormat.getTableFlowNodeList())) {
                tableFlowFormat.setTableFlowNodeList(getTableFlowTree(tableFlowFormat.getSourceDataBase(), tableFlowFormat.getTableFlowNodeList()));
            } else {
                tableFlowFormat.setTableFlowNodeList(new ArrayList<>());
            }

            tableFlowFormat.setTableFlowFormatJason(JSON.toJSONString(tableFlowFormat));
        } catch (DatakitException e) {
            log.error("tableFlow error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }

        model.addAttribute("viewFlag", viewFlag);
        model.addAttribute("tableFlowFormat", tableFlowFormat);
        model.addAttribute("dataBaseMap", getDataBaseMap());
        return "tableFlow/tableFlowPage";
    }

    public List<TableFlowNode> getTableFlowTree(String sourceDataBase, List<TableFlowNode> tableFlowNodeList) throws DatakitException {
        Map<Long, TableFlowNode> nodeMap = new HashMap<>();
        tableFlowNodeList.forEach(x -> nodeMap.put(x.getFlowNodeId(), x));

        List<TableFlowNode> tree = new ArrayList<>();
        for (TableFlowNode node : tableFlowNodeList) {
            if (node.getParentFlowNodeId() == null || node.getParentFlowNodeId().equals(-1L)) {
                tree.add(node);
            } else {
                TableFlowNode parent = nodeMap.get(node.getParentFlowNodeId());
                if (parent != null) {
                    node.setFieldNameList(tableFlowService.getDatabaseTableFieldNameList(sourceDataBase, node.getTableName()));
                    if (CollectionUtils.isEmpty(parent.getFieldNameList())) {
                        parent.setFieldNameList(tableFlowService.getDatabaseTableFieldNameList(sourceDataBase, parent.getTableName()));
                    }
                    node.setParentFieldNameList(parent.getFieldNameList());
                    parent.getChildren().add(node);
                }
            }
        }
        return tree;
    }

    /**
     * 表页面操作：0无动作、1保存、2流程稽核
     */
    private static final int TABLE_FLOW_ACTION_NULL = 0;
    private static final int TABLE_FLOW_ACTION_SAVE = 1;
    private static final int TABLE_FLOW_ACTION_CHECK = 2;

    /**
     * 表流程创建、更新
     * http://127.0.0.1:9193/datakit/table/flow/form/operate
     */
    @PostMapping(value = "/table/flow/form/operate")
    public String tableFlowFormOperate(Model model,
                                       TableFlowFormat tableFlowFormat) {
        tableFlowFormat.setTableFlowAction(Optional.ofNullable(tableFlowFormat.getTableFlowAction()).orElse(TABLE_FLOW_ACTION_NULL));
        if (tableFlowFormat.getTableFlowFormatJason() != null) {
            tableFlowFormat.setTableFlowNodeList(JSON.parseObject(tableFlowFormat.getTableFlowFormatJason(), TableFlowFormat.class).getTableFlowNodeList());
        }

        try {
            switch (tableFlowFormat.getTableFlowAction()) {
                case TABLE_FLOW_ACTION_NULL:
                    tableFlowNodePostOperateAction(tableFlowFormat);
                    break;
                case TABLE_FLOW_ACTION_SAVE:
                    saveTableFlow(model, tableFlowFormat);
                    break;
                case TABLE_FLOW_ACTION_CHECK:
                    checkTableFlow(model, tableFlowFormat);
                    break;
                default:
                    break;
            }

            tableFlowFormat.setTableFlowAction(TABLE_FLOW_ACTION_NULL);
        } catch (DatakitException e) {
            log.error("tableFlowFormOperate error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }

        tableFlowFormat.setTableFlowFormatJason(JSON.toJSONString(tableFlowFormat));
        model.addAttribute("viewFlag", 2);
        model.addAttribute("tableFlowFormat", tableFlowFormat);
        model.addAttribute("dataBaseMap", getDataBaseMap());
        return "tableFlow/tableFlowPage";
    }

    /**
     * 0无动作、1新增、2删除、3编辑
     */
    private static final Integer TABLE_FLOW_NODE_ACTION_NULL = 0;
    private static final Integer TABLE_FLOW_NODE_ACTION_ADD = 1;
    private static final Integer TABLE_FLOW_NODE_ACTION_DELETE = 2;
    private static final Integer TABLE_FLOW_NODE_ACTION_EDIT = 3;

    public void tableFlowNodePostOperateAction(TableFlowFormat tableFlowFormat) throws DatakitException {
        tableFlowFormat.setTableFlowNodeAction(Optional.ofNullable(tableFlowFormat.getTableFlowNodeAction()).orElse(TABLE_FLOW_NODE_ACTION_NULL));

        if (tableFlowFormat.getTableFlowNodeAction().equals(TABLE_FLOW_NODE_ACTION_ADD)) {
            tableFlowNodePostOperateActionAdd(tableFlowFormat);
        } else if (tableFlowFormat.getTableFlowNodeAction().equals(TABLE_FLOW_NODE_ACTION_DELETE)) {
            tableFlowNodePostOperateActionDelete(tableFlowFormat);
        } else if (tableFlowFormat.getTableFlowNodeAction().equals(TABLE_FLOW_NODE_ACTION_EDIT)) {
            tableFlowNodePostOperateActionEdit(tableFlowFormat);
        }

        tableFlowFormat.setTableFlowNodeAction(TABLE_FLOW_NODE_ACTION_NULL);
        tableFlowFormat.setTableFlowNodeTableName(null);
        tableFlowFormat.setTableFlowNodeId(-1L);
    }

    public void tableFlowNodePostOperateActionAdd(TableFlowFormat tableFlowFormat) throws DatakitException {
        if (CollectionUtils.isEmpty(tableFlowFormat.getTableFlowNodeList())) {
            TableFlowNode childFlowNode = new TableFlowNode();
            childFlowNode.setFlowFormatId(tableFlowFormat.getFlowFormatId());
            childFlowNode.setTableName(tableFlowFormat.getTableFlowNodeTableName());
            childFlowNode.setFieldNameList(tableFlowService.getDatabaseTableFieldNameList(tableFlowFormat.getSourceDataBase(), tableFlowFormat.getTableFlowNodeTableName()));
            childFlowNode.setParentFieldNameList(childFlowNode.getFieldNameList());
            childFlowNode.setFlowNodeId(commonProcess.querySequence());
            childFlowNode.setParentFlowNodeId(-1L);
            tableFlowFormat.getTableFlowNodeList().add(childFlowNode);
        } else {
            addTableFlowNode(tableFlowFormat.getTableFlowNodeList().get(0), tableFlowFormat);
        }
    }

    public void tableFlowNodePostOperateActionDelete(TableFlowFormat tableFlowFormat) {
        for (TableFlowNode iterator : tableFlowFormat.getTableFlowNodeList()) {
            if (iterator.getFlowNodeId().equals(tableFlowFormat.getTableFlowNodeId())) {
                tableFlowFormat.getTableFlowNodeList().remove(iterator);
                break;
            } else {
                delTableFlowNode(iterator, tableFlowFormat.getTableFlowNodeId());
            }
        }
    }

    public void tableFlowNodePostOperateActionEdit(TableFlowFormat tableFlowFormat) throws DatakitException {
        for (TableFlowNode iterator : tableFlowFormat.getTableFlowNodeList()) {
            if (iterator.getFlowNodeId().equals(tableFlowFormat.getTableFlowNodeId())) {
                iterator.setFieldNameList(tableFlowService.getDatabaseTableFieldNameList(tableFlowFormat.getSourceDataBase(), tableFlowFormat.getTableFlowNodeTableName()));
                iterator.setTableName(tableFlowFormat.getTableFlowNodeTableName());
                iterator.setParentFieldNameList(iterator.getFieldNameList());
                break;
            } else {
                editTableFlowNode(iterator, tableFlowFormat);
            }
        }
    }

    void addTableFlowNode(TableFlowNode tableFlowNode, final TableFlowFormat tableFlowFormat) throws DatakitException {
        if (tableFlowNode == null || tableFlowNode.getFlowNodeId() == null) {
            return;
        }

        if (tableFlowNode.getFlowNodeId().equals(tableFlowFormat.getTableFlowNodeId())) {
            TableFlowNode childFlowNode = new TableFlowNode();
            childFlowNode.setFlowFormatId(tableFlowFormat.getFlowFormatId());
            childFlowNode.setTableName(tableFlowFormat.getTableFlowNodeTableName());
            childFlowNode.setFieldNameList(tableFlowService.getDatabaseTableFieldNameList(tableFlowFormat.getSourceDataBase(), tableFlowFormat.getTableFlowNodeTableName()));
            childFlowNode.setParentFieldNameList(childFlowNode.getFieldNameList());
            childFlowNode.setFlowNodeId(commonProcess.querySequence());
            childFlowNode.setParentFlowNodeId(tableFlowFormat.getTableFlowNodeId());
            tableFlowNode.getChildren().add(childFlowNode);
            return;
        } else {
            tableFlowNode.setFieldNameList(tableFlowService.getDatabaseTableFieldNameList(tableFlowFormat.getSourceDataBase(), tableFlowNode.getTableName()));
            tableFlowNode.setParentFieldNameList(tableFlowNode.getFieldNameList());
        }

        for (TableFlowNode iterator : tableFlowNode.getChildren()) {
            addTableFlowNode(iterator, tableFlowFormat);
        }
    }

    void delTableFlowNode(TableFlowNode tableFlowNode, Long fLowNodeId) {
        if (tableFlowNode == null || tableFlowNode.getFlowNodeId() == null) {
            return;
        }

        for (TableFlowNode iterator : tableFlowNode.getChildren()) {
            if (iterator.getFlowNodeId().equals(fLowNodeId)) {
                tableFlowNode.getChildren().remove(iterator);
                return;
            }

            delTableFlowNode(iterator, fLowNodeId);
        }
    }

    void editTableFlowNode(TableFlowNode tableFlowNode, TableFlowFormat tableFlowFormat) throws DatakitException {
        if (tableFlowNode == null || tableFlowNode.getFlowNodeId() == null) {
            return;
        }

        for (TableFlowNode iterator : tableFlowNode.getChildren()) {
            if (iterator.getFlowNodeId().equals(tableFlowFormat.getTableFlowNodeId())) {
                iterator.setFieldNameList(tableFlowService.getDatabaseTableFieldNameList(tableFlowFormat.getSourceDataBase(), tableFlowFormat.getTableFlowNodeTableName()));
                iterator.setTableName(tableFlowFormat.getTableFlowNodeTableName());
                iterator.setParentFieldNameList(iterator.getFieldNameList());
                return;
            }

            editTableFlowNode(iterator, tableFlowFormat);
        }
    }

    void saveTableFlow(Model model, TableFlowFormat tableFlowFormat) {
        try {
            //校验
            checkTableFlowNode(tableFlowFormat.getSourceDataBase(), tableFlowFormat.getTargetDataBase(), tableFlowFormat.getTableFlowNodeList());

            if (!Objects.isNull(tableFlowFormat.getFlowFormatId()) && tableFlowFormat.getFlowFormatId().equals(-1L)) {
                //新增
                tableFlowService.createTableFlow(tableFlowFormat);
            } else {
                //修改
                tableFlowService.updateTableFlow(tableFlowFormat);
            }

            String resultUrl = "/datakit/table/flow/query?flowFormatId=" + tableFlowFormat.getFlowFormatId() + "&pageNum=1&viewFlag=2";
            resultModel(model, WARNING, "保存成功!", resultUrl);
        } catch (DatakitException e) {
            log.error("saveTableFlow error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }
    }

    void checkTableFlow(Model model, TableFlowFormat tableFlowFormat) {
        try {
            //校验
            checkTableFlowNode(tableFlowFormat.getSourceDataBase(), tableFlowFormat.getTargetDataBase(), tableFlowFormat.getTableFlowNodeList());

            String resultUrl = "/datakit/table/flow/query?flowFormatId=" + tableFlowFormat.getFlowFormatId() + "&pageNum=1&viewFlag=2";
            resultModel(model, WARNING, "校验成功!", resultUrl);
        } catch (DatakitException e) {
            log.error("checkTableFlow error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }
    }


    void checkTableFlowNode(String sourceDataBase, String targetDataBase, List<TableFlowNode> tableFlowNodeList) throws DatakitException {
        if (CollectionUtils.isEmpty(tableFlowNodeList)) {
            return;
        }

        for (TableFlowNode iterator : tableFlowNodeList) {
            checkTableFlowNode(iterator, sourceDataBase, targetDataBase);
        }
    }

    void checkTableFlowNode(TableFlowNode iterator, String sourceDataBase, String targetDataBase) throws DatakitException {
        if (!iterator.getParentFlowNodeId().equals(-1L) && StringUtils.isEmpty(iterator.getSelectedFieldName())) {
            throw new DatakitException("checkTableFlowNode001", "存在节点字段为空!");
        }
        if (!iterator.getParentFlowNodeId().equals(-1L) && StringUtils.isEmpty(iterator.getSelectedParentFieldName())) {
            throw new DatakitException("checkTableFlowNode002", "存在节点父字段为空!");
        }

        //todo 校验源端和目标端表和字段是否一致
        List<String> sourceTableFields = tableFlowService.getDatabaseTableFieldNameList(sourceDataBase, iterator.getTableName());
        if (CollectionUtils.isEmpty(sourceTableFields)) {
            throw new DatakitException("checkTableFlowNode003", "源端表" + iterator.getTableName() + "不存在!");
        }

        List<String> targetTableFields = tableFlowService.getDatabaseTableFieldNameList(targetDataBase, iterator.getTableName());
        if (CollectionUtils.isEmpty(targetTableFields)) {
            throw new DatakitException("checkTableFlowNode004", "目标端表" + iterator.getTableName() + "不存在!");
        }

        for (String sourceField : sourceTableFields) {
            Boolean findFlag = false;
            for (String targetField : targetTableFields) {
                if (sourceField.equalsIgnoreCase(targetField)) {
                    findFlag = true;
                    break;
                }
            }

            if (!findFlag) {
                throw new DatakitException("checkTableFlowNode005", "源表" + iterator.getTableName() + "字段" + sourceField + "匹配失败");
            }
        }

        checkTableFlowNode(sourceDataBase, targetDataBase, iterator.getChildren());
    }

    /**
     * 任务配置对象记录rows选择,用在配置任务选择表对象
     * 这个时候这个页面提供给任务配置的时候选择表对象
     * http://127.0.0.1:9193/datakit/tableRows/select?pageNum=1&onLineFlag=1&flag=A
     */
    @GetMapping(path = "/tableFlowRows/select")
    public String tableRowsSelectForTask(Model model,
                                         @RequestParam("pageNum") Integer pageNum,
                                         @RequestParam(required = false, name = "onLineFlag") Integer onLineFlag,
                                         @RequestParam(required = false, name = "taskSelectFlag") String taskSelectFlag) {
        TableFlowRowsDTO tableFlowRowsDTO = new TableFlowRowsDTO();
        tableFlowRowsDTO.setOnLineFlag(onLineFlag);
        tableFlowRowsDTO.setTaskSelectFlag(taskSelectFlag);
        return tableFlowRows(model, pageNum, tableFlowRowsDTO);
    }
}
