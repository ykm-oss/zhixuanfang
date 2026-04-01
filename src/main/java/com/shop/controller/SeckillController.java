package com.shop.controller;

import com.shop.common.result.Result;
import com.shop.entity.SeckillProduct;
import com.shop.service.SeckillService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(tags = "秒杀模块")
@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @ApiOperation("获取秒杀商品列表")
    @GetMapping("/products")
    public Result<List<SeckillProduct>> getSeckillProducts() {
        return Result.success(seckillService.getSeckillProducts());
    }

    @ApiOperation("秒杀下单")
    @PostMapping("/{seckillId}")
    public Result<String> seckill(
            HttpServletRequest request,
            @PathVariable Long seckillId) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(2000, "请先登录");
        }

        String result = seckillService.seckill(userId, seckillId);
        return Result.success(result);
    }

    @ApiOperation("查询秒杀结果")
    @GetMapping("/result/{seckillId}")
    public Result<String> getSeckillResult(
            HttpServletRequest request,
            @PathVariable Long seckillId) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(2000, "请先登录");
        }

        String result = seckillService.getSeckillResult(userId, seckillId);
        return Result.success(result);
    }
}