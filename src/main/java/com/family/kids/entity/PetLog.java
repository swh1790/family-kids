package com.family.kids.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 灵宠大事记
 */
@Data
public class PetLog {
    private Long id;
    private Long childId;
    private Long petId;
    private String eventType; // LEVEL_UP / MOOD_GOOD / MOOD_BAD / HABIT_DEDUCT / TASK_DONE
    private String title;
    private String detail;
    private LocalDateTime createdAt;
}
