package com.flashsale.config.datasource;

/**
 * 数据源类型枚举
 * WRITE：主库（写操作）
 * READ ：从库（读操作）
 */
public enum DataSourceType {
    WRITE, READ
}
