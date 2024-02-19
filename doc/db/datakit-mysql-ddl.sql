# Datakit mysql数据库元数据执行脚本
# Copyright (c) 2023-present, ItMan.

create table if not exists `dk_file_format`(
 `file_format_id` bigint(18) not null,
 `file_type` int(2) default null comment '文件类型：1文本、2excel',
 `file_name_type` int(2) not null,
 `file_name_format` varchar(128) COLLATE utf8mb4_bin not null,
 `ftp_host` varchar(128) collate utf8mb4_bin default null comment '远程主机IP',
 `ftp_port` varchar(128) collate utf8mb4_bin default null comment '远程主机FTP端口',
 `ftp_user` varchar(128) collate utf8mb4_bin default null comment '远程主机FTP用户名',
 `ftp_passwd` varchar(128) collate utf8mb4_bin default null comment '远程主机FTP密码',
 `ftp_path` varchar(128) collate utf8mb4_bin default null comment '远程主机数据路径',
 `local_path` varchar(128) collate utf8mb4_bin default null,
 `file_bak_action` int(2) not null,
 `file_bak_path` varchar(128) collate utf8mb4_bin default null,
 `create_date` datetime not null,
 `on_line_flag` int(2) not null comment '发布标志：0下线、1在线',
 `state` int(2) not null,
 `state_date` datetime not null,
 primary key (`file_format_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='文件对象基本定义';

create table if not exists `dk_file_special`(
  `file_special_id` bigint(18) not null,
  `file_format_id` bigint(18) not null,
  `split_flag` int(2) not null,
  `fix_line_position` int(2) not null,
  `remark` varchar(128) collate utf8mb4_bin default null,
  `create_date` datetime not null,
  `state` int(2) not null,
  `state_date` datetime not null,
  primary key (`file_special_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='文件对象特殊行定义';

create table if not exists `dk_file_body`(
 `file_body_id` bigint(18) not null,
 `file_format_id` bigint(18) not null,
 `split_flag` bigint(18) not null,
 `fix_begin_line` int(2) default null,
 `fix_end_line` int(2) default null,
 `create_date` datetime not null,
 `state` int(2) not null,
 `state_date` datetime not null,
 primary key (`file_body_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='文件对象行体定义';

create table if not exists `dk_file_field`(
 `file_field_id` bigint(18) not null,
 `file_format_id` bigint(18) not null,
 `belong_flag` bigint(18) not null comment '1 文件特殊行、2 文件数据正文',
 `belong_id` int(2) not null,
 `field_name` varchar(128) collate utf8mb4_bin not null,
 `fix_width` int(2) default null,
 `position` int(2) default null,
 `sum_line_flag` int(2) default null  comment '用于特殊行定义：1表示该字段记录总行数',
 `sum_field_name` varchar(128) collate utf8mb4_bin default null comment '如果不为空,记录行体用于累加字段名称',
 `create_date` datetime not null,
 `state` int(2) not null,
 `state_date` datetime not null,
 primary key (`file_field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='文件对象字段定义';

create table if not exists `dk_file_filter`(
  `file_filter_id` bigint(18) not null,
  `file_format_id` bigint(18) not null,
  `file_field_id` bigint(18) not null,
  `symbol_id` int(2) not null comment '1(等于=)、2(大于号>)、3(小于号<)、4(大于等于号>=)、3(小于等于号<=)',
  `symbol_group` int(2) not null comment '同一组条件是与关系，不同组条件是或关系',
  `file_field_value` varchar(128) collate utf8mb4_bin not null,
  `create_date` datetime not null,
  `state` int(2) not null,
  `state_date` datetime not null,
    primary key (`file_filter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='文件过滤规则定义';

create table if not exists `dk_table_format`(
 `table_format_id` bigint(18) not null,
 `table_name` varchar(128) collate utf8mb4_bin not null,
 `table_where` varchar(128) collate utf8mb4_bin default null,
 `data_base` varchar(20) collate utf8mb4_bin default null,
 `create_date` datetime not null,
 `on_line_flag` int(2) not null comment '发布标志：0下线、1在线',
 `state` int(2) not null,
 `state_date` datetime not null,
  primary key (`table_format_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='表对象基本定义';

create table if not exists `dk_table_field`(
 `table_field_id` bigint(18) not null,
 `table_format_id` bigint(18) not null,
 `field_name` varchar(128) collate utf8mb4_bin not null,
 `field_type` int(2) not null,
 `field_length` int(2) default null,
 `null_able` int(2) not null comment '0不可空、1可空',
 `key_flag` int(2) not null comment '主键标识,0非主键、1主键',
 `create_date` datetime not null,
 `state` int(2) not null,
 `state_date` datetime not null,
  primary key (`table_field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='表对象字段定义';

create table if not exists `dk_task`(
  `task_id` bigint(18) not null,
  `task_type` int(2) not null comment '1数据转换、2数据比对、3数据稽核、4：数据平帐',
  `a_object_type` int(2) not null comment 'A业务对象类型',
  `a_object_id` bigint(18) not null comment 'A业务对象标识ID',
  `b_object_type` int(2) default null comment 'B业务对象类型',
  `b_object_id` bigint(18) default null comment 'B业务对象标识ID',
  `remark` varchar(128) collate utf8mb4_bin default null,
  `order_flag` int(2) default null,
  `repair_url` varchar(128) collate utf8mb4_bin default null comment '服务修复调用url地址',
  `create_date` datetime not null,
  `on_line_flag` int(2) not null comment '发布标志：0下线、1在线',
  `state` int(2) not null,
  `state_date` datetime not null,
  primary key (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='任务规则定义表';

create table if not exists `dk_task_field_rela`(
  `task_field_rela_id` bigint(18) not null,
  `task_id` bigint(18) not null,
  `a_object_field_id` bigint(18) not null,
  `b_object_field_id` bigint(18) not null,
  `compare_flag` int(2) default null comment '可比较标志:1可参与比较、0或空不参与比较',
  `create_date` datetime not null,
  `state` int(2) not null,
  `state_date` datetime not null,
  primary key (`task_field_rela_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='任务业务对象字段关联';

create table if not exists `dk_task_instance` (
  `task_instance_id` bigint(18) not null,
  `task_id` bigint(18) not null,
  `task_msg` varchar(128) collate utf8mb4_bin default null,
  `host_name` varchar(128) collate utf8mb4_bin default null,
  `host_ip` varchar(20) collate utf8mb4_bin default null,
  `create_date` datetime not null,
  `run_state` int(2) not null comment '运行状态：0等待运行、1运行中、2运行结束、3运行失败',
  `run_state_date` datetime not null,
  `state` int(2) not null,
  `state_date` datetime not null,
  primary key (`task_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='任务实例表';

create table if not exists `dk_task_result` (
    `task_result_id` bigint(18) not null comment '序列：seq_task_result_id',
    `task_instance_id` bigint(18) not null,
    `task_result` varchar(128) collate utf8mb4_bin default null comment '任务运行结果信息',
    `create_date` datetime not null,
    `compare_flag` int(2) default null comment '比较标志:1(A比B多)、2(A比B少)、3:(AB不一致)',
    `compare_info` varchar(128) collate utf8mb4_bin default null,
    `compare_data` varchar(1024) collate utf8mb4_bin default null,
    `repair_state` int(2) default null comment '平帐状态:0未修订、1修订成功、2修订失败',
    `repair_state_date` datetime default null,
    `repair_info` varchar(128) collate utf8mb4_bin default null,
    `task_repair_id` bigint(18) default null comment '修订任务ID',
    primary key (`task_result_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='任务实例结果表';

create table if not exists `dk_run_info` (
    `run_info_id` bigint(18) not null comment '序列：seq_run_info_id',
    `task_instance_id` bigint(18) not null comment '任务实例标识',
    `info_level` int(2) not null comment '日志等级:1(info级别)、2(error级别)',
    `run_info` varchar(4000) collate utf8mb4_bin default null comment '运行信息',
    `create_date` datetime not null,
    primary key (`run_info_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='任务实例运行信息表';

create table if not exists `dk_task_repair` (
    `task_repair_id` bigint(18) not null comment '序列：seq_task_repair_id',
    `task_instance_id` bigint(18) not null,
    `host_name` varchar(128) collate utf8mb4_bin default null,
    `host_ip` varchar(20) collate utf8mb4_bin default null,
    `create_date` datetime not null,
    `repair_state` int(2) not null comment '状态：0待处理、1处理中、2处理成功、3处理失败',
    `repair_state_date` datetime not null,
    `repair_msg` varchar(128) collate utf8mb4_bin default null,
    `state` int(2) not null,
    `state_date` datetime not null,
    primary key (`task_repair_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='任务数据修复表';

create table if not exists `dk_task_transform` (
    `task_transform_id` bigint(18) not null comment '序列：seq_task_transform_id',
    `host_name` varchar(128) collate utf8mb4_bin default null,
    `host_ip` varchar(20) collate utf8mb4_bin default null,
    `create_date` datetime not null,
    `transform_state` int(2) not null comment '状态：0待处理、1处理中、2处理成功、3处理失败',
    `transform_state_date` datetime not null,
    `transform_msg` varchar(128) collate utf8mb4_bin default null,
    `state` int(2) not null,
    `state_date` datetime not null,
    primary key (`task_transform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='库表倒换任务表';

create table if not exists `dk_table_transform` (
    `table_transform_id` bigint(18) not null comment '序列：seq_table_transform_id',
    `task_transform_id` bigint(18) not null comment '关联倒换任务标识',
    `create_date` datetime not null,
    `transform_state` int(2) not null comment '状态：0待处理、1成功、2失败',
    `transform_state_date` datetime not null,
    `transform_msg` varchar(128) collate utf8mb4_bin default null,
    `state` int(2) not null,
    `state_date` datetime not null,
    primary key (`table_transform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='库表倒换明细表表';

create table if not exists `dk_table_flow_format`(
    `flow_format_id` bigint(18) not null comment '序列：seq_flow_format_id',
    `flow_name` varchar(128) collate utf8mb4_bin not null,
    `source_data_base` varchar(20) collate utf8mb4_bin default null,
    `target_data_base` varchar(20) collate utf8mb4_bin default null,
    `create_date` datetime not null,
    `on_line_flag` int(2) not null comment '发布标志：0下线、1在线',
    `remark` varchar(256) collate utf8mb4_bin default null,
    `state` int(2) not null,
    `state_date` datetime not null,
    primary key (`flow_format_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='表流程对象定义';

create table if not exists `dk_table_flow_node` (
    `flow_node_id` bigint(18) NOT NULL COMMENT '序列:seq_flow_node_id',
    `flow_format_id` bigint(18) NOT NULL COMMENT '表流程定义标识',
    `table_name` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '表名称',
    `selected_field_name` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '条件查询字段名称',
    `selected_parent_field_name` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '父级数据字段名称，如果是顶层节点输入具体',
    `parent_flow_node_id` bigint(18) NOT NULL COMMENT '父级节点标识，-1表示顶层节点',
    `create_date` datetime not null,
    `state` int(2) not null,
    `state_date` datetime not null,
    PRIMARY KEY (`flow_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='表流程节点';

