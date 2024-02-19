package com.itman.datakit.admin.controller.controllerThymeleaf;

import com.github.pagehelper.PageInfo;
import com.itman.datakit.admin.common.dataroute.DruidConfig;
import com.itman.datakit.admin.common.dto.*;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.entity.TableTransform;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.service.TableService;
import com.itman.datakit.admin.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.enums.DatakitObjectEnum.DATAKIT_OBJECT_TABLE;
import static com.itman.datakit.admin.common.util.ChangeNameUtil.*;
import static com.itman.datakit.admin.common.util.ResultModelUtil.resultModel;

@Slf4j
@Controller
@RequestMapping("/datakit")
@RequiredArgsConstructor
public class DatakitTableController {
    private final TableService tableService;
    private final DruidConfig druidConfig;
    private final TaskService taskService;

    /**
     * 表页面操作：0无动作、1保存、2校验、3转表、4同步
     */
    private static final int TABLE_ACTION_NULL = 0;
    private static final int TABLE_ACTION_SAVE = 1;
    private static final int TABLE_ACTION_CHECK = 2;
    private static final int TABLE_ACTION_CREATE = 3;
    private static final int TABLE_ACTION_SYNC = 4;
    /**
     * 0无动作、1新增、2删除
     */
    private static final Integer TABLE_FIELD_ACTION_NULL = 0;
    private static final Integer TABLE_FIELD_ACTION_ADD = 1;
    private static final Integer TABLE_FIELD_ACTION_DELETE = 2;

    /**
     * 表记录动作：1删除、2上线、3下线、4复制
     */
    private static final Integer TABLE_ROW_ACTION_DELETE = 1;
    private static final Integer TABLE_ROW_ACTION_ONLINE = 2;
    private static final Integer TABLE_ROW_ACTION_OFFLINE = 3;
    private static final Integer TABLE_ROW_ACTION_COPY = 4;

    /**
     * 查询表对象记录
     * http://127.0.0.1:9193/datakit/tableRows/query?pageNum=1&onLineFlag=1
     */
    @GetMapping(path = "/tableRows/query")
    public String tableRowsQuery(Model model,
                                 @RequestParam(required = false, name = "pageNum") Integer pageNum,
                                 @RequestParam(required = false, name = "onLineFlag") Integer onLineFlag) {
        TableRowsDTO tableListQueryDTO = new TableRowsDTO();
        tableListQueryDTO.setOnLineFlag(onLineFlag);
        return tableRows(model, Optional.ofNullable(pageNum).orElse(1), tableListQueryDTO);
    }

    /**
     * 表对象记录操作
     * http://127.0.0.1:9193/datakit/table/rows/operate?tableFormatId=11&onLineFlag=1&&action=1
     */
    @GetMapping(path = "/table/rows/operate")
    public String tableRowsOperate(Model model,
                                   @RequestParam("tableFormatId") Long tableFormatId,
                                   @RequestParam(required = false, name = "onLineFlag") Integer onLineFlag,
                                   @RequestParam(required = false, name = "action") Integer action) {
        try {
            if (action.equals(TABLE_ROW_ACTION_DELETE)) {
                //删除记录
                tableService.deleteTableInstance(tableFormatId);
            } else if (action.equals(TABLE_ROW_ACTION_ONLINE)) {
                //上线
                tableService.updateTableFormatOnLineFlagById(tableFormatId, ON_LINE_FLAG_ON_LINE);
            } else if (action.equals(TABLE_ROW_ACTION_OFFLINE)) {
                //下线：先校验，校验关联的任务配置表是否都是下线状态
                taskService.checkTaskOnLineByObjectId(DATAKIT_OBJECT_TABLE.getValue(), tableFormatId);

                tableService.updateTableFormatOnLineFlagById(tableFormatId, ON_LINE_FLAG_OFF_LINE);
            } else if (action.equals(TABLE_ROW_ACTION_COPY)) {
                //复制记录
                tableService.copyTableInstance(tableFormatId);
            }
        } catch (DatakitException e) {
            log.error("tableRowsOperate error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }

        TableRowsDTO tableRowsDTO = new TableRowsDTO();
        tableRowsDTO.setOnLineFlag(onLineFlag);
        return tableRows(model, 1, tableRowsDTO);
    }


    /**
     * 表记录查询处理
     *
     * @param model
     * @param pageNum
     * @param tableRowsDTO
     * @return
     */
    public String tableRows(Model model, Integer pageNum, TableRowsDTO tableRowsDTO) {
        tableRowsDTO = Optional.ofNullable(tableRowsDTO).orElse(new TableRowsDTO());
        tableRowsDTO.setOnLineFlag(Optional.ofNullable(tableRowsDTO.getOnLineFlag()).orElse(-1));
        PageInfo<TableFormat> pageInfo = tableService.selectTableFormatByOnLineFlagPage(pageNum, WEB_PAGE_SIZE, tableRowsDTO.getOnLineFlag());
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute("pageLeftSize", (pageInfo.getPageSize() - pageInfo.getSize()));
        model.addAttribute("tableRowsDTO", tableRowsDTO);
        return "table/tableRowsPage";
    }


    /**
     * 查询表实例信息
     * http://127.0.0.1:9193/datakit/table/query?tableFormatId=1&pageNum=1&viewFlag=2
     */
    @GetMapping(path = "/table/query")
    public String tableQuery(Model model,
                             @RequestParam("tableFormatId") Long tableFormatId,
                             @RequestParam("pageNum") Integer pageNum,
                             @RequestParam(required = false, name = "viewFlag") Integer viewFlag,
                             @RequestParam(required = false, name = "sourcePage") String sourcePage) {
        TableDTO tableDTO = new TableDTO();
        tableDTO.setTableFormatId(tableFormatId);
        return table(model, tableDTO, viewFlag, sourcePage);
    }

    /**
     * 表实例处理
     *
     * @param model
     * @param tableDTO
     * @return
     */
    public String table(Model model, TableDTO tableDTO, Integer viewFlag, String sourcePage) {
        viewFlag = Optional.ofNullable(viewFlag).orElse(VIEW_FLAG_SHOW);
        TableFormat tableFormat = tableService.selectTableFormatById(tableDTO.getTableFormatId());
        tableFormat = Optional.ofNullable(tableFormat).orElse(new TableFormat());
        if (tableFormat.getOnLineFlag() != null) {
            tableFormat.setOnLineFlagName(changeOnLineFlagName(tableFormat.getOnLineFlag()));
        }
        tableFormat.setTableFormatId(Optional.ofNullable(tableFormat.getTableFormatId()).orElse(-1L));
        PageInfo<TableField> pageInfo = tableService.selectTableFieldPage(1, 100, tableDTO.getTableFormatId());
        tableFormat.setTableFieldList(pageInfo.getList());
        tableFormat.setTableFieldList(Optional.ofNullable(tableFormat.getTableFieldList()).orElse(new ArrayList<>()));

        model.addAttribute("sourcePage", sourcePage);
        tablePageAddAttribute(model, tableFormat, viewFlag);
        return "table/tablePage";
    }

    Map<String, String> getDataBaseMap() {
        Map<String, String> dataBaseMap = new HashMap<>(druidConfig.getDruid().size());
        druidConfig.getDruid().forEach(x -> dataBaseMap.put(x.getDataBase(), x.getDataBase() + " 【" + x.getUrl() + "】"));
        return dataBaseMap;
    }

    /**
     * 表页面调整：新增或删除特殊行、新增或删除行字段等
     *
     * @param tableFormat
     * @return
     */
    public void tablePostOperateFieldAction(TableFormat tableFormat) {
        tableFormat.setTableFieldAction(Optional.ofNullable(tableFormat.getTableFieldAction()).orElse(TABLE_FIELD_ACTION_NULL));

        if (tableFormat.getTableFieldAction().equals(TABLE_FIELD_ACTION_ADD)) {
            //表字段新增
            tableFormat.setTableFieldList(Optional.ofNullable(tableFormat.getTableFieldList()).orElse(new ArrayList<>()));
            TableField tableField = new TableField();
            tableField.setTableFieldId(-1L);
            tableFormat.getTableFieldList().add(tableField);
        } else if (tableFormat.getTableFieldAction().equals(TABLE_FIELD_ACTION_DELETE)) {
            tableFormat.getTableFieldList().remove(tableFormat.getTableFieldList().get(tableFormat.getTableFieldIndex()));
        }

        tableFormat.setTableFieldAction(TABLE_FIELD_ACTION_NULL);
        tableFormat.setTableFieldIndex(0);
    }

    /**
     * 表对象创建、更新
     * http://127.0.0.1:9193/datakit/table/form/operate
     */
    @GetMapping(value = "/table/form/operate")
    public String tableFormOperate(Model model,
                                   TableFormat tableFormat) {
        if (!CollectionUtils.isEmpty(tableFormat.getTableFieldList())) {
            tableFormat.getTableFieldList().forEach(x -> {
                if (Objects.isNull(x.getNullAble())) x.setNullAble(1);
                if (Objects.isNull(x.getKeyFlag())) x.setKeyFlag(0);
            });
        }

        tableFormat.setTableAction(Optional.ofNullable(tableFormat.getTableAction()).orElse(TABLE_ACTION_NULL));
        switch (tableFormat.getTableAction()) {
            case TABLE_ACTION_NULL:
                tablePostOperateFieldAction(tableFormat);
                break;
            case TABLE_ACTION_SAVE:
                saveTableInstance(model, tableFormat);
                break;
            case TABLE_ACTION_CHECK:
                checkDatabaseTable(model, tableFormat);
                break;
            case TABLE_ACTION_CREATE:
                createDatabaseTable(model, tableFormat);
                break;
            case TABLE_ACTION_SYNC:
                syncDatabaseTable(model, tableFormat);
                break;
            default:
                break;
        }

        tableFormat.setTableAction(TABLE_ACTION_NULL);
        tablePageAddAttribute(model, tableFormat, VIEW_FLAG_EDIT);
        return "table/tablePage";
    }

    void saveTableInstance(Model model, TableFormat tableFormat) {
        try {
            if (!CollectionUtils.isEmpty(tableFormat.getTableFieldList())) {
                for (TableField iterator : tableFormat.getTableFieldList()) {
                    if (iterator.getKeyFlag().equals(1) && iterator.getNullAble().equals(1)) {
                        throw new DatakitException("saveTableInstance", "请确认key值字段设置非空!");
                    }
                }
            }

            if (!Objects.isNull(tableFormat.getTableFormatId()) && tableFormat.getTableFormatId().equals(-1L)) {
                //新增
                tableService.createTableInstance(tableFormat);
            } else {
                //修改
                tableService.modifyTableInstance(tableFormat);
            }

            String resultUrl = "/datakit/table/query?tableFormatId=" + tableFormat.getTableFormatId() + "&pageNum=1&viewFlag=2";
            resultModel(model, WARNING, "保存成功!", resultUrl);
        } catch (DatakitException e) {
            log.error("saveTableInstance error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }
    }

    void checkDatabaseTable(Model model, TableFormat tableFormat) {
        try {
            tableService.checkDatabaseTable(tableFormat);
            resultModel(model, WARNING, "校验一致!");
        } catch (DatakitException e) {
            log.error("checkDatabaseTable error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }
    }

    void createDatabaseTable(Model model, TableFormat tableFormat) {
        try {
            tableService.createDatabaseTable(tableFormat.getTableFormatId());
            resultModel(model, WARNING, "生成数据库表成功!");
        } catch (DatakitException e) {
            log.error("createDatabaseTable error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }
    }

    void syncDatabaseTable(Model model, TableFormat tableFormat) {
        try {
            tableService.syncDatabaseTable(tableFormat);
        } catch (DatakitException e) {
            log.error("syncDatabaseTable error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }
    }

    /**
     * 根据表定义生成数据库表实例
     * http://127.0.0.1:9193/datakit/table/database/create
     */
    @GetMapping(value = "/table/database/create")
    public String tableDatabaseCreate(Model model,
                                      @RequestParam("tableFormatId") Long tableFormatId,
                                      @RequestParam(required = false, name = "onLineFlag") Integer onLineFlag) {
        try {
            tableService.createDatabaseTable(tableFormatId);
        } catch (DatakitException e) {
            log.error("tableDatabaseCreate error={}", e);
            resultModel(model, ERROR, "创建数据库表实例失败");
        }

        TableRowsDTO tableRowsDTO = new TableRowsDTO();
        tableRowsDTO.setOnLineFlag(onLineFlag);
        return tableRows(model, 1, tableRowsDTO);
    }


    /**
     * 任务配置对象记录rows选择,用在配置任务选择表对象
     * 这时候这个页面提供给任务配置的时候选择表对象
     * http://127.0.0.1:9193/datakit/tableRows/select?pageNum=1&onLineFlag=1&flag=A
     */
    @GetMapping(path = "/tableRows/select")
    public String tableRowsSelectForTask(Model model,
                                         @RequestParam("pageNum") Integer pageNum,
                                         @RequestParam(required = false, name = "onLineFlag") Integer onLineFlag,
                                         @RequestParam(required = false, name = "taskSelectFlag") String taskSelectFlag) {
        TableRowsDTO tableRowsDTO = new TableRowsDTO();
        tableRowsDTO.setOnLineFlag(onLineFlag);
        tableRowsDTO.setTaskSelectFlag(taskSelectFlag);
        return tableRows(model, pageNum, tableRowsDTO);
    }

    private static final String MODEL_ATTRIBUTE_DATA_BASE_MAP = "dataBaseMap";

    private void tablePageAddAttribute(Model model, TableFormat tableFormat, Integer viewFlag) {
        model.addAttribute("viewFlag", viewFlag);
        model.addAttribute("tableFormat", tableFormat);
        if (!Objects.isNull(tableFormat.getDataBase())) {
            model.addAttribute("fieldTypeNameMap", tableService.getFieldTypeNameMergeMap(tableFormat.getDataBase()));
        } else {
            model.addAttribute("fieldTypeNameMap", null);
        }

        model.addAttribute(MODEL_ATTRIBUTE_DATA_BASE_MAP, getDataBaseMap());
    }

    /**
     * 表数据结构一键转换
     * http://127.0.0.1:9193/datakit/table/transform/get
     *
     * @param model
     * @return
     */
    @GetMapping(value = "/table/transform/get")
    public String tableTransformGet(Model model) {
        TableTransformDTO tableTransformDTO = new TableTransformDTO();
        tableTransformDTO.setAction(0);
        tableTransformDTO.setTableTransformList(new ArrayList<>());
        model.addAttribute(MODEL_ATTRIBUTE_DATA_BASE_MAP, getDataBaseMap());
        model.addAttribute("tableTransformDTO", tableTransformDTO);
        return "table/tableTransformPage";
    }

    /**
     * 0无动作、1匹配、2转换
     */
    private static final Integer TABLE_TRANSFORM_ACTION_NULL = 0;
    private static final Integer TABLE_TRANSFORM_ACTION_MATCH = 1;
    private static final Integer TABLE_TRANSFORM_ACTION_TRANSFORM = 2;

    @GetMapping(value = "/table/transform/form/get")
    public String tableChangePost(Model model, TableTransformDTO tableTransformDTO) {
        try {
            if (tableTransformDTO.getAction().equals(TABLE_TRANSFORM_ACTION_MATCH)) {
                tableChangePostMatch(tableTransformDTO);
            } else if (tableTransformDTO.getAction().equals(TABLE_TRANSFORM_ACTION_TRANSFORM)) {
                tableChangePostTransform(tableTransformDTO);
            }
        } catch (DatakitException e) {
            log.error("tableChangePost error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }

        tableTransformDTO.setAction(TABLE_TRANSFORM_ACTION_NULL);
        model.addAttribute(MODEL_ATTRIBUTE_DATA_BASE_MAP, getDataBaseMap());
        model.addAttribute("tableTransformDTO", tableTransformDTO);
        return "table/tableTransformPage";
    }

    private void tableChangePostMatch(TableTransformDTO tableTransformDTO) throws DatakitException {
        List<String> sourceTableNameList = tableService.getTableNameList(tableTransformDTO.getSourceDb());
        List<String> targetTableNameList = tableService.getTableNameList(tableTransformDTO.getTargetDb());
        List<TableTransform> tableTransform = new ArrayList<>();
        if (!CollectionUtils.isEmpty(sourceTableNameList)) {
            for (String iterator : sourceTableNameList) {
                TableTransform tableChange = new TableTransform();
                tableChange.setSourceTableName(iterator);
                tableChange.setSelectFlag(0);
                if (targetTableNameList.contains(iterator)) {
                    tableChange.setTargetTableName(iterator);
                    tableChange.setStateName("yes");
                } else {
                    tableChange.setStateName("no");
                }
                tableTransform.add(tableChange);
            }
        }
        tableTransformDTO.setTableTransformList(tableTransform);
    }

    private void tableChangePostTransform(TableTransformDTO tableTransformDTO) throws DatakitException {
        //按表进行转换
        if (!CollectionUtils.isEmpty(tableTransformDTO.getTableTransformList())) {
            for (TableTransform iterator : tableTransformDTO.getTableTransformList()) {
                if (StringUtils.isEmpty(iterator.getTargetTableName()) && !Objects.isNull(iterator.getSelectFlag()) && iterator.getSelectFlag().equals(1)) {
                    iterator.setStateName("yes");
                    tableService.tableTransform(iterator.getSourceTableName(), tableTransformDTO.getSourceDb(), tableTransformDTO.getTargetDb());
                    iterator.setTargetTableName(iterator.getSourceTableName());
                } else if (!StringUtils.isEmpty(iterator.getTargetTableName())) {
                    iterator.setStateName("yes");
                }
            }
        }
    }
}
