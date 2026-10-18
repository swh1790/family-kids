package com.family.kids.config;

import com.family.kids.entity.LoginUser;
import com.family.kids.util.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.family.kids.util.Constants.*;

/**
 * 登录 + 角色权限拦截器。
 * 只做「路径 + 角色」判断；涉及「这条任务是不是我的」的资源级校验，
 * 放在对应 Controller 里做，以便复用现有 GlobalExceptionHandler 的友好提示条。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String ctx = request.getContextPath();
        String path = request.getRequestURI().substring(ctx.length());
        String query = request.getQueryString();

        LoginUser user = AuthContext.current(request);

        // ---- 1. 未登录 ----
        if (user == null) {
            if (path.startsWith("/api/")) {
                writeJson(response, 401, "未登录或登录已过期，请重新登录");
                return false;
            }
            HttpSession session = request.getSession(true);
            if (!"/login".equals(path)) {
                session.setAttribute(SESSION_REDIRECT_AFTER_LOGIN,
                        query == null ? path : path + "?" + query);
            }
            response.sendRedirect(ctx + "/login");
            return false;
        }

        // ---- 2. 孩子角色越权 ----
        if (user.isChild() && !childAllowed(path, user)) {
            if (path.startsWith("/api/")) {
                writeJson(response, 403, "小朋友只能操作自己的主页和任务哦");
                return false;
            }
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_AUTH_ERROR, "这个页面只有爸爸妈妈能进哦～");
            response.sendRedirect(ctx + "/kid/" + user.getChildId());
            return false;
        }

        return true;
    }

    /**
     * 孩子角色允许访问的路径白名单（其余一律拒绝）
     */
    private boolean childAllowed(String path, LoginUser user) {
        // 自己的主页 / 自己的头像上传
        if (path.matches("/kid/" + user.getChildId() + "(/avatar)?")) {
            return true;
        }
        // 提交任务：允许放行，任务归属在 TaskController / ApiController 内二次校验
        if (path.matches("/tasks/\\d+/submit") || path.matches("/api/tasks/\\d+/submit")) {
            return true;
        }
        if (path.equals("/logout")) {
            return true;
        }
        return false;
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }
}
