package com.itman.datakit.admin.plugins.impl;

import com.itman.datakit.admin.common.constants.FileTypeEnum;
import com.itman.datakit.admin.common.entity.FileFormat;
import com.itman.datakit.admin.common.entity.FileLineData;
import com.itman.datakit.admin.common.entity.FileSpecial;
import com.itman.datakit.admin.common.entity.Task;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.plugins.AbstractFile;
import com.itman.datakit.admin.plugins.IFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.constants.FileTypeEnum.TEXT;

/**
 * 文本文件对象
 *
 * @author: ItMan
 * @since: 2023/12/09  16:42
 */
@Slf4j
@Component
public class TextFileImpl extends AbstractFile implements IFile {
    @Override
    public Boolean fileCurrent(FileTypeEnum fileTypeEnum) {
        return fileTypeEnum.equals(TEXT);
    }

    @Override
    public void parseFileLineDataToQueue(final String filePath, final String fileName, final Task task) throws DatakitException {
        Integer lineNumber = 0;
        Map<Long, String> fieldIdNameMap = super.doFieldIdNameMap(task.getAObject());

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath + "/" + fileName))) {
            String lineContent = reader.readLine();
            while (!StringUtils.isEmpty(lineContent)) {
                super.isWorkThreadFail();

                lineNumber++;

                super.parseFileLineDataToQueue(lineNumber, lineContent, task, fieldIdNameMap);

                // read next line
                lineContent = reader.readLine();
            }
        } catch (DatakitException cde) {
            log.error("cde={}", cde);
            throw new DatakitException("parseFileLineDataToQueue", cde.getMessage());
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DatakitException("parseFileLineDataToQueue", UN_KNOW_EXCEPTION);
        }
    }

    @Override
    public void parseFileLineDataToMapCache(final String filePath, final String fileName, final Task task) throws DatakitException {
        Integer lineNumber = 0;
        FileFormat fileFormat = (FileFormat) task.getBObject();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath + "/" + fileName))) {
            String lineContent = reader.readLine();
            while (!StringUtils.isEmpty(lineContent)) {
                lineNumber++;
                //解析文件行变成字段map
                super.parseFileLineToMapCache(task, lineNumber, lineContent, fileFormat);

                lineContent = reader.readLine();
            }
        } catch (DatakitException cde) {
            log.error("cde={}", cde);
            throw new DatakitException("parseFileLineInputMap", cde.getMessage());
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DatakitException("parseFileLineInputMap", UN_KNOW_EXCEPTION);
        }
    }

    /**
     * 文件格式数据稽核
     *
     * @param filePath
     * @param fileName
     * @param fileFormat
     * @throws DatakitException
     */
    @Override
    public void checkFileLineData(final String filePath, final String fileName, final FileFormat fileFormat) throws DatakitException {
        Integer lineNumber = 0;
        Integer bodyLineSum = 0;
        List<FileLineData> fileLineSpecialDataList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath + "/" + fileName))) {
            String lineContent = reader.readLine();
            while (!StringUtils.isEmpty(lineContent)) {
                lineNumber++;

                bodyLineSum = super.checkFileLineData(bodyLineSum, lineNumber, lineContent, fileLineSpecialDataList, fileFormat);

                lineContent = reader.readLine();
            }

            if (!CollectionUtils.isEmpty(fileLineSpecialDataList)) {
                //校验文件中文件头尾送的正文行数和实际计算总行数稽核
                super.checkFileLineSpecialData(bodyLineSum, fileLineSpecialDataList, fileFormat);
            }
        } catch (DatakitException dke) {
            log.error("dke={}", dke);
            throw new DatakitException("doBusinessCheck", dke.getErrMsg());
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DatakitException("doBusinessCheck", "文件格式稽核失败:" + e.getMessage());
        }
    }

    private String getSplitFlag(Integer splitFlagId) {
        switch (splitFlagId) {
            case SPLIT_FLAG_VERTICAL_LINE:
                return "|";
            case SPLIT_FLAG_COMMA:
                return ",";
            case SPLIT_FLAG_AND:
                return "&";
            default:
                break;
        }
        return null;
    }

    public static final String FILE_TEMP_PREFIX = "/tmp.";

    @Override
    public void bodyDataWriteIntoFile(List<List<String>> dataObjectList, final FileFormat fileFormat) throws DatakitException {
        String splitFlag = getSplitFlag(fileFormat.getFileBody().getSplitFlag());
        for (List<String> iterator : dataObjectList) {
            StringBuilder lineContent = new StringBuilder();
            lineContent.append(String.join(splitFlag, iterator));
            lineContent.append(splitFlag);
            lineContent.append("\n");
            bufferedWriterMethodByAppend(fileFormat.getLocalPath() + FILE_TEMP_PREFIX + fileFormat.getFileNameFormat(), lineContent.toString());
        }
    }

    private void bufferedWriterMethodByAppend(String filepath, String content) throws DatakitException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filepath, true))) {
            bufferedWriter.write(content);
        } catch (IOException e) {
            log.error("bufferedWriterMethodByAppend IOException=", e);
            throw new DatakitException("bufferedWriterMethodByAppend", "bufferedWriterMethodByAppend write IOException !");
        }
    }

    private void mergeFile(final String localPath, final String fileNameFormat) throws DatakitException {
        byte[] buffer = new byte[4096]; // 缓冲区大小可以根据需要进行调整
        File file1 = new File(localPath + "/tmp.merge." + fileNameFormat);
        File file2 = new File(localPath + FILE_TEMP_PREFIX + fileNameFormat);

        try (RandomAccessFile raf1 = new RandomAccessFile(file1, "rw"); RandomAccessFile raf2 = new RandomAccessFile(file2, "r");) {
            raf1.seek(file1.length());
            raf2.seek(0);
            while (raf2.length() > raf2.getFilePointer()) {
                raf1.write(buffer, 0, raf2.read(buffer));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void releaseResource(final Long taskInstanceId, final FileFormat fileFormat) {
        //todo
    }

    private void dataWriteIntoTmpFile(final List<String> dataObjectList, final Integer splitFlag, final String localPath, final String fileNameFormat) throws DatakitException {
        String splitStrFlag = getSplitFlag(splitFlag);
        StringBuilder lineContent = new StringBuilder();
        lineContent.append(String.join(splitStrFlag, dataObjectList));
        lineContent.append(splitStrFlag);
        lineContent.append("\n");
        bufferedWriterMethodByAppend(localPath + FILE_TEMP_PREFIX + fileNameFormat, lineContent.toString());
    }

    @Override
    public void fileSpecialHeadLine(Map<Integer, List<String>> specialHeadDataObjectMap, final FileFormat fileFormat) throws DatakitException {
        //文件行头先在tmp.merge.xxx临时文件存储
        //这里需要特殊行序列保持一致
        for (int specialLine = 1; specialLine < fileFormat.getFileBody().getFixBeginLine(); specialLine++) {
            Integer splitFlag = fileFormat.getFileBody().getSplitFlag();
            if (specialHeadDataObjectMap.containsKey(specialLine)) {
                for (FileSpecial iterator : fileFormat.getFileSpecialList()) {
                    if (iterator.getFixLinePosition().equals(specialLine)) {
                        splitFlag = iterator.getSplitFlag();
                        break;
                    }
                }
            }

            dataWriteIntoTmpFile(specialHeadDataObjectMap.get(specialLine), splitFlag, fileFormat.getLocalPath(), "merge." + fileFormat.getFileNameFormat());
        }
        //把之前写入数据文件tmp.xxx合并到tmp.merge.xxx文件上
        mergeFile(fileFormat.getLocalPath(), fileFormat.getFileNameFormat());

        //删除tmp.xxx文件
        super.deleteFile(fileFormat.getLocalPath(), "tmp." + fileFormat.getFileNameFormat());

        super.renameFile(fileFormat.getLocalPath(), "tmp.merge." + fileFormat.getFileNameFormat(), "tmp." + fileFormat.getFileNameFormat());
    }

    @Override
    public void fileSpecialEndLine(Map<Long, List<String>> specialEndDataObjectMap, final FileFormat fileFormat) throws DatakitException {
        for (FileSpecial iterator : fileFormat.getFileSpecialList()) {
            if (specialEndDataObjectMap.containsKey(iterator.getFileSpecialId())) {
                dataWriteIntoTmpFile(specialEndDataObjectMap.get(iterator.getFileSpecialId()), iterator.getSplitFlag(), fileFormat.getLocalPath(), fileFormat.getFileNameFormat());
            }
        }
    }
}
