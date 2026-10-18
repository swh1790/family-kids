package com.family.kids.controller;

import com.family.kids.entity.LoginUser;
import com.family.kids.service.AuthService;
import com.family.kids.util.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.family.kids.util.Constants.*;

@Controller
public class LoginController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        if (AuthContext.current(request) != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpServletRequest request,
                          Model model) {
        try {
            LoginUser loginUser = authService.login(username, password);
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_LOGIN_USER, loginUser);
            session.removeAttribute(SESSION_AUTH_ERROR);

            Object target = session.getAttribute(SESSION_REDIRECT_AFTER_LOGIN);
            session.removeAttribute(SESSION_REDIRECT_AFTER_LOGIN);
            if (target instanceof String t && !t.isBlank()) {
                return "redirect:" + t;
            }
            return loginUser.isChild() ? "redirect:/kid/" + loginUser.getChildId() : "redirect:/";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}
