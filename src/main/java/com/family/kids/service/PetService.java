package com.family.kids.service;

import com.family.kids.entity.Child;
import com.family.kids.entity.Pet;
import com.family.kids.entity.PetLevel;
import com.family.kids.entity.PetLog;
import com.family.kids.mapper.ChildMapper;
import com.family.kids.mapper.PetLevelMapper;
import com.family.kids.mapper.PetLogMapper;
import com.family.kids.mapper.PetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PetService {

    @Autowired
    private ChildMapper childMapper;
    @Autowired
    private PetMapper petMapper;
    @Autowired
    private PetLogMapper petLogMapper;
    @Autowired
    private PetLevelMapper petLevelMapper;
    @Autowired
    private PointService pointService;

    public Map<String, Object> getPetCard(Long childId) {
        Child child = childMapper.getById(childId);
        Pet pet = petMapper.getByChildId(childId);
        PetLevel current = pointService.computeLevel(child.getGrowthPoints());
        int nextExp = pointService.expToNextLevel(child.getGrowthPoints());
        int progress = progressPercent(child.getGrowthPoints(), current);
        List<PetLog> logs = petLogMapper.listByChild(childId, 20);

        Map<String, Object> card = new HashMap<>();
        card.put("child", child);
        card.put("pet", pet);
        card.put("level", current);
        card.put("nextExp", nextExp);
        card.put("progress", progress);
        card.put("logs", logs);
        return card;
    }

    public List<PetLevel> listLevels() {
        return petLevelMapper.listAll();
    }

    private int progressPercent(int exp, PetLevel current) {
        PetLevel next = nextLevel(current.getLevel());
        if (next == null) {
            return 100;
        }
        int diff = next.getMinExp() - current.getMinExp();
        if (diff <= 0) {
            return 100;
        }
        int pct = (int) (((double) (exp - current.getMinExp()) / diff) * 100);
        return Math.min(100, Math.max(0, pct));
    }

    private PetLevel nextLevel(int currentLevel) {
        List<PetLevel> levels = petLevelMapper.listAll();
        for (PetLevel lv : levels) {
            if (lv.getLevel() == currentLevel + 1) {
                return lv;
            }
        }
        return null;
    }
}
