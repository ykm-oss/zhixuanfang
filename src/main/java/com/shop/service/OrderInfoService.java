package com.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.entity.OrderInfo;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {
    // 根据用户ID查订单列表
    List<OrderInfo> listByUserId(Long userId);

    // 创建订单（核心方法）
    String createOrder(Long userId);

    // 模拟支付
    boolean payOrder(String orderNo);

    // 关闭超时未支付订单
    void closeTimeoutOrder();

    OrderInfo getByOrderNO(String orderNo);

    boolean cancelOrder(String orderNo);
}
