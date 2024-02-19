package com.itman.datakit.admin.service;

import com.github.pagehelper.PageInfo;
import com.itman.datakit.admin.common.entity.FileFormat;
import com.itman.datakit.admin.common.exception.DatakitException;

public interface FileService {

    PageInfo<FileFormat> selectFileFormatByOnLineFlagPage(Integer pageNum, Integer PageSize, Integer state);

    void createFileInstance(FileFormat fileFormat) throws DatakitException;

    void modifyFileInstance(FileFormat fileFormat) throws DatakitException;

    void deleteFileInstance(Long fileFormatId) throws DatakitException;

    void copyFileInstance(Long fileFormatId) throws DatakitException;

    FileFormat makeFileObject(Long fileObjectId) throws DatakitException;

    void checkFileFormat(FileFormat fileFormat) throws DatakitException;

    void testFtp(FileFormat fileFormat) throws DatakitException;

    void updateFileFormatOnLineFlagById(Long tableFormatId, Integer onLineFlag);
}
