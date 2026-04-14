package com.flashsale.datasource;

/**
 * 数据源上下文持有者（ThreadLocal 保证线程安全）
 *
 * 使用：写操作 → DataSourceContextHolder.setMaster()
 *       读操作 → DataSourceContextHolder.setSlave()（或不设置，默认 slave）
 *       方法结束后 → DataSourceContextHolder.clear()（防内存泄漏）
 */
public class DataSourceContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static final String MASTER = "master";
    public static final String SLAVE  = "slave";

    public static void setMaster() {
        CONTEXT.set(MASTER);
    }

    public static void setSlave() {
        CONTEXT.set(SLAVE);
    }

    /** 获取当前线程的数据源标识，未设置时默认走主库（保证写安全）*/
    public static String get() {
        String ds = CONTEXT.get();
        return ds != null ? ds : MASTER;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
