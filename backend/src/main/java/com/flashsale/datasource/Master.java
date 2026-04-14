package com.flashsale.datasource;

import java.lang.annotation.*;

/**
 * 标记强制走主库（写操作专用）
 *
 * 用法：在 Service 方法上加 @Master，确保路由到写主库。
 * 未标记的方法默认路由到从库（读操作）。
 *
 * 示例：
 *   @Master
 *   public void createOrder(...) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Master {
}
