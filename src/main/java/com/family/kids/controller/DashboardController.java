package com.family.kids.controller;

import com.family.kids.entity.Child;
import com.family.kids.service.ChildService;
import com.family.kids.service.PetService;
import com.family.kids.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private ReportService reportService;
    @Autowired
    private ChildService childService;
    @Autowired
    private PetService petService;

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            Model model) {
        if (date == null) {
            date = LocalDate.now();
        }
        Map<String, Object> data = reportService.dashboard(date);
        List<Child> children = childService.listActive();
        List<Map<String, Object>> petCards = children.stream()
                .map(c -> petService.getPetCard(c.getId()))
                .toList();
        model.addAttribute("children", children);
        model.addAttribute("petCards", petCards);
        model.addAttribute("summary", data.get("dailySummary"));
        model.addAttribute("recentRecords", data.get("recentRecords"));
        model.addAttribute("date", date);
        return "dashboard";
    }
}
