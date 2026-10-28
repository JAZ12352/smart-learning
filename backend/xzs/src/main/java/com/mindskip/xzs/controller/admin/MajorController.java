package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/major")
public class MajorController {

    private final JdbcTemplate jdbc;

    public MajorController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public RestResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword) {
        String sql = "SELECT id, name, category, degree FROM t_major WHERE deleted = 0";
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql += " AND (name LIKE '%" + keyword + "%' OR category LIKE '%" + keyword + "%')";
        }
        sql += " ORDER BY category, name";
        List<Map<String, Object>> list = jdbc.queryForList(sql);
        return RestResponse.ok(list);
    }

    @GetMapping("/{id}/subjects")
    public RestResponse<List<Map<String, Object>>> getSubjects(@PathVariable Integer id) {
        String sql = "SELECT subject_name, is_required, semester FROM t_major_subject WHERE major_id = ? ORDER BY semester";
        List<Map<String, Object>> list = jdbc.queryForList(sql, id);
        return RestResponse.ok(list);
    }

    @PostMapping("/{id}/bind-subjects")
    public RestResponse bindSubjects(@PathVariable Integer id) {
        String majorSql = "SELECT name FROM t_major WHERE id = ?";
        Map<String, Object> major = jdbc.queryForMap(majorSql, id);
        String majorName = major.get("name").toString();

        String subSql = "SELECT subject_name, semester FROM t_major_subject WHERE major_id = ?";
        List<Map<String, Object>> subjects = jdbc.queryForList(subSql, id);

        int count = 0;
        for (Map<String, Object> s : subjects) {
            String name = s.get("subject_name").toString();
            int semester = Integer.parseInt(s.get("semester").toString());
            String levelName = "大学" + semester + "学期";

            int exists = jdbc.queryForObject("SELECT COUNT(*) FROM t_subject WHERE name = ? AND level_name = ?", Integer.class, name, levelName);
            if (exists == 0) {
                jdbc.update("INSERT INTO t_subject(name, level, level_name, deleted) VALUES (?, ?, ?, 0)", name, semester, levelName);
                count++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("major", majorName);
        result.put("created", count);
        result.put("total", subjects.size());
        return RestResponse.ok(result);
    }
}