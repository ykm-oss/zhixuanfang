package com.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {

    /**
     * 检查用户是否已秒杀过该商品
     */
    @Select("SELECT COUNT(*) FROM seckill_order WHERE user_id = #{userId} AND product_id = #{productId} AND status != 2")
    int countUserSeckill(@Param("userId") Long userId, @Param("productId") Long productId);
}