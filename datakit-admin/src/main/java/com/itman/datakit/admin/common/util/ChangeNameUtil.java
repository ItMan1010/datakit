package com.itman.datakit.admin.common.util;

import com.itman.datakit.admin.common.constants.FileTypeEnum;
import com.itman.datakit.admin.common.enums.DatakitObjectEnum;

import java.util.HashMap;
import java.util.Map;

import static com.itman.datakit.admin.common.constants.DatakitConstant.*;

/**
 * 编码名称转换工具类
 *
 * @author: ItMan
 * @since: 2023/09/03  12:33
 */
public class ChangeNameUtil {
    private ChangeNameUtil() {
    }

    private static final Map<Integer, String> ON_LINE_FLAG_NAME_MAP = new HashMap<>(2);
    private static final Map<Integer, String> TASK_INSTANCE_RUN_STATE_NAME_MAP = new HashMap<>(4);
    private static final Map<Integer, String> RESULT_FLAG_NAME_MAP = new HashMap<>(3);
    private static final Map<Integer, String> REPAIR_STATE_NAME_MAP = new HashMap<>(3);
    private static final Map<Integer, String> OBJECT_TYPE_NAME_MAP = new HashMap<>(3);
    public static final Map<Integer, String> FILE_NAME_TYPE_MAP = new HashMap<>(3);
    public static final Map<Integer, String> FILE_TYPE_MAP = new HashMap<>(2);

    public static final Map<Integer, String> FILE_BAK_ACTION_MAP = new HashMap<>(3);
    public static final Map<Integer, String> FILE_SPLIT_FLAG_MAP = new HashMap<>(4);
    private static final Map<Integer, String> INFO_LEVEL_NAME_MAP = new HashMap<>(2);

    static {
        ON_LINE_FLAG_NAME_MAP.put(ON_LINE_FLAG_OFF_LINE, "下线");
        ON_LINE_FLAG_NAME_MAP.put(ON_LINE_FLAG_ON_LINE, "上线");

        TASK_INSTANCE_RUN_STATE_NAME_MAP.put(TASK_INSTANCE_RUN_STATE_INIT, "待运行");
        TASK_INSTANCE_RUN_STATE_NAME_MAP.put(TASK_INSTANCE_RUN_STATE_MOVE, "运行中");
        TASK_INSTANCE_RUN_STATE_NAME_MAP.put(TASK_INSTANCE_RUN_STATE_FINISH, "运行结束");
        TASK_INSTANCE_RUN_STATE_NAME_MAP.put(TASK_INSTANCE_RUN_STATE_ERROR, "运行失败");

        RESULT_FLAG_NAME_MAP.put(COMPARE_RESULT_FLAG_A_MORE_B, "源比目标多");
        RESULT_FLAG_NAME_MAP.put(COMPARE_RESULT_FLAG_A_LESS_B, "源比目标少");
        RESULT_FLAG_NAME_MAP.put(COMPARE_RESULT_FLAG_A_NOT_EQUAL_B, "源目标不一致");

        REPAIR_STATE_NAME_MAP.put(COMPARE_REPAIR_STATE_UNDO, "待处理");
        REPAIR_STATE_NAME_MAP.put(COMPARE_REPAIR_STATE_SUCCESS, "处理成功");
        REPAIR_STATE_NAME_MAP.put(COMPARE_REPAIR_STATE_FAIL, "处理失败");

        for (DatakitObjectEnum type : DatakitObjectEnum.values()) {
            OBJECT_TYPE_NAME_MAP.put(type.getValue(), type.getDescription());
        }

        for (FileTypeEnum type : FileTypeEnum.values()) {
            FILE_TYPE_MAP.put(type.getId(), type.getName());
        }

        FILE_NAME_TYPE_MAP.put(1, "固定名称");
//        FILE_NAME_TYPE_MAP.put(2, "正则表达式");

        FILE_BAK_ACTION_MAP.put(1, "不处理");
        FILE_BAK_ACTION_MAP.put(2, "直接删除");
        FILE_BAK_ACTION_MAP.put(3, "备份目录");

        FILE_SPLIT_FLAG_MAP.put(SPLIT_FLAG_FIX_WIDTH, "固定长度");
        FILE_SPLIT_FLAG_MAP.put(SPLIT_FLAG_VERTICAL_LINE, "竖线|");
        FILE_SPLIT_FLAG_MAP.put(SPLIT_FLAG_COMMA, "逗号,");
        FILE_SPLIT_FLAG_MAP.put(SPLIT_FLAG_AND, "与符号&");

        INFO_LEVEL_NAME_MAP.put(INFO_LEVEL_INFO, "Info");
        INFO_LEVEL_NAME_MAP.put(INFO_LEVEL_ERROR, "error");
        INFO_LEVEL_NAME_MAP.put(INFO_LEVEL_MONITOR, "monitor");
        INFO_LEVEL_NAME_MAP.put(INFO_LEVEL_WARN, "warn");
    }

    public static String changeOnLineFlagName(Integer onLineFlag) {
        return ON_LINE_FLAG_NAME_MAP.containsKey(onLineFlag) ? ON_LINE_FLAG_NAME_MAP.get(onLineFlag) : "null";
    }

    public static String changeTaskInstanceRunStateName(Integer runState) {
        return TASK_INSTANCE_RUN_STATE_NAME_MAP.containsKey(runState) ? TASK_INSTANCE_RUN_STATE_NAME_MAP.get(runState) : "null";
    }

    public static String changeResultFlagName(Integer resultFlag) {
        return RESULT_FLAG_NAME_MAP.containsKey(resultFlag) ? RESULT_FLAG_NAME_MAP.get(resultFlag) : "null";
    }

    public static String changeRepairStateName(Integer repairState) {
        return REPAIR_STATE_NAME_MAP.containsKey(repairState) ? REPAIR_STATE_NAME_MAP.get(repairState) : "null";
    }

    public static String changeObjectTypeName(Integer objectType) {
        return OBJECT_TYPE_NAME_MAP.containsKey(objectType) ? OBJECT_TYPE_NAME_MAP.get(objectType) : "null";
    }

    public static String changeFileNameTypeName(Integer fileNameType) {
        return FILE_NAME_TYPE_MAP.containsKey(fileNameType) ? FILE_NAME_TYPE_MAP.get(fileNameType) : "null";
    }

    public static String changeFileBakActionName(Integer fileBakAction) {
        return FILE_BAK_ACTION_MAP.containsKey(fileBakAction) ? FILE_BAK_ACTION_MAP.get(fileBakAction) : "null";
    }

    public static String changeSplitFlagName(Integer splitFlag) {
        return FILE_SPLIT_FLAG_MAP.containsKey(splitFlag) ? FILE_SPLIT_FLAG_MAP.get(splitFlag) : "null";
    }

    public static String changeInfoLevelName(Integer infoLevel) {
        return INFO_LEVEL_NAME_MAP.containsKey(infoLevel) ? INFO_LEVEL_NAME_MAP.get(infoLevel) : "null";
    }
}
