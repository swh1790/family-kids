package com.family.kids.controller;

import com.family.kids.service.ChildService;
import com.family.kids.service.PetService;
import com.family.kids.service.ReportService;
import com.family.kids.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class BoardController {

    @Autowired
    private ChildService childService;
    @Autowired
    private PetService petService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ReportService reportService;

    @GetMapping("/board")
    public String board(Model model) {
        var children = childService.listActive();
        List<Map<String, Object>> summary = reportService.dailySummary(LocalDate.now());
        Map<Object, Integer> summaryMap = new HashMap<>();
        for (Map<String, Object> row : summary) {
            summaryMap.put(row.get("child_id"), ((Number) row.get("earned_points")).intValue());
        }
        model.addAttribute("children", children);
        model.addAttribute("summary", summary);
        model.addAttribute("summaryMap", summaryMap);
        model.addAttribute("recentRecords", reportService.recentRecords(8));
        model.addAttribute("petCards", children.stream()
                .map(c -> petService.getPetCard(c.getId()))
                .toList());
        return "board";
    }
}
