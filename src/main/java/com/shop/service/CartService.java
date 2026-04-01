package com.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.entity.Cart;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CartService extends IService<Cart> {
    List<Cart> listByUserId(Long userId);

    // 添加商品到购物车
    boolean addCart(Long userId, Long productId, Integer num);
}
