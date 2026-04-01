package com.shop.controller;

import com.shop.common.dto.CartAddDto;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.Result;
import com.shop.entity.Cart;
import com.shop.entity.Product;
import com.shop.service.CartService;
import com.shop.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 购物车控制器
 */
@Api(tags = "购物车模块")
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @ApiOperation("添加商品到购物车")
    @PostMapping("/add")
    public Result<Void> add(HttpServletRequest request, @Valid @RequestBody CartAddDto cartAddDto) {
        Long userId = (Long) request.getAttribute("userId");

        // 校验商品是否存在且上架
        Product product = productService.getByIdAndStatus(cartAddDto.getProductId());
        if (product == null) {
            throw new BusinessException(3000, "商品不存在或已下架");
        }

        boolean success = cartService.addCart(userId, cartAddDto.getProductId(), cartAddDto.getNum());
        if (!success) {
            return Result.error(500, "添加购物车失败");
        }

        return Result.success();
    }

    @ApiOperation("购物车列表")
    @GetMapping("/list")
    public Result<List<Cart>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Cart> cartList = cartService.listByUserId(userId);
        return Result.success(cartList);
    }

    @ApiOperation("修改购物车商品数量")
    @PutMapping("/update")
    public Result<Void> update(@RequestParam Long cartId, @RequestParam Integer num) {
        if (num <= 0) {
            throw new BusinessException(1000, "数量必须大于0");
        }

        Cart cart = cartService.getById(cartId);
        if (cart == null) {
            throw new BusinessException(5000, "购物车记录不存在");
        }

        cart.setNum(num);
        cartService.updateById(cart);

        return Result.success();
    }

    @ApiOperation("删除购物车商品")
    @DeleteMapping("/delete/{cartId}")
    public Result<Void> delete(@PathVariable Long cartId) {
        boolean success = cartService.removeById(cartId);
        if (!success) {
            throw new BusinessException(5000, "删除失败，购物车记录不存在");
        }
        return Result.success();
    }

    @ApiOperation("清空购物车")
    @DeleteMapping("/clear")
    public Result<Void> clear(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Cart> cartList = cartService.listByUserId(userId);

        for (Cart cart : cartList) {
            cartService.removeById(cart.getId());
        }

        return Result.success();
    }
}