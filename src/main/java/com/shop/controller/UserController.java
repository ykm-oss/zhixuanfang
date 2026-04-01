package com.shop.controller;

import com.shop.common.dto.LoginDto;
import com.shop.common.dto.RegisterDto;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.Result;
import com.shop.common.util.JwtUtil;
import com.shop.entity.User;
import com.shop.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@Api(tags = "用户模块")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDto registerDto){
        // 检查用户名是否已存在
        User user=new User();
        user.setUsername(registerDto.getUsername());
        // MD5加密密码（简单示例，实际建议用BCrypt）
        String encryptedPassword= DigestUtils.md5DigestAsHex(registerDto.getPassword().getBytes(StandardCharsets.UTF_8));
        user.setPassword(encryptedPassword);
        user.setPhone(registerDto.getPhone());
        user.setCreateTime(LocalDateTime.now());

        userService.save(user);
        return Result.success();
    }

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<Map<String,Object>> login(@Valid @RequestBody LoginDto loginDto){
        // 查询用户
        User user=userService.getByUsername(loginDto.getUsername());
        if (user==null){
            throw new BusinessException(2002,"用户名或密码错误");
        }

        // 验证密码
        String encryptedPassword=DigestUtils.md5DigestAsHex(loginDto.getPassword().getBytes(StandardCharsets.UTF_8));
        if (!encryptedPassword.equals(user.getPassword())){
            throw new BusinessException(2002,"用户名或密码错误");
        }

        //生成JWT
        String token= jwtUtil.generateToken(user.getId(), user.getUsername());

        //返回数据
        Map<String ,Object> data=new HashMap<>();
        data.put("token",token);
        data.put("userId",user.getId());
        data.put("username",user.getUsername());

        return Result.success(data);

    }

    @ApiOperation("获取当前用户信息")
    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request){
        Long userId=(Long) request.getAttribute("userId");
        User user=userService.getById(userId);
        if(user==null){
            throw new BusinessException(2001,"用户不存在");
        }

        //隐藏密码
        user.setPassword(null);
        return Result.success(user);
    }
}
