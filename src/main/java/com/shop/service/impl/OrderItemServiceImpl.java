package com.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.entity.OrderItem;
import com.shop.mapper.OrderItemMapper;
import com.shop.service.OrderItemService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {
    @Override
    public List<OrderItem> listByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId,orderId);
        return this.list(wrapper);
    }
}
