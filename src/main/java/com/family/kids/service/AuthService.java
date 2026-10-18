package com.family.kids.service;

import com.family.kids.entity.LoginUser;
import com.family.kids.entity.User;
import com.family.kids.mapper.UserMapper;
import com.family.kids.util.PasswordHashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 校验用户名口令，成功返回可直接入 Session 的 LoginUser；
     * 失败抛 IllegalArgumentException（由 LoginController 捕获并回显）
     */
    public LoginUser login(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("请输入用户名和密码");
        }
        User user = userMapper.findByUsername(username.trim());
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!PasswordHashUtil.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRole(user.getRole());
        loginUser.setChildId(user.getChildId());
        loginUser.setDisplayName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
        return loginUser;
    }
}
