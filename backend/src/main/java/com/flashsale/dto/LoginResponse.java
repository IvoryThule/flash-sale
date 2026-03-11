package com.flashsale.dto;

import lombok.Data;

/**
 * 登录响应（返回 Token）
 */
@Data
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private String nickname;

    public LoginResponse(String token, Long userId, String username, String nickname) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
    }
}
