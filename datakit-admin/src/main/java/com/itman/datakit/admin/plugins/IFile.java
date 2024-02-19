package com.itman.datakit.admin.plugins;

import com.itman.datakit.admin.common.constants.FileTypeEnum;
import com.itman.datakit.admin.common.entity.FileFormat;
import com.itman.datakit.admin.common.entity.Task;
import com.itman.datakit.admin.common.exception.DatakitException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IFile {
    /**
     * 文件类型匹配
     */
    Boolean fileCurrent(FileTypeEnum fileTypeEnum);

    /**
     * 解析文件行数据发送内存队列
     *
     * @param filePath
     * @param fileName
     * @param task
     * @throws DatakitException
     * @throws IOException
     */
    void parseFileLineDataToQueue(final String filePath, final String fileName, final Task task) throws DatakitException;

    /**
     * 解析文件行数据缓存到Map结构中
     *
     * @param filePath
     * @param fileName
     * @param task
     * @throws DatakitException
     */
    void parseFileLineDataToMapCache(final String filePath, final String fileName, final Task task) throws DatakitException;

    void checkFileLineData(final String filePath, final String fileName, final FileFormat fileFormat) throws DatakitException;

    void bodyDataWriteIntoFile(List<List<String>> dataObjectList, final FileFormat fileFormat) throws DatakitException;

    void releaseResource(final Long taskInstanceId, final FileFormat fileFormat);

    void fileSpecialHeadLine(Map<Integer, List<String>> specialHeadDataObjectMap, final FileFormat fileFormat) throws DatakitException;

    void fileSpecialEndLine(Map<Long, List<String>> specialEndDataObjectMap, final FileFormat fileFormat) throws DatakitException;

}
