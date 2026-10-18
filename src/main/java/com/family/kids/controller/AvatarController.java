package com.family.kids.controller;

import com.family.kids.entity.LoginUser;
import com.family.kids.service.AvatarService;
import com.family.kids.util.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class AvatarController {

    @Autowired
    private AvatarService avatarService;

    @PostMapping("/kid/{childId}/avatar")
    public String uploadFromKid(@PathVariable Long childId,
                                @RequestParam("file") MultipartFile file,
                                HttpServletRequest request,
                                RedirectAttributes ra) {
        LoginUser loginUser = AuthContext.current(request);
        if (loginUser == null) {
            return "redirect:/login";
        }
        // 孩子只能换自己的头像；父母不受限
        if (loginUser.isChild() && !Objects.equals(loginUser.getChildId(), childId)) {
            throw new IllegalArgumentException("只能修改自己的头像哦～");
        }
        avatarService.saveAvatar(childId, file);
        ra.addFlashAttribute("msg", "头像已更新 🎉");
        return "redirect:/kid/" + childId;
    }

    @PostMapping("/children/{childId}/avatar")
    public String uploadFromParent(@PathVariable Long childId,
                                   @RequestParam("file") MultipartFile file,
                                   HttpServletRequest request,
                                   RedirectAttributes ra) {
        LoginUser loginUser = AuthContext.current(request);
        if (loginUser == null) {
            return "redirect:/login";
        }
        if (!loginUser.isParent()) {
            throw new IllegalArgumentException("只有家长可以在这里修改头像");
        }
        avatarService.saveAvatar(childId, file);
        ra.addFlashAttribute("msg", "头像已更新");
        return "redirect:/";
    }
}
