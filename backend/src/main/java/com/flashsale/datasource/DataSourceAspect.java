package com.flashsale.datasource;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * 读写分离 AOP 切面
 *
 * 路由规则优先级（从高到低）：
 *  1. 方法或类上有 @Master         → 主库（写）
 *  2. @Transactional(readOnly=false) → 主库（写）
 *  3. @Transactional(readOnly=true)  → 从库（读）
 *  4. 无注解                         → 从库（读，保守策略可改为主库）
 *
 * @Order(1) 保证在 @Transactional (Order=Integer.MAX_VALUE) 之前切入，
 * 使数据源切换在事务开启前生效。
 */
@Slf4j
@Aspect
@Order(1)
@Component
public class DataSourceAspect {

    /** 拦截 service 包下所有方法 */
    @Around("execution(* com.flashsale.service..*(..))")
    public Object route(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Class<?> clazz = pjp.getTarget().getClass();

        try {
            // 优先判断 @Master（方法级 > 类级）
            Master masterOnMethod = AnnotationUtils.findAnnotation(method, Master.class);
            Master masterOnClass  = AnnotationUtils.findAnnotation(clazz,  Master.class);
            if (masterOnMethod != null || masterOnClass != null) {
                DataSourceContextHolder.setMaster();
                log.debug("[DataSource] @Master → 主库  method={}", method.getName());
                return pjp.proceed();
            }

            // 检查 @Transactional 的 readOnly 属性
            Transactional tx = AnnotationUtils.findAnnotation(method, Transactional.class);
            if (tx == null) {
                tx = AnnotationUtils.findAnnotation(clazz, Transactional.class);
            }
            if (tx != null && !tx.readOnly()) {
                DataSourceContextHolder.setMaster();
                log.debug("[DataSource] @Transactional(readOnly=false) → 主库  method={}", method.getName());
                return pjp.proceed();
            }

            // 默认走从库（读操作）
            DataSourceContextHolder.setSlave();
            log.debug("[DataSource] 默认 → 从库  method={}", method.getName());
            return pjp.proceed();

        } finally {
            DataSourceContextHolder.clear();
        }
    }
}
