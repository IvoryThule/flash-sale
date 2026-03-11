package com.flashsale.controller;

import com.flashsale.common.Result;
import com.flashsale.dto.LoginRequest;
import com.flashsale.dto.LoginResponse;
import com.flashsale.dto.RegisterRequest;
import com.flashsale.entity.User;
import com.flashsale.service.UserService;
import com.flashsale.util.JwtUtil;
import com.flashsale.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     * POST /api/user/register
     */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(
                request.getUsername(),
                request.getPassword(),
                request.getNickname(),
                request.getPhone()
        );
        return Result.success("注册成功", user);
    }

    /**
     * 用户登录
     * POST /api/user/login
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        Long userId = jwtUtil.getUserId(token);
        User user = userService.getUserInfo(userId);
        LoginResponse response = new LoginResponse(
                token, user.getId(), user.getUsername(), user.getNickname());
        return Result.success("登录成功", response);
    }

    /**
     * 获取当前登录用户信息
     * GET /api/user/info
     */
    @GetMapping("/info")
    public Result<User> getUserInfo() {
        Long userId = UserContext.getUserId();
        User user = userService.getUserInfo(userId);
        return Result.success(user);
    }
}
