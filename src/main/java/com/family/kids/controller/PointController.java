package com.family.kids.controller;

import com.family.kids.mapper.PointRecordMapper;
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
@RequestMapping("/points")
public class PointController {

    @Autowired
    private PointService pointService;
    @Autowired
    private ChildService childService;
    @Autowired
    private PointRecordMapper pointRecordMapper;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("children", childService.listActive());
        model.addAttribute("records", pointRecordMapper.listRecent(50));
        return "points";
    }

    @PostMapping("/adjust")
    public String adjust(@RequestParam Long childId,
                         @RequestParam Integer points,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes ra) {
        String r = reason != null && !reason.isBlank() ? reason : "家长手动调整";
        pointService.manualAdjust(childId, points, r, "家长");
        ra.addFlashAttribute("msg", "已调整 " + points + " 积分");
        return "redirect:/points";
    }
}
