package com.flashsale.config.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态路由数据源
 * 根据 DataSourceContextHolder 中的类型决定使用主库还是从库。
 */
@Slf4j
public class DynamicDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType type = DataSourceContextHolder.getType();
        log.trace("[DataSource] 路由到 {}", type);
        return type;
    }
}
