package com.family.kids.service;

import com.family.kids.entity.Child;
import com.family.kids.entity.Pet;
import com.family.kids.entity.PetLevel;
import com.family.kids.entity.PetLog;
import com.family.kids.entity.PointRecord;
import com.family.kids.mapper.ChildMapper;
import com.family.kids.mapper.PetLevelMapper;
import com.family.kids.mapper.PetLogMapper;
import com.family.kids.mapper.PetMapper;
import com.family.kids.mapper.PointRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.family.kids.util.Constants.*;

/**
 * 积分与成长值核心引擎
 */
@Service
public class PointService {

    @Autowired
    private ChildMapper childMapper;
    @Autowired
    private PetMapper petMapper;
    @Autowired
    private PetLevelMapper petLevelMapper;
    @Autowired
    private PointRecordMapper pointRecordMapper;
    @Autowired
    private PetLogMapper petLogMapper;

    /**
     * 通过任务获得积分
     */
    @Transactional
    public void earnFromTask(Long childId, int points, Long refId, String reason, String operator) {
        addPoints(childId, Math.abs(points), TYPE_EARN, SRC_TASK, refId, reason, operator);
    }

    /**
     * 坏习惯扣分
     */
    @Transactional
    public void deductFromHabit(Long childId, int points, Long refId, String reason, String operator) {
        addPoints(childId, -Math.abs(points), TYPE_DEDUCT, SRC_HABIT, refId, reason, operator);
    }

    /**
     * 兑换奖励消耗积分
     */
    @Transactional
    public void exchangeReward(Long childId, int points, Long refId, String reason, String operator) {
        addPoints(childId, -Math.abs(points), TYPE_EXCHANGE, SRC_REWARD, refId, reason, operator);
    }

    /**
     * 家长手动调整积分（可正可负）
     */
    @Transactional
    public void manualAdjust(Long childId, int points, String reason, String operator) {
        String type = points >= 0 ? TYPE_ADJUST : TYPE_DEDUCT;
        addPoints(childId, points, type, SRC_MANUAL, null, reason, operator);
    }

    private void addPoints(Long childId, int delta, String type, String source,
                           Long refId, String reason, String operator) {
        Child child = childMapper.getById(childId);
        if (child == null) {
            throw new IllegalArgumentException("孩子不存在: " + childId);
        }
        Pet pet = petMapper.getByChildId(childId);
        if (pet == null) {
            throw new IllegalStateException("孩子尚未创建灵宠: " + childId);
        }

        // 可用积分（有下限 0）：所有类型都影响
        int balance = Math.max(0, child.getBalancePoints() + delta);

        // 成长值（有下限 0）：兑换奖励只花"可用积分"，不扣成长值，
        // 否则孩子攒分换奖励会导致灵宠掉级，反而打击积极性。
        boolean isExchange = TYPE_EXCHANGE.equals(type);
        int growth = isExchange ? child.getGrowthPoints()
                                : Math.max(0, child.getGrowthPoints() + delta);

        // 累计统计：earned 统计获得；deducted 只统计坏习惯/惩罚扣分（兑换属消费，不计入）
        int earned = child.getEarnedPoints();
        int deducted = child.getDeductedPoints();
        if (delta > 0) {
            earned += delta;
        } else if (TYPE_DEDUCT.equals(type)) {
            deducted += Math.abs(delta);
        }

        // 计算等级
        PetLevel currentLevel = computeLevel(growth);
        int newLevel = currentLevel.getLevel();
        int highest = Math.max(child.getHighestLevel(), newLevel);

        // 更新孩子
        child.setGrowthPoints(growth);
        child.setBalancePoints(balance);
        child.setEarnedPoints(earned);
        child.setDeductedPoints(deducted);
        child.setHighestLevel(highest);
        childMapper.updatePoints(child);

        // 更新灵宠
        boolean leveledUp = newLevel > pet.getLevel();
        pet.setExp(growth);
        pet.setLevel(newLevel);
        pet.setStage(currentLevel.getStageName());
        pet.setAvatar(currentLevel.getIcon());
        if (leveledUp) {
            pet.setLastLevelUpAt(LocalDateTime.now());
        }

        // 心情值：加分 +5，惩罚扣分 -5，兑换奖励 +5（兑现心愿是开心事），限制在 0-100
        int moodDelta = TYPE_DEDUCT.equals(type) ? -5 : 5;
        int mood = Math.min(100, Math.max(0, pet.getMood() + moodDelta));
        pet.setMood(mood);
        petMapper.update(pet);

        // 流水
        PointRecord record = new PointRecord();
        record.setChildId(childId);
        record.setChangePoints(delta);
        record.setBalanceAfter(balance);
        record.setGrowthAfter(growth);
        record.setType(type);
        record.setSource(source);
        record.setRefId(refId);
        record.setReason(reason);
        record.setOperator(operator);
        pointRecordMapper.insert(record);

        // 升级大事记
        if (leveledUp) {
            PetLog log = new PetLog();
            log.setChildId(childId);
            log.setPetId(pet.getId());
            log.setEventType(EVT_LEVEL_UP);
            log.setTitle("🎉 灵宠升级到 Lv" + newLevel + " " + currentLevel.getStageName());
            log.setDetail("解锁奖励：" + currentLevel.getRewardTitle() + " — " + currentLevel.getRewardDesc());
            petLogMapper.insert(log);
        }
    }

    /**
     * 根据成长值计算当前等级
     */
    public PetLevel computeLevel(int exp) {
        List<PetLevel> levels = petLevelMapper.listAll();
        PetLevel current = levels.get(0);
        for (PetLevel lv : levels) {
            if (exp >= lv.getMinExp()) {
                current = lv;
            }
        }
        return current;
    }

    /**
     * 计算到下一级还需多少成长值
     */
    public int expToNextLevel(int exp) {
        List<PetLevel> levels = petLevelMapper.listAll();
        PetLevel current = computeLevel(exp);
        for (PetLevel lv : levels) {
            if (lv.getLevel() > current.getLevel()) {
                return Math.max(0, lv.getMinExp() - exp);
            }
        }
        return 0;
    }
}
