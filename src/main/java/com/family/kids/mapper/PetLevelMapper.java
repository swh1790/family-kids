package com.family.kids.mapper;

import com.family.kids.entity.PetLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PetLevelMapper {

    @Select("SELECT * FROM pet_level ORDER BY level")
    List<PetLevel> listAll();

    @Select("SELECT * FROM pet_level WHERE level = #{level}")
    PetLevel getByLevel(Integer level);
}
