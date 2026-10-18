package com.family.kids.mapper;

import com.family.kids.entity.TaskInstance;
import com.family.kids.entity.TaskTemplate;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TaskMapper {

    // ========== 任务模板 ==========

    @Select("""
        SELECT t.*, c.name AS category_name, c.icon AS category_icon
        FROM task_template t
        JOIN task_category c ON t.category_id = c.id
        WHERE t.active = TRUE
        ORDER BY t.sort_order, t.id
        """)
    List<TaskTemplate> listTemplates();

    @Select("""
        SELECT t.*, c.name AS category_name, c.icon AS category_icon
        FROM task_template t
        JOIN task_category c ON t.category_id = c.id
        WHERE t.active = TRUE
          AND (t.child_id IS NULL OR t.child_id = #{childId})
        ORDER BY t.sort_order, t.id
        """)
    List<TaskTemplate> listTemplatesForChild(Long childId);

    @Select("SELECT * FROM task_template WHERE id = #{id}")
    TaskTemplate getTemplateById(Long id);

    @Insert("""
        INSERT INTO task_template (child_id, category_id, title, description, default_points,
                                   frequency, weekdays, require_approval, active, sort_order, created_at, updated_at)
        VALUES (#{childId}, #{categoryId}, #{title}, #{description}, #{defaultPoints},
                #{frequency}, #{weekdays}, #{requireApproval}, #{active}, #{sortOrder}, NOW(), NOW())
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTemplate(TaskTemplate template);

    @Update("""
        UPDATE task_template
        SET child_id = #{childId}, category_id = #{categoryId}, title = #{title},
            description = #{description}, default_points = #{defaultPoints},
            frequency = #{frequency}, weekdays = #{weekdays},
            require_approval = #{requireApproval}, active = #{active},
            sort_order = #{sortOrder}, updated_at = NOW()
        WHERE id = #{id}
        """)
    int updateTemplate(TaskTemplate template);

    @Delete("DELETE FROM task_template WHERE id = #{id}")
    int deleteTemplate(Long id);

    // ========== 任务实例 ==========

    @Insert("""
        INSERT INTO task_instance (child_id, template_id, category_id, title, points,
                                   plan_date, status, source, submitted_at, approved_at,
                                   approver, remark, created_at, updated_at)
        VALUES (#{childId}, #{templateId}, #{categoryId}, #{title}, #{points},
                #{planDate}, #{status}, #{source}, #{submittedAt}, #{approvedAt},
                #{approver}, #{remark}, NOW(), NOW())
        ON CONFLICT (child_id, template_id, plan_date) DO NOTHING
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertInstance(TaskInstance instance);

    @Select("""
        <script>
        SELECT i.*, c.name AS child_name, c.avatar AS child_avatar,
               cat.name AS category_name, cat.icon AS category_icon, cat.color AS category_color
        FROM task_instance i
        JOIN child c ON i.child_id = c.id
        JOIN task_category cat ON i.category_id = cat.id
        WHERE i.plan_date = #{planDate}
        <if test='childId != null'>AND i.child_id = #{childId}</if>
        ORDER BY c.id, cat.sort_order, i.id
        </script>
        """)
    List<TaskInstance> listInstancesByDate(@Param("planDate") LocalDate planDate,
                                           @Param("childId") Long childId);

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM task_instance
        WHERE plan_date = #{planDate}
        <if test='childId != null'>AND child_id = #{childId}</if>
        <if test='status != null'>AND status = #{status}</if>
        </script>
        """)
    int countByDateStatus(@Param("planDate") LocalDate planDate,
                          @Param("childId") Long childId,
                          @Param("status") String status);

    @Select("SELECT * FROM task_instance WHERE id = #{id}")
    TaskInstance getInstanceById(Long id);

    @Update("""
        UPDATE task_instance
        SET status = #{status}, submitted_at = #{submittedAt}, approved_at = #{approvedAt},
            approver = #{approver}, remark = #{remark}, updated_at = NOW()
        WHERE id = #{id}
        """)
    int updateInstanceStatus(TaskInstance instance);

    @Delete("DELETE FROM task_instance WHERE id = #{id}")
    int deleteInstance(Long id);
}
