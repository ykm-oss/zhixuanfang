package com.shop.common.constant;

import lombok.Getter;

@Getter
public enum ResultCodeEnum {
    SUCCESS(200,"成功"),
    FAIL(500,"失败"),

    // 参数错误 1000-1999
    PARAM_ERROR(1000, "参数错误"),
    PARAM_MISSING(1001, "参数缺失"),

    // 用户错误 2000-2999
    USER_NOT_LOGIN(2000, "用户未登录"),
    USER_NOT_EXIST(2001, "用户不存在"),
    USERNAME_PASSWORD_ERROR(2002, "用户名或密码错误"),
    USERNAME_EXIST(2003, "用户名已存在"),

    // 商品错误 3000-3999
    PRODUCT_NOT_EXIST(3000, "商品不存在"),
    PRODUCT_OFF_SHELF(3001, "商品已下架"),
    PRODUCT_STOCK_NOT_ENOUGH(3002, "商品库存不足"),

    // 订单错误 4000-4999
    ORDER_NOT_EXIST(4000, "订单不存在"),
    ORDER_STATUS_ERROR(4001, "订单状态错误"),
    ORDER_CANNOT_PAY(4002, "订单无法支付"),
    ORDER_REPEAT_SUBMIT(4003,"请勿重复提交订单"),
    ORDER_CANNOT_CANCEL(4004,"订单无法取消"),


    // 购物车错误 5000-5999
    CART_EMPTY(5000, "购物车为空"),

    // 系统错误 9000-9999
    SYSTEM_ERROR(9000, "系统繁忙，请稍后重试");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
