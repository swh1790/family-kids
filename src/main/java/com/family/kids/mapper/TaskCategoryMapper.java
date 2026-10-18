package com.family.kids.mapper;

import com.family.kids.entity.TaskCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskCategoryMapper {

    @Select("SELECT * FROM task_category ORDER BY sort_order")
    List<TaskCategory> listAll();
}
