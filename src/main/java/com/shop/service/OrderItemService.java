package com.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface OrderItemService extends IService<OrderItem> {
    // 根据订单ID查订单项列表
    List<OrderItem> listByOrderId(Long orderId);
}
