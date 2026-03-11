package com.flashsale.util;

/**
 * 用户上下文（ThreadLocal 存储当前登录用户信息）
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> USER_HOLDER = new ThreadLocal<>();

    public static void set(Long userId, String username) {
        USER_HOLDER.set(new UserInfo(userId, username));
    }

    public static Long getUserId() {
        UserInfo info = USER_HOLDER.get();
        return info == null ? null : info.userId;
    }

    public static String getUsername() {
        UserInfo info = USER_HOLDER.get();
        return info == null ? null : info.username;
    }

    public static void clear() {
        USER_HOLDER.remove();
    }

    private record UserInfo(Long userId, String username) {}
}
