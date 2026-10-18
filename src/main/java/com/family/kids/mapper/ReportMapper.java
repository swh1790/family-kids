package com.family.kids.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    /**
     * 某日各孩子的任务完成情况汇总
     */
    List<Map<String, Object>> dailySummary(@Param("planDate") LocalDate planDate);

    /**
     * 日期区间内各孩子每日获得积分
     */
    List<Map<String, Object>> weeklyPoints(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * 日期区间内各任务分类获得积分
     */
    List<Map<String, Object>> categoryPoints(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
}
