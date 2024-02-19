package com.itman.datakit.admin.common.util;

import cn.hutool.extra.ftp.Ftp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static cn.hutool.extra.ftp.FtpMode.Passive;

/**
 * Ftp工具类
 *
 * @author: ItMan
 * @since: 2023/12/16  16:02
 */
@Slf4j
public class FTPUtil {
    /**
     * 创建ftp连接
     **/
    private static Ftp create(String host, Integer port, String user, String pwd) {
        Ftp ftp = null;
        try {
            port = Objects.isNull(port) ? Ftp.DEFAULT_PORT : port;
            ftp = StringUtils.isEmpty(user) && StringUtils.isEmpty(pwd)
                    ? new Ftp(host, port)
                    : new Ftp(host, port, user, pwd);

            //一定要设置模式，不然查询不了文件
            ftp.setMode(Passive);
        } catch (Exception e) {
            log.error("创建ftp链接失败：{}", e);
        }
        if (ftp == null) {
            throw new RuntimeException("连接FTP服务器失败,请检查配置是否正确");
        }
        return ftp;
    }

    /**
     * 关闭ftp连接
     **/
    private static void close(Ftp ftp) {
        try {
            if (!Objects.isNull(ftp)) {
                ftp.close();//断开连接
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ftp获取文件列表
     **/
    public static List<String> getFileList(String host, Integer port, String user, String pwd) {
        Ftp ftp = null;
        try {
            ftp = create(host, port, user, pwd);
            List<String> ftpFiles = ftp.ls(ftp.pwd());
            return ftpFiles;
        } catch (Exception e) {
            log.error("获取文件列表异常={}", e);
        } finally {
            close(ftp);
        }
        return null;
    }

    /**
     * ftp文件批量下载
     **/
    public static void download(String host, Integer port, String user, String pwd, String fileName, String localPath) {
        Ftp ftp = null;
        try {
            ftp = create(host, port, user, pwd);

            //新建文件
            String filePath = localPath + "/" + fileName;
            File file = new File(filePath);
            if (!file.exists()) {
                if (!Objects.isNull(file.getParentFile()) &&
                        !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    log.error("创建文件异常：{}", fileName);
                }
            }
            ftp.download(ftp.pwd(), fileName, file);
        } catch (Exception e) {
            log.error("下载文件异常：{}", e);
        } finally {
            close(ftp);
        }
    }
}
