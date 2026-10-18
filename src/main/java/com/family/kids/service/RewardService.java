package com.family.kids.service;

import com.family.kids.entity.Child;
import com.family.kids.entity.RewardClaim;
import com.family.kids.entity.RewardItem;
import com.family.kids.mapper.ChildMapper;
import com.family.kids.mapper.RewardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.family.kids.util.Constants.*;

@Service
public class RewardService {

    @Autowired
    private RewardMapper rewardMapper;
    @Autowired
    private ChildMapper childMapper;
    @Autowired
    private PointService pointService;

    @Value("${app.default-operator:家长}")
    private String defaultOperator;

    public List<RewardItem> listActiveItems() {
        return rewardMapper.listActive();
    }

    public RewardItem getItem(Long id) {
        return rewardMapper.getById(id);
    }

    @Transactional
    public void saveItem(RewardItem item) {
        if (item.getId() == null) {
            rewardMapper.insertItem(item);
        } else {
            rewardMapper.updateItem(item);
        }
    }

    @Transactional
    public void deleteItem(Long id) {
        rewardMapper.deleteItem(id);
    }

    public List<RewardClaim> listClaims() {
        return rewardMapper.listClaims();
    }

    public List<RewardClaim> listClaimsByChild(Long childId) {
        return rewardMapper.listClaimsByChild(childId);
    }

    /**
     * 孩子申请兑换奖励
     */
    @Transactional
    public void claim(Long childId, Long rewardId) {
        Child child = childMapper.getById(childId);
        RewardItem item = rewardMapper.getById(rewardId);
        if (item == null || !Boolean.TRUE.equals(item.getActive())) {
            throw new IllegalArgumentException("奖励不存在或已下架");
        }
        if (item.getStock() == 0) {
            throw new IllegalStateException("奖励库存不足");
        }
        if (child.getBalancePoints() < item.getCostPoints()) {
            throw new IllegalStateException("积分不足，还差 " + (item.getCostPoints() - child.getBalancePoints()) + " 分");
        }

        // 扣减可用积分
        pointService.exchangeReward(childId, item.getCostPoints(), rewardId,
                "兑换奖励：" + item.getName(), child.getName());

        // 库存 -1（-1 表示不限）
        if (item.getStock() != null && item.getStock() > 0) {
            item.setStock(item.getStock() - 1);
            rewardMapper.updateItem(item);
        }

        RewardClaim claim = new RewardClaim();
        claim.setChildId(childId);
        claim.setRewardId(rewardId);
        claim.setCostPoints(item.getCostPoints());
        claim.setStatus(CLAIM_PENDING);
        rewardMapper.insertClaim(claim);
    }

    /**
     * 家长确认兑现
     */
    @Transactional
    public void grantClaim(Long claimId) {
        RewardClaim claim = rewardMapper.listClaims().stream()
                .filter(c -> c.getId().equals(claimId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("兑换记录不存在"));
        if (!CLAIM_PENDING.equals(claim.getStatus())) {
            throw new IllegalStateException("该兑换已处理");
        }
        claim.setStatus(CLAIM_GRANTED);
        claim.setGrantedAt(LocalDateTime.now());
        rewardMapper.updateClaimStatus(claim);
    }

    /**
     * 家长拒绝兑换（积分不退，视为惩罚或家长反悔可手动补回）
     */
    @Transactional
    public void rejectClaim(Long claimId) {
        RewardClaim claim = rewardMapper.listClaims().stream()
                .filter(c -> c.getId().equals(claimId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("兑换记录不存在"));
        if (!CLAIM_PENDING.equals(claim.getStatus())) {
            throw new IllegalStateException("该兑换已处理");
        }
        claim.setStatus(CLAIM_REJECTED);
        rewardMapper.updateClaimStatus(claim);
    }
}
