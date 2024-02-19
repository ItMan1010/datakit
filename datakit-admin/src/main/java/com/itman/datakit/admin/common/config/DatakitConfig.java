package com.itman.datakit.admin.common.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@Slf4j
public class DatakitConfig {
    /**
     * 源对象处理线程数,默认10
     */
    @Value("${datakit.aobject.actual-thread-count:10}")
    private Integer aThreadCount;
    /**
     * 目标对象处理线程数,默认10
     */
    @Value("${datakit.bobject.actual-thread-count:10}")
    private Integer bThreadCount;
    /**
     * 内存队列一次消费个数,默认500
     */
    @Value("${datakit.once-craw-num:500}")
    private Integer onceCrawNum;
    /**
     * 数据库表一次加载数据记录数,默认2000
     */
    @Value("${datakit.table_select_count:2000}")
    private Integer tableSelectCount;
}
