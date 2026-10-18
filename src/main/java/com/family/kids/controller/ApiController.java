package com.family.kids.controller;

import com.family.kids.entity.LoginUser;
import com.family.kids.entity.TaskInstance;
import com.family.kids.service.PetService;
import com.family.kids.service.TaskService;
import com.family.kids.util.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private TaskService taskService;
    @Autowired
    private PetService petService;

    @PostMapping("/tasks/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitTask(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        LoginUser loginUser = AuthContext.current(request);
        TaskInstance task = taskService.getInstance(id);
        if (task == null) {
            result.put("success", false);
            result.put("message", "任务不存在");
            return ResponseEntity.badRequest().body(result);
        }
        if (loginUser != null && loginUser.isChild()
                && !Objects.equals(task.getChildId(), loginUser.getChildId())) {
            result.put("success", false);
            result.put("message", "只能提交自己的任务哦～");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }

        taskService.submit(id);
        result.put("success", true);
        result.put("message", "提交成功");
        return ResponseEntity.ok(result);
    }
}
