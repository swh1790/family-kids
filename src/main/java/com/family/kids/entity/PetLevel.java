package com.family.kids.entity;

import lombok.Data;

/**
 * 灵宠等级配置
 */
@Data
public class PetLevel {
    private Integer level;
    private String stageName;
    private Integer minExp;
    private String icon;
    private String rewardTitle;
    private String rewardDesc;
}
