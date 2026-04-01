package com.shop.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.Result;
import com.shop.entity.Product;
import com.shop.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 商品控制器
 */
@Api(tags = "商品模块")
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @ApiOperation("商品列表（分页）")
    @GetMapping("/list")
    public Result<Page<Product>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {

        // 创建分页对象
        Page<Product> page = new Page<>(pageNum, pageSize);

        // 查询条件
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1); // 只查上架商品
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getName, keyword);
        }
        wrapper.orderByDesc(Product::getCreateTime);

        // 执行分页查询
        Page<Product> productPage = productService.page(page, wrapper);

        return Result.success(productPage);
    }

    @ApiOperation("商品详情")
    @GetMapping("/detail/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        Product product = productService.getByIdAndStatus(id);
        if (product == null) {
            throw new BusinessException(3000, "商品不存在");
        }
        return Result.success(product);
    }

    @ApiOperation("商品上下架（管理员）")
    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException(3000, "商品不存在");
        }

        product.setStatus(status);
        productService.updateById(product);

        return Result.success();
    }
}