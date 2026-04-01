package com.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.entity.Cart;
import com.shop.mapper.CartMapper;
import com.shop.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper,Cart> implements CartService{
    @Override
    public List<Cart> listByUserId(Long userId) {
        LambdaQueryWrapper<Cart> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId,userId);
        return this.list(wrapper);
    }

    @Override
    @Transactional
    public boolean addCart(Long userId, Long productId, Integer num) {
        LambdaQueryWrapper<Cart> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId,userId)
                .eq(Cart::getProductId,productId);
        Cart cart=this.getOne(wrapper);

        if (cart != null){
            cart.setNum(cart.getNum()+num);
            return this.updateById(cart);
        }else{
            Cart newCart=new Cart();
            newCart.setUserId(userId);
            newCart.setProductId(productId);
            newCart.setNum(num);
            return this.save(newCart);
        }
    }
}
