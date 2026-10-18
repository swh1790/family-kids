package com.family.kids.controller;

import com.family.kids.entity.RewardItem;
import com.family.kids.service.ChildService;
import com.family.kids.service.RewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rewards")
public class RewardController {

    @Autowired
    private RewardService rewardService;
    @Autowired
    private ChildService childService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("items", rewardService.listActiveItems());
        model.addAttribute("claims", rewardService.listClaims());
        model.addAttribute("children", childService.listActive());
        model.addAttribute("item", new RewardItem());
        return "rewards";
    }

    @PostMapping("/save")
    public String save(RewardItem item, RedirectAttributes ra) {
        if (item.getStock() == null) {
            item.setStock(-1);
        }
        if (item.getActive() == null) {
            item.setActive(true);
        }
        rewardService.saveItem(item);
        ra.addFlashAttribute("msg", "奖励已保存");
        return "redirect:/rewards";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        rewardService.deleteItem(id);
        ra.addFlashAttribute("msg", "奖励已删除");
        return "redirect:/rewards";
    }

    @PostMapping("/claim")
    public String claim(@RequestParam Long childId,
                        @RequestParam Long rewardId,
                        RedirectAttributes ra) {
        try {
            rewardService.claim(childId, rewardId);
            ra.addFlashAttribute("msg", "兑换申请已提交，等待家长确认");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/rewards";
    }

    @PostMapping("/claim/{id}/grant")
    public String grant(@PathVariable Long id, RedirectAttributes ra) {
        rewardService.grantClaim(id);
        ra.addFlashAttribute("msg", "已确认兑现奖励");
        return "redirect:/rewards";
    }

    @PostMapping("/claim/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes ra) {
        rewardService.rejectClaim(id);
        ra.addFlashAttribute("msg", "已拒绝兑换");
        return "redirect:/rewards";
    }
}
