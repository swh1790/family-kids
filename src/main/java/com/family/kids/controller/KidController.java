package com.family.kids.controller;

import com.family.kids.entity.LoginUser;
import com.family.kids.entity.PointRecord;
import com.family.kids.entity.TaskInstance;
import com.family.kids.mapper.HabitMapper;
import com.family.kids.mapper.PointRecordMapper;
import com.family.kids.service.ChildService;
import com.family.kids.service.PetService;
import com.family.kids.service.TaskService;
import com.family.kids.util.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/kid")
public class KidController {

    @Autowired
    private ChildService childService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private PetService petService;
    @Autowired
    private HabitMapper habitMapper;
    @Autowired
    private PointRecordMapper pointRecordMapper;

    @GetMapping("/{childId}")
    public String kidPage(@PathVariable Long childId, Model model, HttpServletRequest request) {
        LoginUser loginUser = AuthContext.current(request);
        if (loginUser == null) {
            return "redirect:/login";
        }
        // 孩子只能看自己的主页，改 URL 里的 id 一律打回自己的主页
        if (loginUser.isChild() && !Objects.equals(loginUser.getChildId(), childId)) {
            return "redirect:/kid/" + loginUser.getChildId();
        }

        model.addAttribute("child", childService.getById(childId));

        List<TaskInstance> tasks = taskService.listByDate(LocalDate.now(), childId);
        model.addAttribute("tasks", tasks);
        Map<String, List<TaskInstance>> grouped = groupByCategory(tasks);
        model.addAttribute("groupedTasks", grouped);
        model.addAttribute("pendingCounts", countPendingByGroup(grouped));

        // 扣分提醒：规则清单 + 最近被扣分记录（仅 DEDUCT 类型）
        model.addAttribute("habitItems", habitMapper.listActive());
        List<PointRecord> deductions = pointRecordMapper.listByChild(childId, 20).stream()
                .filter(r -> "DEDUCT".equals(r.getType()))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("recentDeductions", deductions);

        model.addAttribute("petCard", petService.getPetCard(childId));
        return "kid";
    }

    /**
     * 按任务分类分组，保持数据库 sort_order 顺序（校内→课外→常规学习→家庭活动→生活习惯）
     */
    private Map<String, List<TaskInstance>> groupByCategory(List<TaskInstance> tasks) {
        Map<String, List<TaskInstance>> grouped = new LinkedHashMap<>();
        for (TaskInstance t : tasks) {
            String key = t.getCategoryName() != null ? t.getCategoryName() : "其它";
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }
        return grouped;
    }

    /**
     * 统计每个分类下状态为 PENDING 的任务数量
     */
    private Map<String, Integer> countPendingByGroup(Map<String, List<TaskInstance>> grouped) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Map.Entry<String, List<TaskInstance>> entry : grouped.entrySet()) {
            long pending = entry.getValue().stream()
                    .filter(t -> "PENDING".equals(t.getStatus()))
                    .count();
            counts.put(entry.getKey(), (int) pending);
        }
        return counts;
    }
}
