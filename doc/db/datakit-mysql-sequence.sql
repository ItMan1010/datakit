# Datakit mysql序列执行脚本
# Copyright (c) 2023-present, ItMan.

CREATE TABLE `dk_sequence` (
  `seq_name` varchar(50) NOT NULL,
  `current_val` int NOT NULL,
  `increment_val` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`seq_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 comment='序列表';


-----MYSQL库实现序列生成函数
CREATE DEFINER=`root`@`%` FUNCTION `dk_currval`(v_seq_name VARCHAR(50)) RETURNS int
    DETERMINISTIC
BEGIN
 declare value integer;
 set value = 0;
select current_val into value  from dk_sequence where seq_name = v_seq_name;
return value;
END

CREATE DEFINER=`root`@`%` FUNCTION `dk_nextval`( v_seq_name VARCHAR(50)) RETURNS int
    DETERMINISTIC
BEGIN
update dk_sequence set current_val = current_val + increment_val  where seq_name = v_seq_name;
return dk_currval(v_seq_name);
END

INSERT INTO dk_sequence (seq_name, current_val, increment_val) VALUES ('seq_datakit_id', 1, 1);

