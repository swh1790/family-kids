package com.family.kids.mapper;

import com.family.kids.entity.Child;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChildMapper {

    @Select("SELECT * FROM child WHERE active = TRUE ORDER BY id")
    List<Child> listActive();

    @Select("SELECT * FROM child WHERE id = #{id}")
    Child getById(Long id);

    @Select("SELECT avatar_url FROM child WHERE id = #{id}")
    String getAvatarUrl(Long id);

    @Update("UPDATE child SET avatar_url = #{avatarUrl}, updated_at = NOW() WHERE id = #{id}")
    int updateAvatarUrl(@Param("id") Long id, @Param("avatarUrl") String avatarUrl);

    @Insert("""
        INSERT INTO child (name, nickname, grade, avatar, color,
                           growth_points, balance_points, earned_points, deducted_points,
                           highest_level, active, created_at, updated_at)
        VALUES (#{name}, #{nickname}, #{grade}, #{avatar}, #{color},
                #{growthPoints}, #{balancePoints}, #{earnedPoints}, #{deductedPoints},
                #{highestLevel}, #{active}, NOW(), NOW())
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Child child);

    @Update("""
        UPDATE child
        SET name = #{name}, nickname = #{nickname}, grade = #{grade},
            avatar = #{avatar}, color = #{color}, updated_at = NOW()
        WHERE id = #{id}
        """)
    int update(Child child);

    @Update("""
        UPDATE child
        SET growth_points = #{growthPoints}, balance_points = #{balancePoints},
            earned_points = #{earnedPoints}, deducted_points = #{deductedPoints},
            highest_level = #{highestLevel}, updated_at = NOW()
        WHERE id = #{id}
        """)
    int updatePoints(Child child);
}
