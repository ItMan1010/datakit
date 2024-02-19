package com.itman.datakit.admin.service.impl;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.itman.datakit.admin.common.constants.DataBaseTypeEnum;
import com.itman.datakit.admin.common.constants.FileTypeEnum;
import com.itman.datakit.admin.common.dataroute.DruidConfig;
import com.itman.datakit.admin.common.entity.*;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.component.CommonProcess;
import com.itman.datakit.admin.component.DatakitProcess;
import com.itman.datakit.admin.component.impl.ObjectFileProcessImpl;
import com.itman.datakit.admin.dao.ObjectFileDao;
import com.itman.datakit.admin.plugins.ITable;
import com.itman.datakit.admin.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.enums.DatakitObjectEnum.DATAKIT_OBJECT_FILE;
import static com.itman.datakit.admin.common.util.ChangeNameUtil.*;
import static com.itman.datakit.admin.common.util.FTPUtil.getFileList;


@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final ObjectFileDao objectFileDao;
    private final DatakitProcess datakitProcess;
    private final ObjectFileProcessImpl fileFileProcessImpl;
    private final CommonProcess commonProcess;
    private final List<ITable> dataBaseList;
    private final DruidConfig druidConfig;

    public FileFormat makeFileObject(Long fileObjectId) throws DatakitException {
        return (FileFormat) datakitProcess.makeDatakitObject(DATAKIT_OBJECT_FILE.getValue(), fileObjectId, true);
    }

    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void createFileInstance(FileFormat fileFormat) throws DatakitException {
        if (StringUtils.isEmpty(fileFormat.getFileNameFormat())) {
            throw new DatakitException("createFileInstance001", " FileName is null ! ");
        }

        fileFormat.setFileFormatId(commonProcess.querySequence());
        fileFormat.setOnLineFlag(ON_LINE_FLAG_OFF_LINE);
        if (!objectFileDao.insertFile(fileFormat, getSqlSystemDate()).equals(1)) {
            throw new DatakitException("createFileInstance002", " insertFile error ! ");
        }

        fileFormat.getFileBody().setFileFormatId(fileFormat.getFileFormatId());
        fileFormat.getFileBody().setFileBodyId(commonProcess.querySequence());
        if (!objectFileDao.insertFileBody(fileFormat.getFileBody(), getSqlSystemDate()).equals(1)) {
            throw new DatakitException("createFileInstance003", " insertFileBody error ! ");
        }

        if (CollectionUtils.isEmpty(fileFormat.getFileBody().getFileFieldList())) {
            throw new DatakitException("createFileInstance004", " fileFieldList is null ! ");
        }

        modifyFileFieldList(FILE_LINE_BODY, fileFormat.getFileBody());

        if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            for (FileSpecial iterator : fileFormat.getFileSpecialList()) {
                if (!iterator.getSplitFlag().equals(-1)) {
                    iterator.setFileFormatId(fileFormat.getFileFormatId());
                    iterator.setFileSpecialId(commonProcess.querySequence());
                    //这个和下面顺序不能颠倒，该函数需要下面插入需要字段标识值
                    modifyFileFieldList(FILE_LINE_SPECIAL, iterator);

                    if (!objectFileDao.insertFileSpecial(iterator, getSqlSystemDate()).equals(1)) {
                        throw new DatakitException("createFileInstance005", " insertFileSpecial error ! ");
                    }
                }
            }
        }
    }

    void modifyFileFieldList(Integer belongFlag, Object fileFieldObject) throws DatakitException {
        List<FileField> fileFieldList = new ArrayList<>();
        Long fileFormatId = null;
        Long belongId = null;
        if (belongFlag.equals(FILE_LINE_SPECIAL)) {
            FileSpecial fileLineSpecial = (FileSpecial) fileFieldObject;
            fileFieldList = fileLineSpecial.getFileFieldList();
            fileFormatId = fileLineSpecial.getFileFormatId();
            belongId = fileLineSpecial.getFileSpecialId();
        } else if (belongFlag.equals(FILE_LINE_BODY)) {
            FileBody fileLineBody = (FileBody) fileFieldObject;
            fileFieldList = fileLineBody.getFileFieldList();
            fileFormatId = fileLineBody.getFileFormatId();
            belongId = fileLineBody.getFileBodyId();
        }

        modifyFileFieldList(belongFlag, belongId, fileFormatId, fileFieldList);
    }

    void modifyFileFieldList(Integer belongFlag, Long belongId, Long fileFormatId, List<FileField> fileFieldList) throws DatakitException {

        deleteFileFieldList(belongFlag, belongId, fileFieldList);

        if (!CollectionUtils.isEmpty(fileFieldList)) {
            List<FileField> fileFieldListInsert = new ArrayList<>();
            List<FileField> fileFieldListUpdate = new ArrayList<>();

            getFileFieldListInsertAndUpdate(belongFlag, belongId, fileFormatId, fileFieldList, fileFieldListInsert, fileFieldListUpdate);

            if (!CollectionUtils.isEmpty(fileFieldListInsert)) {
                if (!objectFileDao.insertFileFieldList(druidConfig.getUrlDbType("db0"), fileFieldListInsert, getSqlSystemDate()).equals(fileFieldListInsert.size())) {
                    throw new DatakitException("modifyFileFieldList", " insertFileFieldList error ! ");
                }
            }

            if (!CollectionUtils.isEmpty(fileFieldListUpdate)) {
                for (FileField iterator : fileFieldListUpdate) {
                    objectFileDao.updateFileField(iterator);
                }
            }
        }
    }

    private void getFileFieldListInsertAndUpdate(Integer belongFlag, Long belongId, Long fileFormatId, List<FileField> fileFieldList, List<FileField> fileFieldListInsert, List<FileField> fileFieldListUpdate) throws DatakitException {
        List<Integer> positionList = new ArrayList<>();
        for (FileField iterator : fileFieldList) {
            //在前端也校验一下
            if (positionList.contains(iterator.getPosition())) {
                throw new DatakitException("modifyFileFieldList001", "位移字段有重复,请确认");
            }

            positionList.add(iterator.getPosition());

            iterator.setFileFormatId(fileFormatId);
            iterator.setBelongFlag(belongFlag);
            iterator.setBelongId(belongId);

            if (iterator.getFileFieldId().equals(-1L)) {
                //新增字段
                iterator.setFileFieldId(commonProcess.querySequence());
                iterator.setPosition(iterator.getPosition());
                fileFieldListInsert.add(iterator);
            } else if (iterator.getFileFieldId() > 0L) {
                //更新
                fileFieldListUpdate.add(iterator);
            }
        }

    }


    void deleteFileFieldList(Integer belongFlag, Long belongId, List<FileField> fileFieldList) throws DatakitException {
        //先增加校验字段记录那些是要删除的
        List<FileField> fileFieldListDelete = new ArrayList<>();
        List<FileField> fileFieldListCompared = objectFileDao.selectFileField(belongFlag, belongId);
        for (FileField iterator : fileFieldListCompared) {
            if (fileFieldList == null ||
                    fileFieldList.stream().filter(x -> x.getFileFieldId().equals(iterator.getFileFieldId())).collect(Collectors.toList()).size() == 0) {
                fileFieldListDelete.add(iterator);
            }
        }

        for (FileField iterator : fileFieldListDelete) {
            if (!objectFileDao.deleteFileField(iterator.getFileFieldId()).equals(1)) {
                throw new DatakitException("deleteFileFieldList", "输出文件字段数据失败");
            }
        }
    }


    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void modifyFileInstance(FileFormat fileFormat) throws DatakitException {
        if (fileFormat.getFileFormatId() == null || fileFormat.getFileFormatId() <= 0L) {
            throw new DatakitException("updateFileInstance001", " fileFormatId is null ! ");
        }

        if (!objectFileDao.updateFile(fileFormat).equals(1)) {
            throw new DatakitException("updateFileInstance002", " updateFile error ! ");
        }

        fileFormat.getFileBody().setFileFormatId(fileFormat.getFileFormatId());
        if (fileFormat.getFileBody().getFileBodyId().equals(-1L)) {
            //body新增
            fileFormat.getFileBody().setFileBodyId(commonProcess.querySequence());
            modifyFileFieldList(FILE_LINE_BODY, fileFormat.getFileBody());
            if (!objectFileDao.insertFileBody(fileFormat.getFileBody(), getSqlSystemDate()).equals(1)) {
                throw new DatakitException("updateFileInstance003", " insertFileBody error ! ");
            }
        } else {
            //body修改
            if (!objectFileDao.updateFileBody(fileFormat.getFileBody()).equals(1)) {
                throw new DatakitException("updateFileInstance004", " updateFileBody error ! ");
            }

            modifyFileFieldList(FILE_LINE_BODY, fileFormat.getFileBody());
        }

        //特殊行判断删除操作
        modifyFileInstanceDelete(fileFormat);

        modifyFileInstanceUpdateOrInsert(fileFormat);
    }

    public void modifyFileInstanceDelete(FileFormat fileFormat) {
        //特殊行判断删除操作
        List<FileSpecial> fileLineSpecialListDelete = new ArrayList<>();
        List<FileSpecial> fileLineSpecialListCompared = objectFileDao.selectFileSpecialByFileFormatId(fileFormat.getFileFormatId());
        if (!CollectionUtils.isEmpty(fileLineSpecialListCompared)) {
            for (FileSpecial iterator : fileLineSpecialListCompared) {
                if (fileFormat.getFileSpecialList() == null ||
                        fileFormat.getFileSpecialList().stream().filter(x -> x.getFileSpecialId().equals(iterator.getFileSpecialId())).collect(Collectors.toList()).size() == 0) {
                    fileLineSpecialListDelete.add(iterator);
                }
            }
        }

        //特殊行delete操作
        if (!CollectionUtils.isEmpty(fileLineSpecialListDelete)) {
            //先删除特殊关联字段信息
            for (FileSpecial iterator : fileLineSpecialListDelete) {
                if (!CollectionUtils.isEmpty(iterator.getFileFieldList())) {
                    iterator.getFileFieldList().forEach(x -> {
                        if (x.getFileFieldId() > 0L) {
                            objectFileDao.deleteFileField(x.getFileFieldId());
                        }
                    });
                }

                //删除特殊行
                objectFileDao.deleteFileSpecialById(iterator.getFileSpecialId());
            }
        }
    }

    public void modifyFileInstanceUpdateOrInsert(FileFormat fileFormat) throws DatakitException {
        if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            for (FileSpecial iterator : fileFormat.getFileSpecialList()) {
                iterator.setFileFormatId(fileFormat.getFileFormatId());
                if (iterator.getFileSpecialId().equals(-1L)) {
                    iterator.setFileSpecialId(commonProcess.querySequence());
                    //这个和下面顺序不能颠倒，该函数需要下面插入需要字段标识值
                    modifyFileFieldList(FILE_LINE_SPECIAL, iterator);

                    if (!objectFileDao.insertFileSpecial(iterator, getSqlSystemDate()).equals(1)) {
                        throw new DatakitException("updateFileInstance005", " insertFileSpecial fail");
                    }
                } else {
                    if (!objectFileDao.updateFileSpecial(iterator).equals(1)) {
                        throw new DatakitException("updateFileInstance006", " updateFileSpecial fail");
                    }
                    modifyFileFieldList(FILE_LINE_SPECIAL, iterator);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void deleteFileInstance(Long fileFormatId) {
        objectFileDao.deleteFileFieldByFileFormatId(fileFormatId);
        objectFileDao.deleteFileFilterByFileFormatId(fileFormatId);
        objectFileDao.deleteFileBodyByFileFormatId(fileFormatId);
        objectFileDao.deleteFileSpecialByFileFormatId(fileFormatId);
        objectFileDao.deleteFileFormatById(fileFormatId);
    }

    @Override
    @Transactional(rollbackFor = DatakitException.class)
    public void copyFileInstance(Long fileFormatId) throws DatakitException {
        FileFormat fileFormat = makeFileObject(fileFormatId);
        fileFormat.setFileNameFormat("copy_" + fileFormat.getFileNameFormat());
        if (!CollectionUtils.isEmpty(fileFormat.getFileBody().getFileFieldList())) {
            fileFormat.getFileBody().getFileFieldList().forEach(x -> x.setFileFieldId(-1L));
        }

        if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            fileFormat.getFileSpecialList().forEach(x -> {
                if (!CollectionUtils.isEmpty(x.getFileFieldList())) {
                    x.getFileFieldList().forEach(y -> y.setFileFieldId(-1L));
                }
            });
        }
        createFileInstance(fileFormat);
    }

    @Override
    public PageInfo<FileFormat> selectFileFormatByOnLineFlagPage(Integer pageNum, Integer PageSize, Integer onLineFlag) {
        //开启分页功能，设置每页显示条数
        PageMethod.startPage(pageNum, PageSize);
        List<FileFormat> fileFormatList = objectFileDao.selectFileFormatByOnLineFlag(onLineFlag);
        if (!CollectionUtils.isEmpty(fileFormatList)) {
            fileFormatList.forEach(iterator -> {
                iterator.setOnLineFlagName(changeOnLineFlagName(iterator.getOnLineFlag()));
                iterator.setFileTypeName(FileTypeEnum.of(iterator.getFileType()).getName());
                iterator.setFileNameTypeName(changeFileNameTypeName(iterator.getFileNameType()));
                iterator.setFileBakActionName(changeFileBakActionName(iterator.getFileBakAction()));
            });
        }
        //获取分页相关数据，设置导航分页的页码数
        return new PageInfo<>(fileFormatList, 3);
    }


    @Override
    public void checkFileFormat(FileFormat fileFormat) throws DatakitException {
        fileFileProcessImpl.doBusinessCheck(fileFormat);
    }

    @Override
    public void testFtp(FileFormat fileFormat) throws DatakitException {
        List<String> ftpNameList = getFileList(fileFormat.getFtpHost(), Integer.parseInt(fileFormat.getFtpPort()), fileFormat.getFtpUser(), fileFormat.getFtpPasswd());
        if (CollectionUtils.isEmpty(ftpNameList)) {
            throw new DatakitException("doBusinessInputQueue", "获取ftp文件列表为空");
        }
    }

    @Override
    public void updateFileFormatOnLineFlagById(Long fileFormatId, Integer onLineFlag) {
        objectFileDao.updateFileFormatOnLineFlagById(fileFormatId, onLineFlag);
    }

    private String getSqlSystemDate() {
        try {
            return dataBaseList.stream()
                    .filter(x -> x.dataBaseCurrent(DataBaseTypeEnum.of(druidConfig.getUrlDbType("db0"))))
                    .findFirst()
                    .orElseThrow(() -> new DatakitException("getSqlSystemDate", "can not match DataBaseTypeEnum!"))
                    .getSqlSystemDate();
        } catch (DatakitException e) {
            throw new RuntimeException(e);
        }
    }
}
