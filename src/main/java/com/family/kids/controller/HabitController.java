package com.family.kids.controller;

import com.family.kids.mapper.HabitMapper;
import com.family.kids.service.ChildService;
import com.family.kids.service.PointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/habits")
public class HabitController {

    @Autowired
    private HabitMapper habitMapper;
    @Autowired
    private ChildService childService;
    @Autowired
    private PointService pointService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("habits", habitMapper.listActive());
        model.addAttribute("children", childService.listActive());
        return "habits";
    }

    @PostMapping("/deduct")
    public String deduct(@RequestParam Long childId,
                         @RequestParam Long habitId,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes ra) {
        var habit = habitMapper.getById(habitId);
        if (habit == null) {
            ra.addFlashAttribute("err", "坏习惯项不存在");
            return "redirect:/habits";
        }
        String r = reason != null && !reason.isBlank() ? reason : habit.getName();
        pointService.deductFromHabit(childId, habit.getPoints(), habitId,
                "坏习惯：" + r, "家长");
        ra.addFlashAttribute("msg", "已记录坏习惯并扣分 " + habit.getPoints());
        return "redirect:/habits";
    }
}
