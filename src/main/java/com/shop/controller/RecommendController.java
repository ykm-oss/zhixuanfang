package com.shop.controller;

import com.shop.common.result.Result;
import com.shop.entity.Product;
import com.shop.service.RecommendService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(tags = "推荐模块")
@RestController
@RequestMapping("/recommend")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @ApiOperation("获取商品推荐（综合）")
    @GetMapping("/products")
    public Result<List<Product>> recommend(
            HttpServletRequest request,
            @RequestParam(required = false) Long productId) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.success(recommendService.recommendHotProducts(null));
        }

        List<Product> recommendations = recommendService.recommend(userId, productId);
        return Result.success(recommendations);
    }

    @ApiOperation("基于协同过滤推荐")
    @GetMapping("/collaborative")
    public Result<List<Product>> collaborativeRecommend(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.success();
        }
        return Result.success(recommendService.recommendByCollaborativeFiltering(userId));
    }

    @ApiOperation("基于内容推荐")
    @GetMapping("/content")
    public Result<List<Product>> contentRecommend(@RequestParam Long productId) {
        return Result.success(recommendService.recommendByContentBased(productId));
    }

    @ApiOperation("热门商品推荐")
    @GetMapping("/hot")
    public Result<List<Product>> hotRecommend(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(recommendService.recommendHotProducts(userId));
    }

    @ApiOperation("记录用户浏览行为")
    @PostMapping("/browse/{productId}")
    public Result<Void> recordBrowse(
            HttpServletRequest request,
            @PathVariable Long productId) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            recommendService.recordBehavior(userId, productId, 1);  // 1=浏览
        }
        return Result.success();
    }

    @ApiOperation("记录用户收藏行为")
    @PostMapping("/favorite/{productId}")
    public Result<Void> recordFavorite(
            HttpServletRequest request,
            @PathVariable Long productId) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            recommendService.recordBehavior(userId, productId, 2);  // 2=收藏
        }
        return Result.success();
    }

    @ApiOperation("记录用户购买行为")
    @PostMapping("/purchase/{productId}")
    public Result<Void> recordPurchase(
            HttpServletRequest request,
            @PathVariable Long productId) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            recommendService.recordBehavior(userId, productId, 3);  // 3=购买
        }
        return Result.success();
    }
}