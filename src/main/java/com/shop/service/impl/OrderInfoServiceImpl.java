package com.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.common.constant.ResultCodeEnum;
import com.shop.common.exception.BusinessException;
import com.shop.common.util.RedisUtil;
import com.shop.entity.Cart;
import com.shop.entity.OrderInfo;
import com.shop.entity.OrderItem;
import com.shop.entity.Product;
import com.shop.mapper.OrderInfoMapper;
import com.shop.service.CartService;
import com.shop.service.OrderInfoService;
import com.shop.service.OrderItemService;
import com.shop.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private RedisUtil redisUtil;


    @Override
    public List<OrderInfo> listByUserId(Long userId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, userId);
        return this.list(wrapper);
    }

    @Override
    @Transactional
    public String createOrder(Long userId) {

        String lockKey = "order:lock:" + userId;

        boolean locked = redisUtil.setLock(lockKey, String.valueOf(userId), 5);

        if (!locked) {
            throw new BusinessException(4003, "请勿重复提交订单");
        }

        try{
            //1.查用户购物车
            List<Cart> cartList=cartService.listByUserId(userId);
            if (cartList==null||cartList.isEmpty()){
                throw new BusinessException(ResultCodeEnum.CART_EMPTY);
            }

            //2. 计算总价 + 校验商品库存
            BigDecimal totalPrice=BigDecimal.ZERO;
            for (Cart cart:cartList){
                Product product=productService.getByIdAndStatus(cart.getProductId());
                if (product==null){
                    throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
                }
                if (product.getStock()<cart.getNum()){
                    throw new BusinessException(ResultCodeEnum.PRODUCT_STOCK_NOT_ENOUGH);
                }
                totalPrice=totalPrice.add(product.getPrice().multiply(new BigDecimal(cart.getNum())));
            }

            // 3. 创建订单主表
            OrderInfo orderInfo=new OrderInfo();
            orderInfo.setOrderNo(UUID.randomUUID().toString().replace("-",""));
            orderInfo.setUserId(userId);
            orderInfo.setTotalPrice(totalPrice);
            orderInfo.setStatus(0);
            this.save(orderInfo);

            // 4. 创建订单明细表 + 扣减库存 + 清空购物车
            for (Cart cart:cartList){
                Product product=productService.getById(cart.getProductId());

                OrderItem orderItem=new OrderItem();
                orderItem.setOrderId(orderInfo.getId());
                orderItem.setProductId(product.getId());
                orderItem.setProductName(product.getName());
                orderItem.setProductPrice(product.getPrice());
                orderItem.setNum(cart.getNum());
                orderItemService.save(orderItem);

                // 扣减库存
                product.setStock(product.getStock()-cart.getNum());
                productService.updateById(product);

                //清空购物车
                cartService.removeById(cart.getId());
            }

            return orderInfo.getOrderNo();
        }finally {
            System.out.println("=== 释放锁，key: " + lockKey);
            redisUtil.releaseLock(lockKey);
        }
    }

    @Override
    @Transactional
    public boolean payOrder(String orderNo) {
        //查订单
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = this.getOne(wrapper);

        if (orderInfo == null) {
            throw new RuntimeException("订单不存在");
        }
        if (orderInfo.getStatus() != 0) {
            throw new RuntimeException("订单已支付或取消，无法重复支付");
        }

        // 更新订单状态
        orderInfo.setStatus(1);
        orderInfo.setPayTime(LocalDateTime.now());
        return this.updateById(orderInfo);
    }

    @Override
    @Transactional
    public void closeTimeoutOrder() {
        log.info("========== 开始执行超时订单关闭任务 ==========");

        // 1. 查询超时未支付的订单
        // 条件：status = 0 且 create_time < 当前时间 - 30分钟
        LocalDateTime timeoutTime=LocalDateTime.now().minusMinutes(30);

        LambdaQueryWrapper<OrderInfo> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getStatus,0)
                .lt(OrderInfo::getCreateTime,timeoutTime);

        List<OrderInfo> timeoutOrders=this.list(wrapper);

        if (timeoutOrders.isEmpty()){
            log.info("没有需要关闭的超时订单");
            return;
        }

        log.info("发现 {} 个超时订单，开始处理...", timeoutOrders.size());

        // 2. 逐个处理超时订单
        for (OrderInfo order:timeoutOrders){
            try{
                // 2.1 更新订单状态为"已关闭"(3)
                order.setStatus(3);
                this.updateById(order);
                log.info("订单 {} 已关闭", order.getOrderNo());

                // 2.2 查询订单明细，恢复库存
                List<OrderItem> orderItems=orderItemService.listByOrderId(order.getId());
                for (OrderItem item:orderItems){
                    Product product=productService.getById(item.getProductId());
                    if (product!=null){
                        // 恢复库存
                        int newStock=product.getStock()+ item.getNum();
                        product.setStock(newStock);
                        productService.updateById(product);
                        log.info("恢复库存：商品 {}，数量 +{}，当前库存：{}",
                                product.getName(), item.getNum(), newStock);
                    }
                }
            }catch (Exception e){
                log.error("处理超时订单 {} 失败：{}", order.getOrderNo(), e.getMessage(), e);
            }
        }

        log.info("========== 超时订单关闭任务执行完成 ==========");
    }

    @Override
    public OrderInfo getByOrderNO(String orderNo) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getOrderNo, orderNo);
        return this.getOne(wrapper);
    }

    @Override
    @Transactional
    public boolean cancelOrder(String orderNo) {
        OrderInfo orderInfo = getByOrderNO(orderNo);
        if (orderInfo == null) {
            throw new BusinessException(4000, "订单不存在");
        }
        if (orderInfo.getStatus() != 0) {
            throw new BusinessException(4001, "只能取消待支付的订单");
        }

        orderInfo.setStatus(2); // 2已取消
        this.updateById(orderInfo);

        // 恢复库存
        List<OrderItem> orderItems = orderItemService.listByOrderId(orderInfo.getId());
        for (OrderItem item : orderItems) {
            Product product = productService.getById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getNum());
                productService.updateById(product);
            }
        }

        return true;
    }

}
