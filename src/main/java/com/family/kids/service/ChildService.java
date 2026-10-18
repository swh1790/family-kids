package com.family.kids.service;

import com.family.kids.entity.Child;
import com.family.kids.mapper.ChildMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChildService {

    @Autowired
    private ChildMapper childMapper;

    public List<Child> listActive() {
        return childMapper.listActive();
    }

    public Child getById(Long id) {
        return childMapper.getById(id);
    }

    public String getAvatarUrl(Long id) {
        return childMapper.getAvatarUrl(id);
    }

    @Transactional
    public void updateAvatarUrl(Long id, String avatarUrl) {
        childMapper.updateAvatarUrl(id, avatarUrl);
    }

    @Transactional
    public void add(Child child) {
        if (child.getGrowthPoints() == null) child.setGrowthPoints(0);
        if (child.getBalancePoints() == null) child.setBalancePoints(0);
        if (child.getEarnedPoints() == null) child.setEarnedPoints(0);
        if (child.getDeductedPoints() == null) child.setDeductedPoints(0);
        if (child.getHighestLevel() == null) child.setHighestLevel(1);
        if (child.getActive() == null) child.setActive(true);
        if (child.getAvatar() == null) child.setAvatar("👦");
        if (child.getColor() == null) child.setColor("#0d6efd");
        childMapper.insert(child);
    }

    @Transactional
    public void update(Child child) {
        childMapper.update(child);
    }
}
