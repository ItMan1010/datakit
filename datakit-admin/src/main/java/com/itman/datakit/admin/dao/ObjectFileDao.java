package com.itman.datakit.admin.dao;

import com.itman.datakit.admin.common.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;

@Mapper
public interface ObjectFileDao {
    FileBody selectFileBodyById(@Param("fileBodyId") Long fileBodyId) throws DataAccessException;

    List<FileBody> selectFileBodyByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    List<FileSpecial> selectFileSpecialByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    List<FileField> selectFileField(@Param("belongFlag") Integer belongFlag, @Param("belongId") Long belongId) throws DataAccessException;

    List<FileField> selectFileFieldByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    List<FileField> selectFileFieldById(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    FileFormat selectFileFormat(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    List<FileFilter> selectFileFilter(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    List<FileFormat> selectFileFormatByOnLineFlag(@Param("onLineFlag") Integer onLineFlag) throws DataAccessException;

    Integer deleteFileFormatById(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    Integer deleteFileSpecialByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    Integer deleteFileSpecialById(@Param("fileSpecialId") Long fileSpecialId) throws DataAccessException;

    Integer deleteFileBodyByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    Integer deleteFileFilterByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    Integer deleteFileFieldByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    Integer insertFile(@Param("fileFormat") FileFormat fileFormat, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateFile(@Param("fileFormat") FileFormat fileFormat) throws DataAccessException;

    Integer insertFileBody(@Param("fileBody") FileBody fileBody, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateFileBody(@Param("fileBody") FileBody fileBody) throws DataAccessException;

    Integer insertFileFieldList(@Param("dataBaseType") String dataBaseType, @Param("fileFieldList") List<FileField> fileFieldList, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer deleteFileField(@Param("fileFieldId") Long fileFieldId) throws DataAccessException;

    Integer updateFileField(@Param("fileField") FileField fileField) throws DataAccessException;

    Integer insertFileSpecial(@Param("fileSpecial") FileSpecial fileSpecial, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateFileSpecial(@Param("fileSpecial") FileSpecial fileSpecial) throws DataAccessException;

    Integer updateFileFormatOnLineFlagById(@Param("fileFormatId") Long fileFormatId, @Param("onLineFlag") Integer onLineFlag) throws DataAccessException;

}
