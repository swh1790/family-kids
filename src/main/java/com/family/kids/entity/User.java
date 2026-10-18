package com.family.kids.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录账号（对应 family_user 表；表名避开 PostgreSQL 保留字 user）
 */
@Data
public class User {
    private Long id;
    private String username;
    private String passwordHash;   // password_hash -> passwordHash
    private String role;           // PARENT / CHILD
    private Long childId;          // 父母为 null
    private String displayName;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
