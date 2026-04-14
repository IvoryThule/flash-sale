package com.flashsale.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态路由数据源
 *
 * 通过 ThreadLocal（DataSourceContextHolder）决定当前线程使用主库还是从库：
 *  - master：写操作（INSERT / UPDATE / DELETE）
 *  - slave ：读操作（SELECT）
 */
@Slf4j
public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        String ds = DataSourceContextHolder.get();
        log.debug("[DataSource] 当前使用数据源：{}", ds);
        return ds;
    }
}
