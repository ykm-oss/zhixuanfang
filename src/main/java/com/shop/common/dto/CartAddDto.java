package com.shop.common.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 添加购物车请求DTO
 */
@Data
public class CartAddDto {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1,message = "数量必须大于0")
    private Integer num;
}
