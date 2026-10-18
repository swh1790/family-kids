package com.family.kids.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分与成长值流水
 */
@Data
public class PointRecord {
    private Long id;
    private Long childId;
    private Integer changePoints;
    private Integer balanceAfter;
    private Integer growthAfter;
    private String type;   // EARN / DEDUCT / EXCHANGE / ADJUST
    private String source; // TASK / HABIT / MANUAL / REWARD / LEVELUP
    private Long refId;
    private String reason;
    private String operator;
    private LocalDateTime createdAt;

    // 展示用
    private String childName;
    private String childAvatar;
}
