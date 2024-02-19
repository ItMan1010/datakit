package com.itman.datakit.admin.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileUtil {
    private FileUtil() {
    }

    public static List<String> getFiles(String path) {
        List<String> fileNameList = new ArrayList<>();
        File file = new File(path);
        if (file.isDirectory()) {
            // 获取路径下的所有文件
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile() && !files[i].getName().substring(0, 1).equals(".")) {
                    fileNameList.add(files[i].getName());
                }
            }
        } else {
            fileNameList.add(file.getName());
        }
        return fileNameList;
    }
}
