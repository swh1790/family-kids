package com.family.kids.util;

import com.family.kids.entity.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 从 Session 中取当前登录用户（不会主动创建 Session）
 */
public final class AuthContext {

    private AuthContext() {
    }

    public static LoginUser current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (LoginUser) session.getAttribute(Constants.SESSION_LOGIN_USER);
    }
}
