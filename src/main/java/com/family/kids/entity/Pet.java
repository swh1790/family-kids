package com.family.kids.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 灵宠
 */
@Data
public class Pet {
    private Long id;
    private Long childId;
    private String name;
    private String species;
    private String avatar;
    private Integer level;
    private String stage;
    private Integer exp;
    private Integer mood;
    private LocalDateTime lastLevelUpAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
