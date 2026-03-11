package com.flashsale.service;

import com.flashsale.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    User register(String username, String password, String nickname, String phone);

    /**
     * 用户登录
     */
    String login(String username, String password);

    /**
     * 获取用户信息
     */
    User getUserInfo(Long userId);
}
