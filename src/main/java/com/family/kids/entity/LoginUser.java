package com.family.kids.entity;

import lombok.Data;

/**
 * 存放在 HttpSession 中的登录身份（不含 passwordHash，避免口令摘要进会话）
 */
@Data
public class LoginUser {
    private Long id;
    private String username;
    private String role;        // PARENT / CHILD
    private Long childId;       // 父母为 null
    private String displayName;

    public boolean isParent() {
        return "PARENT".equals(role);
    }

    public boolean isChild() {
        return "CHILD".equals(role);
    }
}
