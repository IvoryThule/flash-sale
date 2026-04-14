package com.flashsale.config.datasource;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 数据源切换 AOP 切面
 *
 * 拦截所有标注了 {@link ReadOnly} 的方法，
 * 执行前将 ThreadLocal 设为 READ（从库），执行后恢复并清理。
 *
 * {@code @Order(Ordered.HIGHEST_PRECEDENCE)} 保证此切面比
 * {@code @Transactional} 切面先执行，从而在事务开启前正确切换数据源。
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataSourceAspect {

    @Around("@annotation(com.flashsale.config.datasource.ReadOnly)" +
            " || @within(com.flashsale.config.datasource.ReadOnly)")
    public Object switchToReadDataSource(ProceedingJoinPoint jp) throws Throwable {
        DataSourceType previous = DataSourceContextHolder.getType();
        DataSourceContextHolder.setType(DataSourceType.READ);
        log.debug("[DataSource] 切换到从库 READ，方法={}", jp.getSignature().toShortString());
        try {
            return jp.proceed();
        } finally {
            DataSourceContextHolder.setType(previous);
            log.debug("[DataSource] 恢复数据源 -> {}", previous);
        }
    }
}
