package com.shop.common.interceptor;

import com.shop.common.constant.ResultCodeEnum;
import com.shop.common.exception.BusinessException;
import com.shop.common.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoginInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头获取 Authorization
        String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader)) {
            log.warn("用户未登录，Authorization为空");
            throw new BusinessException(ResultCodeEnum.USER_NOT_LOGIN);
        }

        // 去掉 "Bearer " 前缀（如果存在）
        String token = authHeader;
        if (authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            log.info("提取token: {}", token);
        }

        // 验证token
        if (!jwtUtil.validateToken(token)) {
            log.warn("token无效: {}", token);
            throw new BusinessException(ResultCodeEnum.USER_NOT_LOGIN);
        }

        // 获取用户ID并存入request
        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        log.info("用户登录成功，userId: {}", userId);

        return true;
    }
}