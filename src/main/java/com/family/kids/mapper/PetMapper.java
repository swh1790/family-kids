package com.family.kids.mapper;

import com.family.kids.entity.Pet;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PetMapper {

    @Select("SELECT * FROM pet WHERE child_id = #{childId}")
    Pet getByChildId(Long childId);

    @Insert("""
        INSERT INTO pet (child_id, name, species, avatar, level, stage, exp, mood,
                         last_level_up_at, created_at, updated_at)
        VALUES (#{childId}, #{name}, #{species}, #{avatar}, #{level}, #{stage}, #{exp}, #{mood},
                #{lastLevelUpAt}, NOW(), NOW())
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Pet pet);

    @Update("""
        UPDATE pet
        SET name = #{name}, species = #{species}, avatar = #{avatar},
            level = #{level}, stage = #{stage}, exp = #{exp}, mood = #{mood},
            last_level_up_at = #{lastLevelUpAt}, updated_at = NOW()
        WHERE child_id = #{childId}
        """)
    int update(Pet pet);
}
