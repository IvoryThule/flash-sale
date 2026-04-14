package com.flashsale.config.datasource;

import java.lang.annotation.*;

/**
 * 标记方法或类使用只读数据源（从库）。
 * AOP 切面 {@link DataSourceAspect} 会在方法执行前将数据源切换到 READ，
 * 执行结束后恢复默认（WRITE）。
 *
 * 使用示例：
 * <pre>{@code
 * @ReadOnly
 * public Product getProductDetail(Long id) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnly {
}
