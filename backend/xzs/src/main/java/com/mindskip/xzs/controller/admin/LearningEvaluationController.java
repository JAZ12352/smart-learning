package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.LearningPath;
import com.mindskip.xzs.domain.StudentProfile;
import com.mindskip.xzs.service.DeepSeekService;
import com.mindskip.xzs.service.LearningPathService;
import com.mindskip.xzs.service.StudentProfileService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@RestController
@RequestMapping("/api/admin/evaluation")
public class LearningEvaluationController {

    private static final Logger log = LoggerFactory.getLogger(LearningEvaluationController.class);

    private final LearningPathService learningPathService;
    private final StudentProfileService studentProfileService;
    private final DeepSeekService deepSeekService;

    public LearningEvaluationController(LearningPathService learningPathService,
                                        StudentProfileService studentProfileService,
                                        DeepSeekService deepSeekService) {
        this.learningPathService = learningPathService;
        this.studentProfileService = studentProfileService;
        this.deepSeekService = deepSeekService;
    }

    @GetMapping("/progress/{userId}")
    public RestResponse<Map<String, Object>> getProgress(@PathVariable String userId) {
        int uid = safeParseInt(userId);
        Map<String, Object> progress = new HashMap<>();

        StudentProfile profile = studentProfileService.getProfileByUserId(uid);
        List<LearningPath> paths = learningPathService.getPathsByUserId(uid);

        int totalPaths = paths.size();
        int completedPaths = 0;
        double totalProgress = 0.0;
        for (LearningPath path : paths) {
            if (path.getProgressPercent() != null) {
                totalProgress += path.getProgressPercent();
                if (path.getProgressPercent() >= 100.0) {
                    completedPaths++;
                }
            }
        }

        double averageProgress = totalPaths > 0 ? totalProgress / totalPaths : 0.0;

        progress.put("userId", userId);
        progress.put("totalPaths", totalPaths);
        progress.put("completedPaths", completedPaths);
        progress.put("averageProgress", Math.round(averageProgress * 100.0) / 100.0);
        progress.put("hasProfile", profile != null);
        if (profile != null) {
            progress.put("profileVersion", profile.getProfileVersion());
            progress.put("motivationLevel", profile.getMotivationLevel());
        }

        List<Map<String, Object>> pathDetails = new ArrayList<>();
        for (LearningPath path : paths) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", path.getId());
            detail.put("pathName", path.getPathName());
            detail.put("subjectId", path.getSubjectId());
            detail.put("currentNodeId", path.getCurrentNodeId());
            detail.put("progressPercent", path.getProgressPercent());
            detail.put("modifyTime", path.getModifyTime());
            pathDetails.add(detail);
        }
        progress.put("paths", pathDetails);

        return RestResponse.ok(progress);
    }

    @PostMapping("/report/{userId}")
    public void generateReport(@PathVariable String userId, HttpServletResponse response) throws IOException {
        int uid = safeParseInt(userId);
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        StudentProfile profile = studentProfileService.getProfileByUserId(uid);
        List<LearningPath> paths = learningPathService.getPathsByUserId(uid);

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("学生学习数据汇总：\n\n");

        if (profile != null) {
            contextBuilder.append("【学生画像】\n");
            contextBuilder.append("知识基础：").append(defaultText(profile.getKnowledgeLevel(), "未评估")).append("\n");
            contextBuilder.append("认知风格：").append(defaultText(profile.getCognitiveStyle(), "未评估")).append("\n");
            contextBuilder.append("薄弱点：").append(defaultText(profile.getWeakPoints(), "未评估")).append("\n");
            contextBuilder.append("学习目标：").append(defaultText(profile.getLearningGoals(), "未评估")).append("\n");
            contextBuilder.append("学习风格：").append(defaultText(profile.getLearningStyle(), "未评估")).append("\n");
            contextBuilder.append("动机水平：").append(profile.getMotivationLevel() == null ? "5" : profile.getMotivationLevel()).append("/10\n\n");
        }

        contextBuilder.append("【学习路径进度】\n");
        if (paths.isEmpty()) {
            contextBuilder.append("暂无学习路径数据。\n");
        } else {
            for (LearningPath path : paths) {
                contextBuilder.append("- ").append(defaultText(path.getPathName(), "未命名"))
                    .append("：进度 ").append(path.getProgressPercent() == null ? 0 : path.getProgressPercent())
                    .append("%\n");
            }
        }

        contextBuilder.append("\n学习效果评估报告：\n");
        contextBuilder.append("1. 学习进度总览\n");
        contextBuilder.append("2. 各学科掌握情况分析\n");
        contextBuilder.append("3. 优势与不足分析\n");
        contextBuilder.append("4. 个性化改进建议\n");
        contextBuilder.append("5. 下一阶段学习建议\n");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", "你是一位高校教学评估与学情分析专家。请基于学生的学习画像和课程学习路径进度数据，生成一份专业的学情评估报告。报告需包含：学习进度总览、各学科掌握情况分析、优势与不足诊断、个性化改进建议、下一阶段学习规划。使用Markdown格式，报告需体现高等教育质量保障理念和OBE（成果导向教育）评估框架。"));
        messages.add(message("user", contextBuilder.toString()));

        try (PrintWriter writer = response.getWriter()) {
            deepSeekService.stream(messages, chunk -> {
                writer.write(chunk);
                writer.flush();
            });
        } catch (Exception e) {
            log.error("评估报告生成流式响应失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(e.getMessage());
                writer.flush();
            }
        }
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt));
        return messages;
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    private String defaultText(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String val = String.valueOf(value).trim();
        return StringUtils.isBlank(val) ? defaultValue : val;
    }

    private int safeParseInt(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return 1; }
    }
}
