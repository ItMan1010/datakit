package com.itman.datakit.admin.common.constants;

public class DatakitConstant {
    private DatakitConstant() {
        // 私有构造函数的代码逻辑
    }

    public static final String SUCCESS = "0";
    public static final String ERROR = "-1";
    public static final String WARNING = "-2";

    public static final String UN_KNOW_EXCEPTION = "unknow exception";

    /**
     * A线程池执行器
     */
    public static final String A_THREAD_POOL_EXECUTOR = "SourceExecutor";
    /**
     * B线程池执行器
     */
    public static final String B_THREAD_POOL_EXECUTOR = "TargetExecutor";

    /**
     * 定义不同文件结构体里对应字段
     * 1 文件特殊行
     * 2 文件数据正文
     */
    public static final Integer FILE_LINE_SPECIAL = 1;
    public static final Integer FILE_LINE_BODY = 2;
    /**
     * 行间隔字符标记符：(1)固定长度、(2)竖线|、(3)逗号，、(4)与符号&
     */
    public static final int SPLIT_FLAG_FIX_WIDTH = 1;
    public static final int SPLIT_FLAG_VERTICAL_LINE = 2;
    public static final int SPLIT_FLAG_COMMA = 3;
    public static final int SPLIT_FLAG_AND = 4;

    /**
     * 运行状态： 0等待运行、1运行中、2运行结束、3运行失败
     */
    public static final int TASK_INSTANCE_RUN_STATE_INIT = 0;
    public static final int TASK_INSTANCE_RUN_STATE_MOVE = 1;
    public static final int TASK_INSTANCE_RUN_STATE_FINISH = 2;
    public static final int TASK_INSTANCE_RUN_STATE_ERROR = 3;
    public static final String MAP_KEY_PREFIX = "mapKey=";
    /**
     * 数据对象比较结果标志
     * 1:A比B多;2:A比B少；3:AB指定字段值不一致
     */
    public static final int COMPARE_RESULT_FLAG_A_MORE_B = 1;
    public static final int COMPARE_RESULT_FLAG_A_LESS_B = 2;
    public static final int COMPARE_RESULT_FLAG_A_NOT_EQUAL_B = 3;

    /**
     * 数据对象比较结果标志
     * 0:待处理、1处理成功、2处理失败
     */
    public static final int COMPARE_REPAIR_STATE_UNDO = 0;
    public static final int COMPARE_REPAIR_STATE_SUCCESS = 1;
    public static final int COMPARE_REPAIR_STATE_FAIL = 2;

    //固定名称
    public static final int FILE_NAME_TYPE_FIX = 1;
    //正则表达式名称,可能匹配多个文件
    public static final int FILE_NAME_TYPE_PATTERN = 2;
    //可扩展文件名称
    public static final int FILE_NAME_TYPE_EXTEND = 3;

    /**
     * 符号标识：
     * 1(等于=)、
     * 2(大于号>)、
     * 3(小于号<)、
     * 4(大于等于号>=)、
     * 5(小于等于号<=)
     */
    public static final int SYMBOL_ID_EQUAL = 1;
    public static final int SYMBOL_ID_MORE = 2;
    public static final int SYMBOL_ID_LESS = 3;
    public static final int SYMBOL_ID_MORE_EQUAL = 4;
    public static final int SYMBOL_ID_LESS_EQUAL = 5;

    public static final int WEB_PAGE_SIZE = 10;

    //记录状态：0无效、1有效
    public static final int RECORD_STATE_INVALID = 0;
    public static final int RECORD_STATE_VALID = 1;

    //发布状态：0下线、1在线
    public static final int ON_LINE_FLAG_OFF_LINE = 0;
    public static final int ON_LINE_FLAG_ON_LINE = 1;

    //前端页面处理方式：1展示、2编辑
    public static final int VIEW_FLAG_SHOW = 1;
    public static final int VIEW_FLAG_EDIT = 2;

    /**
     * 比对标志，0未比较、1已比较
     */
    public static final int COMPARED_FLAG_NO = 0;
    public static final int COMPARED_FLAG_YES = 1;

    //可比较标志:1可参与比较、0或空不参与比较
    public static final int COMPARE_ABLE_FLAG_NO = 0;
    public static final int COMPARE_ABLE_FLAG_YES = 1;

    /**
     * 数据修复动作：1插入、2删除
     */
    public static final int REPAIR_DATA_ACTION_INSERT = 1;
    public static final int REPAIR_DATA_ACTION_DELETE = 2;

    /**
     * 文件处理完备份动作：1不处理、2删除、3备份目录
     */
    public static final int FILE_BAK_ACTION_NULL = 1;
    public static final int FILE_BAK_ACTION_DELETE = 2;
    public static final int FILE_BAK_ACTION_MOVE = 3;

    /**
     * 任务运行信息级别：1提示INfO、2报错ERROR、3监控MONITOR、4告警WARN
     */

    public static final int INFO_LEVEL_INFO = 1;
    public static final int INFO_LEVEL_ERROR = 2;
    public static final int INFO_LEVEL_MONITOR = 3;
    public static final int INFO_LEVEL_WARN = 4;

    /**
     * 字段类型分类：1整型、2字符串、3时间
     */
    public static final int TABLE_FIELD_TYPE_CLASSIFY_INTEGER = 1;
    public static final int TABLE_FIELD_TYPE_CLASSIFY_STRING = 2;
    public static final int TABLE_FIELD_TYPE_CLASSIFY_DATETIME = 3;

    /**
     * 字段是否需要指定长度：1指定、0不指定
     */
    public static final int TABLE_FIELD_TYPE_IF_LENGTH_NO = 0;
    public static final int TABLE_FIELD_TYPE_IF_LENGTH_YES = 1;

}
