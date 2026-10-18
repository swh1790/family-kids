package com.family.kids.entity;

import lombok.Data;

/**
 * 坏习惯/扣分项
 */
@Data
public class HabitItem {
    private Long id;
    private String name;
    private Integer points;
    private String icon;
    private Boolean active;
    private Integer sortOrder;
}
