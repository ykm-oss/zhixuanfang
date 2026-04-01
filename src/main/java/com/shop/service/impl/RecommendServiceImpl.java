package com.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.entity.Product;
import com.shop.entity.UserBehavior;
import com.shop.mapper.UserBehaviorMapper;
import com.shop.service.ProductService;
import com.shop.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private UserBehaviorMapper userBehaviorMapper;

    @Autowired
    private ProductService productService;

    // 行为权重：购买 > 收藏 > 浏览
    private static final Map<Integer,Integer> BEHAVIOR_WEIGHT=new HashMap<>();
    static {
        BEHAVIOR_WEIGHT.put(1,1);
        BEHAVIOR_WEIGHT.put(2,3);
        BEHAVIOR_WEIGHT.put(3,5);
    }

    //推荐数量
    private static final int RECOMMEND_SIZE=10;

    @Override
    @Cacheable(value = "recommend", key = "'collaborative:' + #userId", unless = "#result == null || #result.isEmpty()")
    public List<Product> recommendByCollaborativeFiltering(Long userId) {
        log.info("开始协同过滤推荐，userId:{}",userId);

        // 1. 获取用户购买过的商品
        List<Long> purchasedProductIds=userBehaviorMapper.getPurchasedProductIds(userId);

        // 如果没有购买记录，返回空
        if (purchasedProductIds==null||purchasedProductIds.isEmpty()){
            log.info("用户无购买记录，无法进行协同过滤推荐");
            return new ArrayList<>();
        }

        // 2. 找到购买过相同商品的其他用户
        String productIdsStr=purchasedProductIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        List<Long> similarUserIds=userBehaviorMapper.findSimilarUsers(productIdsStr);
        if (similarUserIds==null||similarUserIds.isEmpty()){
            log.info("未找到相似用户");
            return new ArrayList<>();
        }

        // 3. 获取相似用户购买的其他商品（排除已购买的）
        String userIdsStr=similarUserIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String excludeIdsStr=purchasedProductIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        List<Long> recommendProductIds = userBehaviorMapper.getRecommendations(userIdsStr, excludeIdsStr);

        if (recommendProductIds == null || recommendProductIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 根据商品ID查询商品详情
        List<Product> recommendations = productService.listByIds(recommendProductIds);

        log.info("协同过滤推荐完成，共推荐 {} 个商品", recommendations.size());
        return recommendations;
    }

    @Override
    @Cacheable(value = "recommend", key = "'content:' + #productId", unless = "#result == null || #result.isEmpty()")
    public List<Product> recommendByContentBased(Long productId) {
        log.info("开始基于内容的推荐，productId: {}", productId);

        // 1. 获取当前商品
        Product currentProduct = productService.getById(productId);
        if (currentProduct == null) {
            return new ArrayList<>();
        }

        // 2. 提取商品名称关键词
        String name = currentProduct.getName();
        String[] keywords = extractKeywords(name);

        // 3. 根据关键词搜索相似商品
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)  // 只推荐上架商品
                .ne(Product::getId, productId);  // 排除当前商品

        // 构建关键词搜索条件
        if (keywords.length > 0) {
            wrapper.and(w -> {
                for (String keyword : keywords) {
                    w.or().like(Product::getName, keyword);
                }
            });
        }

        wrapper.last("LIMIT " + RECOMMEND_SIZE);

        List<Product> recommendations = productService.list(wrapper);

        log.info("基于内容的推荐完成，共推荐 {} 个商品", recommendations.size());
        return recommendations;
    }

    /**
     * 提取关键词
     */
    private String[] extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        // 简单分词（按空格和常见分隔符）
        return text.split("[\\s\\-\\/]+");
    }

    @Override
    @Cacheable(value = "recommend", key = "'hot:' + #userId", unless = "#result == null || #result.isEmpty()")
    public List<Product> recommendHotProducts(Long userId) {
        log.info("开始热门商品推荐，userId: {}", userId);

        // 按销量排序（需要先给商品表增加 sales 字段，或用订单统计）
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getStock);  // 暂时用库存替代，实际应该用销量

        // 排除用户已购买的商品
        List<Long> purchasedProductIds = userBehaviorMapper.getPurchasedProductIds(userId);
        if (purchasedProductIds != null && !purchasedProductIds.isEmpty()) {
            wrapper.notIn(Product::getId, purchasedProductIds);
        }

        wrapper.last("LIMIT " + RECOMMEND_SIZE);

        List<Product> recommendations = productService.list(wrapper);

        log.info("热门商品推荐完成，共推荐 {} 个商品", recommendations.size());
        return recommendations;
    }

    @Override
    public List<Product> recommend(Long userId, Long currentProductId) {
        Set<Product> resultSet = new LinkedHashSet<>();

        // 1. 优先使用协同过滤（基于购买历史）
        List<Product> collaborativeResults = recommendByCollaborativeFiltering(userId);
        if (collaborativeResults != null) {
            resultSet.addAll(collaborativeResults);
        }

        // 2. 如果协同过滤结果不足，补充基于内容的推荐
        if (resultSet.size() < RECOMMEND_SIZE && currentProductId != null) {
            List<Product> contentResults = recommendByContentBased(currentProductId);
            if (contentResults != null) {
                resultSet.addAll(contentResults);
            }
        }

        // 3. 如果结果仍不足，补充热门商品推荐
        if (resultSet.size() < RECOMMEND_SIZE) {
            List<Product> hotResults = recommendHotProducts(userId);
            if (hotResults != null) {
                resultSet.addAll(hotResults);
            }
        }

        // 4. 限制返回数量
        return resultSet.stream()
                .limit(RECOMMEND_SIZE)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void recordBehavior(Long userId, Long productId, Integer behaviorType) {
        if (userId == null || productId == null) {
            return;
        }

        // 检查是否已存在相同行为（避免重复记录）
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getProductId, productId)
                .eq(UserBehavior::getBehaviorType, behaviorType);

        long count = userBehaviorMapper.selectCount(wrapper);
        if (count > 0) {
            return;  // 已存在，不再重复记录
        }

        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setProductId(productId);
        behavior.setBehaviorType(behaviorType);

        userBehaviorMapper.insert(behavior);
        log.info("记录用户行为：userId={}, productId={}, type={}", userId, productId, behaviorType);
    }
}
