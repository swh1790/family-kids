package com.family.kids.controller;

import com.family.kids.entity.LoginUser;
import com.family.kids.entity.TaskInstance;
import com.family.kids.entity.TaskTemplate;
import com.family.kids.service.ChildService;
import com.family.kids.service.TaskService;
import com.family.kids.mapper.TaskMapper;
import com.family.kids.mapper.TaskCategoryMapper;
import com.family.kids.util.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private ChildService childService;
    @Autowired
    private TaskCategoryMapper categoryMapper;

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(required = false) Long childId,
                       Model model) {
        if (date == null) {
            date = LocalDate.now();
        }
        List<TaskInstance> tasks = taskService.listByDate(date, childId);
        model.addAttribute("tasks", tasks);
        model.addAttribute("children", childService.listActive());
        model.addAttribute("categories", categoryMapper.listAll());
        model.addAttribute("date", date);
        model.addAttribute("selectedChildId", childId);
        return "tasks";
    }

    @PostMapping("/generate")
    public String generate(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           RedirectAttributes ra) {
        if (date == null) {
            date = LocalDate.now();
        }
        int created = taskService.generateDaily(date);
        ra.addFlashAttribute("msg", "已生成 " + created + " 个今日任务");
        return "redirect:/tasks?date=" + date;
    }

    @PostMapping("/add")
    public String addManual(@RequestParam Long childId,
                            @RequestParam Integer categoryId,
                            @RequestParam String title,
                            @RequestParam(defaultValue = "5") Integer points,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planDate,
                            @RequestParam(required = false) String remark,
                            RedirectAttributes ra) {
        if (planDate == null) {
            planDate = LocalDate.now();
        }
        taskService.addManual(childId, categoryId, title, points, planDate, remark);
        ra.addFlashAttribute("msg", "已添加临时任务");
        return "redirect:/tasks?date=" + planDate;
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id,
                         @RequestParam(required = false) String redirect,
                         HttpServletRequest request,
                         RedirectAttributes ra) {
        LoginUser loginUser = AuthContext.current(request);
        TaskInstance task = taskService.getInstance(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        // 孩子只允许提交自己的任务；父母代提交不受限
        if (loginUser != null && loginUser.isChild()
                && !Objects.equals(task.getChildId(), loginUser.getChildId())) {
            throw new IllegalArgumentException("只能提交自己的任务哦～");
        }

        taskService.submit(id);
        ra.addFlashAttribute("msg", "任务已提交，等待家长确认");
        TaskInstance ins = taskService.getInstance(id);
        if ("kid".equals(redirect)) {
            return "redirect:/kid/" + ins.getChildId();
        }
        return "redirect:/tasks?date=" + ins.getPlanDate();
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          RedirectAttributes ra) {
        taskService.approve(id, "家长");
        ra.addFlashAttribute("msg", "任务已通过，积分已发放");
        TaskInstance ins = taskService.getInstance(id);
        return "redirect:/tasks?date=" + (date != null ? date : ins.getPlanDate());
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String remark,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         RedirectAttributes ra) {
        taskService.reject(id, remark, "家长");
        ra.addFlashAttribute("msg", "任务已驳回");
        TaskInstance ins = taskService.getInstance(id);
        return "redirect:/tasks?date=" + (date != null ? date : ins.getPlanDate());
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         RedirectAttributes ra) {
        TaskInstance ins = taskService.getInstance(id);
        taskService.deleteInstance(id);
        ra.addFlashAttribute("msg", "任务已删除");
        return "redirect:/tasks?date=" + (date != null ? date : ins.getPlanDate());
    }

    // 任务模板管理
    @GetMapping("/templates")
    public String templates(Model model) {
        model.addAttribute("templates", taskMapper.listTemplates());
        model.addAttribute("children", childService.listActive());
        model.addAttribute("categories", categoryMapper.listAll());
        model.addAttribute("template", new TaskTemplate());
        return "templates";
    }

    @PostMapping("/templates/save")
    public String saveTemplate(TaskTemplate template, RedirectAttributes ra) {
        if (template.getChildId() == null || template.getChildId() <= 0) {
            template.setChildId(null);
        }
        if (template.getRequireApproval() == null) {
            template.setRequireApproval(true);
        }
        if (template.getActive() == null) {
            template.setActive(true);
        }
        if (template.getId() == null) {
            taskMapper.insertTemplate(template);
        } else {
            taskMapper.updateTemplate(template);
        }
        ra.addFlashAttribute("msg", "模板已保存");
        return "redirect:/tasks/templates";
    }

    @PostMapping("/templates/{id}/delete")
    public String deleteTemplate(@PathVariable Long id, RedirectAttributes ra) {
        taskMapper.deleteTemplate(id);
        ra.addFlashAttribute("msg", "模板已删除");
        return "redirect:/tasks/templates";
    }
}
