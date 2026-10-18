package com.family.kids.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 孩子信息
 */
@Data
public class Child {
    private Long id;
    private String name;
    private String nickname;
    private String grade;
    private String avatar;
    private String avatarUrl;
    private String color;
    private Integer growthPoints;
    private Integer balancePoints;
    private Integer earnedPoints;
    private Integer deductedPoints;
    private Integer highestLevel;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
