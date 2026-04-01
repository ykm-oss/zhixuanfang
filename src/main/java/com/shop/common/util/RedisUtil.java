package com.shop.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 */
@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 设置分布式锁
     * @param key 锁的 key
     * @param value 锁的值（可以用 userId）
     * @param timeout 过期时间（秒）
     * @return true=获取锁成功，false=获取锁失败
     */
    public boolean setLock(String key,String value,long timeout){
        //setIfAbsent 相当于Redis的SETNX命令
        Boolean result=redisTemplate.opsForValue()
                .setIfAbsent(key, value,timeout, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放分布式锁
     * @param key 锁的 key
     */
    public void releaseLock(String key){
        redisTemplate.delete(key);
    }
}
