package com.family.kids.config;

import com.family.kids.util.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import static com.family.kids.util.Constants.*;

/**
 * 把登录身份注入所有页面模型，Thymeleaf 里直接用 ${loginUser}；
 * 同时消费拦截器写的一次性错误提示（复用 fragments :: alerts 展示）。
 */
@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute
    public void addAuthInfo(HttpServletRequest request, Model model) {
        model.addAttribute("loginUser", AuthContext.current(request));

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object authError = session.getAttribute(SESSION_AUTH_ERROR);
            if (authError != null) {
                model.addAttribute("error", authError);
                session.removeAttribute(SESSION_AUTH_ERROR);
            }
        }
    }
}
