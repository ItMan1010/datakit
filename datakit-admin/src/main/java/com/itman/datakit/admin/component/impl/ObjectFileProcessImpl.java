package com.itman.datakit.admin.component.impl;

import com.itman.datakit.admin.common.constants.DatakitConstant;
import com.itman.datakit.admin.common.constants.FileTypeEnum;
import com.itman.datakit.admin.common.dataqueue.QueueData;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.enums.DatakitObjectEnum;
import com.itman.datakit.admin.common.enums.ObjectTaskEnum;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.common.util.FileUtil;
import com.itman.datakit.admin.component.AbstractObjectProcess;
import com.itman.datakit.admin.component.CommonProcess;
import com.itman.datakit.admin.component.IObjectProcess;
import com.itman.datakit.admin.dao.ObjectFileDao;
import com.itman.datakit.admin.plugins.IFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.enums.DatakitObjectEnum.DATAKIT_OBJECT_FILE;
import static com.itman.datakit.admin.common.exception.DatakitError.ERROR_MATCH_FILE_OBJET_BY_FILE_TYPE_FAIL;
import static com.itman.datakit.admin.common.util.FTPUtil.download;
import static com.itman.datakit.admin.common.util.FTPUtil.getFileList;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObjectFileProcessImpl extends AbstractObjectProcess implements IObjectProcess {
    private final ObjectFileDao fileDao;
    private final List<IFile> fileList;
    private final CommonProcess commonProcess;

    @Override
    public Boolean objectCurrent(DatakitObjectEnum objectEnum) {
        return objectEnum.equals(DATAKIT_OBJECT_FILE);
    }

    /**
     * 根据文件定义获取文件名称集合
     * 如果文件名称定义是正则表达式，可以获取多个文件
     *
     * @param fileFormat
     * @return List<String> 文件名称集合
     */
    private List<String> getFileNameList(FileFormat fileFormat) {
        List<String> fileNameList = new ArrayList<>();

        switch (fileFormat.getFileNameType()) {
            case FILE_NAME_TYPE_FIX:
                fileNameList.add(fileFormat.getFileNameFormat());
                break;
            case FILE_NAME_TYPE_PATTERN:
                Pattern pattern = Pattern.compile(fileFormat.getFileNameFormat());
                List<String> pathFileNameList = FileUtil.getFiles(fileFormat.getLocalPath());
                pathFileNameList.forEach(fileName -> {
                    if (pattern.matcher(fileName).matches()) {
                        fileNameList.add(fileName);
                    }
                });
                break;
            case FILE_NAME_TYPE_EXTEND:
                //todo 待扩展
                break;
            default:
                break;
        }

        return fileNameList;
    }

    @Override
    public void doBusinessInputQueue(final Long taskInstanceId, final Task task) throws DatakitException, IOException {
        FileFormat fileFormat = (FileFormat) task.getAObject();

        //支持一批数据存在多个文件中
        List<String> fileNameList = getFileNameList(fileFormat);
        //获取ftp文件列表
        if (!StringUtils.isEmpty(fileFormat.getFtpHost()) &&
                !StringUtils.isEmpty(fileFormat.getFtpPort()) &&
                !StringUtils.isEmpty(fileFormat.getFtpUser()) &&
                !StringUtils.isEmpty(fileFormat.getFtpPasswd())) {
            List<String> ftpNameList = getFileList(fileFormat.getFtpHost(), Integer.parseInt(fileFormat.getFtpPort()), fileFormat.getFtpUser(), fileFormat.getFtpPasswd());
            if (CollectionUtils.isEmpty(ftpNameList)) {
                throw new DatakitException("doBusinessInputQueue", "获取ftp文件列表为空");
            }

            for (String iterator : fileNameList) {
                List<String> ftpNameListFilter = ftpNameList.stream().filter(x -> x.equals(iterator)).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(ftpNameListFilter)) {
                    throw new DatakitException("doBusinessInputQueue", "匹配远程文件[" + iterator + "]失败！");
                }
                download(fileFormat.getFtpHost(), Integer.parseInt(fileFormat.getFtpPort()), fileFormat.getFtpUser(), fileFormat.getFtpPasswd(), iterator, fileFormat.getLocalPath());
            }
        }

        for (String fileName : fileNameList) {
            commonProcess.saveRunInfo(INFO_LEVEL_INFO, taskInstanceId, "文件【" + fileName + "】数据加载发送队列开始");

            parseFileLineInputQueue(fileFormat.getFileType(), fileFormat.getLocalPath(), fileName, task);

            doFileBakAction(fileFormat.getFileBakAction(), fileName, fileFormat.getLocalPath(), fileFormat.getFileBakPath());
        }
    }

    private void doFileBakAction(Integer fileBakAction, String fileName, String localPath, String bakPath) throws DatakitException {
        //文件处理
        if (fileBakAction.equals(FILE_BAK_ACTION_DELETE)) {
            deleteFile(localPath + "/" + fileName);
        } else if (fileBakAction.equals(FILE_BAK_ACTION_MOVE)) {
            moveFile(localPath + "/" + fileName, bakPath + "/" + fileName);
        }
    }

    private void deleteFile(String filePath) throws DatakitException {
        try {
            // 创建 ProcessBuilder 对象，并设置命令及参数
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", "rm " + filePath);
            // 启动进程并等待命令执行完成
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            // 检查命令是否执行成功
            if (exitCode == 0) {
                throw new DatakitException("deleteFile", "删除文件[" + filePath + "]失败！");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void moveFile(String sourceFilePath, String targetFilePath) throws DatakitException {
        try {
            // 创建 ProcessBuilder 对象，并设置命令及参数
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", "mv " + sourceFilePath + " " + targetFilePath);
            // 启动进程并等待命令执行完成
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            // 检查命令是否执行成功
            if (exitCode == 0) {
                throw new DatakitException("moveFile", "备份文件[" + sourceFilePath + "]失败！");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析文件中数据送入内存队列中
     * 注：解析文件只能单线程处理
     *
     * @param filePath 文件路径
     * @param fileName 文件名称
     * @throws DatakitException,IOException
     */
    private void parseFileLineInputQueue(final Integer fileType, final String filePath, final String fileName, final Task task) throws DatakitException, IOException {
        fileList.stream()
                .filter(x -> x.fileCurrent(FileTypeEnum.of(fileType)))
                .findFirst()
                .orElseThrow(() -> new DatakitException("parseFileLineInputQueue", ERROR_MATCH_FILE_OBJET_BY_FILE_TYPE_FAIL))
                .parseFileLineDataToQueue(filePath, fileName, task);
    }

    @Override
    public Object makeObjectFormat(Long objectFormatId, Boolean tableMetaFlag) throws DatakitException {
        return makeFileFormat(objectFormatId);
    }

    /**
     * 组装文件对象, 包括: 文件头、文件体、文件尾
     *
     * @param fileFormatId 文件对象标识
     * @throws DatakitException
     */
    private FileFormat makeFileFormat(Long fileFormatId) throws DatakitException {
        //获取文件定义
        FileFormat fileFormat = fileDao.selectFileFormat(fileFormatId);
        Optional.ofNullable(fileFormat).orElseThrow(() -> new DatakitException("makeFileFormat", "查询文件对象定义报错!"));

        //获取文件过滤规则
        fileFormat.setFileFilterList(fileDao.selectFileFilter(fileFormatId));
        if (!CollectionUtils.isEmpty(fileFormat.getFileFilterList())) {
            Map<Integer, List<FileFilter>> fileFilterMap = new HashMap<>();
            fileFormat.getFileFilterList().forEach(iterator -> fileFilterMap.computeIfAbsent(iterator.getSymbolGroup(), key -> new ArrayList<>()).add(iterator));
            fileFormat.setFileFilterMap(fileFilterMap);
        }

        //获取文件体
        makeFileFormatBody(fileFormat);

        //获取文件特殊行
        makeFileFormatSpecial(fileFormat);

        return fileFormat;
    }

    private void makeFileFormatSpecial(FileFormat fileFormat) {
        //get file line
        List<FileSpecial> fileSpecialList = fileDao.selectFileSpecialByFileFormatId(fileFormat.getFileFormatId());
        if (!CollectionUtils.isEmpty(fileSpecialList)) {
            //get file headline fields
            for (FileSpecial iterator : fileSpecialList) {
                List<FileField> fileFieldList = fileDao.selectFileField(DatakitConstant.FILE_LINE_SPECIAL, iterator.getFileSpecialId());
                iterator.setFileFieldList(fileFieldList);
            }

            //just only one head
            fileFormat.setFileSpecialList(fileSpecialList);

            //初始化用于数量累加字段
            fileFormat.setBodyHaveSumFieldMap(new HashMap<>());
            Set<String> sumFieldNameSet = new HashSet<>();
            fileSpecialList.forEach(x -> {
                x.getFileFieldList().forEach(y -> {
                    if (!StringUtils.isEmpty(y.getSumFieldName())) {
                        sumFieldNameSet.add(y.getSumFieldName().toUpperCase());
                    }
                });
            });

            if (!CollectionUtils.isEmpty(sumFieldNameSet) && !CollectionUtils.isEmpty(fileFormat.getFileBody().getFileFieldList())) {
                fileFormat.getFileBody().getFileFieldList().forEach(x -> {
                    if (sumFieldNameSet.contains(x.getFieldName().toUpperCase())) {
                        fileFormat.getBodyHaveSumFieldMap().put(x.getFileFieldId(), 0L);
                    }
                });
            }
        }
    }

    private void makeFileFormatBody(FileFormat fileFormat) {
        //get file body
        List<FileBody> fileBodyList = fileDao.selectFileBodyByFileFormatId(fileFormat.getFileFormatId());
        if (!CollectionUtils.isEmpty(fileBodyList)) {
            //get file body line fields
            for (FileBody iterator : fileBodyList) {
                List<FileField> fileFieldList = fileDao.selectFileField(DatakitConstant.FILE_LINE_BODY, iterator.getFileBodyId());
                iterator.setFileFieldList(fileFieldList);
            }

            //just only one body
            fileFormat.setFileBody(fileBodyList.get(0));
        }
    }

    public void matchDataQueueDoBusiness(List<QueueData> objectDataList, final TaskInstance taskInstance) throws DatakitException {
        switch (ObjectTaskEnum.of(taskInstance.getTask().getTaskType())) {
            case OBJECT_TASK_TYPE_DATA_EXCHANGE:
                objectDataIntoFile(objectDataList, taskInstance.getTask());
                break;
            case OBJECT_TASK_TYPE_DATA_COMPARE:
                super.objectDataCompareToMapCache(objectDataList, taskInstance.getTaskId(), taskInstance.getTaskInstanceId());
                break;
            default:
                break;
        }
    }

    /**
     * 对象数据写入文件
     *
     * @param queueDataList
     * @param task
     * @throws DatakitException
     */
    private void objectDataIntoFile(List<QueueData> queueDataList, final Task task) throws DatakitException {
        bodyDataWriteIntoFile(changeObjectDataByFileFormatB(queueDataList, task), (FileFormat) task.getBObject());
    }


    /**
     * 获取内存队列中标准格式数据转换成目标端(B)业务对象文件格式的数据
     *
     * @param queueDataList
     * @param task
     * @return
     * @throws DatakitException
     */
    private List<List<String>> changeObjectDataByFileFormatB(List<QueueData> queueDataList, final Task task) throws DatakitException {
        List<List<String>> dataFileLineList = new ArrayList<>();
        FileFormat fileFormat = (FileFormat) task.getBObject();

        for (QueueData iterator : queueDataList) {
            //循环文件格式字段，根据字段匹配映射字段值
            List<String> fileLineList = new ArrayList<>();
            for (FileField fileFieldIterator : fileFormat.getFileBody().getFileFieldList()) {
                List<TaskFieldRela> taskFieldRelaList = task.getTaskFieldRelaList().
                        stream().filter(x -> x.getBObjectFieldId().equals(fileFieldIterator.getFileFieldId())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(taskFieldRelaList)) {
                    //可能存在一个表部分字段没有参与关联业务处理,所有匹配不到
                    continue;
                }

                if (!iterator.getDataMap().containsKey(taskFieldRelaList.get(0).getTaskFieldRelaId())) {
                    throw new DatakitException("changeObjectDataByFileFormatB", "根据字段映射标识匹配对象数据失败!");
                }

                fileLineList.add(String.valueOf(iterator.getDataMap().get(taskFieldRelaList.get(0).getTaskFieldRelaId())));
            }

            dataFileLineList.add(fileLineList);
        }

        return dataFileLineList;
    }

    private void bodyDataWriteIntoFile(List<List<String>> dataObjectList, final FileFormat fileFormat) throws DatakitException {
        fileList.stream()
                .filter(x -> x.fileCurrent(FileTypeEnum.of(fileFormat.getFileType())))
                .findFirst()
                .orElseThrow(() -> new DatakitException("dataWriteIntoFile", ERROR_MATCH_FILE_OBJET_BY_FILE_TYPE_FAIL))
                .bodyDataWriteIntoFile(dataObjectList, fileFormat);
    }

    @Override
    public void doBusinessLoadDataToMapCache(final Task task) throws DatakitException {
        parseFileLineToMapCache(task);
    }

    private void parseFileLineToMapCache(final Task task) throws DatakitException {
        FileFormat fileFormat = (FileFormat) task.getBObject();
        List<String> fileNameList = getFileNameList(fileFormat);
        for (String fileName : fileNameList) {
            fileList.stream()
                    .filter(x -> x.fileCurrent(FileTypeEnum.of(fileFormat.getFileType())))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("parseFileLineToMapCache", ERROR_MATCH_FILE_OBJET_BY_FILE_TYPE_FAIL))
                    .parseFileLineDataToMapCache(fileFormat.getLocalPath(), fileName, task);
        }
    }

    public void doBusinessCheck(final FileFormat fileFormat) throws DatakitException {
        List<String> fileNameList = getFileNameList(fileFormat);
        for (String fileName : fileNameList) {
            fileList.stream()
                    .filter(x -> x.fileCurrent(FileTypeEnum.of(fileFormat.getFileType())))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("doBusinessCheck", ERROR_MATCH_FILE_OBJET_BY_FILE_TYPE_FAIL))
                    .checkFileLineData(fileFormat.getLocalPath(), fileName, fileFormat);
        }
    }

    public void doBusinessCreateTable(final Object objectFormat) throws DatakitException {
        //todo building
        throw new DatakitException("doBusinessCreateTable", "文件暂不支持处理");
    }

    public Map<Long, String> genFieldIdNameMap(final Object objectFormat) {
        FileFormat fileFormat = (FileFormat) objectFormat;
        Map<Long, String> fieldIdNameMap = new HashMap<>(fileFormat.getFileBody().getFileFieldList().size());
        fileFormat.getFileBody().getFileFieldList().forEach(x -> fieldIdNameMap.put(x.getFileFieldId(), x.getFieldName()));
        return fieldIdNameMap;
    }

    public void repairData(final Integer repairAction, final Task task, final QueueData queueData) throws DatakitException {
        //todo building
        throw new DatakitException("repairData", "文件暂不支持处理");
    }

    public Map<Long, String> queryFieldIDNameMap(Long objectId) {
        List<FileField> fileFieldList = fileDao.selectFileFieldByFileFormatId(objectId);
        if (!CollectionUtils.isEmpty(fileFieldList)) {
            Map<Long, String> fieldNameMap = new HashMap<>(fileFieldList.size());
            fileFieldList.forEach(x -> {
                if (x.getBelongFlag().equals(FILE_LINE_BODY)) {
                    fieldNameMap.put(x.getFileFieldId(), x.getFieldName());
                }
            });
            return fieldNameMap;
        }
        return null;
    }

    public String getObjectName(final Object objectFormat) {
        if (!Objects.isNull(objectFormat)) {
            FileFormat fileFormat = (FileFormat) objectFormat;
            return fileFormat.getFileNameFormat();
        }
        return null;
    }

    public String queryObjectName(final Long objectId) {
        if (!Objects.isNull(objectId)) {
            FileFormat fileFormat = fileDao.selectFileFormat(objectId);
            if (!Objects.isNull(fileFormat)) {
                return fileFormat.getFileNameFormat();
            }
        }
        return null;
    }

    public Integer getObjectOnLineFlag(final Object objectFormat) {
        if (!Objects.isNull(objectFormat)) {
            return ((FileFormat) objectFormat).getOnLineFlag();
        }
        return null;
    }

    public void renameFile(final String filePath, String oldFileName, String newFileName) {
        File oldFile = new File(filePath + "/" + oldFileName);
        if (oldFile.exists()) {
            File newFile = new File(filePath + "/" + newFileName);
            if (!oldFile.renameTo(newFile)) {
                log.error("文件重命名失败:oldName:{}=>newName:{}", oldFileName, newFileName);
            }
        }
    }

    public void followUpObject(final Long taskInstanceId, final Task task) throws DatakitException {
        FileFormat fileFormat = ((FileFormat) task.getBObject());

        //文件头行数据整理
        followUpFileHead(taskInstanceId, fileFormat, task.getSumFieldRelaIdMap());

        //文件行尾数据整理
        followUpFileEnd(taskInstanceId, fileFormat, task.getSumFieldRelaIdMap());

        //释放各种资源
        releaseResource(taskInstanceId, fileFormat);

        //修改临时名称为正式名称
        renameFile(fileFormat.getLocalPath(), "tmp." + fileFormat.getFileNameFormat(), fileFormat.getFileNameFormat());
    }

    public void followUpFileHead(final Long taskInstanceId, final FileFormat fileFormat, final Map<String, Long> sumFieldRelaIdMap) throws DatakitException {
        //文件头行数据整理
        if (fileFormat.getFileBody().getFixBeginLine() >= 1 && !CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            Map<Integer, List<String>> specialHeadDataObjectMap = new HashMap<>(fileFormat.getFileBody().getFixBeginLine());
            fileFormat.getFileSpecialList().forEach(iterator -> {
                if (iterator.getFixLinePosition() > 0) {
                    List<String> dataObjectList = specialFieldListToDataObjectList(taskInstanceId, sumFieldRelaIdMap, iterator.getFileFieldList());
                    if (!CollectionUtils.isEmpty(dataObjectList)) {
                        specialHeadDataObjectMap.put(iterator.getFixLinePosition(), dataObjectList);
                    }
                }
            });

            //这里不需要判断，,有可能存在空行继续处理
            fileSpecialHeadLine(specialHeadDataObjectMap, fileFormat);
        }
    }

    public void followUpFileEnd(final Long taskInstanceId, final FileFormat fileFormat, final Map<String, Long> sumFieldRelaIdMap) throws DatakitException {
        //文件行尾数据整理
        if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            Map<Long, List<String>> specialEndDataObjectMap = new HashMap<>();
            for (FileSpecial iterator : fileFormat.getFileSpecialList()) {
                if (iterator.getFixLinePosition().equals(-1)) {
                    List<String> dataObjectList = specialFieldListToDataObjectList(taskInstanceId, sumFieldRelaIdMap, iterator.getFileFieldList());
                    if (!CollectionUtils.isEmpty(dataObjectList)) {
                        specialEndDataObjectMap.put(iterator.getFileSpecialId(), dataObjectList);
                    }
                }
            }

            if (!CollectionUtils.isEmpty(specialEndDataObjectMap)) {
                fileSpecialEndLine(specialEndDataObjectMap, fileFormat);
            }
        }
    }


    List<String> specialFieldListToDataObjectList(final Long taskInstanceId, final Map<String, Long> sumFieldRelaIdMap, final List<FileField> fileFieldList) {
        List<String> dataObjectList = new ArrayList<>();
        for (FileField fileFieldIterator : fileFieldList) {
            if (!Objects.isNull(fileFieldIterator.getSumLineFlag()) &&
                    fileFieldIterator.getSumLineFlag().equals(1)) {
                dataObjectList.add(super.getDataSumCount(taskInstanceId).toString());
            } else if (!StringUtils.isEmpty(fileFieldIterator.getSumFieldName()) &&
                    !CollectionUtils.isEmpty(sumFieldRelaIdMap) &&
                    sumFieldRelaIdMap.containsKey(fileFieldIterator.getSumFieldName())) {

                dataObjectList.add(super.getDataSumAmount(sumFieldRelaIdMap.get(fileFieldIterator.getSumFieldName())).toString());
            }
        }
        return dataObjectList;
    }


    private void releaseResource(final Long taskInstanceId, final FileFormat fileFormat) throws DatakitException {
        fileList.stream()
                .filter(x -> x.fileCurrent(FileTypeEnum.of(fileFormat.getFileType())))
                .findFirst()
                .orElseThrow(() -> new DatakitException("releaseResource", ERROR_MATCH_FILE_OBJET_BY_FILE_TYPE_FAIL))
                .releaseResource(taskInstanceId, fileFormat);
    }

    private void fileSpecialHeadLine(Map<Integer, List<String>> specialHeadDataObjectMap, final FileFormat fileFormat) throws DatakitException {
        fileList.stream()
                .filter(x -> x.fileCurrent(FileTypeEnum.of(fileFormat.getFileType())))
                .findFirst()
                .orElseThrow(() -> new DatakitException("fileSpecialHeadLine", ERROR_MATCH_FILE_OBJET_BY_FILE_TYPE_FAIL))
                .fileSpecialHeadLine(specialHeadDataObjectMap, fileFormat);
    }

    private void fileSpecialEndLine(Map<Long, List<String>> specialEndDataObjectMap, final FileFormat fileFormat) throws DatakitException {
        fileList.stream()
                .filter(x -> x.fileCurrent(FileTypeEnum.of(fileFormat.getFileType())))
                .findFirst()
                .orElseThrow(() -> new DatakitException("fileSpecialEndLine", ERROR_MATCH_FILE_OBJET_BY_FILE_TYPE_FAIL))
                .fileSpecialEndLine(specialEndDataObjectMap, fileFormat);
    }

    public Map<String, Long> getObjectSumFieldRelIdList(final Task task) {
        Map<String, Long> sumFieldRelaIdMap = new HashMap<>();

        FileFormat fileFormat = (FileFormat) task.getBObject();
        if (CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            return sumFieldRelaIdMap;
        }

        //找到特殊行定义需要汇总金额字段
        fileFormat.getFileSpecialList().forEach(fileSpecialIterator -> {
            if (!CollectionUtils.isEmpty(fileSpecialIterator.getFileFieldList())) {
                getObjectSumFieldRelIdListBySpecialLine(fileSpecialIterator,
                        fileFormat.getFileBody().getFileFieldList(),
                        task.getTaskFieldRelaList(),
                        sumFieldRelaIdMap);
            }
        });

        return sumFieldRelaIdMap;
    }

    private void getObjectSumFieldRelIdListBySpecialLine(final FileSpecial fileSpecial, final List<FileField> fileBodyFieldList, final List<TaskFieldRela> taskFieldRelaList, Map<String, Long> sumFieldRelaIdMap) {
        //找到特殊行定义需要汇总金额字段
        fileSpecial.getFileFieldList().forEach(fileFieldIterator -> {
            if (!StringUtils.isEmpty(fileFieldIterator.getSumFieldName())) {
                Long fileFieldId = getLineBodyFileFieldIdByFieldName(fileFieldIterator.getSumFieldName(), fileBodyFieldList);
                if (fileFieldId != null) {
                    //找任务关联ID
                    for (TaskFieldRela taskFieldRelaIterator : taskFieldRelaList) {
                        if (taskFieldRelaIterator.getBObjectFieldId().equals(fileFieldId)) {
                            sumFieldRelaIdMap.put(fileFieldIterator.getSumFieldName(), taskFieldRelaIterator.getTaskFieldRelaId());
                            break;
                        }
                    }
                }
            }
        });
    }

    Long getLineBodyFileFieldIdByFieldName(final String fieldName, final List<FileField> fileBodyFieldList) {
        for (FileField bodyFileFieldIterator : fileBodyFieldList) {
            if (bodyFileFieldIterator.getFieldName().equalsIgnoreCase(fieldName)) {
                return bodyFileFieldIterator.getFileFieldId();
            }
        }
        return null;
    }
}