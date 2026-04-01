package com.shop.controller;

import com.shop.common.exception.BusinessException;
import com.shop.common.result.Result;
import com.shop.entity.OrderInfo;
import com.shop.entity.OrderItem;
import com.shop.service.OrderInfoService;
import com.shop.service.OrderItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 */
@Api(tags = "订单模块")
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private OrderItemService orderItemService;

    @ApiOperation("从购物车创建订单")
    @PostMapping("/create")
    public Result<Map<String, String>> create(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 创建订单
        String orderNo = orderInfoService.createOrder(userId);

        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);

        return Result.success(result);
    }

    @ApiOperation("订单列表")
    @GetMapping("/list")
    public Result<List<OrderInfo>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<OrderInfo> orderList = orderInfoService.listByUserId(userId);
        return Result.success(orderList);
    }

    @ApiOperation("订单详情")
    @GetMapping("/detail/{orderNo}")
    public Result<Map<String, Object>> detail(@PathVariable String orderNo) {
        // 查询订单
        OrderInfo orderInfo = orderInfoService.getByOrderNO(orderNo);
        if (orderInfo == null) {
            throw new BusinessException(4000, "订单不存在");
        }

        // 查询订单项
        List<OrderItem> orderItems = orderItemService.listByOrderId(orderInfo.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("order", orderInfo);
        result.put("items", orderItems);

        return Result.success(result);
    }

    @ApiOperation("模拟支付")
    @PutMapping("/pay/{orderNo}")
    public Result<Void> pay(@PathVariable String orderNo) {
        boolean success = orderInfoService.payOrder(orderNo);
        if (!success) {
            return Result.error(500, "支付失败");
        }
        return Result.success();
    }

    @ApiOperation("取消订单")
    @PutMapping("/cancel/{orderNo}")
    public Result<Void> cancel(@PathVariable String orderNo) {
        boolean success = orderInfoService.cancelOrder(orderNo);
        if (!success) {
            return Result.error(500, "取消订单失败");
        }
        return Result.success();
    }
}