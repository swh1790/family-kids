package com.family.kids.mapper;

import com.family.kids.entity.RewardClaim;
import com.family.kids.entity.RewardItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RewardMapper {

    // 奖励目录
    @Select("SELECT * FROM reward_item WHERE active = TRUE ORDER BY sort_order")
    List<RewardItem> listActive();

    @Select("SELECT * FROM reward_item WHERE id = #{id}")
    RewardItem getById(Long id);

    @Insert("""
        INSERT INTO reward_item (name, description, icon, cost_points, stock, active, sort_order)
        VALUES (#{name}, #{description}, #{icon}, #{costPoints}, #{stock}, #{active}, #{sortOrder})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertItem(RewardItem item);

    @Update("""
        UPDATE reward_item
        SET name = #{name}, description = #{description}, icon = #{icon},
            cost_points = #{costPoints}, stock = #{stock}, active = #{active}, sort_order = #{sortOrder}
        WHERE id = #{id}
        """)
    int updateItem(RewardItem item);

    @Delete("DELETE FROM reward_item WHERE id = #{id}")
    int deleteItem(Long id);

    // 兑换记录
    @Select("""
        SELECT rc.*, c.name AS child_name, c.avatar AS child_avatar,
               ri.name AS reward_name, ri.icon AS reward_icon
        FROM reward_claim rc
        JOIN child c ON rc.child_id = c.id
        JOIN reward_item ri ON rc.reward_id = ri.id
        ORDER BY rc.created_at DESC
        """)
    List<RewardClaim> listClaims();

    @Select("""
        SELECT rc.*, c.name AS child_name, c.avatar AS child_avatar,
               ri.name AS reward_name, ri.icon AS reward_icon
        FROM reward_claim rc
        JOIN child c ON rc.child_id = c.id
        JOIN reward_item ri ON rc.reward_id = ri.id
        WHERE rc.child_id = #{childId}
        ORDER BY rc.created_at DESC
        """)
    List<RewardClaim> listClaimsByChild(Long childId);

    @Insert("""
        INSERT INTO reward_claim (child_id, reward_id, cost_points, status, created_at, granted_at)
        VALUES (#{childId}, #{rewardId}, #{costPoints}, #{status}, NOW(), #{grantedAt})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertClaim(RewardClaim claim);

    @Update("""
        UPDATE reward_claim
        SET status = #{status}, granted_at = #{grantedAt}
        WHERE id = #{id}
        """)
    int updateClaimStatus(RewardClaim claim);
}
