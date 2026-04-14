package com.flashsale.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.BusinessException;
import com.flashsale.common.ResultCode;
import com.flashsale.entity.Product;
import com.flashsale.mapper.ProductMapper;
import com.flashsale.service.ProductService;
import com.flashsale.config.datasource.ReadOnly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现
 *
 * Redis 缓存三重保护：
 *  - 缓存穿透：DB 无结果时写入空值哨兵（NULL_PRODUCT），TTL=5 min，防止恶意刷不存在 ID 打穿 DB
 *  - 缓存击穿：热点 Key 过期时用 Redis SETNX 互斥锁，保证只有一个线程重建缓存
 *  - 缓存雪崩：基础 TTL=30 min + Random(0~300 s) 随机抖动，避免大批 Key 同时过期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** 商品详情缓存 Key 前缀 */
    private static final String CACHE_KEY  = "product:detail:";
    /** 互斥锁 Key 前缀（防击穿）*/
    private static final String LOCK_KEY   = "lock:product:detail:";
    /** 空值哨兵（防穿透）*/
    private static final String NULL_VALUE = "NULL_PRODUCT";
    /** 基础 TTL：30 min（防雪崩再叠加随机量）*/
    private static final long   BASE_TTL   = 1800;
    /** 空值 TTL：5 min */
    private static final long   NULL_TTL   = 300;
    /** 锁 TTL：10 s（防死锁）*/
    private static final long   LOCK_TTL   = 10;

    private static final Random RANDOM = new Random();

    @ReadOnly
    @Override
    public List<Product> getProductList() {
        return productMapper.selectOnShelfList();
    }

    /**
     * 获取商品详情（带 Redis 缓存三重保护）
     *
     * 执行流程：
     *  1. 查缓存 → 命中直接返回
     *  2. 命中空值哨兵 → DB 确实无此记录，抛 404（防穿透）
     *  3. 未命中 → SETNX 争抢互斥锁（防击穿）
     *     a. 加锁成功 → 二次查缓存 → 查 DB → 写缓存（随机 TTL 防雪崩）→ 释放锁
     *     b. 加锁失败 → 等 100ms 重试，最多 3 次，仍失败则降级直查 DB
     */
    @ReadOnly
    @Override
    public Product getProductDetail(Long productId) {
        String cacheKey = CACHE_KEY + productId;

        // Step 1: 查缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (NULL_VALUE.equals(cached)) {
                log.debug("[Cache] 命中空值哨兵，productId={}", productId);
                throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
            }
            log.debug("[Cache] 缓存命中，productId={}", productId);
            return toProduct(cached);
        }

        // Step 2: 缓存未命中，争抢分布式锁（防击穿）
        String lockKey = LOCK_KEY + productId;
        for (int attempt = 0; attempt < 3; attempt++) {
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(locked)) {
                try {
                    // 二次查缓存（锁等待期间可能已被其他线程写入）
                    Object doubleCheck = redisTemplate.opsForValue().get(cacheKey);
                    if (doubleCheck != null) {
                        if (NULL_VALUE.equals(doubleCheck)) throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
                        return toProduct(doubleCheck);
                    }

                    Product product = productMapper.selectById(productId);

                    if (product == null) {
                        // 防穿透：写入空值哨兵
                        log.warn("[Cache] DB 无此商品，写入空值哨兵，productId={}", productId);
                        redisTemplate.opsForValue().set(cacheKey, NULL_VALUE, NULL_TTL, TimeUnit.SECONDS);
                        throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
                    }

                    // 防雪崩：基础 TTL + 随机抖动
                    long ttl = BASE_TTL + RANDOM.nextInt(300);
                    redisTemplate.opsForValue().set(cacheKey, product, ttl, TimeUnit.SECONDS);
                    log.debug("[Cache] 缓存写入，productId={}，TTL={}s", productId, ttl);
                    return product;

                } finally {
                    stringRedisTemplate.delete(lockKey);
                }
            }

            log.debug("[Cache] 锁竞争，等待重试 attempt={}，productId={}", attempt + 1, productId);
            try { Thread.sleep(100); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }

        // 降级：直查 DB
        log.warn("[Cache] 获取锁超时，降级直查 DB，productId={}", productId);
        Product fallback = productMapper.selectById(productId);
        if (fallback == null) throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        return fallback;
    }

    /** 将缓存对象反序列化为 Product（处理 Jackson 类型转换）*/
    private Product toProduct(Object obj) {
        if (obj instanceof Product) return (Product) obj;
        return objectMapper.convertValue(obj, Product.class);
    }
}
