package com.flashsale.config.datasource;

/**
 * 数据源上下文（ThreadLocal）
 * 在同一请求线程内持有当前应使用的数据源类型。
 */
public class DataSourceContextHolder {

    private static final ThreadLocal<DataSourceType> CONTEXT =
            ThreadLocal.withInitial(() -> DataSourceType.WRITE);

    public static void setType(DataSourceType type) {
        CONTEXT.set(type);
    }

    public static DataSourceType getType() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
