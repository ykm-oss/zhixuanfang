package com.shop.service;

import com.shop.entity.SeckillProduct;
import java.util.List;

public interface SeckillService {

    /**
     * 秒杀（Redis 预减库存）
     * @param userId 用户ID
     * @param seckillId 秒杀商品ID
     * @return 秒杀结果
     */
    String seckill(Long userId, Long seckillId);

    /**
     * 获取秒杀商品列表
     */
    List<SeckillProduct> getSeckillProducts();

    /**
     * 查询秒杀订单状态
     */
    String getSeckillResult(Long userId, Long seckillId);

    /**
     * 初始化秒杀商品到 Redis
     */
    void initSeckillStockToRedis();
}