package com.family.kids.mapper;

import com.family.kids.entity.PointRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PointRecordMapper {

    @Insert("""
        INSERT INTO point_record (child_id, change_points, balance_after, growth_after,
                                  type, source, ref_id, reason, operator, created_at)
        VALUES (#{childId}, #{changePoints}, #{balanceAfter}, #{growthAfter},
                #{type}, #{source}, #{refId}, #{reason}, #{operator}, NOW())
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PointRecord record);

    @Select("""
        SELECT r.*, c.name AS child_name, c.avatar AS child_avatar
        FROM point_record r
        JOIN child c ON r.child_id = c.id
        ORDER BY r.created_at DESC
        LIMIT #{limit}
        """)
    List<PointRecord> listRecent(int limit);

    @Select("""
        SELECT r.*, c.name AS child_name, c.avatar AS child_avatar
        FROM point_record r
        JOIN child c ON r.child_id = c.id
        WHERE r.child_id = #{childId}
        ORDER BY r.created_at DESC
        LIMIT #{limit}
        """)
    List<PointRecord> listByChild(@Param("childId") Long childId, @Param("limit") int limit);
}
