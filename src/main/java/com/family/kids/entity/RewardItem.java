package com.family.kids.entity;

import lombok.Data;

/**
 * 奖励商城目录
 */
@Data
public class RewardItem {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer costPoints;
    private Integer stock;   // -1 表示不限
    private Boolean active;
    private Integer sortOrder;
}
