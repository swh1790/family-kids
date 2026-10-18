package com.family.kids.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 奖励兑换记录
 */
@Data
public class RewardClaim {
    private Long id;
    private Long childId;
    private Long rewardId;
    private Integer costPoints;
    private String status; // PENDING / GRANTED / REJECTED
    private LocalDateTime createdAt;
    private LocalDateTime grantedAt;

    // 展示用
    private String childName;
    private String childAvatar;
    private String rewardName;
    private String rewardIcon;
}
