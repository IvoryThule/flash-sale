package com.flashsale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Flash Sale System - 应用启动入口
 *
 * 排除 DataSourceAutoConfiguration / DruidDataSourceAutoConfigure：
 * 由 DataSourceConfig 手动配置读写分离双数据源（主库 + 从库）
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure.class
})
@EnableScheduling
public class FlashSaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSaleApplication.class, args);
    }
}
