package com.itman.datakit.admin.plugins.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.itman.datakit.admin.common.constants.FileTypeEnum;
import com.itman.datakit.admin.common.entity.FileFormat;
import com.itman.datakit.admin.common.entity.FileLineData;
import com.itman.datakit.admin.common.entity.Task;
import com.itman.datakit.admin.common.exception.DatakitException;
import com.itman.datakit.admin.component.CommonProcess;
import com.itman.datakit.admin.plugins.AbstractFile;
import com.itman.datakit.admin.plugins.IFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;
import static com.itman.datakit.admin.common.constants.FileTypeEnum.EXCEL;

/**
 * excel文件对象对象
 *
 * @author: ItMan
 * @since: 2023/12/09  16:46
 */
@Slf4j
@Component
public class ExcelFileImpl extends AbstractFile implements IFile {
    private final CommonProcess commonProcess;

    public ExcelFileImpl(CommonProcess commonProcess) {
        this.commonProcess = commonProcess;
    }

    @Override
    public Boolean fileCurrent(FileTypeEnum fileTypeEnum) {
        return fileTypeEnum.equals(EXCEL);
    }

    @Override
    public void parseFileLineDataToQueue(final String filePath, final String fileName, final Task task) throws DatakitException {
        String pathFile = filePath + "/" + fileName;
        String fileNameSuffixes = pathFile.substring(pathFile.lastIndexOf(".") + 1);
        if (fileNameSuffixes.equals("xls")) {
            parseXlsFileLineDataToQueue(filePath, fileName, task);
        } else if (fileNameSuffixes.equals("xlsx")) {
            parseXlsxFileLineDataToQueue(filePath, fileName, task);
        }
    }

    public void parseXlsFileLineDataToQueue(final String filePath, final String fileName, final Task task) throws DatakitException {

        String pathFile = filePath + "/" + fileName;

        try (InputStream inputStream = new FileInputStream(pathFile); Workbook workbook = new HSSFWorkbook(inputStream)) {
            parseFileLineDataToQueue(workbook.getSheetAt(0), task);
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DatakitException("parseFileLineDataToQueue001", UN_KNOW_EXCEPTION);
        }
    }

    public void parseXlsxFileLineDataToQueue(final String filePath, final String fileName, final Task task) throws DatakitException {
        String pathFile = filePath + "/" + fileName;
        try (InputStream inputStream = new FileInputStream(pathFile); Workbook workbook = new XSSFWorkbook(inputStream)) {
            parseFileLineDataToQueue(workbook.getSheetAt(0), task);
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DatakitException("parseFileLineDataToQueue001", UN_KNOW_EXCEPTION);
        }
    }

    public void parseFileLineDataToQueue(Sheet sheet, final Task task) throws DatakitException {
        Map<Long, String> fieldIdNameMap = super.doFieldIdNameMap(task.getAObject());

        for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
            super.isWorkThreadFail();

            super.parseFileLineDataToQueue(i + 1, changRowToString(sheet.getRow(i)), task, fieldIdNameMap);
        }
    }

    @Override
    public void parseFileLineDataToMapCache(final String filePath, final String fileName, final Task task) throws DatakitException {
        String pathFile = filePath + "/" + fileName;
        String fileNameSuffixes = pathFile.substring(pathFile.lastIndexOf(".") + 1);

        if (fileNameSuffixes.equals("xls")) {
            parseXlsFileLineDataToMapCache(filePath, fileName, task);
        } else if (fileNameSuffixes.equals("xlsx")) {
            parseXlsxFileLineDataToMapCache(filePath, fileName, task);
        }
    }

    public void parseXlsFileLineDataToMapCache(final String filePath, final String fileName, final Task task) throws DatakitException {
        FileFormat fileFormat = (FileFormat) task.getBObject();
        String pathFile = filePath + "/" + fileName;
        try (InputStream inputStream = new FileInputStream(pathFile); Workbook workbook = new HSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                super.isWorkThreadFail();

                //解析文件行变成字段map
                super.parseFileLineToMapCache(task, i + 1, changRowToString(sheet.getRow(i)), fileFormat);
            }
        } catch (DatakitException cde) {
            log.error("cde={}", cde);
            throw new DatakitException("parseXlsFileLineDataToMapCache", cde.getMessage());
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DatakitException("parseXlsFileLineDataToMapCache", UN_KNOW_EXCEPTION);
        }
    }

    public void parseXlsxFileLineDataToMapCache(final String filePath, final String fileName, final Task task) throws DatakitException {
        FileFormat fileFormat = (FileFormat) task.getBObject();
        String pathFile = filePath + "/" + fileName;
        try (InputStream inputStream = new FileInputStream(pathFile); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                super.isWorkThreadFail();

                //解析文件行变成字段map
                super.parseFileLineToMapCache(task, i + 1, changRowToString(sheet.getRow(i)), fileFormat);
            }
        } catch (DatakitException cde) {
            log.error("cde={}", cde);
            throw new DatakitException("parseXlsxFileLineDataToMapCache", cde.getMessage());
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DatakitException("parseXlsxFileLineDataToMapCache", UN_KNOW_EXCEPTION);
        }
    }

    @Override
    public void checkFileLineData(final String filePath, final String fileName, final FileFormat fileFormat) throws DatakitException {
        String pathFile = filePath + "/" + fileName;
        String fileNameSuffixes = pathFile.substring(pathFile.lastIndexOf(".") + 1);
        if (fileNameSuffixes.equals("xls")) {
            checkXlsFileLineData(filePath, fileName, fileFormat);
        } else if (fileNameSuffixes.equals("xlsx")) {
            checkXlsxFileLineData(filePath, fileName, fileFormat);
        }
    }

    public void checkXlsFileLineData(final String filePath, final String fileName, final FileFormat fileFormat) throws DatakitException {
        Integer bodyLineSum = 0;
        List<FileLineData> fileLineSpecialDataList = new ArrayList<>();
        String pathFile = filePath + "/" + fileName;

        try (InputStream inputStream = new FileInputStream(pathFile); Workbook workbook = new HSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                bodyLineSum = super.checkFileLineData(bodyLineSum, i + 1, changRowToString(sheet.getRow(i)), fileLineSpecialDataList, fileFormat);
            }

            if (!CollectionUtils.isEmpty(fileLineSpecialDataList)) {
                //校验文件中文件头尾送的正文行数和实际计算总行数稽核
                super.checkFileLineSpecialData(bodyLineSum, fileLineSpecialDataList, fileFormat);
            }
        } catch (DatakitException dke) {
            throw new DatakitException("checkXlsFileLineData001", dke.getErrMsg());
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DatakitException("checkXlsFileLineData002", "打开文件失败");
        }
    }

    public void checkXlsxFileLineData(final String filePath, final String fileName, final FileFormat fileFormat) throws DatakitException {
        Integer bodyLineSum = 0;
        List<FileLineData> fileLineSpecialDataList = new ArrayList<>();
        String pathFile = filePath + "/" + fileName;

        try (InputStream inputStream = new FileInputStream(pathFile); Workbook workbook = new XSSFWorkbook(inputStream)) {
            for (int i = 0; i < workbook.getSheetAt(0).getPhysicalNumberOfRows(); i++) {
                bodyLineSum = super.checkFileLineData(bodyLineSum, i + 1, changRowToString(workbook.getSheetAt(0).getRow(i)), fileLineSpecialDataList, fileFormat);
            }

            if (!CollectionUtils.isEmpty(fileLineSpecialDataList)) {
                //校验文件中文件头尾送的正文行数和实际计算总行数稽核
                super.checkFileLineSpecialData(bodyLineSum, fileLineSpecialDataList, fileFormat);
            }
        } catch (DatakitException dke) {
            throw new DatakitException("checkXlsxFileLineData001", dke.getErrMsg());
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DatakitException("checkXlsxFileLineData002", "打开文件失败");
        }
    }

    private String changRowToString(Row row) {
        StringBuilder line = new StringBuilder();
        for (int index = 0; index < row.getPhysicalNumberOfCells(); index++) {
            Cell cell = row.getCell(index);
            switch (cell.getCellType()) {
                case STRING:
                    line.append(cell.getStringCellValue());
                    break;
                case NUMERIC:
                    line.append(cell.getNumericCellValue());
                    break;
                case BOOLEAN:
                    line.append(cell.getBooleanCellValue());
                    break;
                default:
            }
            line.append("|");
        }
        return line.toString();
    }

    private static List<List<String>> staticDataObjectList = new ArrayList<>(100000);
    private static final Object lock = new Object();

    private static void setDataObjectList(String fileNameSuffixes, Integer dataFixBeginLine, List<List<String>> dataObjectList) throws DatakitException {
        synchronized (lock) {
            if (dataFixBeginLine > 1 && CollectionUtils.isEmpty(staticDataObjectList)) {
                //保留特殊行位置
                List<List<String>> tmpDataObjectListList = new ArrayList<>(dataFixBeginLine);
                for (int i = 1; i < dataFixBeginLine; i++) {
                    List<String> tmpDataObjectList = new ArrayList<>(1);
                    tmpDataObjectList.add("");
                    tmpDataObjectListList.add(tmpDataObjectList);
                }

                if (CollectionUtils.isEmpty(staticDataObjectList) && !CollectionUtils.isEmpty(tmpDataObjectListList)) {
                    staticDataObjectList.addAll(tmpDataObjectListList);
                }
            }

            staticDataObjectList.addAll(dataObjectList);
            if (fileNameSuffixes.equals("xlsx") && staticDataObjectList.size() >= 1040000) {
                staticDataObjectList.clear();
                throw new DatakitException("setDataObjectList", "写入xlsx数量超过104w!");
            } else if (fileNameSuffixes.equals("xls") && staticDataObjectList.size() >= 65000) {
                staticDataObjectList.clear();
                throw new DatakitException("setDataObjectList", "写入xls数量超过6.5w!");
            }
        }
    }

    private static void setDataObjectSpecialLine(Integer specialLine, List<String> dataObjectList) {
        if (CollectionUtils.isEmpty(dataObjectList)) {
            return;
        }

        synchronized (lock) {
            //specialLineFlag=-1表是行尾巴
            if (!CollectionUtils.isEmpty(staticDataObjectList) && specialLine.equals(-1)) {
                staticDataObjectList.add(dataObjectList);
            } else if (!CollectionUtils.isEmpty(staticDataObjectList)) {
                staticDataObjectList.set((specialLine - 1), dataObjectList);
            }
        }
    }

    @Override
    public void bodyDataWriteIntoFile(List<List<String>> dataObjectList, final FileFormat fileFormat) throws DatakitException {
        String fileNameSuffixes = fileFormat.getFileNameFormat().substring(fileFormat.getFileNameFormat().lastIndexOf(".") + 1);
        setDataObjectList(fileNameSuffixes, fileFormat.getFileBody().getFixBeginLine(), dataObjectList);
    }

    @Override
    public void releaseResource(final Long taskInstanceId, final FileFormat fileFormat) {
        String filename = fileFormat.getLocalPath() + "/tmp." + fileFormat.getFileNameFormat();
        commonProcess.saveRunInfo(INFO_LEVEL_INFO, taskInstanceId, "数据开始写入excel文件中");
        synchronized (lock) {
            if (!CollectionUtils.isEmpty(staticDataObjectList)) {
                EasyExcelFactory.write(filename, null).sheet("sheet").doWrite(staticDataObjectList);
                staticDataObjectList.clear();
            }
        }
    }

    @Override
    public void fileSpecialHeadLine(Map<Integer, List<String>> specialHeadDataObjectMap, final FileFormat fileFormat) {
        for (int specialLine = 1; specialLine < fileFormat.getFileBody().getFixBeginLine(); specialLine++) {
            if (specialHeadDataObjectMap.containsKey(specialLine)) {
                setDataObjectSpecialLine(specialLine, specialHeadDataObjectMap.get(specialLine));
            } else {
                //空行
                List<String> dataList = new ArrayList<>(1);
                dataList.add("");
                setDataObjectSpecialLine(specialLine, dataList);
            }
        }
    }

    @Override
    public void fileSpecialEndLine(Map<Long, List<String>> specialEndDataObjectMap, final FileFormat fileFormat) {
        specialEndDataObjectMap.forEach((x, y) -> {
            setDataObjectSpecialLine(-1, y);
        });
    }
}
