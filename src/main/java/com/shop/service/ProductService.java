package com.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.entity.Product;
import org.springframework.stereotype.Service;

@Service
public interface ProductService extends IService<Product> {
    // 扩展方法：根据ID查上架商品
    Product getByIdAndStatus(Long id);
}
