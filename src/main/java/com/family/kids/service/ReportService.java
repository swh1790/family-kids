package com.family.kids.service;

import com.family.kids.entity.Child;
import com.family.kids.entity.PointRecord;
import com.family.kids.mapper.ChildMapper;
import com.family.kids.mapper.PointRecordMapper;
import com.family.kids.mapper.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private ChildMapper childMapper;
    @Autowired
    private PointRecordMapper pointRecordMapper;

    public List<Map<String, Object>> dailySummary(LocalDate date) {
        return reportMapper.dailySummary(date);
    }

    public List<Map<String, Object>> weeklyPoints(LocalDate startDate, LocalDate endDate) {
        return reportMapper.weeklyPoints(startDate, endDate);
    }

    public List<Map<String, Object>> categoryPoints(LocalDate startDate, LocalDate endDate) {
        return reportMapper.categoryPoints(startDate, endDate);
    }

    public List<PointRecord> recentRecords(int limit) {
        return pointRecordMapper.listRecent(limit);
    }

    public Map<String, Object> dashboard(LocalDate date) {
        Map<String, Object> data = new HashMap<>();
        List<Child> children = childMapper.listActive();
        data.put("children", children);
        data.put("dailySummary", dailySummary(date));
        data.put("recentRecords", recentRecords(10));
        return data;
    }
}
