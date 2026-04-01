package com.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    /**
     * 获取用户购买过的商品ID列表
     */
    @Select("SELECT DISTINCT product_id FROM user_behavior WHERE user_id = #{userId} AND behavior_type = 3")
    List<Long> getPurchasedProductIds(@Param("userId") Long userId);

    /**
     * 获取购买过指定商品列表的用户ID
     */
    @Select("SELECT DISTINCT user_id FROM user_behavior WHERE product_id IN (${productIds}) AND behavior_type = 3")
    List<Long> findSimilarUsers(@Param("productIds") String productIds);

    /**
     * 获取相似用户购买的其他商品（排除已购买的）
     */
    @Select("SELECT product_id, COUNT(*) as count FROM user_behavior " +
            "WHERE user_id IN (${userIds}) " +
            "AND product_id NOT IN (${excludeProductIds}) " +
            "AND behavior_type = 3 " +
            "GROUP BY product_id " +
            "ORDER BY count DESC LIMIT 10")
    List<Long> getRecommendations(@Param("userIds") String userIds,
                                  @Param("excludeProductIds") String excludeProductIds);
}