package com.itman.datakit.admin;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据工具启动类
 *
 * @author: ItMan
 * @since: 2023/08/16  11:05
 */
@Slf4j
@EnableFeignClients
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackages = {
        "com.itman.datakit.admin.common",
        "com.itman.datakit.admin.plugins",
        "com.itman.datakit.admin.dao",
        "com.itman.datakit.admin.component",
        "com.itman.datakit.admin.service",
        "com.itman.datakit.admin.controller"
})
@MapperScan({
        "com.itman.datakit.admin.dao"})
@EnableAsync
@EnableScheduling
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class DatakitApplication {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(DatakitApplication.class);
        springApplication.setBannerMode(Banner.Mode.OFF);
        ConfigurableApplicationContext run = springApplication.run(args);
        log.info("********启动DatakitApplication成功****************!");
    }
}
