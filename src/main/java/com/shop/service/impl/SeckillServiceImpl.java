package com.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.entity.SeckillOrder;
import com.shop.entity.SeckillProduct;
import com.shop.mapper.SeckillOrderMapper;
import com.shop.mapper.SeckillProductMapper;
import com.shop.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private SeckillProductMapper seckillProductMapper;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Redis Key 前缀
    private static final String SECKILL_STOCK_KEY = "seckill:stock:";      // 库存
    private static final String SECKILL_USER_KEY = "seckill:user:";        // 用户限购
    private static final String SECKILL_RESULT_KEY = "seckill:result:";    // 秒杀结果

    // Lua 脚本：原子性扣减库存
    private static final String LUA_DECR_STOCK =
            "local stock = redis.call('get', KEYS[1]) " +
                    "if stock and tonumber(stock) > 0 then " +
                    "    return redis.call('decr', KEYS[1]) " +
                    "else " +
                    "    return -1 " +
                    "end";

    /**
     * 项目启动时，将秒杀商品库存加载到 Redis
     */
    @PostConstruct
    @Override
    public void initSeckillStockToRedis() {
        log.info("========== 开始初始化秒杀商品库存到 Redis ==========");

        List<SeckillProduct> activeList = seckillProductMapper.getActiveSeckillProducts();

        for (SeckillProduct product : activeList) {
            String key = SECKILL_STOCK_KEY + product.getId();
            redisTemplate.opsForValue().set(key, String.valueOf(product.getStock()));
            log.info("初始化秒杀商品库存: seckillId={}, stock={}", product.getId(), product.getStock());
        }

        log.info("========== 秒杀商品库存初始化完成 ==========");
    }

    /**
     * 秒杀核心方法
     */
    @Override
    public String seckill(Long userId, Long seckillId) {
        log.info("用户 {} 发起秒杀，商品 {}", userId, seckillId);

        // ==================== 1. 查询秒杀商品信息 ====================
        SeckillProduct seckillProduct = seckillProductMapper.selectById(seckillId);
        if (seckillProduct == null) {
            return "秒杀商品不存在";
        }

        // 检查秒杀是否进行中
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckillProduct.getStartTime())) {
            return "秒杀尚未开始";
        }
        if (now.isAfter(seckillProduct.getEndTime())) {
            return "秒杀已结束";
        }

        // ==================== 2. 检查用户是否已秒杀过 ====================
        String userKey = SECKILL_USER_KEY + seckillId + ":" + userId;
        Boolean alreadySeckill = redisTemplate.hasKey(userKey);
        if (Boolean.TRUE.equals(alreadySeckill)) {
            log.warn("用户 {} 已秒杀过商品 {}", userId, seckillId);
            return "您已参与过该秒杀，请勿重复参与";
        }

        // 检查数据库是否有未支付的秒杀订单
        int count = seckillOrderMapper.countUserSeckill(userId, seckillProduct.getProductId());
        if (count > 0) {
            return "您已有未支付的秒杀订单，请先支付";
        }

        // ==================== 3. Redis 预减库存（使用 Lua 脚本） ====================
        String stockKey = SECKILL_STOCK_KEY + seckillId;

        // 创建 Lua 脚本对象
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LUA_DECR_STOCK);
        script.setResultType(Long.class);

        // 执行 Lua 脚本，原子性扣减库存
        Long stock = redisTemplate.execute(script, Collections.singletonList(stockKey));

        if (stock == null || stock < 0) {
            log.warn("秒杀失败：库存不足，商品 {}", seckillId);
            return "手速太慢，商品已抢完";
        }

        log.info("Redis 减库存成功，剩余库存: {}", stock);

        // ==================== 4. 记录用户秒杀标记 ====================
        redisTemplate.opsForValue().set(userKey, "1", 1, TimeUnit.HOURS);

        // ==================== 5. 生成订单号，异步创建订单 ====================
        String orderNo = UUID.randomUUID().toString().replace("-", "");
        String resultKey = SECKILL_RESULT_KEY + userId + ":" + seckillId;
        redisTemplate.opsForValue().set(resultKey, orderNo, 5, TimeUnit.MINUTES);

        // 异步创建订单
        asyncCreateOrder(userId, seckillProduct, orderNo);

        return "秒杀成功，订单创建中";
    }

    /**
     * 异步创建订单
     */
    @Transactional
    public void asyncCreateOrder(Long userId, SeckillProduct seckillProduct, String orderNo) {
        try {
            // 1. 扣减数据库库存
            int result = seckillProductMapper.reduceStock(seckillProduct.getId());
            if (result <= 0) {
                log.error("数据库扣减库存失败，秒杀商品ID: {}", seckillProduct.getId());
                return;
            }

            // 2. 创建秒杀订单
            SeckillOrder order = new SeckillOrder();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setProductId(seckillProduct.getProductId());
            order.setSeckillPrice(seckillProduct.getSeckillPrice());
            order.setNum(1);
            order.setStatus(0);  // 待支付
            seckillOrderMapper.insert(order);

            log.info("秒杀订单创建成功: orderNo={}, userId={}", orderNo, userId);

        } catch (Exception e) {
            log.error("异步创建订单失败: {}", e.getMessage(), e);
            // 失败时恢复 Redis 库存
            String stockKey = SECKILL_STOCK_KEY + seckillProduct.getId();
            redisTemplate.opsForValue().increment(stockKey, 1);

            String userKey = SECKILL_USER_KEY + seckillProduct.getId() + ":" + userId;
            redisTemplate.delete(userKey);
        }
    }

    /**
     * 获取秒杀结果
     */
    @Override
    public String getSeckillResult(Long userId, Long seckillId) {
        String resultKey = SECKILL_RESULT_KEY + userId + ":" + seckillId;
        String orderNo = redisTemplate.opsForValue().get(resultKey);

        if (orderNo == null) {
            SeckillProduct seckillProduct = seckillProductMapper.selectById(seckillId);
            if (seckillProduct == null) {
                return "秒杀失败";
            }

            LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SeckillOrder::getUserId, userId)
                    .eq(SeckillOrder::getProductId, seckillProduct.getProductId())
                    .orderByDesc(SeckillOrder::getCreateTime)
                    .last("LIMIT 1");
            SeckillOrder order = seckillOrderMapper.selectOne(wrapper);

            if (order != null) {
                return "秒杀成功，订单号: " + order.getOrderNo();
            }
            return "秒杀失败";
        }

        return "秒杀中，请稍后查看结果";
    }

    /**
     * 获取秒杀商品列表
     */
    @Override
    public List<SeckillProduct> getSeckillProducts() {
        return seckillProductMapper.getActiveSeckillProducts();
    }
}