package com.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.entity.User;

public interface UserService extends IService<User> {
    // 扩展方法：根据用户名查用户
    User getByUsername(String username);
}
