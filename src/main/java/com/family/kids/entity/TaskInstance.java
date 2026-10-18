package com.family.kids.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务实例（今日任务）
 */
@Data
public class TaskInstance {
    private Long id;
    private Long childId;
    private Long templateId;
    private Integer categoryId;
    private String title;
    private Integer points;
    private LocalDate planDate;
    private String status;     // PENDING / DONE / APPROVED / REJECTED / MISSED
    private String source;     // AUTO / MANUAL
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private String approver;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 展示用
    private String childName;
    private String childAvatar;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
}
