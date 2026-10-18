package com.family.kids.entity;

import lombok.Data;

/**
 * 任务分类
 */
@Data
public class TaskCategory {
    private Integer id;
    private String code;
    private String name;
    private String icon;
    private String color;
    private Integer sortOrder;
    private String pointHint;
}
