package com.family.kids.mapper;

import com.family.kids.entity.PetLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PetLogMapper {

    @Insert("""
        INSERT INTO pet_log (child_id, pet_id, event_type, title, detail, created_at)
        VALUES (#{childId}, #{petId}, #{eventType}, #{title}, #{detail}, NOW())
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PetLog log);

    @Select("SELECT * FROM pet_log WHERE child_id = #{childId} ORDER BY created_at DESC LIMIT #{limit}")
    List<PetLog> listByChild(@Param("childId") Long childId, @Param("limit") int limit);
}
