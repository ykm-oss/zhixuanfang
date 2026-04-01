package com.shop.service;

import com.shop.entity.Product;

import java.util.List;

public interface RecommendService {


    /**
     * 基于协同过滤的推荐（找到相似用户，推荐他们买的商品）
     */
    List<Product> recommendByCollaborativeFiltering(Long userId);

    /**
     * 基于内容的推荐（根据商品名称/关键词推荐相似商品）
     */
    List<Product> recommendByContentBased(Long productId);

    /**
     * 热门商品推荐（冷启动时使用）
     */
    List<Product> recommendHotProducts(Long userId);

    /**
     * 综合推荐（结合多种算法）
     */
    List<Product> recommend(Long userId,Long currentProductId);

    /**
     * 记录用户行为
     */
    void recordBehavior(Long userId,Long productId,Integer behaviorType);
}
