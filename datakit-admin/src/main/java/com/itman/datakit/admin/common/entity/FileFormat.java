package com.itman.datakit.admin.common.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 文件定义对象
 *
 * @author: ItMan
 * @since: 2023/08/16  11:05
 */
@Data
public class FileFormat extends BaseEntity {
    /**
     * 文件唯一标识
     */
    private Long fileFormatId;
    /**
     * 文件类型:
     * 1 文本文件
     * 2 excel文件
     */
    private Integer fileType;
    /**
     * 文件名称类型:
     * 1 固定不变
     * 2 通过Java正则表达式定义，可能会匹配到多个文件
     * 3 扩展自定义规则
     */
    private Integer fileNameType;
    /**
     * 文件名称格式
     */
    private String fileNameFormat;
    /**
     * 文件特殊行，如行头、行尾,
     * 可能存在多个特殊行，如一个行头标识记录总是，一个行头标识记录金额总数
     */
    private List<FileSpecial> fileSpecialList;
    /**
     * 文件数据行体
     */
    private FileBody fileBody;
    /**
     * 远程文件所在主机IP
     */
    private String ftpHost;
    /**
     * 远程文件所在主机端口
     */
    private String ftpPort;
    /**
     * 远程主机FTP登录用户名
     */
    private String ftpUser;
    /**
     * 远程主机FTP登录密码
     */
    private String ftpPasswd;
    /**
     * 远程文件所在路径
     */
    private String ftpPath;
    /**
     * 本地文件所在路径
     */
    private String localPath;
    /**
     * 文件处理完备份动作：1不处理、2删除、3备份目录
     */
    private Integer fileBakAction;
    /**
     * 文件备份目录
     */
    private String fileBakPath;
    /**
     * 文件处理过滤条件
     */
    private List<FileFilter> fileFilterList;
    private Map<Integer, List<FileFilter>> fileFilterMap;
    /**
     * 文件正文用于做数量累计字段map结构
     * key:字段标识、value:累计值
     */
    private Map<Long, Long> bodyHaveSumFieldMap;
    /**
     * 发布标志：0下线、1在线
     */
    private Integer onLineFlag;

    //-------------界面扩展使用-------------------------------
    private String fileBakActionName;
    private String fileNameTypeName;
    private String fileTypeName;
    private String onLineFlagName;

    /**
     * 文件特殊行页面操作标识：1新增、2删除
     */
    private Integer fileSpecialAction;
    /**
     * 文件特殊行页面字段操作标识：1新增、2删除
     */
    private Integer fileSpecialFieldAction;
    /**
     * 页面操作特殊行索引值
     */
    private Integer fileSpecialIndex;
    /**
     * 页面操作特殊行字段索引值
     */
    private Integer fileSpecialFieldIndex;
    /**
     * 文件行体页面字段操作标识：1新增、2删除
     */
    private Integer fileBodyFieldAction;
    /**
     * 页面操作行体字段索引值
     */
    private Integer fileBodyFieldIndex;
    /**
     * 表操作标识：1保存、2校验
     */
    private Integer fileAction;
    //-------------界面扩展使用-------------------------------
}
