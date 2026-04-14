package com.flashsale.service.impl;

import com.flashsale.common.BusinessException;
import com.flashsale.common.ResultCode;
import com.flashsale.entity.User;
import com.flashsale.mapper.UserMapper;
import com.flashsale.service.UserService;
import com.flashsale.datasource.Master;
import com.flashsale.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Master  // 写操作：强制路由到主库
    @Override
    public User register(String username, String password, String nickname, String phone) {
        // 检查用户名是否已存在
        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 检查手机号是否已注册
        if (phone != null && !phone.isEmpty() && userMapper.selectByPhone(phone) != null) {
            throw new BusinessException(ResultCode.PHONE_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(ENCODER.encode(password));
        user.setNickname(nickname != null ? nickname : username);
        user.setPhone(phone);
        user.setStatus(1);

        userMapper.insert(user);
        log.info("用户注册成功: userId={}, username={}", user.getId(), username);

        // 清除密码后返回
        user.setPassword(null);
        return user;
    }

    @Override
    public String login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        if (!ENCODER.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        log.info("用户登录成功: userId={}, username={}", user.getId(), username);
        return token;
    }

    @Override
    public User getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(null);
        return user;
    }
}
