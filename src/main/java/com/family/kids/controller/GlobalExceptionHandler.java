package com.family.kids.controller;

import com.family.kids.entity.LoginUser;
import com.family.kids.util.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 业务异常统一处理：把「任务不存在」「积分不足」这类提示以页面上方的红色提示条展示，
 * 而不是抛一个 500 错误页给家长/孩子看。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handleBizException(RuntimeException ex,
                                     RedirectAttributes ra,
                                     HttpServletRequest request) {
        ra.addFlashAttribute("error", ex.getMessage());
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        // 没有 Referer 时，孩子回到自己的主页，家长回控制台
        LoginUser loginUser = AuthContext.current(request);
        if (loginUser != null && loginUser.isChild()) {
            return "redirect:/kid/" + loginUser.getChildId();
        }
        return "redirect:/";
    }
}
