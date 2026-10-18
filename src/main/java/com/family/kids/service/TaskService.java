package com.family.kids.service;

import com.family.kids.entity.Child;
import com.family.kids.entity.TaskInstance;
import com.family.kids.entity.TaskTemplate;
import com.family.kids.mapper.ChildMapper;
import com.family.kids.mapper.TaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.family.kids.util.Constants.*;

@Service
public class TaskService {

    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private ChildMapper childMapper;
    @Autowired
    private PointService pointService;

    @Value("${app.default-operator:家长}")
    private String defaultOperator;

    public List<TaskInstance> listByDate(LocalDate date, Long childId) {
        return taskMapper.listInstancesByDate(date, childId);
    }

    public TaskInstance getInstance(Long id) {
        return taskMapper.getInstanceById(id);
    }

    /**
     * 根据模板生成某天的任务实例
     */
    @Transactional
    public int generateDaily(LocalDate date) {
        List<Child> children = childMapper.listActive();
        List<TaskTemplate> templates = taskMapper.listTemplates();
        String weekday = String.valueOf(date.getDayOfWeek().getValue());
        int created = 0;

        for (Child child : children) {
            for (TaskTemplate tpl : templates) {
                // 专属任务只给对应孩子
                if (tpl.getChildId() != null && !tpl.getChildId().equals(child.getId())) {
                    continue;
                }
                // 一次性任务不自动重复生成
                if ("ONCE".equals(tpl.getFrequency())) {
                    continue;
                }
                // 周频次检查
                if ("WEEKLY".equals(tpl.getFrequency()) && tpl.getWeekdays() != null
                        && !tpl.getWeekdays().isBlank()) {
                    if (!Arrays.asList(tpl.getWeekdays().split(",")).contains(weekday)) {
                        continue;
                    }
                }

                TaskInstance ins = new TaskInstance();
                ins.setChildId(child.getId());
                ins.setTemplateId(tpl.getId());
                ins.setCategoryId(tpl.getCategoryId());
                ins.setTitle(tpl.getTitle());
                ins.setPoints(tpl.getDefaultPoints());
                ins.setPlanDate(date);
                ins.setStatus(TASK_PENDING);
                ins.setSource(SOURCE_AUTO);
                int rows = taskMapper.insertInstance(ins);
                created += rows;
            }
        }
        return created;
    }

    /**
     * 孩子提交任务
     */
    @Transactional
    public void submit(Long instanceId) {
        TaskInstance ins = taskMapper.getInstanceById(instanceId);
        if (ins == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (TASK_APPROVED.equals(ins.getStatus()) || TASK_REJECTED.equals(ins.getStatus())) {
            throw new IllegalStateException("该任务已处理，无法提交");
        }

        TaskTemplate tpl = ins.getTemplateId() != null ? taskMapper.getTemplateById(ins.getTemplateId()) : null;
        boolean needApproval = tpl == null || tpl.getRequireApproval() == null || tpl.getRequireApproval();

        if (needApproval) {
            ins.setStatus(TASK_DONE);
            ins.setSubmittedAt(LocalDateTime.now());
            taskMapper.updateInstanceStatus(ins);
        } else {
            // 无需审批，直接通过并加分
            approve(instanceId, defaultOperator);
        }
    }

    /**
     * 家长审批通过
     */
    @Transactional
    public void approve(Long instanceId, String operator) {
        TaskInstance ins = taskMapper.getInstanceById(instanceId);
        if (ins == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (TASK_APPROVED.equals(ins.getStatus())) {
            return;
        }
        ins.setStatus(TASK_APPROVED);
        ins.setApprovedAt(LocalDateTime.now());
        ins.setApprover(operator);
        taskMapper.updateInstanceStatus(ins);

        pointService.earnFromTask(ins.getChildId(), ins.getPoints(), ins.getId(),
                "完成任务：" + ins.getTitle(), operator);
    }

    /**
     * 家长驳回
     */
    @Transactional
    public void reject(Long instanceId, String remark, String operator) {
        TaskInstance ins = taskMapper.getInstanceById(instanceId);
        if (ins == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (TASK_APPROVED.equals(ins.getStatus())) {
            throw new IllegalStateException("已通过的任务请先撤销后再驳回");
        }
        ins.setStatus(TASK_REJECTED);
        ins.setApprovedAt(LocalDateTime.now());
        ins.setApprover(operator);
        ins.setRemark(remark);
        taskMapper.updateInstanceStatus(ins);
    }

    /**
     * 手动添加临时任务
     */
    @Transactional
    public void addManual(Long childId, Integer categoryId, String title, Integer points,
                          LocalDate planDate, String remark) {
        TaskInstance ins = new TaskInstance();
        ins.setChildId(childId);
        ins.setCategoryId(categoryId);
        ins.setTitle(title);
        ins.setPoints(points != null ? points : 5);
        ins.setPlanDate(planDate != null ? planDate : LocalDate.now());
        ins.setStatus(TASK_PENDING);
        ins.setSource(SOURCE_MANUAL);
        ins.setRemark(remark);
        taskMapper.insertInstance(ins);
    }

    @Transactional
    public void deleteInstance(Long id) {
        taskMapper.deleteInstance(id);
    }
}
