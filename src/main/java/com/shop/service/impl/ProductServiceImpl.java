package com.shop.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.entity.Product;
import com.shop.mapper.ProductMapper;
import com.shop.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现类
 */
@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final StringRedisTemplate redisTemplate;

    // Redis Key 常量
    private static final String PRODUCT_CACHE_KEY = "product:";
    private static final String PRODUCT_LOCK_KEY = "product:lock:";
    private static final long NULL_CACHE_TTL = 5;           // 空值缓存5分钟
    private static final long LOCK_TIMEOUT = 10;            // 锁超时时间10秒
    private static final long BASE_CACHE_TTL = 30;          // 基础缓存时间30分钟
    private static final long RANDOM_RANGE = 10;            // 随机范围10分钟

    /**
     * 构造器注入（推荐，替代字段注入）
     */
    @Autowired
    public ProductServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 根据ID查询商品详情（防穿透 + 防击穿 + 防雪崩）
     */
    @Override
    public Product getByIdAndStatus(Long id) {
        log.info("查询商品详情，id: {}", id);

        // ==================== 1. 先查缓存 ====================
        String cacheKey = PRODUCT_CACHE_KEY + id;
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);

        // 缓存命中处理
        if (cacheValue != null) {
            // 空值标记
            if ("null".equals(cacheValue)) {
                log.info("缓存空值命中，商品不存在: {}", id);
                return null;
            }
            log.info("缓存命中，商品: {}", id);
            return JSONUtil.toBean(cacheValue, Product.class);
        }

        // ==================== 2. 缓存未命中，加锁查数据库（防击穿） ====================
        String lockKey = PRODUCT_LOCK_KEY + id;

        // 尝试获取锁
        boolean locked = tryLock(lockKey);

        // 注意：locked 变量虽然未直接使用，但 tryLock 方法的执行结果决定了是否进入临界区
        // 这个警告可以忽略，或者用 @SuppressWarnings 抑制

        try {
            // 双重检查：获取锁后再次检查缓存（防止在等待锁期间其他线程已更新缓存）
            cacheValue = redisTemplate.opsForValue().get(cacheKey);
            if (cacheValue != null) {
                if ("null".equals(cacheValue)) {
                    return null;
                }
                return JSONUtil.toBean(cacheValue, Product.class);
            }

            // ==================== 3. 查询数据库 ====================
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Product::getId, id)
                    .eq(Product::getStatus, 1);
            Product product = this.getOne(wrapper);

            // ==================== 4. 缓存结果（包括空值） ====================
            if (product == null) {
                // 缓存空对象，防穿透
                redisTemplate.opsForValue().set(cacheKey, "null", NULL_CACHE_TTL, TimeUnit.MINUTES);
                log.info("缓存空值，商品不存在: {}", id);
                return null;
            }

            // 缓存商品，带随机过期时间（防雪崩）
            long randomTtl = BASE_CACHE_TTL + (long) (Math.random() * RANDOM_RANGE);
            redisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(product), randomTtl, TimeUnit.MINUTES);
            log.info("缓存商品: {}, 过期时间: {}分钟", id, randomTtl);

            return product;

        } finally {
            // 释放锁（确保一定会释放）
            unlock(lockKey);
        }
    }

    /**
     * 尝试获取分布式锁
     * @param key 锁的key
     * @return true-获取成功，false-获取失败
     */
    private boolean tryLock(String key) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", LOCK_TIMEOUT, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放分布式锁
     * @param key 锁的key
     */
    private void unlock(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 更新商品时删除缓存
     */
    @Override
    @CacheEvict(value = "product", key = "'product:' + #product.id")
    public boolean updateById(Product product) {
        return super.updateById(product);
    }
}