package com.family.kids.mapper;

import com.family.kids.entity.HabitItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HabitMapper {

    @Select("SELECT * FROM habit_item WHERE active = TRUE ORDER BY sort_order")
    List<HabitItem> listActive();

    @Select("SELECT * FROM habit_item WHERE id = #{id}")
    HabitItem getById(Long id);
}
