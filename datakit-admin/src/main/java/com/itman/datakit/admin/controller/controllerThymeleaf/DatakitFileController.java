package com.itman.datakit.admin.controller.controllerThymeleaf;

import com.github.pagehelper.PageInfo;
import com.itman.datakit.admin.common.constants.FileTypeEnum;
import com.itman.datakit.admin.common.dto.*;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.service.FileService;
import com.itman.datakit.admin.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.constants.FileTypeEnum.EXCEL;
import static com.itman.datakit.admin.common.enums.DatakitObjectEnum.DATAKIT_OBJECT_FILE;
import static com.itman.datakit.admin.common.util.ChangeNameUtil.*;
import static com.itman.datakit.admin.common.util.ResultModelUtil.resultModel;

/**
 * 文件对象相关服务接口
 */
@Slf4j
@Controller
@RequestMapping("/datakit")
@RequiredArgsConstructor
public class DatakitFileController {
    private final FileService fileService;
    private final TaskService taskService;

    /**
     * 0无动作、1保存、2校验、3ftp测试
     */
    private static final int FILE_ACTION_NULL = 0;
    private static final int FILE_ACTION_SAVE = 1;
    private static final int FILE_ACTION_CHECK = 2;
    private static final int FILE_ACTION_FTP = 3;

    /**
     * 0无动作、1新增、2删除
     */
    private static final int FILE_DATA_ACTION_NULL = 0;
    private static final int FILE_DATA_ACTION_ADD = 1;
    private static final int FILE_DATA_ACTION_DELETE = 2;

    /**
     * 文件记录动作：1删除、2上线、3下线、4复制
     */
    private static final int FILE_ROW_ACTION_DELETE = 1;
    private static final int FILE_ROW_ACTION_ONLINE = 2;
    private static final int FILE_ROW_ACTION_OFFLINE = 3;
    private static final int FILE_ROW_ACTION_COPY = 4;

    /**
     * 查询文件对象记录
     * http://127.0.0.1:9193/datakit/fileRows/query?pageNum=1&onLineFlag=1
     */
    @GetMapping(path = "/fileRows/query")
    public String fileRowsQuery(Model model,
                                @RequestParam(required = false, name = "pageNum") Integer pageNum,
                                @RequestParam(required = false, name = "onLineFlag") Integer onLineFlag,
                                @RequestParam(required = false, name = "taskSelectFlag") String taskSelectFlag) {
        FileRowsDTO fileRowsDTO = new FileRowsDTO();
        fileRowsDTO.setOnLineFlag(onLineFlag);
        fileRowsDTO.setTaskSelectFlag(taskSelectFlag);
        return fileRows(model, Optional.ofNullable(pageNum).orElse(1), fileRowsDTO);
    }


    /**
     * 文件记录查询处理
     *
     * @param model
     * @param pageNum
     * @return
     */
    public String fileRows(Model model, Integer pageNum, FileRowsDTO fileRowsDTO) {
        fileRowsDTO = Optional.ofNullable(fileRowsDTO).orElse(new FileRowsDTO());
        fileRowsDTO.setOnLineFlag(Optional.ofNullable(fileRowsDTO.getOnLineFlag()).orElse(-1));
        PageInfo<FileFormat> pageInfo = fileService.selectFileFormatByOnLineFlagPage(pageNum, WEB_PAGE_SIZE, fileRowsDTO.getOnLineFlag());
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute("pageLeftSize", (pageInfo.getPageSize() - pageInfo.getSize()));
        model.addAttribute("fileRowsDTO", fileRowsDTO);
        return "file/fileRowsPage";
    }

    /**
     * 删除文件对象记录
     * http://127.0.0.1:9193/datakit/file/rows/operate?fileFormatId=11&onLineFlag=-1&action=1
     */
    @GetMapping(path = "/file/rows/operate")
    public String fileRowsOperate(Model model,
                                  @RequestParam("fileFormatId") Long fileFormatId,
                                  @RequestParam(required = false, name = "onLineFlag") Integer onLineFlag,
                                  @RequestParam(required = false, name = "action") Integer action) {
        try {
            if (action.equals(FILE_ROW_ACTION_DELETE)) {
                fileService.deleteFileInstance(fileFormatId);
            } else if (action.equals(FILE_ROW_ACTION_ONLINE)) {
                //上线
                fileService.updateFileFormatOnLineFlagById(fileFormatId, ON_LINE_FLAG_ON_LINE);
            } else if (action.equals(FILE_ROW_ACTION_OFFLINE)) {
                //下线：先校验，校验关联的任务配置表是否都是下线状态
                taskService.checkTaskOnLineByObjectId(DATAKIT_OBJECT_FILE.getValue(), fileFormatId);

                fileService.updateFileFormatOnLineFlagById(fileFormatId, ON_LINE_FLAG_OFF_LINE);
            } else if (action.equals(FILE_ROW_ACTION_COPY)) {
                fileService.copyFileInstance(fileFormatId);
            }
        } catch (DatakitException e) {
            log.error("error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }

        FileRowsDTO fileRowsDTO = new FileRowsDTO();
        fileRowsDTO.setOnLineFlag(onLineFlag);
        return fileRows(model, 1, fileRowsDTO);
    }

    /**
     * 查询文件实例信息
     * http://127.0.0.1:9193/datakit/file/query?fileFormatId=1&pageNum=1&viewFlag=2
     */
    @GetMapping(path = "/file/query")
    public String fileQuery(Model model,
                            @RequestParam("fileFormatId") Long fileFormatId,
                            @RequestParam("pageNum") Integer pageNum,
                            @RequestParam(required = false, name = "viewFlag") Integer viewFlag,
                            @RequestParam(required = false, name = "sourcePage") String sourcePage) throws DatakitException {
        return file(model, fileFormatId, viewFlag, sourcePage);
    }

    /**
     * 文件实例处理
     *
     * @param model
     * @param fileFormatId
     * @return
     */
    public String file(Model model, Long fileFormatId, Integer viewFlag, String sourcePage) throws DatakitException {
        viewFlag = Optional.ofNullable(viewFlag).orElse(VIEW_FLAG_SHOW);

        //新增文件对象流程
        FileFormat fileFormat = null;
        if (!(viewFlag.equals(VIEW_FLAG_EDIT) && fileFormatId.equals(-1L))) {
            fileFormat = fileService.makeFileObject(fileFormatId);
        }

        if (Objects.isNull(fileFormat)) {
            fileFormat = new FileFormat();
            fileFormat.setFileFormatId(-1L);
            fileFormat.setFileBody(new FileBody());
            fileFormat.getFileBody().setFileBodyId(-1L);
        } else {
            fileFormat.setOnLineFlagName(!Objects.isNull(fileFormat.getOnLineFlag()) ? changeOnLineFlagName(fileFormat.getOnLineFlag()) : null);
            fileFormat.setFileNameTypeName(changeFileNameTypeName(fileFormat.getFileNameType()));
            fileFormat.setFileTypeName(FileTypeEnum.of(fileFormat.getFileType()).getName());
            fileFormat.setFileBakActionName(changeFileBakActionName(fileFormat.getFileBakAction()));
            if (!Objects.isNull(fileFormat.getFileBody())) {
                fileFormat.getFileBody().setSplitFlagName(changeSplitFlagName(fileFormat.getFileBody().getSplitFlag()));
            }

            if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
                for (FileSpecial iterator : fileFormat.getFileSpecialList()) {
                    iterator.setSplitFlagName(changeSplitFlagName(iterator.getSplitFlag()));
                }
            }
        }

        model.addAttribute("sourcePage", sourcePage);
        filePageAddAttribute(model, fileFormat, viewFlag);
        return "file/filePage";
    }

    private void filePageAddAttribute(Model model, FileFormat fileFormat, Integer viewFlag) {
        model.addAttribute("viewFlag", viewFlag);
        model.addAttribute("fileFormat", fileFormat);
        model.addAttribute("splitFlagNameMap", FILE_SPLIT_FLAG_MAP);
        model.addAttribute("fileNameTypeNameMap", FILE_NAME_TYPE_MAP);
        model.addAttribute("fileTypeNameMap", FILE_TYPE_MAP);
        model.addAttribute("fileBakActionNameMap", FILE_BAK_ACTION_MAP);
    }


    private void filePostOperateAction(FileFormat fileFormat) {
        filePostOperateBodyFieldAction(fileFormat);

        filePostOperateSpecialAction(fileFormat);

        filePostOperateSpecialFieldAction(fileFormat);

        fileFormat.setFileSpecialIndex(0);
    }


    private void filePostOperateBodyFieldAction(FileFormat fileFormat) {
        fileFormat.setFileBodyFieldAction(Optional.ofNullable(fileFormat.getFileBodyFieldAction()).orElse(FILE_DATA_ACTION_NULL));
        if (fileFormat.getFileBodyFieldAction().equals(FILE_DATA_ACTION_ADD)) {
            //文件行体字段新增
            fileFormat.getFileBody().setFileFieldList(Optional.ofNullable(fileFormat.getFileBody().getFileFieldList()).orElse(new ArrayList<>()));
            FileField fileField = new FileField();
            fileField.setFileFieldId(-1L);
            fileFormat.getFileBody().getFileFieldList().add(fileField);
        } else if (fileFormat.getFileBodyFieldAction().equals(FILE_DATA_ACTION_DELETE)) {
            fileFormat.getFileBody().getFileFieldList().remove(fileFormat.getFileBody().getFileFieldList().get(fileFormat.getFileBodyFieldIndex()));
        }

        fileFormat.setFileBodyFieldAction(FILE_DATA_ACTION_NULL);
        fileFormat.setFileBodyFieldIndex(0);
    }

    private void filePostOperateSpecialAction(FileFormat fileFormat) {
        fileFormat.setFileSpecialAction(Optional.ofNullable(fileFormat.getFileSpecialAction()).orElse(FILE_DATA_ACTION_NULL));
        if (fileFormat.getFileSpecialAction().equals(FILE_DATA_ACTION_ADD)) {
            //新增特殊行配置
            fileFormat.setFileSpecialList(Optional.ofNullable(fileFormat.getFileSpecialList()).orElse(new ArrayList<>()));
            FileSpecial fileSpecial = new FileSpecial();
            fileSpecial.setFileSpecialId(-1L);
            fileFormat.getFileSpecialList().add(fileSpecial);
        } else if (fileFormat.getFileSpecialAction().equals(FILE_DATA_ACTION_DELETE)) {
            //删除特殊行配置
            fileFormat.getFileSpecialList().remove(fileFormat.getFileSpecialList().get(fileFormat.getFileSpecialIndex()));
        }

        fileFormat.setFileSpecialAction(FILE_DATA_ACTION_NULL);
    }

    private void filePostOperateSpecialFieldAction(FileFormat fileFormat) {
        fileFormat.setFileSpecialFieldAction(Optional.ofNullable(fileFormat.getFileSpecialFieldAction()).orElse(FILE_DATA_ACTION_NULL));

        if (fileFormat.getFileSpecialFieldAction().equals(FILE_DATA_ACTION_ADD)) {
            //新增字段
            fileFormat.getFileSpecialList().get(fileFormat.getFileSpecialIndex()).
                    setFileFieldList(Optional.ofNullable(fileFormat.getFileSpecialList().
                            get(fileFormat.getFileSpecialIndex()).getFileFieldList()).orElse(new ArrayList<>()));

            FileField fileField = new FileField();
            fileField.setFileFieldId(-1L);
            fileFormat.getFileSpecialList().get(fileFormat.getFileSpecialIndex()).getFileFieldList().add(fileField);
        } else if (fileFormat.getFileSpecialFieldAction().equals(FILE_DATA_ACTION_DELETE)) {
            //删除字段
            FileField fileFieldDelete = fileFormat.getFileSpecialList().get(fileFormat.getFileSpecialIndex()).getFileFieldList().get(fileFormat.getFileSpecialFieldIndex());
            fileFormat.getFileSpecialList().get(fileFormat.getFileSpecialIndex()).getFileFieldList().remove(fileFieldDelete);
        }

        fileFormat.setFileSpecialFieldAction(FILE_DATA_ACTION_NULL);
        fileFormat.setFileSpecialFieldIndex(0);
    }

    @GetMapping(value = "/file/form/operate")
    public String fileGetOperate(Model model,
                                 FileFormat fileFormat) {
        return filePostOperate(model, fileFormat);
    }

    /**
     * 文件对象创建、更新
     * http://127.0.0.1:9193/datakit/file/form/operate
     */
    @PostMapping(value = "/file/form/operate")
    public String filePostOperate(Model model,
                                  FileFormat fileFormat) {
        fileFormat.setFileAction(Optional.ofNullable(fileFormat.getFileAction()).orElse(FILE_ACTION_NULL));

        if (fileFormat.getFileAction().equals(FILE_ACTION_NULL)) {
            //文件其他动作
            filePostOperateAction(fileFormat);
        } else if (fileFormat.getFileAction().equals(FILE_ACTION_SAVE)) {
            filePostOperateSave(model, fileFormat);
        } else if (fileFormat.getFileAction().equals(FILE_ACTION_CHECK)) {
            filePostOperateCheck(model, fileFormat.getFileFormatId());
        } else if (fileFormat.getFileAction().equals(FILE_ACTION_FTP)) {
            filePostOperateFtp(model, fileFormat);
        }

        fileFormat.setFileAction(FILE_ACTION_NULL);
        filePageAddAttribute(model, fileFormat, VIEW_FLAG_EDIT);
        return "file/filePage";
    }


    private void filePostOperateSaveCheck(FileFormat fileFormat) throws DatakitException {
        //校验特殊行存在情况下
        if (FileTypeEnum.of(fileFormat.getFileType()).equals(EXCEL)) {
            String fileNamSuffixes = fileFormat.getFileNameFormat().substring(fileFormat.getFileNameFormat().lastIndexOf(".") + 1);
            if (!fileNamSuffixes.equals("xls") && !fileNamSuffixes.equals("xlsx")) {
                throw new DatakitException("filePostOperateSaveCheck001", "请明确excel文件后缀类型xls、xlsx");
            }
        }

        if (CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            return;
        }

        for (FileSpecial iterator : fileFormat.getFileSpecialList()) {
            filePostOperateSaveCheckFileSpecial(iterator, fileFormat.getFileBody().getFixBeginLine(), fileFormat.getFileBody().getFileFieldList());
        }
    }


    private void filePostOperateSaveCheckFileSpecial(FileSpecial fileSpecial, Integer fixBeginLine, final List<FileField> fileBodyFieldList) throws DatakitException {
        if (fileSpecial.getSplitFlag() == null || fileSpecial.getSplitFlag() <= 0L) {
            throw new DatakitException("filePostOperateSaveCheck002", "请配置特殊行属性:分隔符!");
        }

        if (fileSpecial.getFixLinePosition() == null) {
            throw new DatakitException("filePostOperateSaveCheck003", "请配置特殊行属性:固定行位!");
        }

        if (CollectionUtils.isEmpty(fileSpecial.getFileFieldList())) {
            throw new DatakitException("filePostOperateSaveCheck004", "请配置特殊行字段明细!");
        }

        if (fileSpecial.getFixLinePosition() >= fixBeginLine) {
            throw new DatakitException("filePostOperateSaveCheck005", "请配置特殊行固定行位值必须小于行体固定开始行值!");
        }

        Set<Integer> positionSet = new HashSet<>();
        for (FileField iterator : fileSpecial.getFileFieldList()) {
            filePostOperateSaveCheckFileSpecialField(iterator, positionSet, fileBodyFieldList);
        }

        if (fileSpecial.getFileFieldList().size() != positionSet.size()) {
            throw new DatakitException("filePostOperateSaveCheck010", "特殊行请合理配置字段占位,同一行不能冲突!");
        }
    }

    private void filePostOperateSaveCheckFileSpecialField(FileField fileField, Set<Integer> positionSet, final List<FileField> fileBodyFieldList) throws DatakitException {
        if (StringUtils.isEmpty(fileField.getFieldName())) {
            throw new DatakitException("filePostOperateSaveCheck006", "请配置特殊行字段名称!");
        }

        if (fileField.getPosition() == null) {
            throw new DatakitException("filePostOperateSaveCheck007", "请配置特殊行字段占位!");
        }

        if (!StringUtils.isEmpty(fileField.getSumFieldName())) {
            //如果特殊行存在汇总字段不为空，则需要去行体明细去配置对应字段名称
            List<FileField> fileFieldFilterList = fileBodyFieldList.stream().
                    filter(x -> x.getFieldName().equalsIgnoreCase(fileField.getSumFieldName())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(fileFieldFilterList)) {
                throw new DatakitException("filePostOperateSaveCheck008", "特殊行汇总字段匹配不到行体字段!");
            }

            if (!Objects.isNull(fileField.getSumLineFlag()) && fileField.getSumLineFlag().equals(1)) {
                throw new DatakitException("filePostOperateSaveCheck009", "同一个字段不能即是总行数又是字段数量值累加!");
            }
        }

        positionSet.add(fileField.getPosition());
    }

    private void filePostOperateSave(Model model,
                                     FileFormat fileFormat) {
        try {
            //excel在系统里默认按竖线分割
            if (fileFormat.getFileType().equals(EXCEL.getId())) {
                fileFormat.getFileBody().setSplitFlag(SPLIT_FLAG_VERTICAL_LINE);
                if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
                    fileFormat.getFileSpecialList().forEach(x -> x.setSplitFlag(SPLIT_FLAG_VERTICAL_LINE));
                }
            }

            //特殊行校验
            filePostOperateSaveCheck(fileFormat);

            if (!Objects.isNull(fileFormat.getFileFormatId()) && fileFormat.getFileFormatId().equals(-1L)) {
                //文件新增
                fileService.createFileInstance(fileFormat);
            } else if (!Objects.isNull(fileFormat.getFileFormatId()) && fileFormat.getFileFormatId() > 0L) {
                //文件修改
                fileService.modifyFileInstance(fileFormat);
            } else {
                throw new DatakitException("filePostAction", " 未知文件处理!");
            }
            resultModel(model, WARNING, "保存成功!", String.format("/datakit/file/query?fileFormatId=%d&pageNum=1&viewFlag=2", fileFormat.getFileFormatId()));
        } catch (DatakitException e) {
            log.error("filePostOperateSave error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }
    }

    private void filePostOperateCheck(Model model, Long fileFormatId) {
        try {
            if (fileFormatId == null || fileFormatId.equals(-1L)) {
                throw new DatakitException("filePostOperateCheck", "请先保存再做校验!");
            }

            //文件稽核必须用文件保存后的数据
            FileFormat fileFormat = fileService.makeFileObject(fileFormatId);

            fileService.checkFileFormat(fileFormat);
            resultModel(model, WARNING, "校验一致!");
        } catch (DatakitException e) {
            log.error("filePostOperateCheck error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }
    }

    private void filePostOperateFtp(Model model,
                                    FileFormat fileFormat) {
        try {
            fileService.testFtp(fileFormat);
            resultModel(model, WARNING, "ftp测试通过!");
        } catch (DatakitException e) {
            log.error("filePostOperateFtp error={}", e);
            resultModel(model, ERROR, e.getErrMsg());
        }
    }

    /**
     * 提供给任务页面做对象选择：文件对象记录rows选择
     * http://127.0.0.1:9193/datakit/fileRows/select?pageNum=1&onLineFlag=1&flag=A
     */
    @GetMapping(path = "/fileRows/select")
    public String fileRowsSelectForTask(Model model,
                                        @RequestParam("pageNum") Integer pageNum,
                                        @RequestParam(required = false, name = "onLineFlag") Integer onLineFlag,
                                        @RequestParam(required = false, name = "taskSelectFlag") String taskSelectFlag) {
        FileRowsDTO fileRowsDTO = new FileRowsDTO();
        fileRowsDTO.setOnLineFlag(onLineFlag);
        fileRowsDTO.setTaskSelectFlag(taskSelectFlag);
        return fileRows(model, pageNum, fileRowsDTO);
    }
}
