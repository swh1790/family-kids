package com.family.kids.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务模板
 */
@Data
public class TaskTemplate {
    private Long id;
    private Long childId;
    private Integer categoryId;
    private String title;
    private String description;
    private Integer defaultPoints;
    private String frequency;   // DAILY / WEEKLY / ONCE
    private String weekdays;    // 0-6 逗号分隔
    private Boolean requireApproval;
    private Boolean active;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 展示用
    private String categoryName;
    private String categoryIcon;
    private String childName;
}
