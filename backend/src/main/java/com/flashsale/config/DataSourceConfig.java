package com.flashsale.config;

import com.flashsale.config.datasource.DataSourceType;
import com.flashsale.config.datasource.DynamicDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.alibaba.druid.pool.DruidDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 读写分离数据源配置
 *
 * 配置两个独立的 Druid 连接池：
 *  - writeDataSource：主库（处理写操作）
 *  - readDataSource ：从库（处理只读查询）
 *
 * 二者包装在 {@link DynamicDataSource} 中，由 {@link com.flashsale.config.datasource.DataSourceAspect}
 * 根据方法注解自动选择目标库。
 *
 * 注意：application.yml 中已通过 spring.autoconfigure.exclude 排除
 * DruidDataSourceAutoConfigure，避免与本手动配置冲突。
 */
@Configuration
public class DataSourceConfig {

    /** 主库（写）数据源 */
    @Bean("writeDataSource")
    @ConfigurationProperties("spring.datasource.master")
    public DataSource writeDataSource() {
        return new DruidDataSource();
    }

    /** 从库（读）数据源 */
    @Bean("readDataSource")
    @ConfigurationProperties("spring.datasource.slave")
    public DataSource readDataSource() {
        return new DruidDataSource();
    }

    /**
     * 动态路由数据源（作为全局 Primary DataSource）
     * MyBatis / Spring JDBC 均使用此 Bean。
     */
    @Bean
    @Primary
    public DynamicDataSource dynamicDataSource(
            @Qualifier("writeDataSource") DataSource write,
            @Qualifier("readDataSource")  DataSource read) {

        Map<Object, Object> targets = new HashMap<>();
        targets.put(DataSourceType.WRITE, write);
        targets.put(DataSourceType.READ,  read);

        DynamicDataSource ds = new DynamicDataSource();
        ds.setTargetDataSources(targets);
        ds.setDefaultTargetDataSource(write);   // 默认走主库
        return ds;
    }
}
