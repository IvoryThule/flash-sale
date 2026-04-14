package com.flashsale.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 多数据源配置（读写分离）
 *
 * 数据源拓扑：
 *   master（主库）── 写操作  → flash-sale-mysql:3306
 *   slave （从库）── 读操作  → flash-sale-mysql-replica:3306
 *
 * 通过 RoutingDataSource + DataSourceAspect 在运行时自动切换。
 */
@Slf4j
@Configuration
@MapperScan("com.flashsale.mapper")
public class DataSourceConfig {

    // ==================== Master 配置 ====================
    @Value("${spring.datasource.master.url}")
    private String masterUrl;

    @Value("${spring.datasource.master.username}")
    private String masterUsername;

    @Value("${spring.datasource.master.password}")
    private String masterPassword;

    // ==================== Slave 配置 ====================
    @Value("${spring.datasource.slave.url}")
    private String slaveUrl;

    @Value("${spring.datasource.slave.username}")
    private String slaveUsername;

    @Value("${spring.datasource.slave.password}")
    private String slavePassword;

    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        DruidDataSource ds = buildDataSource(masterUrl, masterUsername, masterPassword);
        log.info("[DataSource] 主库初始化完成：{}", masterUrl);
        return ds;
    }

    @Bean(name = "slaveDataSource")
    public DataSource slaveDataSource() {
        DruidDataSource ds = buildDataSource(slaveUrl, slaveUsername, slavePassword);
        log.info("[DataSource] 从库初始化完成：{}", slaveUrl);
        return ds;
    }

    /**
     * 动态路由数据源（@Primary，替换 Spring Boot 自动配置的单数据源）
     */
    @Bean
    @Primary
    public DataSource routingDataSource() {
        RoutingDataSource routing = new RoutingDataSource();

        Map<Object, Object> targets = new HashMap<>();
        targets.put(DataSourceContextHolder.MASTER, masterDataSource());
        targets.put(DataSourceContextHolder.SLAVE,  slaveDataSource());

        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(masterDataSource());  // 默认主库（保证写安全）
        routing.afterPropertiesSet();
        return routing;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(routingDataSource());
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml")
        );
        // MyBatis 全局配置
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        config.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
        factoryBean.setConfiguration(config);
        factoryBean.setTypeAliasesPackage("com.flashsale.entity");
        return factoryBean.getObject();
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(routingDataSource());
    }

    // ==================== 工具方法 ====================
    private DruidDataSource buildDataSource(String url, String username, String password) {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setInitialSize(5);
        ds.setMinIdle(5);
        ds.setMaxActive(20);
        ds.setMaxWait(60000);
        ds.setValidationQuery("SELECT 1");
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(false);
        return ds;
    }
}
