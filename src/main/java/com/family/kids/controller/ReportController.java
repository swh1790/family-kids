package com.family.kids.controller;

import com.family.kids.service.ChildService;
import com.family.kids.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private ReportService reportService;
    @Autowired
    private ChildService childService;

    @GetMapping
    public String report(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                         Model model) {
        if (end == null) {
            end = LocalDate.now();
        }
        if (start == null) {
            start = end.minusDays(6);
        }
        model.addAttribute("children", childService.listActive());
        model.addAttribute("weeklyPoints", reportService.weeklyPoints(start, end));
        model.addAttribute("categoryPoints", reportService.categoryPoints(start, end));
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        return "report";
    }
}
