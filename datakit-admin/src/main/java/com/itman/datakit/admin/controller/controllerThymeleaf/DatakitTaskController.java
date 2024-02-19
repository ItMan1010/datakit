package com.itman.datakit.admin.controller.controllerThymeleaf;

import com.github.pagehelper.PageInfo;
import com.itman.datakit.admin.common.dto.TaskInstanceDTO;
import com.itman.datakit.admin.common.dto.TaskRowsDTO;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.util.ChangeNameUtil.*;
import static com.itman.datakit.admin.common.util.CommonUtil.formatDate;
import static com.itman.datakit.admin.common.util.CommonUtil.getLocalTime;
import static com.itman.datakit.admin.common.util.ResultModelUtil.resultModel;

/**
 * 这里不允许调用具体业务对象，如：TableService、FileService
 */
@Slf4j
@Controller
@RequestMapping("/datakit")
@RequiredArgsConstructor
public class DatakitTaskController {
    private final TaskService datakitService;

    /**
     * 任务页面操作：0无动作、1保存、2匹配
     */
    private static final int TASK_ACTION_NULL = 0;
    private static final int TASK_ACTION_SAVE = 1;
    private static final int TASK_ACTION_MATCH = 2;

    /**
     * 任务配置字段映射动作：0无动作、1新增、2删除、3子页面赋值
     */
    private static final Integer TASK_FIELD_MAPPED_ACTION_NULL = 0;
    private static final Integer TASK_FIELD_MAPPED_ACTION_ADD = 1;
    private static final Integer TASK_FIELD_MAPPED_ACTION_DELETE = 2;

    /**
     * 任务记录动作：1删除、2上线、3下线、4复制
     */
    private static final Integer TASK_ROW_ACTION_DELETE = 1;
    private static final Integer TASK_ROW_ACTION_ONLINE = 2;
    private static final Integer TASK_ROW_ACTION_OFFLINE = 3;
    private static final Integer TASK_ROW_ACTION_COPY = 4;

    private static final String MODEL_ATTRIBUTE_VIEW_FLAG = "viewFlag";
    private static final String MODEL_ATTRIBUTE_PAGE_LEFT_SIZE = "pageLeftSize";

    private void formatTaskInstanceDTO(TaskInstanceDTO taskInstanceDTO) {
        String localDateTime = getLocalTime("yyyyMMdd");
        taskInstanceDTO = Optional.ofNullable(taskInstanceDTO).orElse(new TaskInstanceDTO());
        taskInstanceDTO.setRunState(Optional.ofNullable(taskInstanceDTO.getRunState()).orElse(-1));
        taskInstanceDTO.setBeginDate(Optional.ofNullable(taskInstanceDTO.getBeginDate()).orElse(localDateTime));
        taskInstanceDTO.setBeginDate(taskInstanceDTO.getBeginDate().replace("-", ""));
        taskInstanceDTO.setEndDate(Optional.ofNullable(taskInstanceDTO.getEndDate()).orElse(localDateTime));
        taskInstanceDTO.setEndDate(taskInstanceDTO.getEndDate().replace("-", ""));
    }

    /**
     * 任务实例明细查询展示，通过表单提交post方式
     * http://127.0.0.1:9193/datakit/taskInstanceRows/post/query?pageNum=1
     */
    @PostMapping(path = "/taskInstanceRows/post/query")
    public String taskInstanceRowsPostQuery(Model model,
                                            @RequestParam("pageNum") Integer pageNum,
                                            TaskInstanceDTO taskInstanceDTO) {
        formatTaskInstanceDTO(taskInstanceDTO);

        return String.format("redirect:/datakit/taskInstanceRows/query?pageNum=1&beginDate=%s&endDate=%s&runState=%s", taskInstanceDTO.getBeginDate(), taskInstanceDTO.getEndDate(), taskInstanceDTO.getRunState());
    }

    /**
     * 任务实例明细展示get
     * http://127.0.0.1:9193/datakit/taskInstanceRows/query?pageNum=1&beginDate=20230101&endDate=20231212&runState=2
     */
    @GetMapping(path = "/taskInstanceRows/query")
    public String taskInstanceRowsQuery(Model model,
                                        @RequestParam("pageNum") Integer pageNum,
                                        @RequestParam(required = false, name = "beginDate") String beginDate,
                                        @RequestParam(required = false, name = "endDate") String endDate,
                                        @RequestParam(required = false, name = "runState") Integer runState) {
        TaskInstanceDTO taskInstanceDTO = new TaskInstanceDTO();
        taskInstanceDTO.setBeginDate(beginDate);
        taskInstanceDTO.setEndDate(endDate);
        taskInstanceDTO.setRunState(runState);
        return queryTaskInstanceRows(model, pageNum, taskInstanceDTO);
    }

    /**
     * 生成任务实例
     * http://127.0.0.1:9193/datakit/taskInstance/create?taskId=1
     */
    @GetMapping(path = "/taskInstance/create")
    public String taskInstanceCreate(Model model,
                                     @RequestParam(required = false, name = "taskId") Long taskId) {
        try {
            datakitService.createTaskInstance(taskId);
        } catch (DatakitException e) {
            log.error("taskInstanceCreate error: ", e);
        }

        resultModel(model, SUCCESS, "登录成功!");
        return "redirect:/datakit/taskInstanceRows/query?pageNum=1";
    }

    /**
     * 更新、删除任务实例
     * http://127.0.0.1:9193/datakit/taskInstance/operate?action=1&taskInstanceId=24&action=delete&createDate=2023-01-01&endDate=2023-09-16&runState=-1&pageNum=2
     */
    @GetMapping(path = "/taskInstance/operate")
    public String taskInstanceOperate(Model model,
                                      @RequestParam(required = false, name = "taskInstanceId") Long taskInstanceId,
                                      @RequestParam(required = false, name = "action") String action,
                                      @RequestParam(required = false, name = "createDate") String createDate,
                                      @RequestParam(required = false, name = "endDate") String endDate,
                                      @RequestParam(required = false, name = "runState") Integer runState,
                                      @RequestParam(required = false, name = "pageNum") Integer pageNum) {
        try {
            if (action.equals("delete")) {
                datakitService.deleteTaskInstance(taskInstanceId);
            } else if (action.equals("update")) {
                datakitService.updateTaskInstance(taskInstanceId);
            } else if (action.equals("repair")) {
                datakitService.createTaskRepair(taskInstanceId);
            } else if (action.equals("stop")) {
                datakitService.stopTaskInstance(taskInstanceId);
            }
        } catch (DatakitException e) {
            log.error("taskInstanceOperate error: ", e);
            resultModel(model, ERROR, e.getErrMsg());
        }

        TaskInstanceDTO taskInstanceDTO = new TaskInstanceDTO();
        taskInstanceDTO.setBeginDate(createDate);
        taskInstanceDTO.setEndDate(endDate);
        taskInstanceDTO.setRunState(runState);
        return queryTaskInstanceRows(model, pageNum, taskInstanceDTO);
    }

    /**
     * 任务实例明细展示
     *
     * @param model
     * @param pageNum
     * @param taskInstanceDTO
     * @return
     */
    public String queryTaskInstanceRows(Model model, Integer pageNum, TaskInstanceDTO taskInstanceDTO) {
        formatTaskInstanceDTO(taskInstanceDTO);

        PageInfo<TaskInstance> pageInfo = datakitService.selectTaskInstancePage(pageNum, WEB_PAGE_SIZE, taskInstanceDTO.getBeginDate(), taskInstanceDTO.getEndDate(), taskInstanceDTO.getRunState());
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute(MODEL_ATTRIBUTE_PAGE_LEFT_SIZE, (pageInfo.getPageSize() - pageInfo.getSize()));

        taskInstanceDTO.setBeginDate(formatDate(taskInstanceDTO.getBeginDate()));
        taskInstanceDTO.setEndDate(formatDate(taskInstanceDTO.getEndDate()));
        model.addAttribute("taskInstanceDTO", taskInstanceDTO);

        return "task/taskInstanceRowsPage";
    }

    @PostMapping(path = "/taskRows/post/query")
    public String taskRowsPostQuery(Model model,
                                    @RequestParam("pageNum") Integer pageNum,
                                    TaskRowsDTO taskRowsDTO) {
        taskRowsDTO = Optional.ofNullable(taskRowsDTO).orElse(new TaskRowsDTO());
        taskRowsDTO.setTaskType(Optional.ofNullable(taskRowsDTO.getTaskType()).orElse(0));

        return String.format("redirect:/datakit/taskRows/query?pageNum=1&taskType=%d", taskRowsDTO.getTaskType());
    }

    /**
     * 查询任务list明显数据
     * http://127.0.0.1:9193/datakit/taskRows/query?pageNum=1&taskType=1&taskId=1
     */
    @GetMapping(path = "/taskRows/query")
    public String taskRowsQuery(Model model,
                                @RequestParam("pageNum") Integer pageNum,
                                @RequestParam(required = false, name = "taskType") Integer taskType,
                                @RequestParam(required = false, name = "taskId") Integer taskId) {
        TaskRowsDTO taskRowsDTO = new TaskRowsDTO();
        taskRowsDTO.setTaskType(taskType);
        taskRowsDTO.setTaskId(taskId);
        return selectTaskRows(model, pageNum, taskRowsDTO);
    }

    public String selectTaskRows(Model model, Integer pageNum, TaskRowsDTO taskRowsDTO) {
        taskRowsDTO = Optional.ofNullable(taskRowsDTO).orElse(new TaskRowsDTO());
        taskRowsDTO.setTaskType(Optional.ofNullable(taskRowsDTO.getTaskType()).orElse(0));

        PageInfo<Task> pageInfo = datakitService.selectTaskPage(pageNum, WEB_PAGE_SIZE, taskRowsDTO.getTaskType(), taskRowsDTO.getTaskId());
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute(MODEL_ATTRIBUTE_PAGE_LEFT_SIZE, (pageInfo.getPageSize() - pageInfo.getSize()));
        model.addAttribute("taskRowsDTO", taskRowsDTO);

        return "task/taskRowsPage";
    }

    /**
     * 页面增加字段映射操作
     *
     * @param task
     */
    public void taskOperateFieldMapped(Task task) {
        if (task == null) {
            return;
        }

        if (task.getTaskFieldMappedAction() == null || task.getTaskFieldMappedAction().equals(TASK_FIELD_MAPPED_ACTION_NULL)) {
            return;
        }

        task.setTaskFieldMappedAction(Optional.ofNullable(task.getTaskFieldMappedAction()).orElse(TASK_FIELD_MAPPED_ACTION_NULL));

        if (task.getTaskFieldMappedAction().equals(TASK_FIELD_MAPPED_ACTION_ADD)) {
            //文件行体字段新增
            task.setTaskFieldRelaList(Optional.ofNullable(task.getTaskFieldRelaList()).orElse(new ArrayList<>()));
            TaskFieldRela taskFieldRela = new TaskFieldRela();
            taskFieldRela.setTaskFieldRelaId(-1L);
            task.getTaskFieldRelaList().add(taskFieldRela);
        } else if (task.getTaskFieldMappedAction().equals(TASK_FIELD_MAPPED_ACTION_DELETE)) {
            task.getTaskFieldRelaList().remove(task.getTaskFieldRelaList().get(task.getTaskFieldMappedIndex()));
        }

        task.setTaskFieldMappedAction(TASK_FIELD_MAPPED_ACTION_NULL);
    }

    /**
     * 新任务创建Post
     * http://127.0.0.1:9193/datakit/task/form/operate
     */
    @GetMapping(value = "/task/form/operate")
    public String taskOperate(Model model, Task task) {
        task.setTaskAction(Optional.ofNullable(task.getTaskAction()).orElse(TASK_ACTION_NULL));

        switch (task.getTaskAction()) {
            case TASK_ACTION_NULL:
                taskOperateFieldMapped(task);
                break;
            case TASK_ACTION_SAVE:
                taskOperateSave(model, task);
                break;
            case TASK_ACTION_MATCH:
                taskOperateMatch(model, task);
                break;
            default:
                break;
        }

        setFieldNameMap(task);
        task.setTaskAction(TASK_ACTION_NULL);
        model.addAttribute(MODEL_ATTRIBUTE_VIEW_FLAG, VIEW_FLAG_EDIT);
        model.addAttribute("pageSize", WEB_PAGE_SIZE);
        model.addAttribute("task", task);
        return "task/taskPage";
    }

    private void taskOperateSave(Model model, Task task) {
        try {
            if (task.getTaskId() == null || task.getTaskId().equals(-1L)) {
                datakitService.createTask(task);
                resultModel(model, WARNING, "创建任务标识:" + task.getTaskId(), "/datakit/taskRows/query?pageNum=1");
            } else {
                datakitService.modifyTask(task);
                resultModel(model, WARNING, "更新任务标识:" + task.getTaskId(), "/datakit/taskRows/query?pageNum=1");
            }
        } catch (DatakitException e) {
            log.error("taskOperateSave error: ", e);
            resultModel(model, ERROR, e.getMessage());
        }
    }

    private void taskOperateMatch(Model model, Task task) {
        try {
            if (!CollectionUtils.isEmpty(task.getTaskFieldRelaList())) {
                task.getTaskFieldRelaList().clear();
            } else {
                task.setTaskFieldRelaList(new ArrayList<>());
            }

            //查询来源断字段
            Map<Long, String> aObjectFieldIDNameMap = datakitService.queryFieldIDNameMap(task.getAObjectType(), task.getAObjectId());
            //查询目标端字段
            Map<Long, String> bObjectFieldIDNameMap = datakitService.queryFieldIDNameMap(task.getBObjectType(), task.getBObjectId());
            Map<String, Long> bObjectFieldNameIdMap = new HashMap<>(bObjectFieldIDNameMap.size());
            bObjectFieldIDNameMap.forEach((x, y) -> bObjectFieldNameIdMap.put(y.toLowerCase(), x));

            aObjectFieldIDNameMap.forEach((x, y) -> {
                if (bObjectFieldNameIdMap.containsKey(y.toLowerCase())) {
                    TaskFieldRela taskFieldRela = new TaskFieldRela();
                    taskFieldRela.setTaskFieldRelaId(-1L);
                    taskFieldRela.setAObjectFieldId(x);
                    taskFieldRela.setBObjectFieldId(bObjectFieldNameIdMap.get(y.toLowerCase()));
                    task.getTaskFieldRelaList().add(taskFieldRela);
                }
            });
        } catch (DatakitException e) {
            log.error("taskOperateMatch error: ", e);
            resultModel(model, ERROR, e.getMessage());
        }
    }

    private void setFieldNameMap(Task task) {
        try {
            if (!Objects.isNull(task.getAObjectType()) && !Objects.isNull(task.getAObjectId())) {
                task.setAFieldNameMap(datakitService.queryFieldIDNameMap(task.getAObjectType(), task.getAObjectId()));
            }

            if (!Objects.isNull(task.getBObjectType()) && !Objects.isNull(task.getBObjectId())) {
                task.setBFieldNameMap(datakitService.queryFieldIDNameMap(task.getBObjectType(), task.getBObjectId()));
            }
        } catch (DatakitException e) {
            log.error("setFieldNameMap error: ", e);
        }
    }

    /**
     * 任务创建初始化页面
     * http://127.0.0.1:9193/datakit/task/query?viewFlag=1&taskId=1
     */
    @GetMapping(value = "/task/query")
    public String taskQuery(Model model,
                            @RequestParam(required = false, name = "taskId") Long taskId,
                            @RequestParam(required = false, name = "viewFlag") Integer viewFlag) {
        return taskInfo(model, taskId, viewFlag);
    }

    private String taskInfo(Model model, Long taskId, Integer viewFlag) {
        viewFlag = Optional.ofNullable(viewFlag).orElse(VIEW_FLAG_SHOW);

        Task task = null;
        if (!Objects.isNull(taskId) && taskId > 0L) {
            try {
                task = datakitService.makeTaskInfoById(taskId);
            } catch (DatakitException e) {
                log.error("taskInfo error: ", e);
            }
        }

        task = Optional.ofNullable(task).orElse(new Task());
        task.setOnLineFlag(Optional.ofNullable(task.getOnLineFlag()).orElse(ON_LINE_FLAG_ON_LINE));
        task.setOnLineFlagName(changeOnLineFlagName(task.getOnLineFlag()));
        setFieldNameMap(task);

        model.addAttribute(MODEL_ATTRIBUTE_VIEW_FLAG, viewFlag);
        model.addAttribute("pageSize", WEB_PAGE_SIZE);
        model.addAttribute("task", task);
        return "task/taskPage";
    }

    /**
     * 任务操作处理
     * http://127.0.0.1:9193/datakit/task/operate?taskId=1&action=1
     */
    @GetMapping(value = "/task/operate")
    public String taskOperate(Model model,
                              @RequestParam(required = false, name = "taskId") Long taskId,
                              @RequestParam(required = false, name = "action") Integer action) {
        try {
            if (action.equals(TASK_ROW_ACTION_DELETE)) {
                datakitService.deleteTask(taskId);
            } else if (action.equals(TASK_ROW_ACTION_ONLINE)) {
                //上线,先校验A、B对象的状态
                datakitService.checkTaskObjectOnLine(taskId);

                datakitService.updateTaskOnLineFlagById(taskId, ON_LINE_FLAG_ON_LINE);
            } else if (action.equals(TASK_ROW_ACTION_OFFLINE)) {
                //下线，先检验是否在正在处理或待处理任务实例
                datakitService.checkTaskOffLine(taskId);

                datakitService.updateTaskOnLineFlagById(taskId, ON_LINE_FLAG_OFF_LINE);
            } else if (action.equals(TASK_ROW_ACTION_COPY)) {
                datakitService.copyTask(taskId);
            }

            resultModel(model, SUCCESS, "操作成功!");
        } catch (DatakitException e) {
            log.error("taskOperate error: ", e);
            resultModel(model, ERROR, e.getErrMsg());
        }

        return selectTaskRows(model, 1, new TaskRowsDTO());
    }

    /**
     * 任务实例详情页面
     * http://127.0.0.1:9193/datakit/taskInstance/query?taskInstanceId=1
     */
    @GetMapping(value = "/taskInstance/query")
    public String taskInstanceQuery(Model model,
                                    @RequestParam(required = false, name = "taskInstanceId") Long taskInstanceId) {
        List<TaskResult> taskResultList = datakitService.selectTaskResultByTaskInstanceId(taskInstanceId);
        if (!CollectionUtils.isEmpty(taskResultList)) {
            taskResultList.forEach(x -> {
                if (x.getCompareFlag() != null) {
                    x.setCompareFlagName(changeResultFlagName(x.getCompareFlag()));
                    x.setRepairStateName(changeRepairStateName(x.getRepairState()));
                }
            });
        }

        List<RunInfo> runInfoList = datakitService.selectRunInfoByTaskInstanceId(taskInstanceId);
        if (!CollectionUtils.isEmpty(runInfoList)) {
            runInfoList.forEach(x -> {
                if (x.getInfoLevel() != null) {
                    x.setInfoLevelName(changeInfoLevelName(x.getInfoLevel()));
                }
            });
        }

        model.addAttribute("taskInstance", datakitService.selectTaskInstanceById(taskInstanceId));
        model.addAttribute("taskResultList", taskResultList);
        model.addAttribute("runInfoList", runInfoList);
        model.addAttribute(MODEL_ATTRIBUTE_PAGE_LEFT_SIZE, WEB_PAGE_SIZE - taskResultList.size());
        return "task/taskInstancePage";
    }

    /**
     * 任务对象页面
     * http://127.0.0.1:9193/datakit/task/object
     */
    @GetMapping(value = "/task/object")
    public String taskInstanceQuery(Model model, @RequestParam(required = false, name = "viewFlag") Integer viewFlag) {

        model.addAttribute(MODEL_ATTRIBUTE_VIEW_FLAG, viewFlag);
        return "task/taskObject";
    }
}
