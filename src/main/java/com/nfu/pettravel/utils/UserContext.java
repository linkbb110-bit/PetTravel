package com.nfu.pettravel.utils;

import com.nfu.pettravel.enums.UserRole;

public final class UserContext {
    private static final ThreadLocal<AuthenticatedUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUser(Long userId, UserRole role) {
        CURRENT_USER.set(new AuthenticatedUser(userId, role));
    }

    public static void setUserId(Long userId) {
        setUser(userId, UserRole.USER);
    }

    public static Long getUserId() {
        AuthenticatedUser user = CURRENT_USER.get();
        return user == null ? null : user.userId();
    }

    public static UserRole getRole() {
        AuthenticatedUser user = CURRENT_USER.get();
        return user == null ? null : user.role();
    }

    public static void removeUserId() {
        CURRENT_USER.remove();
    }

    private record AuthenticatedUser(Long userId, UserRole role) {
    }
}
